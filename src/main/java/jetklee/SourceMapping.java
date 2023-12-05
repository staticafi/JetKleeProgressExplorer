package jetklee;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SourceMapping {
    public List<String> sourceC;
    public List<String> sourceLL;

    public SourceMapping(){
    }
    private List<String> loadFile(File sourceFile) throws IOException {
        List<String> source = new ArrayList<>();
        if (sourceFile.isFile())
            source = Files.lines(Paths.get(sourceFile.getPath())).toList();
        else
            source.add("Cannot access source code file: " + sourceFile.getAbsolutePath());
        return source;
    }
    public void load(String dir) throws IOException {
        sourceC = loadFile(new File(dir + "/source.c"));
        sourceLL = loadFile(new File(dir + "/source.ll"));
    }

    public void clear() {
        sourceC = null;
        sourceLL = null;
    }
}
