import jetklee.ExecutionState;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static jetklee.CompleteMemoryRetriever.mergeDiff;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMergeDiff {

    // Method to convert Map<String, List<Integer>> to ByteMap
    private static ExecutionState.ByteMap convertToByteMap(Map<String, List<Integer>> map) {
        ExecutionState.ByteMap byteMap = new ExecutionState.ByteMap();
        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
            byteMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return byteMap;
    }

    @Test
    public void testMergeDiffSimple() {
        // Arrange: Set up diff1, diff2, and the expected result
        ExecutionState.Diff diff1 = new ExecutionState.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(0, 1, 2, 3)
                )),
                // deletions
                new ExecutionState.ByteMap()
        );

        ExecutionState.Diff diff2 = new ExecutionState.Diff(
                // additions
                convertToByteMap(Map.of(
                        "1", List.of(1, 4)
                )),
                // deletions
                convertToByteMap(Map.of(
                        "0", List.of(1, 2)
                ))
        );

        ExecutionState.Diff expected = new ExecutionState.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(0, 3),
                        "1", List.of(1, 4)
                )),
                // deletions
                new ExecutionState.ByteMap()
        );


        // Act: Call the mergeDiff function
        ExecutionState.Diff result = mergeDiff(diff1, diff2);

        // check if the original diff1 and diff2 are not modified
        ExecutionState.Diff diff1Expected = new ExecutionState.Diff(
                convertToByteMap(Map.of(
                        "0", List.of(0, 1, 2, 3)
                )),
                new ExecutionState.ByteMap()
        );
        ExecutionState.Diff diff2Expected = new ExecutionState.Diff(
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
        ExecutionState.Diff diff1 = new ExecutionState.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(0, 1, 2, 3)
                )),
                // deletions
                new ExecutionState.ByteMap()
        );

        ExecutionState.Diff diff2 = new ExecutionState.Diff(
                // additions
                convertToByteMap(Map.of(
                        "222", List.of(0)
                )),
                // deletions
                convertToByteMap(Map.of(
                        "0", List.of(0)
                ))
        );

        ExecutionState.Diff expected = new ExecutionState.Diff(
                // additions
                convertToByteMap(Map.of(
                        "0", List.of(1, 2, 3),
                        "222", List.of(0)
                )),
                new ExecutionState.ByteMap()
        );

        // Act: Call the mergeDiff function
        ExecutionState.Diff result = mergeDiff(diff1, diff2);

        // Assert: Check if the result matches the expected output
        assertEquals(expected, result, "The merged diff is incorrect");
    }
}
