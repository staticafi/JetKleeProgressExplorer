package jetklee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Tree {
    public Node root;
    public HashMap<Integer, Node> nodes;
    public List<String> rounds;
    public int roundCounter;

    public Tree() {
        root = null;
        nodes = new HashMap<>();
        rounds = new ArrayList<>();
        roundCounter = 0;
    }

    public void load(Path dir) throws Exception {
        Files.list(dir)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparingInt(this::pathToInt))
                .forEach(file -> {
                    try {
                        loadFile(file);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    roundCounter++;
                    rounds.add(file.getFileName().toString().replace(".json", ""));
                });

        // set endRound for nodes without EraseNode action
        for (Node node : nodes.values()) {
            if (node.endRound == 0) {
                node.endRound = roundCounter;
            }
        }
    }

    private int pathToInt(final Path path) {
        return Integer.parseInt(path.getFileName()
                .toString()
                .replace(".json", "")
        );
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
                    Node node = insertNode(actionJSON);
                    node.executionState = new ExecutionState(actionJSON);
                    break;
                case INSERT_EDGE:
                    insertEdge(actionJSON);
                    break;
                case ERASE_NODE:
                    eraseNode(actionJSON);
                    break;
            }
        }
    }

    private Node insertNode(JSONObject actionJSON) {
        int nodeID = actionJSON.getInt("nodeID");

        Node node = new Node(nodeID);
        node.startRound = roundCounter;
        if (nodeID == 1) root = node;
        nodes.put(nodeID, node);
        return node;
    }

    private void insertEdge(JSONObject actionJSON) {
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

    private void eraseNode(JSONObject actionJSON) {
        int nodeID = actionJSON.getInt("nodeID");
        Node node = nodes.get(nodeID);
        node.endRound = roundCounter;
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

            if (node.left != null)
                queue.add(node.left);
            if (node.right != null)
                queue.add(node.right);
        }
    }

    public void clear() {
        root = null;
        nodes = null;
        rounds = null;
        roundCounter = 0;
    }

}
