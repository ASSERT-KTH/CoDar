package se.kth.assertgroup.codar.codex;

import org.apache.commons.io.FileUtils;
import se.kth.assertgroup.codar.Constants;

import java.io.File;
import java.io.IOException;

public class SonarFixPrompt {
    private String ruleKey;
    private String buggyCode;
    private PromptType promptType = PromptType.TITLE_DESCRIPTION_EXAMPLE; // Default prompt type is set

    public SonarFixPrompt (String ruleKey, String buggyCode){
        this.ruleKey = ruleKey;
        this.buggyCode = buggyCode;
    }

    public SonarFixPrompt (String ruleKey, String buggyCode, PromptType promptType){
        this.ruleKey = ruleKey;
        this.buggyCode = buggyCode;
        this.promptType = promptType;
    }

    public String getPromptAsStr() throws IOException {
        String promptTemplate = FileUtils.readFileToString(new File(Constants.PROMPT_TEMPLATE_BASE +
                        promptType.toString() + File.separator + ruleKey),
                "UTF-8");
        return promptTemplate.replace(Constants.PROMPT_BUGGY_CODE_PLACEHOLDER, buggyCode);
    }

    public String getBuggyCode(){
        return buggyCode;
    }
}
