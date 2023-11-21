package jetklee;

import java.util.ArrayList;

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
    public int stateID;
    public boolean uniqueState;
    public ArrayList<String> constraints;
    ViewProps viewProps;

    public Node(int id_, int stateID_, boolean uniqueState_, ArrayList<String> constraints_) {
        id = id_;
        stateID = stateID_;
        uniqueState = uniqueState_;
        constraints = constraints_;
        endRound = 0;

        parent = null;
        left = null;
        right = null;
        viewProps = new ViewProps();
    }
}
