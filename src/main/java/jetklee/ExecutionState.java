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

    public record Context(int nodeID, int stateID, boolean uniqueState, Location location, int depth,
                          boolean coveredNew,
                          boolean forkDisabled, ArrayList<Location> stack) {
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
                            "location: %s\n" +
                            "depth: %d\n" +
                            "coveredNew: %s\n" +
                            "forkDisabled: %s\n" +
                            "stack: %s",
                    nodeID, stateID, uniqueState, location.toString(), depth, coveredNew, forkDisabled, stackStr
            );
        }
    }

    public record Object(int objID, int segment, String name, String size, boolean isLocal, boolean isGlobal,
                         boolean isFixed, boolean isLazy, String symAddress) {
        @Override
        public String toString() {
            return String.format(
                    "    objID: %d, " +
                            "segment: %d, " +
                            "name: %s, " +
                            "size: %s, " +
                            "isLocal: %s, " +
                            "isGlobal: %s, " +
                            "isFixed: %s, " +
                            "isLazy: %s, " +
                            "symAddress: %s",
                    objID, segment, name, size, isLocal, isGlobal, isFixed, isLazy, symAddress
            );
        }
    }

    public record Byte(boolean concrete, boolean knownSym, boolean unflushed, String value) {
        @Override
        public String toString() {
            return String.format(
                    "concrete: %s, " +
                            "knownSym: %s, " +
                            "unflushed: %s, " +
                            "value: %s",
                    concrete, knownSym, unflushed, value
            );
        }
    }

    public record Plane(Type type, int memoryObjectID, String rootObject, int sizeBound,
                        boolean initialized, boolean symbolic, int initialValue,
                        ArrayList<Byte> bytes, LinkedHashMap<Integer, String> updates) {
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
            StringBuilder bytesStr = new StringBuilder();
            for (Byte byte_ : bytes) {
                bytesStr.append("            ");
                bytesStr.append(byte_.toString()).append("\n");
            }
            StringBuilder updatesStr = new StringBuilder();
            for (Map.Entry<Integer, String> update : updates.entrySet()) {
                bytesStr.append("            ");
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
                    memoryObjectID, rootObject, sizeBound, initialized, symbolic, initialValue, bytesStr, updatesStr
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
    public ArrayList<Object> objects;
    public ArrayList<ObjectState> objectStates;

    /**
     * @param data information about one execution state
     */
    public ExecutionState(JSONObject data) {
        context = parseContext(data);
        constraints = parseConstraints(data);
        objects = parseObjects(data);
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

    private ArrayList<Object> parseObjects(JSONObject data) {
        objects = new ArrayList<>();
        if (!data.has("objects")) return objects;

        JSONArray objectsJSON = data.getJSONArray("objects");
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
                    objectJSON.getInt("isLazy") == 1,
                    objectJSON.getString("symAddress")
            );
            objects.add(object);
        }
        return objects;
    }

    private Context parseContext(JSONObject data) {
        JSONArray locationJSON = data.getJSONArray("location");
        Location location = new Location(
                locationJSON.getString(0),
                locationJSON.getInt(1),
                locationJSON.getInt(2),
                locationJSON.getInt(3)
        );

        JSONArray stackJSON = data.getJSONArray("stack");
        ArrayList<Location> stack = new ArrayList<>();
        for (int i = 0; i < stackJSON.length(); i++) {
            Location stackLocation = new Location(
                    stackJSON.getString(0),
                    stackJSON.getInt(1),
                    stackJSON.getInt(2),
                    stackJSON.getInt(3)
            );
            stack.add(stackLocation);
        }

        return new Context(
                data.getInt("nodeID"),
                data.getInt("stateID"),
                data.getInt("uniqueState") == 1,
                location,
                data.getInt("depth"),
                data.getInt("coveredNew") == 1,
                data.getInt("forkDisabled") == 1,
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

    private Plane parsePlane(JSONObject data, Plane.Type type) {
        JSONObject planeJSON = data.getJSONObject(Plane.Type.toString(type));
        if (planeJSON.isEmpty()) return null;

        JSONArray bytesJSON = planeJSON.getJSONArray("bytes");
        ArrayList<Byte> bytes = new ArrayList<>();
        for (int i = 0; i < bytesJSON.length(); ++i) {
            JSONObject byteJSON = bytesJSON.getJSONObject(i);
            bytes.add(new Byte(
                            byteJSON.getInt("concrete") == 1,
                            byteJSON.getInt("knownSym") == 1,
                            byteJSON.getInt("unflushed") == 1,
                            byteJSON.getString("value")
                    )
            );
        }

        JSONArray updatesJSON = planeJSON.getJSONArray("updates");
        LinkedHashMap<Integer, String> updates = new LinkedHashMap<>();
        for (int i = 0; i < updatesJSON.length(); ++i) {
            JSONObject updateJSON = updatesJSON.getJSONObject(i);
            String key = updateJSON.keys().next();
            String value = updateJSON.getString(key);
            updates.put(Integer.parseInt(key), value);
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
}
