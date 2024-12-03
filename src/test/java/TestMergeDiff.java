import jetklee.NodeMemory;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static jetklee.CompleteMemoryRetriever.mergeDiff;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMergeDiff {

    // Method to convert Map<String, List<Integer>> to ByteMap
    private static NodeMemory.ByteMap convertToByteMap(Map<String, List<Integer>> map) {
        NodeMemory.ByteMap byteMap = new NodeMemory.ByteMap();
        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
            byteMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return byteMap;
    }

    @Test
    public void testMergeDiffSimple() {
        // Arrange: Set up diff1, diff2, and the expected result
        NodeMemory.Diff diff1 = new NodeMemory.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(0, 1, 2, 3)
                )),
                // deletions
                new NodeMemory.ByteMap()
        );

        NodeMemory.Diff diff2 = new NodeMemory.Diff(
                // additions
                convertToByteMap(Map.of(
                        "1", List.of(1, 4)
                )),
                // deletions
                convertToByteMap(Map.of(
                        "0", List.of(1, 2)
                ))
        );

        NodeMemory.Diff expected = new NodeMemory.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(0, 3),
                        "1", List.of(1, 4)
                )),
                // deletions
                new NodeMemory.ByteMap()
        );


        // Act: Call the mergeDiff function
        NodeMemory.Diff result = mergeDiff(diff1, diff2);

        // check if the original diff1 and diff2 are not modified
        NodeMemory.Diff diff1Expected = new NodeMemory.Diff(
                convertToByteMap(Map.of(
                        "0", List.of(0, 1, 2, 3)
                )),
                new NodeMemory.ByteMap()
        );
        NodeMemory.Diff diff2Expected = new NodeMemory.Diff(
                convertToByteMap(Map.of(
                        "1", List.of(1, 4)
                )),
                convertToByteMap(Map.of(
                        "0", List.of(1, 2)
                ))
        );

        assertEquals(diff1Expected, diff1, "The original diff1 is modified");
        assertEquals(diff2Expected, diff2, "The original diff2 is modified");

        // Assert: Check if the result matches the expected output
        assertEquals(expected, result, "The merged diff is incorrect");
    }

    @Test
    public void testMergeDiffDuplicateKey() {
        // Arrange: Set up diff1, diff2, and the expected result
        NodeMemory.Diff diff1 = new NodeMemory.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(0, 1, 2, 3)
                )),
                // deletions
                new NodeMemory.ByteMap()
        );

        NodeMemory.Diff diff2 = new NodeMemory.Diff(
                // additions
                convertToByteMap(Map.of(
                        "222", List.of(0)
                )),
                // deletions
                convertToByteMap(Map.of(
                        "0", List.of(0)
                ))
        );

        NodeMemory.Diff expected = new NodeMemory.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(1, 2, 3),
                        "222", List.of(0)
                )),
                new NodeMemory.ByteMap()
        );

        // Act: Call the mergeDiff function
        NodeMemory.Diff result = mergeDiff(diff1, diff2);

        // Assert: Check if the result matches the expected output
        assertEquals(expected, result, "The merged diff is incorrect");
    }
}
