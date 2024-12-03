package jetklee;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Retrieves the complete memory of a node (decompress memory).
 */
public class CompleteMemoryRetriever {
    public static NodeMemory.Memory getCompleteMemory(Node node) {
        HashMap<Integer, NodeMemory.ObjectState> complete_memory = new HashMap<>();
        ArrayList<Node> nodes = new ArrayList<>();

        nodes.add(node);
        while (node.getParent() != null) {
            node = node.getParent();
            nodes.add(node);
        }
        // The nodes are in order from leaf to root, traverse them in reverse order (from root to leaf)
        for (int i = nodes.size() - 1; i >= 0; i--) {
            NodeMemory.Memory node_memory = nodes.get(i).getMemory().getMemory();

            // Add newly added objects
            for (NodeMemory.ObjectState addition : node_memory.additions()) {
                complete_memory.put(addition.objID(), addition);
            }

            // Apply changes to changed objects
            for (NodeMemory.ObjectState change : node_memory.changes()) {
                NodeMemory.ObjectState oldObjectState = complete_memory.get(change.objID());
                complete_memory.put(change.objID(), mergeObjectState(oldObjectState, change));
            }

            // Remove deleted objects
            for (NodeMemory.Deletion deletion : node_memory.deletions()) {
                complete_memory.remove(deletion.objID());
            }
        }

        // Save the complete memory in additions
        return new NodeMemory.Memory(new ArrayList<>(complete_memory.values()), new ArrayList<>(), new ArrayList<>());
    }

    private static NodeMemory.ObjectState mergeObjectState(NodeMemory.ObjectState a, NodeMemory.ObjectState b) {
        NodeMemory.Plane mergedSegmentPlane = mergePlane(a.segmentPlane(), b.segmentPlane());
        NodeMemory.Plane mergedOffsetPlane = mergePlane(a.offsetPlane(), b.offsetPlane());

        return new NodeMemory.ObjectState(
                a.objID(), a.type(), a.segment(), a.name(), a.size(), a.isLocal(), a.isGlobal(),
                a.isFixed(), a.isUserSpec(), a.isLazy(), a.symAddress(), a.copyOnWriteOwner(),
                a.readOnly(), a.allocSite(), mergedSegmentPlane, mergedOffsetPlane);
    }

    private static NodeMemory.Plane mergePlane(NodeMemory.Plane a, NodeMemory.Plane b) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        return new NodeMemory.Plane(
                a.type(), a.memoryObjectID(), a.rootObject(), a.sizeBound(), a.initialized(),
                a.symbolic(), a.initialValue(),
                mergeDiff(a.concreteStore(), b.concreteStore()),
                mergeDiff(a.concreteMask(), b.concreteMask()),
                mergeDiff(a.knownSymbolics(), b.knownSymbolics()),
                mergeUpdates(a.updates(), b.updates()));
    }

    private static NodeMemory.Updates mergeUpdates(NodeMemory.Updates a, NodeMemory.Updates b) {
        NodeMemory.Updates mergedUpdates = new NodeMemory.Updates();
        if (a != null) {
            mergedUpdates.putAll(a);
        }
        if (b != null) {
            mergedUpdates.putAll(b);
        }

        return mergedUpdates;
    }

    public static NodeMemory.Diff mergeDiff(NodeMemory.Diff a, NodeMemory.Diff b) {
        NodeMemory.ByteMap mergedByteMap = new NodeMemory.ByteMap();

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

        return new NodeMemory.Diff(
                mergedByteMap,
                // The complete state does not contain deletions
                new NodeMemory.ByteMap());
    }
}
