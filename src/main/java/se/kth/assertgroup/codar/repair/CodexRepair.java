package se.kth.assertgroup.codar.repair;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;
import se.kth.assertgroup.codar.Constants;
import se.kth.assertgroup.codar.codex.PromptType;
import se.kth.assertgroup.codar.codex.SonarFixPrompt;
import se.kth.assertgroup.codar.sorald.MineResParser;
import se.kth.assertgroup.codar.sorald.models.ViolationScope;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodexRepair {

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

    /**
     * This method repairs all violations of a specific rule
     *
     * @param root    The root directory of the source file
     * @param mineRes The output file of Sorald mining command.
     * @param rule    The ID of the rule whose violations should be fixed
     */
    public void repair(File root, File mineRes, String rule, PromptType promptType) throws IOException, ParseException {
        MineResParser mineResParser = new MineResParser();
        Map<ViolationScope, Set<Integer>> scopeToRuleViolations =
                mineResParser.getCodeScopeToViolations(root, mineRes, rule);

        for (Map.Entry<ViolationScope, Set<Integer>> e : scopeToRuleViolations.entrySet()) {
            repair(root, e.getKey(), e.getValue(), rule, promptType);
        }
    }

    private void repair(File root, ViolationScope vs, Set<Integer> buggyLines, String rule, PromptType promptType)
            throws IOException {
        File src = new File(root.getPath() + File.separator + vs.getSrcPath());

        SonarFixPrompt prompt = generatePrompt(src, vs.getStartLine(), vs.getEndLine(), buggyLines, rule, promptType);

        String token = System.getenv(Constants.CODEX_API_TOKEN_ENV_NAME);
        OpenAiService service = new OpenAiService(token, Duration.ofSeconds(Constants.CODEX_TIMEOUT));

        int maxTokens = prompt.getBuggyCode().length() * 3;

        if (maxTokens > Constants.CODEX_MAX_TOKENS)
            throw new RuntimeException("The buggy code is too long for " + vs);

        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(Constants.CODEX_MODEL)
                .prompt(prompt.getPromptAsStr())
                .echo(false)
                .maxTokens(maxTokens)
                .stop(List.of("#"))
                .n(1)
                .bestOf(1)
                .temperature(0.0)
                .build();

        try {
            List<CompletionChoice> choices = service.createCompletion(completionRequest).getChoices();
            String fixedCode = choices.get(0).getText();

            while(fixedCode.charAt(fixedCode.length() - 1) == '\n'){
                fixedCode = StringUtils.chomp(fixedCode);
            }

            List<String> srcLines = FileUtils.readLines(src, "UTF-8");
            srcLines.subList(vs.getStartLine() - 1, vs.getEndLine()).clear();
            srcLines.add(vs.getStartLine() - 1, fixedCode);
            FileUtils.writeLines(src, srcLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SonarFixPrompt generatePrompt
            (
                    File src,
                    Integer startLine,
                    Integer endLine,
                    Set<Integer> buggyLines,
                    String rule,
                    PromptType promptType
            ) throws IOException {
        List<String> lines = FileUtils.readLines(src, "UTF-8");
        String buggyCode = "";

        for (int i = startLine - 1; i <= endLine - 1; i++) {
            buggyCode += lines.get(i);
            if (buggyLines.contains(i + 1))
                buggyCode += " // " + Constants.CODEX_NONCOMPLIANT_KEYWORD;
            if (i < endLine)
                buggyCode += System.lineSeparator();
        }

        return new SonarFixPrompt(rule, buggyCode, promptType);
    }
}
