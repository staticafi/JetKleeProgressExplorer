package jetklee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses and holds information about execution state of the process tree
 */
public class ExecutionState {
    public record Diff(ByteMap additions, ByteMap deletions) {
    }

    public enum OperationType {
        ADDITION, DELETION, CHANGE;
    }

    public record ObjectState(int objID, OperationType type, int segment, String name, String size, boolean isLocal,
                              boolean isFixed, boolean isUserSpec, boolean isLazy, int copyOnWriteOwner,
                              boolean readOnly, AllocSite allocSite, Plane segmentPlane, Plane offsetPlane) {
    }

    public static class ByteMap extends HashMap<String, ArrayList<Integer>> {
    }

    public record AllocSite(String scope, String name, String code) {
    }

    public record Plane(PlaneType type, int memoryObjectID, String rootObject, int sizeBound,
                        boolean initialized, boolean symbolic, int initialValue,
                        Diff concreteStore, Diff concreteMask, Diff knownSymbolics,
                        Updates updates) {
        public enum PlaneType {
            SEGMENT, OFFSET;

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

    public record Deletion(int objID, OperationType type) {
    }

    public static class Updates extends ArrayList<Map.Entry<String, String>> {
    }

    public record Location(String file, int line, int column, int assemblyLine) {
    }

    public record Context(InsertContext insertContext, int parentID, int parentJSON, Location lastLocation,
                          boolean coveredNew, boolean forkDisabled, int instsSinceCovNew, int steppedInstructions) {
    }

    public record InsertContext(int nodeID, int stateID, boolean uniqueState, Location firstLocation, int depth,
                                ArrayList<Location> stack){}

    private Context context;
    private InsertContext insertContext;
    private Memory memory;
    private ArrayList<String> constraints;

    private int id;

    /**
     * @param treeData information about one execution state
     */
    public ExecutionState(JSONObject treeData, int id) {
        this.id = id;
        this.insertContext = parseContextInsert(treeData);
    }

    public void setNodeInfoData(JSONObject nodeInfoData, int parentID) {
        this.memory = parseMemory(nodeInfoData);
        this.constraints = parseConstraints(nodeInfoData);
        this.context = parseContext(nodeInfoData, parentID);
    }

    private Context parseContext(JSONObject memoryData, int parentID) {
        Location lastLocation = parseLocation(memoryData, "lastLocation");
        return new Context(
                insertContext,
                parentID,
                memoryData.getInt("parentIter"),
                lastLocation,
                memoryData.getInt("coveredNew") == 1,
                memoryData.getInt("forkDisabled") == 1,
                memoryData.getInt("instsSinceCovNew"),
                memoryData.getInt("steppedInstructions")
        );
    }

    private InsertContext parseContextInsert(JSONObject treeData) {
        Location firstLocation = parseLocation(treeData, "firstLocation");
        JSONArray stackJSON = treeData.getJSONArray("stack");
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

        return new InsertContext(
                treeData.getInt("nodeID"),
                treeData.getInt("stateID"),
                treeData.getInt("uniqueState") == 1,
                firstLocation,
                treeData.getInt("depth"),
                stack
        );
    }

    private Location parseLocation(JSONObject data, String location) {
        JSONArray locationJSON = data.getJSONArray(location);
        return new Location(
                locationJSON.getString(0),
                locationJSON.getInt(1),
                locationJSON.getInt(2),
                locationJSON.getInt(3)
        );
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

            AllocSite allocSite = null;
            if (objectStateJSON.has("allocSite")) {
                JSONObject allocSiteJSON = objectStateJSON.getJSONObject("allocSite");
                
                allocSite = new AllocSite(
                        allocSiteJSON.has("scope") ? allocSiteJSON.getString("scope") : "",
                        allocSiteJSON.has("name") ? allocSiteJSON.getString("name") : "",
                        allocSiteJSON.has("code") ? allocSiteJSON.getString("code") : ""
                );
            }

            int objID = objectStateJSON.getInt("objID");
            ObjectState objectState = new ObjectState(
                    objID,
                    type,
                    objectStateJSON.getInt("segment"),
                    objectStateJSON.getString("name"),
                    objectStateJSON.getString("size"),
                    objectStateJSON.getInt("isLocal") == 1,
                    objectStateJSON.getInt("isFixed") == 1,
                    objectStateJSON.getInt("isUserSpec") == 1,
                    objectStateJSON.getInt("isLazy") == 1,
                    objectStateJSON.getInt("copyOnWriteOwner"),
                    objectStateJSON.getInt("readOnly") == 1,
                    allocSite,
                    parsePlane(objectStateJSON, Plane.PlaneType.SEGMENT, objID),
                    parsePlane(objectStateJSON, Plane.PlaneType.OFFSET, objID)
            );
            objectStates.add(objectState);
        }
        return objectStates;
    }

    private ByteMap parseByte(JSONArray bytesJSON, boolean isAddition) {
        ByteMap byteMap = new ByteMap();

        for (int i = 0; i < bytesJSON.length(); ++i) {
            JSONObject byteObject = bytesJSON.getJSONObject(i);
            String value = byteObject.keys().next();
            JSONArray indicesJSON = byteObject.getJSONArray(value);
            ArrayList<Integer> indices = new ArrayList<>();

            for (int j = 0; j < indicesJSON.length(); ++j) {
                indices.add(indicesJSON.getInt(j));
            }

            byteMap.put(value, indices);
        }
        return byteMap;
    }

    private Diff parseBytes(JSONObject bytes) {
        ByteMap additions = new ByteMap();
        ByteMap deletions = new ByteMap();

        if (bytes.has("add")) {
            JSONArray additionsJSON = bytes.getJSONArray("add");
            additions = parseByte(additionsJSON, true);
        }
        if (bytes.has("del")) {
            JSONArray additionsJSON = bytes.getJSONArray("del");
            deletions = parseByte(additionsJSON, false);
        }
        return new Diff(additions, deletions);
    }

    private Plane parsePlane(JSONObject data, Plane.PlaneType type, int objID) {
        JSONObject planeJSON = data.getJSONObject(Plane.PlaneType.toString(type));
        if (planeJSON.isEmpty()) return null;


        Diff concreteStore = new Diff(new ByteMap(), new ByteMap());
        Diff concreteMask = new Diff(new ByteMap(), new ByteMap());
        Diff knownSymbolics = new Diff(new ByteMap(), new ByteMap());

        if (planeJSON.has("concreteStore")) {
            JSONObject bytesJSON = planeJSON.getJSONObject("concreteStore");
            concreteStore = parseBytes(bytesJSON);
        }

        if (planeJSON.has("concreteMask")) {
            JSONObject bytesJSON = planeJSON.getJSONObject("concreteMask");
            concreteMask = parseBytes(bytesJSON);
        }

        if (planeJSON.has("knownSymbolics")) {
            JSONObject bytesJSON = planeJSON.getJSONObject("knownSymbolics");
            knownSymbolics = parseBytes(bytesJSON);
        }

        Updates updates = new Updates();
        if (planeJSON.has("updates")) {
            JSONArray updatesJSON = planeJSON.getJSONArray("updates");
            for (int i = 0; i < updatesJSON.length(); ++i) {
                JSONObject updateJSON = updatesJSON.getJSONObject(i);
                String key = updateJSON.keys().next();
                String value = updateJSON.getString(key);
                updates.add(Map.entry(key, value));
            }
        }

        return new Plane(
                type,
                objID,
                planeJSON.getString("rootObject"),
                planeJSON.getInt("sizeBound"),
                planeJSON.getInt("initialized") == 1,
                planeJSON.getInt("symbolic") == 1,
                planeJSON.getInt("initialValue"),
                concreteStore,
                concreteMask,
                knownSymbolics,
                updates
        );
    }

    private ArrayList<String> parseConstraints(JSONObject data) {
        ArrayList<String> constraints = new ArrayList<>();
        JSONArray constraintsJSON = data.getJSONArray("constraints");

        for (int i = 0; i < constraintsJSON.length(); i++) {
            constraints.add(constraintsJSON.get(i).toString());
        }
        return constraints;
    }
    public ArrayList<String> getConstraints() {
        return constraints;
    }

    public Memory getMemory() {
        return memory;
    }

    public int getId() {
        return id;
    }
    public Context getContext() {
        return context;
    }
}
