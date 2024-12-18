package jetklee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Process Tree created based on the data stored in the json files.
 */
public class Tree {
    private int roundCounter;
    private Node root;
    private HashMap<Integer, Node> nodes;
    private List<String> rounds;
    private static final String TREE_DIR = "Tree";
    private static final String MEMORY_DIR = "States";

    public Tree() {
        roundCounter = 0;
        root = null;
        nodes = new HashMap<>();
        rounds = new ArrayList<>();
    }

    /**
     * Loads json files containing data about the process tree.
     *
     * @param dir directory with json files.
     * @throws Exception thrown if file can't be loaded.
     */
    public void load(String dir) throws Exception {
        loadTree(Paths.get(dir, TREE_DIR));
        loadMemory(Paths.get(dir, MEMORY_DIR));
    }

    private void loadTree(Path dir) throws Exception {
        roundCounter = 0;
        rounds = new ArrayList<>();

        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparingInt(this::pathToInt))
                    .forEach(file -> {
                        try {
                            System.out.println("Loading Tree file: " + file.getFileName().toString());
                            loadTreeFile(file);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        roundCounter++;
                        rounds.add(file.getFileName().toString().replace(".json", ""));
                    });
        }
    }

    /**
     * Loads one json file and performs the actions (insert node, insert edge, erase node).
     *
     * @param filePath of the json file.
     * @throws Exception thrown if the action is unknown.
     */
    private void loadTreeFile(Path filePath) throws Exception {
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
                    insertNode(actionJSON);
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

    private void loadMemory(Path dir) throws Exception {
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparingInt(this::pathToInt))

                    .forEach(file -> {
                        try {
                            System.out.println("Loading Memory file: " + file.getFileName().toString());
                            if (Files.size(file) != 0) {
                                loadMemoryFile(file);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

        }

        // set endRound for nodes without EraseNode action
        for (Node node : nodes.values()) {
            if (node.getEndRound() == 0) {
                node.setEndRound(roundCounter);
            }
        }

    }

    /**
     * Loads one json file and performs the insert memory action.
     *
     * @param filePath of the json file.
     * @throws Exception thrown if the action is unknown.
     */
    private void loadMemoryFile(Path filePath) throws Exception {
        String fileContent;

        try {
            fileContent = new String(Files.readAllBytes(filePath));
        } catch (IOException e) {
            System.out.println("Unable to read file: " + filePath);
            return;
        }
        JSONObject actionJSON = new JSONObject(fileContent);
        String actionStr = actionJSON.getString("action");
        Action action = Action.parse(actionStr);

        if (action == Action.INSERT_MEMORY) {
            insertMemory(actionJSON);
        }
    }

    private int pathToInt(final Path path) {
        return Integer.parseInt(path.getFileName()
                .toString()
                .replace(".json", "")
        );
    }

    /**
     * Corresponds to action in json files.
     */
    private enum Action {
        INSERT_NODE, INSERT_MEMORY, INSERT_EDGE, ERASE_NODE;

        private static Action parse(String actionStr) throws Exception {
            return switch (actionStr) {
                case "InsertNode" -> INSERT_NODE;
                case "InsertEdge" -> INSERT_EDGE;
                case "InsertInfo" -> INSERT_MEMORY;
                case "EraseNode" -> ERASE_NODE;
                default -> throw new Exception("Unknown tree action: " + actionStr);
            };
        }

    }

    private void insertNode(JSONObject actionJSON) {
        int nodeID = actionJSON.getInt("nodeID");

        Node node = new Node(nodeID);
        node.setStartRound(roundCounter);
        if (nodeID == 1) {
            root = node;
        }
        nodes.put(nodeID, node);

        node.setExecutionState(new ExecutionState(actionJSON, nodeID));
    }

    private void insertMemory(JSONObject actionJSON) {
        int nodeID = actionJSON.getInt("nodeID");

        Node node = nodes.get(nodeID);
        int parentID = node.getParent() != null ? node.getParent().getId() : -1;

        ExecutionState es = node.getExecutionState();
        es.setNodeInfoData(actionJSON, parentID);
        node.setExecutionState(es);
    }

    private void insertEdge(JSONObject actionJSON) {
        int parentID = actionJSON.getInt("parentID");
        int childID = actionJSON.getInt("nodeID");

        Node parent = nodes.get(parentID);
        Node child = nodes.get(childID);

        // Left child is inserted first
        if (parent.getLeft() == null) {
            parent.setLeft(child);
        }
        else {
            parent.setRight(child);
        }
        child.setParent(parent);
    }

    private void eraseNode(JSONObject actionJSON) {
        int nodeID = actionJSON.getInt("nodeID");

        Node node = nodes.get(nodeID);
        node.setEndRound(roundCounter);
    }

    public int getRoundCounter() {
        return roundCounter;
    }

    public Node getRoot() {
        return root;
    }

    public HashMap<Integer, Node> getNodes() {
        return nodes;
    }

    public List<String> getRounds() {
        return rounds;
    }
}
