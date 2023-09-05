package se.kth.assertgroup.codar.sorald;

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.assertgroup.codar.Constants;
import se.kth.assertgroup.codar.repair.FixScale;
import se.kth.assertgroup.codar.sorald.models.ViolationScope;
import se.kth.assertgroup.codar.utils.PH;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SonarViolationMiner {
    private static final Logger logger = LoggerFactory.getLogger(SonarViolationMiner.class);
    private static final String SORALD_PATH = System.getenv(Constants.SORALD_PATH_ENV_NAME);

    private MineResParser mineResParser = new MineResParser();

    public long countViolations(File root, ViolationScope vs, String rule, FixScale fixScale)
            throws IOException, InterruptedException, ParseException {
        File mineRes = Files.createTempFile("tmp-mine-res", "json").toFile();
        File srcParent = root.toPath().resolve(vs.getSrcPath()).getParent().toFile();
        PH.run(null, new File(System.getProperty("user.dir")), "Running sonar violation miner", "java",
                "-jar", SORALD_PATH, "mine", "--source", srcParent.getPath(),
                "--stats-output-file", mineRes.getPath());

        return mineResParser.countViolations(srcParent, new ViolationScope(vs.getSrcPath(), vs.getStartLine(), vs.getEndLine()),
                rule, mineRes, fixScale);
    }
}
