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
import se.kth.assertgroup.codar.sorald.SonarViolationMiner;
import se.kth.assertgroup.codar.sorald.models.ViolationScope;
import se.kth.assertgroup.codar.utils.PH;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

public class CodexRepair {

    private OpenAiService service;
    private SonarViolationMiner miner;

    public CodexRepair() {
        String token = System.getenv(Constants.OPENAI_API_TOKEN_ENV_NAME);
        service = new OpenAiService(token, Duration.ofSeconds(Constants.OPENAI_REQUEST_TIMEOUT));
        miner = new SonarViolationMiner();
    }

    public void repairSingleLine(File inputSrc, File outputSrc,
                                 int bugStartLine, int bugEndLine, int nonCompliantLine, String ruleKey) throws IOException {
        List<String> lines = FileUtils.readLines(inputSrc, "UTF-8");
        String buggyCode = "";

        boolean isFirstCurlyBracketPassed = false;

        for (int i = 0; i <= bugEndLine - 1; i++) {
            String currentLine = lines.get(i);
            if (i >= bugStartLine - 1) {
                buggyCode += currentLine;
                if (i - 1 == nonCompliantLine)
                    buggyCode += " // Non-compliant";
                buggyCode += System.lineSeparator();
            } else {
                if (currentLine.trim().startsWith("import ")) {
                    buggyCode += currentLine + System.lineSeparator();
                } else if (currentLine.contains("{") && !isFirstCurlyBracketPassed) {
                    buggyCode += currentLine + System.lineSeparator();
                    isFirstCurlyBracketPassed = true;
                }
            }
        }

        buggyCode += "}";

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
        Map<String, Map<ViolationScope, Set<Integer>>> ruleToViolations =
                mineResParser.getRuleToScopeViolations(root, mineRes);

        for(Map.Entry<String, Map<ViolationScope, Set<Integer>>> e : ruleToViolations.entrySet()){
            String curRule = e.getKey();
            Map<ViolationScope, Set<Integer>> scopeToViolations = e.getValue();

            if((rule == null && isHandled(curRule)) || curRule.equals(rule)){
                for (Map.Entry<ViolationScope, Set<Integer>> scopeViolations : scopeToViolations.entrySet()) {
                    int numberOfAddedLines = repairAndGetNumberOfAddedLines(root, scopeViolations.getKey(),
                            scopeViolations.getValue(), rule, promptType);
                    updateRuleToScopeViolations(ruleToViolations, scopeViolations, numberOfAddedLines);
                }
            }
        }
    }

    private void updateRuleToScopeViolations(Map<String, Map<ViolationScope, Set<Integer>>> ruleViolations,
                                             Map.Entry<ViolationScope, Set<Integer>> e, int numberOfAddedLines) {
        ruleViolations.forEach((rule, scopeToViolations)
                -> updateScopeToRuleViolations(scopeToViolations, e, numberOfAddedLines));
    }

    private boolean isHandled(String rule) {
        return true;
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

        Set<String> fixedCodes = new HashSet<>();

        double temperature = Constants.OPENAI_LOW_TEMPERATURE;

        int currentConversationLen = 0;
        boolean repeatedResponse = false;

        while (temperature <= Constants.OPENAI_MAX_TEMPERATURE) {
            if (repeatedResponse || currentConversationLen++ > Constants.MAX_CONVERSATION_LENGTH) {
                temperature += Constants.TEMPERATURE_INCREASE_STEP;

                if (temperature > Constants.OPENAI_MAX_TEMPERATURE) {
                    break;
                }

                messages = messages.subList(0, 2);
                currentConversationLen = 0;
                repeatedResponse = false;
                fixedCodes.clear();
            }

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(Constants.TURBO_MODEL)
                    .messages(messages)
                    .stop(List.of("#"))
                    .n(1)
                    .temperature(temperature)
                    .build();

            try {
                List<ChatCompletionChoice> choices = service.createChatCompletion(completionRequest).getChoices();
                String fixedCode = choices.get(0).getMessage().getContent();

                while (fixedCode.charAt(fixedCode.length() - 1) == '\n') {
                    fixedCode = StringUtils.chomp(fixedCode);
                }

                List<String> srcLines = FileUtils.readLines(src, "UTF-8");

                if (fixedCode.contains(Constants.OPENAI_RESPONSE_SNIPPET_SEPARATOR)) {
                    fixedCode = extractCodeFromGPTResponse(fixedCode);
                }

                fixedCode = extractFixedMethod(fixedCode);

                if (fixedCodes.contains(fixedCode)) {
                    repeatedResponse = true;
                    continue;
                }

                fixedCodes.add(fixedCode);

                File backupOriginalSrc = Files.createTempFile("original_src", ".java").toFile();
                FileUtils.copyFile(src, backupOriginalSrc);

                srcLines.subList(vs.getStartLine() - 1, vs.getEndLine()).clear();
                srcLines.add(vs.getStartLine() - 1, fixedCode);
                FileUtils.writeLines(src, srcLines);

                String failureMessage = getMvnFailureMessage(root);

                boolean issuesRemain = failureMessage != null ? false : miner.containsViolation(root, vs, rule);

                if (failureMessage != null || issuesRemain) {
                    FileUtils.copyFile(backupOriginalSrc, src);

                    String userMessage;

                    if (failureMessage != null) {
                        userMessage = "The generated code causes the following error: " +
                                System.lineSeparator() + failureMessage + System.lineSeparator()
                                + "Generate another fixed version." + System.lineSeparator();
                    } else {
                        userMessage = "There are still some violations of rule " + rule + " in the code." +
                                System.lineSeparator() + "Generate another fixed version." + System.lineSeparator();
                    }

                    messages.add(new ChatMessage("assistant", fixedCode));
                    messages.add(new ChatMessage("user", userMessage));
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

    /**
     * Extracts the method from the GPT response.
     * The method is the first code snippet that is not an import statement and not the class definition.
     *
     * @param fixedCode the GPT response
     * @return the method extracted from the GPT response
     */
    private String extractFixedMethod(String fixedCode) {
        String[] lines = fixedCode.split("\r\n|\r|\n");
        List<String> extractedLines = new ArrayList<>();
        boolean isClassPassed = false, isFirstCurlyBracketPassed = false;
        for (String line : lines) {
            if (line.trim().startsWith("import ")) {
                continue;
            }
            if (line.contains("class ")) {
                isClassPassed = true;
            }
            if (line.contains("{")) {
                if (isClassPassed && !isFirstCurlyBracketPassed) {
                    isFirstCurlyBracketPassed = true;
                    continue;
                } else {
                    isFirstCurlyBracketPassed = true;
                }
            }
            if (isFirstCurlyBracketPassed) {
                extractedLines.add(line);
            }
        }
        if (isClassPassed) {
            while (extractedLines.size() > 0) {
                boolean shouldBreak = false;
                if (extractedLines.get(extractedLines.size() - 1).contains("}"))
                    shouldBreak = true;
                extractedLines.remove(extractedLines.size() - 1);

                if (shouldBreak)
                    break;
            }
        }
        return extractedLines.size() > 0 ? StringUtils.join(extractedLines, "\n") : fixedCode;
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

        for (failureLine = 0; failureLine < mvnOutputLines.size(); failureLine++) {
            if (mvnOutputLines.get(failureLine).startsWith("[ERROR] Errors:") ||
                    mvnOutputLines.get(failureLine).startsWith("[ERROR] Failures:") ||
                    mvnOutputLines.get(failureLine).contains("BUILD FAILURE") ||
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
                for (; failureLine < mvnOutputLines.size() && consideredLines < Constants.MAX_FEEDBACK_LINES; failureLine++) {
                    if (mvnOutputLines.get(failureLine).startsWith("[ERROR] Failed to execute goal")) {
                        String line = mvnOutputLines.get(failureLine);
                        line = line.substring("[ERROR] ".length());
                        errorMessage += line + System.lineSeparator();
                        consideredLines++;
                    }
                }
                break;
            }
        }

        return errorMessage;
    }

    @NotNull
    private static String extractCodeFromGPTResponse(String fixedCode) {
        List<String> fixedCodeSourceLines = new ArrayList<>();
        String[] fixedCodeLines = fixedCode.split("\r\n|\r|\n");

        boolean isSource = false;

        for (int i = fixedCodeLines.length - 1; i >= 0; i--) {
            if (fixedCodeLines[i].startsWith(Constants.OPENAI_RESPONSE_SNIPPET_SEPARATOR)) {
                if (isSource)
                    break;
                isSource = true;
                continue;
            }

            if (isSource) {
                fixedCodeSourceLines.add(0, fixedCodeLines[i]);
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

        boolean isFirstCurlyBracketPassed = false, isClassPassed = false;

        for (int i = 0; i <= endLine - 1; i++) {
            String currentLine = lines.get(i);
            if (i >= startLine - 1) {
                buggyCode += currentLine;
                if (buggyLines.contains(i + 1))
                    buggyCode += " // " + Constants.PROMPT_NONCOMPLIANT_KEYWORD;
                buggyCode += System.lineSeparator();
            } else {
                if (currentLine.contains("class ")) {
                    isClassPassed = true;
                }
                if (currentLine.trim().startsWith("import ")) {
                    buggyCode += currentLine + System.lineSeparator();
                } else if (isClassPassed && !isFirstCurlyBracketPassed) {
                    buggyCode += currentLine + System.lineSeparator();
                    if (currentLine.contains("{")) {
                        isFirstCurlyBracketPassed = true;
                    }
                }
            }
        }

        buggyCode += "}";

        return new SonarFixPrompt(rule, buggyCode, promptType);
    }
}
