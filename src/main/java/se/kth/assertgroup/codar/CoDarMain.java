package se.kth.assertgroup.codar;

import picocli.CommandLine;
import se.kth.assertgroup.codar.gpt.PromptType;
import se.kth.assertgroup.codar.repair.GPTRepair;

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
            names = {Constants.ARG_ROOT_DIR},
            description = "The path to the source root.")
    File rootDir;

    @CommandLine.Option(
            names = {Constants.ARG_MINE_RES},
            description = "The path to the mining result file.")
    File mineRes;

    @CommandLine.Option(
            names = {Constants.ARG_RULE},
            required = true,
            description = "The rule whose violations should be fixed.")
    String rule;

    @CommandLine.Option(
            names = {Constants.ARG_PROMPT_TYPE},
            description = "The type of prompt that should be sent to Codex.")
    PromptType promptType;

    @Override
    public Integer call() throws Exception {
        GPTRepair gptRepair = new GPTRepair();
        if(rule != null && !gptRepair.isHandled(rule, promptType)){
            System.out.println("Rule " + rule + " is not handled by CoDar.");
            return 0;
        }
        gptRepair.repair(rootDir, mineRes, rule, promptType);
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
