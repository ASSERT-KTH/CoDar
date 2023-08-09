package se.kth.assertgroup.codar.sorald;

import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.assertgroup.codar.Constants;
import se.kth.assertgroup.codar.sorald.models.ViolationScope;
import se.kth.assertgroup.codar.utils.PH;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SonarViolationMiner {
    private static final Logger logger = LoggerFactory.getLogger(SonarViolationMiner.class);
    private static final String SORALD_PATH = System.getenv(Constants.SORALD_PATH_ENV_NAME);

    private MineResParser miner = new MineResParser();

    public boolean containsViolation(File root, ViolationScope vs, String rule)
            throws IOException, InterruptedException, ParseException {
        File mineRes = Files.createTempFile("tmp-mine-res", "json").toFile();
        File srcParent = root.toPath().resolve(vs.getSrcPath()).getParent().toFile();
        PH.run(null, new File(System.getProperty("user.dir")), "Running sonar violation miner", "java",
                "-jar", SORALD_PATH, "mine", "--source", srcParent.getPath(), "--handled-rules",
                "--stats-output-file", mineRes.getPath());

        return miner.containsViolation(srcParent, new ViolationScope(vs.getSrcPath(), vs.getStartLine(), vs.getEndLine()),
                rule, mineRes);
    }
}
