package jetklee;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Retrieves the complete memory of a node (decompress memory).
 */
public class CompleteMemoryRetriever {
    public static ExecutionState.ObjectState getDeletedObjectState(Node node, int objID) {
        Node current = node;

        // search for the memory of the deleted object
        while (true) {
            ExecutionState.Memory memory = current.getExecutionState().getMemory();
            for (ExecutionState.ObjectState addition : memory.additions()) {
                if (addition.objID() == objID) {
                    return addition;
                }
            }
            for (ExecutionState.ObjectState change : memory.changes()) {
                if (change.objID() == objID) {
                    return change;
                }
            }
            assert current.getParent() != null; // the node which was deleted must have been created before
            current = current.getParent();
        }
    }
    public static ExecutionState.Memory getCompleteMemory(Node node) {
        HashMap<Integer, ExecutionState.ObjectState> complete_memory = new HashMap<>();
        ArrayList<Node> nodes = new ArrayList<>();

        nodes.add(node);
        while (node.getParent() != null) {
            node = node.getParent();
            nodes.add(node);
        }
        // The nodes are in order from leaf to root, traverse them in reverse order (from root to leaf)
        for (int i = nodes.size() - 1; i >= 0; i--) {
            ExecutionState.Memory node_memory = nodes.get(i).getExecutionState().getMemory();

            // Add newly added objects
            for (ExecutionState.ObjectState addition : node_memory.additions()) {
                complete_memory.put(addition.objID(), addition);
            }

            // Apply changes to changed objects
            for (ExecutionState.ObjectState change : node_memory.changes()) {
                ExecutionState.ObjectState oldObjectState = complete_memory.get(change.objID());
                complete_memory.put(change.objID(), mergeObjectState(oldObjectState, change));
            }

            // Remove deleted objects
            for (ExecutionState.Deletion deletion : node_memory.deletions()) {
                complete_memory.remove(deletion.objID());
            }
        }

        // Save the complete memory in additions
        return new ExecutionState.Memory(new ArrayList<>(complete_memory.values()), new ArrayList<>(), new ArrayList<>());
    }

    private static ExecutionState.ObjectState mergeObjectState(ExecutionState.ObjectState a, ExecutionState.ObjectState b) {
        ExecutionState.Plane mergedSegmentPlane = mergePlane(a.segmentPlane(), b.segmentPlane());
        ExecutionState.Plane mergedOffsetPlane = mergePlane(a.offsetPlane(), b.offsetPlane());

        return new ExecutionState.ObjectState(
                a.objID(), a.type(), a.segment(), a.name(), a.size(), a.isLocal(), a.isFixed(), a.isUserSpec(),
                a.isLazy(), a.copyOnWriteOwner(), a.readOnly(), a.allocSite(), mergedSegmentPlane, mergedOffsetPlane);
    }

    private static ExecutionState.Plane mergePlane(ExecutionState.Plane a, ExecutionState.Plane b) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        return new ExecutionState.Plane(
                a.type(), a.memoryObjectID(), a.rootObject(), a.sizeBound(), a.initialized(),
                a.symbolic(), a.initialValue(),
                mergeDiff(a.concreteStore(), b.concreteStore()),
                mergeDiff(a.concreteMask(), b.concreteMask()),
                mergeDiff(a.knownSymbolics(), b.knownSymbolics()),
                mergeUpdates(a.updates(), b.updates()));
    }

    private static ExecutionState.Updates mergeUpdates(ExecutionState.Updates a, ExecutionState.Updates b) {
        ExecutionState.Updates mergedUpdates = new ExecutionState.Updates();
        if (a != null) {
            mergedUpdates.addAll(a);
        }
        if (b != null) {
            mergedUpdates.addAll(b);
        }

        return mergedUpdates;
    }

    public static ExecutionState.Diff mergeDiff(ExecutionState.Diff a, ExecutionState.Diff b) {
        ExecutionState.ByteMap mergedByteMap = new ExecutionState.ByteMap();

        // Copy all entries from 'a' into 'mergedByteMap'
        a.additions().forEach((key, indices) -> {
            mergedByteMap.put(key, new ArrayList<>(indices));
        });

        // Delete all entries from 'b's deletions
        b.deletions().forEach((key, indices) -> {
            mergedByteMap.get(key).removeAll(indices);
        });

        // Remove all empty entries from 'mergedByteMap'
        mergedByteMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Iterate over each addition in 'b' and merge it into 'mergedByteMap'
        b.additions().forEach((key, indices) -> {
            // If the key exists in 'mergedByteMap', merge the indices by adding all elements of 'indices' to 'mergedByteMap'
            mergedByteMap.merge(key, indices, (v1, v2) -> {
                v1.addAll(v2);
                return v1;
            });
        });

        return new ExecutionState.Diff(
                mergedByteMap,
                // The complete state does not contain deletions
                new ExecutionState.ByteMap());
    }
}
