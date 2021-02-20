import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class CompareCSVs {
    public static void compare(File export1, File export2) throws IOException {
        List<String> lines1 = Files.readAllLines(export1.toPath());
        List<String> lines2 = Files.readAllLines(export2.toPath());
    }
}
