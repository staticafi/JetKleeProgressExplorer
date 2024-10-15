package jetklee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Parses and holds information about execution state of the process tree
 */
public class ExecutionState {
    public record Location(String file, int line, int column, int assemblyLine) {
        @Override
        public String toString() {
            return String.format(
                    "\n    file: %s\n" +
                            "    line: %d\n" +
                            "    column: %d\n" +
                            "    assemblyLine: %d",
                    file, line, column, assemblyLine
            );
        }
    }

    public record Context(int nodeID, int stateID, boolean uniqueState, int parentID, int parentJSON, Location location,
                          Location nextLocation, int incomingBBIndex, int depth,
                          boolean coveredNew,
                          boolean forkDisabled, int instsSinceCovNew, int nextID, int steppedInstructions,
                          ArrayList<Location> stack) {
        @Override
        public String toString() {
            StringBuilder stackStr = new StringBuilder();
            for (Location stackLocation : stack) {
                stackStr.append(stackLocation.toString());
                stackStr.append("\n");
            }
            return String.format(
                    "nodeID: %d\n" +
                            "stateID: %d\n" +
                            "uniqueState: %s\n" +
                            "parentID: %d\n" +
                            "parentJSON: %d\n" +
                            "location: %s\n" +
                            "nextLocation: %s\n" +
                            "incomingBBIndex: %d\n" +
                            "depth: %d\n" +
                            "coveredNew: %s\n" +
                            "forkDisabled: %s\n" +
                            "instsSinceCovNew: %d\n" +
                            "nextID: %d\n" +
                            "steppedInstructions: %d\n" +
                            "stack: %s",
                    nodeID, stateID, uniqueState, parentID, parentJSON, location.toString(), nextLocation.toString(),
                    incomingBBIndex, depth, coveredNew, forkDisabled, instsSinceCovNew, nextID, steppedInstructions, stackStr
            );
        }
    }

    public record Diff<T>(ArrayList<T> additions, ArrayList<T> deletions) {
    }

    public enum OperationType {
        ADDITION, DELETION, CHANGE;
    }

    public record ObjectState(int objID, OperationType type, int segment, String name, String size, boolean isLocal,
                              boolean isGlobal,
                              boolean isFixed, boolean isUserSpec, boolean isLazy, String symAddress,
                              int copyOnWriteOwner, boolean readOnly, Plane segmentPlane, Plane offsetPlane) {
    }

    public record ByteGroup(boolean concrete, boolean knownSym, boolean unflushed, String value,
                            ArrayList<Integer> indices, boolean isAddition) {

    }

    public record Plane(PlaneType type, int memoryObjectID, String rootObject, int sizeBound,
                        boolean initialized, boolean symbolic, int initialValue,
                        Diff<ByteGroup> bytes, Updates updates) {
        public enum PlaneType {
            SEGMENT, OFFSET;

            public boolean isSegment(PlaneType type) {
                return type == SEGMENT;
            }

            public static String toString(PlaneType type) {
                return switch (type) {
                    case SEGMENT -> "segmentPlane";
                    case OFFSET -> "offsetPlane";
                };
            }
        }
    }

    public record Memory(ArrayList<ObjectState> additions, ArrayList<ObjectState> changes,
                         ArrayList<Deletion> deletions) {
    }

    public class Updates extends LinkedHashMap<String, String> {
    }

    public record Deletion(int objID, OperationType type) {
    }

    public Context context;
    public ArrayList<String> constraints;

    public Memory memory;
    public Node node;

    /**
     * @param data information about one execution state
     */
    public ExecutionState(JSONObject data, Node node_) {
        context = parseContext(data);
        constraints = parseConstraints(data);
        memory = parseMemory(data);
        node = node_;
    }

    private Memory parseMemory(JSONObject data) {
        ArrayList<ObjectState> additions = new ArrayList<>();
        ArrayList<ObjectState> changes = new ArrayList<>();
        ArrayList<Deletion> deletions = new ArrayList<>();

        if (!data.has("objects")) return new Memory(additions, changes, deletions);

        JSONObject objectsJSON = data.getJSONObject("objects");
        if (objectsJSON.has("added")) {
            JSONArray additionsJSON = objectsJSON.getJSONArray("added");
            additions = parseObjectStates(additionsJSON, OperationType.ADDITION);
        }
        if (objectsJSON.has("changed")) {
            JSONArray changesJSON = objectsJSON.getJSONArray("changed");
            changes = parseObjectStates(changesJSON, OperationType.CHANGE);
        }
        if (objectsJSON.has("deleted")) {
            JSONArray deletionsJSON = objectsJSON.getJSONArray("deleted");
            for (int i = 0; i < deletionsJSON.length(); ++i) {
                deletions.add(new Deletion(deletionsJSON.getInt(i), OperationType.DELETION));
            }
        }

        return new Memory(additions, changes, deletions);
    }

    private ArrayList<ObjectState> parseObjectStates(JSONArray objectStatesJSON, OperationType type) {
        ArrayList<ObjectState> objectStates = new ArrayList<>();

        for (int i = 0; i < objectStatesJSON.length(); i++) {
            JSONObject objectStateJSON = objectStatesJSON.getJSONObject(i);

            ObjectState objectState = new ObjectState(
                    objectStateJSON.getInt("objID"),
                    type,
                    objectStateJSON.getInt("segment"),
                    objectStateJSON.getString("name"),
                    objectStateJSON.getString("size"),
                    objectStateJSON.getInt("isLocal") == 1,
                    objectStateJSON.getInt("isGlobal") == 1,
                    objectStateJSON.getInt("isFixed") == 1,
                    objectStateJSON.getInt("isUserSpec") == 1,
                    objectStateJSON.getInt("isLazy") == 1,
                    objectStateJSON.getString("symAddress"),
                    objectStateJSON.getInt("copyOnWriteOwner"),
                    objectStateJSON.getInt("readOnly") == 1,
                    parsePlane(objectStateJSON, Plane.PlaneType.SEGMENT),
                    parsePlane(objectStateJSON, Plane.PlaneType.OFFSET)
            );
            objectStates.add(objectState);
        }
        return objectStates;
    }

    private ArrayList<ByteGroup> parseByte(JSONArray bytesJSON, boolean isAddition) {
        ArrayList<ByteGroup> bytes = new ArrayList<>();

        for (int i = 0; i < bytesJSON.length(); ++i) {
            JSONArray byteJSON = bytesJSON.getJSONArray(i);

            // skip first 3 elements (concrete, knownSym, unflushed) in byte array: [concrete, knownSym, unflushed, [value, index, index, ..., index]]
            for (int j = 3; j < byteJSON.length(); ++j) {
                ArrayList<Integer> indices = new ArrayList<>();
                JSONArray indicesJSON = byteJSON.getJSONArray(j);

                // skip first element (value) in index array: [value, index, index, ..., index]
                for (int k = 1; k < indicesJSON.length(); ++k) {
                    indices.add(indicesJSON.getInt(k));
                }

                ByteGroup b = new ByteGroup(
                        byteJSON.getInt(0) == 1, // concrete
                        byteJSON.getInt(1) == 1, // knownSym
                        byteJSON.getInt(2) == 1, // unflushed
                        indicesJSON.getString(0), //value
                        indices,
                        isAddition
                );
                bytes.add(b);
            }
        }
        return bytes;
    }

    private Diff<ByteGroup> parseBytes(JSONObject bytes) {
        ArrayList<ByteGroup> additions = new ArrayList<>();
        ArrayList<ByteGroup> deletions = new ArrayList<>();

        // TODO refactor to "additions, deletions" instead of "add, del"
        if (bytes.has("add")) {
            JSONArray additionsJSON = bytes.getJSONArray("add");
            additions = parseByte(additionsJSON, true);
        }
        if (bytes.has("del")) {
            JSONArray additionsJSON = bytes.getJSONArray("del");
            deletions = parseByte(additionsJSON, false);
        }
        return new Diff<>(additions, deletions);
    }

    private Plane parsePlane(JSONObject data, Plane.PlaneType type) {
        JSONObject planeJSON = data.getJSONObject(Plane.PlaneType.toString(type));
        if (planeJSON.isEmpty()) return null;


        Diff<ByteGroup> bytes = new Diff<>(new ArrayList<>(), new ArrayList<>());

        if (planeJSON.has("bytes")) {
            JSONObject bytesJSON = planeJSON.getJSONObject("bytes");
            bytes = parseBytes(bytesJSON);
        }

        Updates updates = new Updates();
        if (planeJSON.has("updates")) {
            JSONArray updatesJSON = planeJSON.getJSONArray("updates");
            for (int i = 0; i < updatesJSON.length(); ++i) {
                JSONObject updateJSON = updatesJSON.getJSONObject(i);
                String key = updateJSON.keys().next();
                String value = updateJSON.getString(key);
                updates.put(key, value);
            }
        }

        return new Plane(
                type,
                planeJSON.getInt("memoryObjectID"),
                planeJSON.getString("rootObject"),
                planeJSON.getInt("sizeBound"),
                planeJSON.getInt("initialized") == 1,
                planeJSON.getInt("symbolic") == 1,
                planeJSON.getInt("initialValue"),
                bytes,
                updates
        );
    }

    private Location getLocation(JSONObject data, String location) {
        JSONArray locationJSON = data.getJSONArray(location);
        return new Location(
                locationJSON.getString(0),
                locationJSON.getInt(1),
                locationJSON.getInt(2),
                locationJSON.getInt(3)
        );
    }

    private Context parseContext(JSONObject data) {
        Location location = getLocation(data, "location");
        Location nextLocation = getLocation(data, "nextLocation");

        JSONArray stackJSON = data.getJSONArray("stack");
        ArrayList<Location> stack = new ArrayList<>();
        for (int i = 0; i < stackJSON.length(); i++) {
            JSONArray stackLocationJSON = stackJSON.getJSONArray(i);
            Location stackLocation = new Location(
                    stackLocationJSON.getString(0),
                    stackLocationJSON.getInt(1),
                    stackLocationJSON.getInt(2),
                    stackLocationJSON.getInt(3)
            );
            stack.add(stackLocation);
        }


        return new Context(
                data.getInt("nodeID"),
                data.getInt("stateID"),
                data.getInt("uniqueState") == 1,
                data.getInt("parentID"),
                data.getInt("parentJSON"),
                location,
                nextLocation,
                data.getInt("incomingBBIndex"),
                data.getInt("depth"),
                data.getInt("coveredNew") == 1,
                data.getInt("forkDisabled") == 1,
                data.getInt("instsSinceCovNew"),
                data.getInt("nextID"),
                data.getInt("steppedInstructions"),
                stack
        );
    }

    private ArrayList<String> parseConstraints(JSONObject data) {
        constraints = new ArrayList<>();
        JSONArray constraintsJSON = data.getJSONArray("constraints");
        for (int i = 0; i < constraintsJSON.length(); i++) {
            constraints.add(constraintsJSON.get(i).toString());
        }
        return constraints;
    }
}
