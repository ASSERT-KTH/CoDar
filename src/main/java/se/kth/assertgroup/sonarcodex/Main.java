package se.kth.assertgroup.sonarcodex;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import org.apache.commons.io.FileUtils;
import se.kth.assertgroup.sonarcodex.repair.CodexRepair;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        new CodexRepair().repairSingleLine(
                new File("/home/khaes/tmp/sds/sds-admin/src/main/java/com/didiglobal/sds/admin/util/BloomFileter.java"),
                new File("/home/khaes/tmp/BloomFileter.java"),
                23, 36, 28, "2184"
        );
    }
}
