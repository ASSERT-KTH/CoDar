package se.kth.assertgroup.codar.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

// ProcessHelper
public class PH {
    private static final Logger logger = LoggerFactory.getLogger(PH.class);

    public static int run(File output, File dir, String message, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        if (output != null) {
            pb.redirectOutput(output);
        } else {
            pb.inheritIO();
        }
        pb.directory(dir);
        logger.info(message);
        Process p = pb.start();
        return p.waitFor();
    }
}
