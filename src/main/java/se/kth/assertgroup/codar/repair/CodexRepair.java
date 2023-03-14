package se.kth.assertgroup.codar.repair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import org.apache.commons.io.FileUtils;
import se.kth.assertgroup.codar.Constants;
import se.kth.assertgroup.codar.codex.SonarFixPrompt;
import sorald.event.collectors.MinerStatisticsCollector;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class CodexRepair implements RepairTool {

    @Override
    public void repairSingleLine(File inputSrc, File outputSrc,
                                 int bugStartLine, int bugEndLine, int nonCompliantLine, String ruleKey) throws IOException {
        List<String> lines = FileUtils.readLines(inputSrc, "UTF-8");
        String buggyCode = "";

        for (int i = bugStartLine - 1; i <= bugEndLine - 1; i++) {
            buggyCode += lines.get(i);
            if (i - 1 == nonCompliantLine)
                buggyCode += " // Non-compliant";
            if (i < bugEndLine)
                buggyCode += System.lineSeparator();
        }

        String prompt = new SonarFixPrompt(ruleKey, buggyCode).getPromptAsStr();

        String token = System.getenv(Constants.CODEX_API_TOKEN_ENV_NAME);
        OpenAiService service = new OpenAiService(token, Duration.ofSeconds(Constants.CODEX_TIMEOUT));

        CompletionRequest completionRequest = CompletionRequest.builder()
                .model("code-davinci-002")
                .prompt(prompt)
                .echo(false)
                .maxTokens(buggyCode.length() * 3)
                .stop(List.of("#"))
                .n(1)
                .bestOf(1)
                .temperature(0.0)
                .build();

        List<CompletionChoice> choices = service.createCompletion(completionRequest).getChoices();
        String fixedCode = choices.get(0).getText().replaceAll(System.lineSeparator(), "");

        lines.set(nonCompliantLine - 1, fixedCode);

        FileUtils.writeLines(outputSrc, lines);
    }

    public void repair(File src, File mineRes) throws IOException {

    }
}
