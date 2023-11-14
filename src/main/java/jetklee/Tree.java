package jetklee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Tree {
    public Node root;
    public HashMap<Integer, Node> nodes;
    public List<String> rounds;
    private int roundCounter;

    public Tree() {
        root = null;
        nodes = new HashMap<>();
        rounds = new ArrayList<>();
        roundCounter = 0;
    }

    public void loadFiles(String dir) throws Exception {
        File[] files = new File(dir).listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")){
                String fileName = file.getName();
                loadFile(Paths.get(dir, fileName));
                roundCounter++;
                rounds.add(fileName);
            }
        }
    }

    private enum Action {
        INSERT_NODE, INSERT_EDGE, ERASE_NODE;

        private static Action parse(String actionStr) throws Exception {
            return switch (actionStr) {
                case "InsertNode" -> INSERT_NODE;
                case "InsertEdge" -> INSERT_EDGE;
                case "EraseNode" -> ERASE_NODE;
                default -> throw new Exception("Unknown action: " + actionStr);
            };
        }
    }

    private void loadFile(Path filePath) throws Exception {
        String fileContent;

        try {
            fileContent = new String(Files.readAllBytes(filePath));
        } catch (IOException e) {
            System.out.println("Unable to read file: " + filePath);
            return;
        }
        JSONArray actions = new JSONArray(fileContent);

        for (Object actionObj : actions) {
            JSONObject actionJSON = (JSONObject) actionObj;
            String actionStr = actionJSON.getString("action");
            Action action = Action.parse(actionStr);

            switch (action) {
                case INSERT_NODE:
                    InsertNode(actionJSON);
                    break;
                case INSERT_EDGE:
                    InsertEdge(actionJSON);
                    break;
                case ERASE_NODE:
                    EraseNode(actionJSON);
                    break;
            }
        }
    }

    private void InsertNode(JSONObject actionJSON) {
        int nodeID = actionJSON.getInt("nodeID");
        int stateID = actionJSON.getInt("stateID");
        boolean uniqueState = actionJSON.getInt("uniqueState") == 1;
        JSONArray constraintsJSON = actionJSON.getJSONArray("constraints");
        ArrayList<String> constraints = new ArrayList<>();

        for (int i = 0; i < constraintsJSON.length(); i++) {
            constraints.add(constraintsJSON.get(i).toString());
        }

        Node node = new Node(nodeID, stateID, uniqueState, constraints);
        node.viewProps.startRound = roundCounter;
        if (nodeID == 1) root = node;
        nodes.put(nodeID, node);
    }

    private void InsertEdge(JSONObject actionJSON) {
        int parentID = actionJSON.getInt("parentID");
        int childID = actionJSON.getInt("childID");
        int tag = actionJSON.getInt("tag");

        Node parent = nodes.get(parentID);
        Node child = nodes.get(childID);

        if (parent.left == null)
            parent.left = child;
        else
            parent.right = child;
        child.parent = parent;
    }

    private void EraseNode(JSONObject actionJSON) {
        int nodeID = actionJSON.getInt("nodeID");
        Node node = nodes.get(nodeID);
        node.viewProps.endRound = roundCounter;
    }

    private void dumpAction(String actionStr) {
        System.out.print("Action: " + actionStr + ", ");
        System.out.print("Nodes: ");
        for (Integer id : nodes.keySet()) {
            System.out.print(id + " ");
        }
        System.out.println();
    }

    public void dumpTree() {
        if (root == null) return;
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            System.out.print(node.id + "\n");
            System.out.println("Start: " + node.viewProps.startRound + ", End: " + node.viewProps.endRound);

            if (node.left != null)
                queue.add(node.left);
            if (node.right != null)
                queue.add(node.right);
        }
    }
}
