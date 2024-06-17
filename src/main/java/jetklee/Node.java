package jetklee;

/**
 * Node of the process tree
 */
public class Node {

    public class ViewProps {
        int x = 0;
        int y = 0;
        int subTreeMinX = 0;
        int subTreeMaxX = 0;
    }

    public int startRound;
    public int endRound;
    public Node parent;
    public Node left;
    public Node right;
    public int id;
    ViewProps viewProps;
    public ExecutionState executionState;

    /**
     * @param id_ unique node id
     */
    public Node(int id_) {
        id = id_;
        endRound = 0;

        parent = null;
        left = null;
        right = null;
        viewProps = new ViewProps();
    }

    public boolean isRoot() {
        return parent == null;
    }
}
