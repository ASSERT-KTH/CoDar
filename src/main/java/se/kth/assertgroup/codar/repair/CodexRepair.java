package se.kth.assertgroup.codar.repair;

import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.ParseException;
import se.kth.assertgroup.codar.Constants;
import se.kth.assertgroup.codar.codex.PromptType;
import se.kth.assertgroup.codar.codex.SonarFixPrompt;
import se.kth.assertgroup.codar.sorald.MineResParser;
import se.kth.assertgroup.codar.sorald.models.ViolationScope;
import se.kth.assertgroup.codar.utils.PH;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

public class CodexRepair {

    private OpenAiService service;

    public CodexRepair() {
        String token = System.getenv(Constants.OPENAI_API_TOKEN_ENV_NAME);
        service = new OpenAiService(token, Duration.ofSeconds(Constants.OPENAI_REQUEST_TIMEOUT));
    }

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

        String token = System.getenv(Constants.OPENAI_API_TOKEN_ENV_NAME);
        OpenAiService service = new OpenAiService(token, Duration.ofSeconds(Constants.OPENAI_REQUEST_TIMEOUT));

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
            int numberOfAddedLines = repairAndGetNumberOfAddedLines(root, e.getKey(), e.getValue(), rule, promptType);
            updateScopeToRuleViolations(scopeToRuleViolations, e, numberOfAddedLines);
        }
    }

    private void updateScopeToRuleViolations(Map<ViolationScope, Set<Integer>> scopeToRuleViolations,
                                             Map.Entry<ViolationScope, Set<Integer>> e, int numberOfAddedLines) {
        scopeToRuleViolations.entrySet().stream()
                .filter(entry -> entry.getKey().getSrcPath().equals(e.getKey().getSrcPath()))
                .forEach(entry -> {
                    if (entry.getKey().getStartLine() >= e.getKey().getStartLine())
                        entry.getKey().setStartLine(entry.getKey().getStartLine() + numberOfAddedLines);
                    if (entry.getKey().getEndLine() >= e.getKey().getEndLine())
                        entry.getKey().setEndLine(entry.getKey().getEndLine() + numberOfAddedLines);
                });
    }

    private int repairAndGetNumberOfAddedLines
            (
                    File root,
                    ViolationScope vs,
                    Set<Integer> buggyLines,
                    String rule,
                    PromptType promptType
            )
            throws IOException {
        File src = new File(root.getPath() + File.separator + vs.getSrcPath());

        SonarFixPrompt initialPrompt =
                generatePrompt(src, vs.getStartLine(), vs.getEndLine(), buggyLines, rule, promptType);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are a super smart automated program repair tool." +
                " You do not generate extra comments or logging statements."));
        messages.add(new ChatMessage("user", initialPrompt.getPromptAsStr()));

        for (int i = 0; i < Constants.MAX_TRIES; i++) {
            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(Constants.TURBO_MODEL)
                    .messages(messages)
                    .stop(List.of("#"))
                    .n(1)
                    .temperature(Constants.OPENAI_LOW_TEMPERATURE)
                    .build();

            try {
                List<ChatCompletionChoice> choices = service.createChatCompletion(completionRequest).getChoices();
                String fixedCode = choices.get(0).getMessage().getContent();

                while (fixedCode.charAt(fixedCode.length() - 1) == '\n') {
                    fixedCode = StringUtils.chomp(fixedCode);
                }

                List<String> srcLines = FileUtils.readLines(src, "UTF-8");

                if (fixedCode.contains(Constants.OPENAI_RESPONSE_SNIPPET_SEPARATOR)) {
                    String functionFirstToken = srcLines.get(vs.getStartLine() - 1).trim().split(" ")[0];
                    fixedCode = extractCodeFromGPTResponse(fixedCode, functionFirstToken);
                }

                File backupOriginalSrc = Files.createTempFile("original_src", ".java").toFile();
                FileUtils.copyFile(src, backupOriginalSrc);

                srcLines.subList(vs.getStartLine() - 1, vs.getEndLine()).clear();
                srcLines.add(vs.getStartLine() - 1, fixedCode);
                FileUtils.writeLines(src, srcLines);

                String failureMessage = getMvnFailureMessage(root);

                if (failureMessage != null) {
                    FileUtils.copyFile(backupOriginalSrc, src);

                    if(messages.size() > 2) { // messages related to the previous response should be removed
                        messages.remove(messages.size() - 1);
                        messages.remove(messages.size() - 1);
                    }

                    messages.add(new ChatMessage("assistant", fixedCode));
                    messages.add(new ChatMessage("user", "This response is not correct." +
                            " It gets the following error:" + System.lineSeparator() + failureMessage +
                            System.lineSeparator() + "Please generate another response."));
                    continue;
                }

                return (fixedCode.split("\r\n|\r|\n").length) - (vs.getEndLine() - vs.getStartLine() + 1);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        return 0;
    }

    private String getMvnFailureMessage(File root) throws IOException, InterruptedException {
        File mvnOutput = Files.createTempFile("mvn_output", ".txt").toFile();
        PH.run(mvnOutput, root, "Running maven ...", "mvn", "test");

        List<String> mvnOutputLines = FileUtils.readLines(mvnOutput, "UTF-8");
        int failureLine;

        // finding the first line error / failure messages
        for (failureLine = mvnOutputLines.size() - 1; failureLine >= 0; failureLine--) {
            if (mvnOutputLines.get(failureLine).contains("BUILD SUCCESS"))
                return null;
            if (mvnOutputLines.get(failureLine).contains("BUILD FAILURE"))
                break;
        }

        if (failureLine < 0)
            return "Maven ran unsuccessfully.";

        String errorMessage = "";

        for(failureLine = 0; failureLine < mvnOutputLines.size(); failureLine++){
            if (mvnOutputLines.get(failureLine).startsWith("[ERROR] Errors:") ||
                    mvnOutputLines.get(failureLine).startsWith("[ERROR] Failures:") ||
                    mvnOutputLines.get(failureLine).startsWith("[ERROR] COMPILATION ERROR :")) {
                break;
            }
        }

        int consideredLines = 0;
        for (; failureLine < mvnOutputLines.size() && consideredLines < Constants.MAX_FEEDBACK_LINES; failureLine++) {
            if (mvnOutputLines.get(failureLine).startsWith("[ERROR]")) {
                String line = mvnOutputLines.get(failureLine);
                line = line.substring("[ERROR] ".length());
                errorMessage += line + System.lineSeparator();
                consideredLines++;
            } else if (mvnOutputLines.get(failureLine).contains("BUILD FAILURE")) {
                break;
            }
        }

        return errorMessage;
    }

    @NotNull
    private static String extractCodeFromGPTResponse(String fixedCode, String functionFirstToken) {
        List<String> fixedCodeSourceLines = new ArrayList<>();
        String[] fixedCodeLines = fixedCode.split("\r\n|\r|\n");

        boolean isSource = false, isNewFunction = false;

        for (int i = 0; i < fixedCodeLines.length; i++) {
            if (fixedCodeLines[i].startsWith(Constants.OPENAI_RESPONSE_SNIPPET_SEPARATOR)) {
                isSource = !isSource;
                if(fixedCodeLines.length > i + 1 && isSource){
                    String codeFirstToken = fixedCodeLines[i + 1].trim().split(" ")[0];
                    isNewFunction = codeFirstToken.equals(functionFirstToken);
                }else{
                    isNewFunction = false;
                }
                continue;
            }

            if (isSource && isNewFunction) {
                fixedCodeSourceLines.add(fixedCodeLines[i]);
            }
        }

        fixedCode = String.join(System.lineSeparator(), fixedCodeSourceLines);
        return fixedCode;
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
            if (buggyLines.contains(i))
                buggyCode += " // " + Constants.PROMPT_NONCOMPLIANT_KEYWORD;
            if (i < endLine)
                buggyCode += System.lineSeparator();
        }

        return new SonarFixPrompt(rule, buggyCode, promptType);
    }
}
