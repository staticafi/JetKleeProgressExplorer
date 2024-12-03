package jetklee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Class that holds information about the node (constraints, context).
 */
public class NodeInfo {
    private ArrayList<String> constraints;
    private Context context;

    public NodeInfo(JSONObject data) {
        this.constraints = parseConstraints(data);
        this.context = parseContext(data);
    }

    public record Location(String file, int line, int column, int assemblyLine) {
    }

    public record Context(int nodeID, int stateID, boolean uniqueState, int parentID, int parentJSON, Location location,
                          Location nextLocation, int depth, boolean coveredNew, boolean forkDisabled,
                          int instsSinceCovNew, int nextID, int steppedInstructions, ArrayList<Location> stack) {
    }

    private Context parseContext(JSONObject data) {
        Location location = parseLocation(data, "location");
        Location nextLocation = parseLocation(data, "nextLocation");

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
                data.getInt("depth"),
                data.getInt("coveredNew") == 1,
                data.getInt("forkDisabled") == 1,
                data.getInt("instsSinceCovNew"),
                data.getInt("nextID"),
                data.getInt("steppedInstructions"),
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

    public Context getContext() {
        return context;
    }
}
