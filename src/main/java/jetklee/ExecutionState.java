package jetklee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses and holds information about execution state of the process tree
 */
public class ExecutionState {
    public static final int BYTES_INDENT = 12;
    public static final int OBJS_INDENT = 4;
    public int nodeID;

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

    public record Diff<T>(ArrayList<T> additions, ArrayList<T> deletions, int indent) {
        @Override
        public String toString() {
            if (additions.isEmpty() && deletions.isEmpty()) return "";

            StringBuilder addStr = new StringBuilder();
            for (T addition : additions) {
                addStr.append(" ".repeat(indent + 4));
                addStr.append(addition.toString()).append("\n");
            }
            StringBuilder delStr = new StringBuilder();
            for (T deletion : deletions) {
                delStr.append(" ".repeat(indent + 4));
                delStr.append(deletion.toString()).append("\n");
            }

            return String.format(
                    " ".repeat(indent) + "add: \n%s" +
                            " ".repeat(indent) + "del: \n%s ",
                    addStr, delStr
            );
        }
    }

    public record Object(int objID, int segment, String name, String size, boolean isLocal, boolean isGlobal,
                         boolean isFixed, boolean isUserSpec, boolean isLazy, String symAddress) {
        @Override
        public String toString() {
            return String.format(
                    "objID: %d, " +
                            "segment: %d, " +
                            "name: %s, " +
                            "size: %s, " +
                            "isLocal: %s, " +
                            "isGlobal: %s, " +
                            "isFixed: %s, " +
                            "isUserSpec: %s, " +
                            "isLazy: %s, " +
                            "symAddress: %s",
                    objID, segment, name, size, isLocal, isGlobal, isFixed, isUserSpec, isLazy, symAddress
            );
        }
    }

    public record ByteGroup (boolean concrete, boolean knownSym, boolean unflushed, String value, ArrayList<Integer> indices) {
        @Override
        public String toString() {
            return String.format(
                            "concrete: %s, " +
                            "knownSym: %s, " +
                            "unflushed: %s, " +
                            "value: %s, " +
                            "indices: %s",
                    concrete, knownSym, unflushed, value, indices.toString()
            );
        }
    }

    public record Plane(Type type, int memoryObjectID, String rootObject, int sizeBound,
                        boolean initialized, boolean symbolic, int initialValue,
                        Diff<ByteGroup> bytes, LinkedHashMap<Integer, String> updates) {
        public enum Type {
            SEGMENT, OFFSET;

            public static String toString(Type type) {
                return switch (type) {
                    case SEGMENT -> "segmentPlane";
                    case OFFSET -> "offsetPlane";
                };
            }
        }

        @Override
        public String toString() {
            StringBuilder updatesStr = new StringBuilder();
            for (Map.Entry<Integer, String> update : updates.entrySet()) {
                updatesStr.append(update.getKey().toString());
                updatesStr.append(" : ");
                updatesStr.append(update.getValue());
                updatesStr.append("\n");
            }

            return String.format(
                    "memoryObjectID: %d, " +
                            "rootObject: %s, " +
                            "sizeBound: %d, " +
                            "initialized: %s, " +
                            "symbolic: %s, " +
                            "initialValue: %d, " +
                            "\n        bytes:\n%s" +
                            "\n        updates:\n%s",
                    memoryObjectID, rootObject, sizeBound, initialized, symbolic, initialValue, bytes.toString(), updatesStr
            );
        }
    }

    public record ObjectState(int objID, int copyOnWriteOwner, boolean readOnly, Plane segmentPlane,
                              Plane offsetPlane) {
        @Override
        public String toString() {
            return String.format(
                    "    objID: %d, " +
                            "copyOnWriteOwner: %d, " +
                            "readOnly: %s\n" +
                            "    segmentPlane: %s\n" +
                            "    offsetPlane: %s",
                    objID, copyOnWriteOwner, readOnly, segmentPlane == null ? "" : segmentPlane.toString(),
                    offsetPlane == null ? "" : offsetPlane.toString()
            );
        }
    }

    public Context context;
    public ArrayList<String> constraints;
    public Diff<Object> objectsDiff;
    public ArrayList<ObjectState> objectStates;

    /**
     * @param data information about one execution state
     */
    public ExecutionState(JSONObject data) {
        context = parseContext(data);
        constraints = parseConstraints(data);
        objectsDiff = parseObjects(data);
        objectStates = parseObjectStates(data);
    }

    private ArrayList<ObjectState> parseObjectStates(JSONObject data) {
        objectStates = new ArrayList<>();
        if (!data.has("objectStates")) return objectStates;

        JSONArray objectStatesJSON = data.getJSONArray("objectStates");
        for (int i = 0; i < objectStatesJSON.length(); i++) {
            JSONObject objectStateJSON = objectStatesJSON.getJSONObject(i);

            ObjectState objectState = new ObjectState(
                    objectStateJSON.getInt("objID"),
                    objectStateJSON.getInt("copyOnWriteOwner"),
                    objectStateJSON.getInt("readOnly") == 1,
                    parsePlane(objectStateJSON, Plane.Type.SEGMENT),
                    parsePlane(objectStateJSON, Plane.Type.OFFSET)
            );
            objectStates.add(objectState);
        }
        return objectStates;
    }

    private ArrayList<Object> parseObject(JSONArray objectsJSON) {
        ArrayList<Object> objects = new ArrayList<>();
        for (int i = 0; i < objectsJSON.length(); i++) {
            JSONObject objectJSON = objectsJSON.getJSONObject(i);
            Object object = new Object(
                    objectJSON.getInt("objID"),
                    objectJSON.getInt("segment"),
                    objectJSON.getString("name"),
                    objectJSON.getString("size"),
                    objectJSON.getInt("isLocal") == 1,
                    objectJSON.getInt("isGlobal") == 1,
                    objectJSON.getInt("isFixed") == 1,
                    objectJSON.getInt("isUserSpec") == 1,
                    objectJSON.getInt("isLazy") == 1,
                    objectJSON.getString("symAddress")
            );
            objects.add(object);
        }
        return objects;
    }

    private Diff<Object> parseObjects(JSONObject data) {
        ArrayList<Object> additions = new ArrayList<>();
        ArrayList<Object> deletions = new ArrayList<>();

        if (data.has("objects")) {
            JSONObject objectsJSON = data.getJSONObject("objects");
            if (objectsJSON.has("add")) {
                JSONArray additionsJSON = objectsJSON.getJSONArray("add");
                additions = parseObject(additionsJSON);
            }
            if (objectsJSON.has("del")) {
                JSONArray additionsJSON = objectsJSON.getJSONArray("del");
                deletions = parseObject(additionsJSON);
            }
        }

        return new Diff<>(additions, deletions, OBJS_INDENT);
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
        nodeID = data.getInt("nodeID");
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

    private ArrayList<ByteGroup> parseByte(JSONArray bytesJSON) {
        ArrayList<ByteGroup> bytes = new ArrayList<>();
        for (int i = 0; i < bytesJSON.length(); i++) {
            JSONArray byteJSON = bytesJSON.getJSONArray(i);

            ArrayList<Integer> indices = new ArrayList<>();
            JSONArray indicesJSON = byteJSON.getJSONArray(3);

            // skip first element (value)
            for (int j = 1; j < indicesJSON.length(); ++j) {
                indices.add(indicesJSON.getInt(j));
            }

            ByteGroup b = new ByteGroup(
                    byteJSON.getInt(0) == 1, // concrete
                    byteJSON.getInt(1) == 1, // knownSym
                    byteJSON.getInt(2) == 1, // unflushed
                    byteJSON.getJSONArray(3).getString(0), //value
                    indices
            );
            bytes.add(b);
        }
        return bytes;
    }

    private Diff<ByteGroup> parseBytes(JSONObject bytes) {
        ArrayList<ByteGroup> additions = new ArrayList<>();
        ArrayList<ByteGroup> deletions = new ArrayList<>();

        if (bytes.has("add")) {
            JSONArray additionsJSON = bytes.getJSONArray("add");
            additions = parseByte(additionsJSON);
        }
        if (bytes.has("del")) {
            JSONArray additionsJSON = bytes.getJSONArray("del");
            deletions = parseByte(additionsJSON);
        }
        return new Diff<>(additions, deletions, BYTES_INDENT);
    }

    private Plane parsePlane(JSONObject data, Plane.Type type) {
        JSONObject planeJSON = data.getJSONObject(Plane.Type.toString(type));
        if (planeJSON.isEmpty()) return null;


        Diff<ByteGroup> bytes = new Diff<>(new ArrayList<>(), new ArrayList<>(), BYTES_INDENT);

        if (planeJSON.has("bytes")) {
            JSONObject bytesJSON = planeJSON.getJSONObject("bytes");
            bytes = parseBytes(bytesJSON);
        }

        LinkedHashMap<Integer, String> updates = new LinkedHashMap<>();
        if (planeJSON.has("updates")) {
            JSONArray updatesJSON = planeJSON.getJSONArray("updates");
            for (int i = 0; i < updatesJSON.length(); ++i) {
                JSONObject updateJSON = updatesJSON.getJSONObject(i);
                String key = updateJSON.keys().next();
                String value = updateJSON.getString(key);
                updates.put(Integer.parseInt(key), value);
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

    public void getCompleteMemory() {

    }
}
