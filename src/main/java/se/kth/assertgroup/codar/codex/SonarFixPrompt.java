package se.kth.assertgroup.codar.codex;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import se.kth.assertgroup.codar.Constants;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

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
        String promptSetupStr = IOUtils.toString(SonarFixPrompt.class.getClassLoader()
                        .getResourceAsStream(Constants.PROMPT_SETUP_PATH), "UTF-8")
                .replace(Constants.PROMPT_SETUP_RULE_PLACEHOLDER, ruleKey);
        String repairPrompt = promptSetupStr +
                IOUtils.toString(SonarFixPrompt.class.getClassLoader()
                                .getResourceAsStream(Constants.PROMPT_TEMPLATE_BASE + promptType.toString()
                                        + File.separator + ruleKey),
                                "UTF-8")
                        .replace(Constants.PROMPT_BUGGY_CODE_PLACEHOLDER, buggyCode);
        return repairPrompt;
    }

    public String getBuggyCode(){
        return buggyCode;
    }

    public static void main(String[] args) throws IOException {
        String promptSetupStr = IOUtils.toString(SonarFixPrompt.class.getClassLoader()
                        .getResourceAsStream(Constants.PROMPT_SETUP_PATH), "UTF-8")
                .replace(Constants.PROMPT_SETUP_RULE_PLACEHOLDER, "S1132");
    }
}
