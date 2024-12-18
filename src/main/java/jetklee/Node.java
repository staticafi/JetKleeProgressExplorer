package jetklee;

/**
 * Node of the process tree
 */
public class Node {

    public class ViewProps {
        private int x = 0;
        private int y = 0;
        private int subTreeMinX = 0;
        private int subTreeMaxX = 0;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getSubTreeMinX() {
            return subTreeMinX;
        }

        public int getSubTreeMaxX() {
            return subTreeMaxX;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void setSubTreeMinX(int subTreeMinX) {
            this.subTreeMinX = subTreeMinX;
        }

        public void setSubTreeMaxX(int subTreeMaxX) {
            this.subTreeMaxX = subTreeMaxX;
        }
    }
    private final int id;
    private int startRound;
    private int endRound;
    private Node parent;
    private Node left;
    private Node right;
    private ViewProps viewProps;

    private ExecutionState es;

    /**
     * @param id_ unique node id
     */
    public Node(int id_) {
        id = id_;
        endRound = 0;

        es = null;
        parent = null;
        left = null;
        right = null;
        viewProps = new ViewProps();
    }
    public void setExecutionState(ExecutionState es) {
        this.es = es;
    }

    public ExecutionState getExecutionState() {
        return es;
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
}
