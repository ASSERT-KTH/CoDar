package se.kth.assertgroup.codar.codex;

import org.apache.commons.io.FileUtils;
import se.kth.assertgroup.codar.Constants;

import java.io.File;
import java.io.IOException;

public class SonarFixPrompt {
    private String ruleKey;
    private String buggyCode;

    public SonarFixPrompt (String ruleKey, String buggyCode){
        this.ruleKey = ruleKey;
        this.buggyCode = buggyCode;
    }

    public String getPromptAsStr() throws IOException {
        String promptTemplate = FileUtils.readFileToString(new File(Constants.PROMPT_TEMPLATE_BASE + ruleKey),
                "UTF-8");
        return promptTemplate.replace(Constants.CODEX_PROMPT_BUGGY_CODE_PLACEHOLDER, buggyCode);
    }
}
