package se.kth.assertgroup.codar;

import picocli.CommandLine;
import se.kth.assertgroup.codar.repair.CodexRepair;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = Constants.CODAR_COMMAND_NAME,
        mixinStandardHelpOptions = true,
        description =
                "The CoDar command line tool for fixing SonarJava warnings.",
        synopsisSubcommandLabel = "<COMMAND>")
public class CoDarMain implements Callable<Integer> {
    @CommandLine.Option(
            names = {Constants.ARG_SRC_DIR},
            description = "The path to the source root.")
    File srcDir;

    @CommandLine.Option(
            names = {Constants.ARG_MINE_RES},
            description = "The path to the mining result file.")
    File mineRes;

    @Override
    public Integer call() throws Exception {
        new CodexRepair().repair(srcDir, mineRes);
        return 0;
    }

    public static void main(String[] args) throws IOException {
//        new CodexRepair().repairSingleLine(
//                new File("/home/khaes/tmp/sds/sds-admin/src/main/java/com/didiglobal/sds/admin/util/BloomFileter.java"),
//                new File("/home/khaes/tmp/BloomFileter.java"),
//                23, 36, 28, "2184"
//        );
        int exitCode = new CommandLine(new CoDarMain()).execute(args);
        System.exit(exitCode);
    }
}
