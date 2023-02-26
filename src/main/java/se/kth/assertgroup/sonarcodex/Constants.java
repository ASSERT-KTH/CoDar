package se.kth.assertgroup.sonarcodex;

import java.io.File;
import java.util.List;

public class Constants {
    public static final String PROMPT_TEMPLATE_BASE = "files/prompts/";
    public static final String CODEX_PROMPT_BUGGY_CODE_PLACEHOLDER = "{{BUGGY}}";
    public static final String CODEX_API_TOKEN_ENV_NAME = "CODEX_API_TOKEN";
    public static final String CODEX_NONCOMPLIANT_HEADER = "### Noncompliant";
    public static final String CODEX_COMPLIANT_HEADER = "### Compliant";
    public static final String CODEX_PROMPT_ENDING = CODEX_NONCOMPLIANT_HEADER + System.lineSeparator()
            + CODEX_PROMPT_BUGGY_CODE_PLACEHOLDER + System.lineSeparator()
            + CODEX_COMPLIANT_HEADER + System.lineSeparator();
    public static final int CODEX_TIMEOUT = 50;

    public static final String SONAR_DOC_URL_TEMPLATE = "https://rules.sonarsource.com/java/RSPEC-{{rule}}";
    public static final String SONAR_DOC_URL_RULE_PLACEHOLDER = "{{rule}}";

    public static final String TITLE_PROMPTS_DIR =
            PROMPT_TEMPLATE_BASE + "TITLE" + File.separatorChar;
    public static final String TITLE_DESCRIPTION_PROMPTS_DIR =
            PROMPT_TEMPLATE_BASE + "TITLE_DESCRIPTION" + File.separatorChar;
    public static final String TITLE_DESCRIPTION_Example_PROMPTS_DIR =
            PROMPT_TEMPLATE_BASE + "TITLE_DESCRIPTION_EXAMPLE" + File.separatorChar;
    public static final String FULL_DESCRIPTION_PROMPTS_DIR =
            PROMPT_TEMPLATE_BASE + "FULL_DESCRIPTION" + File.separatorChar;
    public static final String EXAMPLE_PROMPTS_DIR = PROMPT_TEMPLATE_BASE + "EXAMPLE" + File.separatorChar;
    public static final String ZERO_SHOT_PROMPTS_DIR = PROMPT_TEMPLATE_BASE + "ZERO_SHOT" + File.separatorChar;
}
