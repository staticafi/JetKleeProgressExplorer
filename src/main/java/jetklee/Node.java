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

    private ViewProps viewProps;
    private int startRound;
    private int endRound;
    private Node parent;
    private Node left;
    private Node right;
    private int id;
    private int memoryId;
    private NodeInfo info;
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

    public ViewProps getViewProps() {
        return viewProps;
    }

    public int getStartRound() {
        return startRound;
    }

    public int getEndRound() {
        return endRound;
    }

    public Node getParent() {
        return parent;
    }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

    public int getId() {
        return id;
    }

    public int getMemoryId() {
        return memoryId;
    }

    public NodeInfo getInfo() {
        return info;
    }

    public void setViewProps(ViewProps viewProps) {
        this.viewProps = viewProps;
    }

    public void setStartRound(int startRound) {
        this.startRound = startRound;
    }

    public void setEndRound(int endRound) {
        this.endRound = endRound;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public void setRight(Node right) {
        this.right = right;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMemoryId(int memoryId) {
        this.memoryId = memoryId;
    }

    public void setInfo(NodeInfo info) {
        this.info = info;
    }
}
