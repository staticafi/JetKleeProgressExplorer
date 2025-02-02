package jetklee;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads source code lines.
 */

public class SourceLoader {
    protected List<String> sourceC;
    protected List<String> sourceLL;

    public SourceLoader() {
    }

    private List<String> loadFile(File sourceFile) throws IOException {
        List<String> source = new ArrayList<>();

        if (sourceFile.isFile()) {
            try (Stream<String> lines = Files.lines(Paths.get(sourceFile.getPath()))) {
                source = lines.collect(Collectors.toList());
            }
        } else {
            source.add("Cannot access source code file: " + sourceFile.getAbsolutePath());
        }
        return source;
    }

    public void load(String dir) throws IOException {
        sourceC = loadFile(new File(dir + "/source.c"));
        sourceLL = loadFile(new File(dir + "/source.ll"));
    }
}
