package jetklee;

import java.util.ArrayList;

public class Node {

    public class ViewProps {
        int x = 0;
        int y = 0;
        int startRound;
        int endRound;
    }

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

        parent = null;
        left = null;
        right = null;
        viewProps = new ViewProps();
    }
}
