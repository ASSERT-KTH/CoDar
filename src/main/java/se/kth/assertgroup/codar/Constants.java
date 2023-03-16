package se.kth.assertgroup.codar;

import java.io.File;

public class Constants {
    public static final String CODAR_COMMAND_NAME = "CoDar";

    public static final String ARG_ROOT_DIR = "--root";
    public static final String ARG_MINE_RES = "--mine-res";
    public static final String ARG_RULE = "--rule";
    public static final String ARG_PROMPT_TYPE = "--prompt-type";

    public static final String PROMPT_TEMPLATE_BASE = "files/prompts/";
    public static final String CODEX_PROMPT_BUGGY_CODE_PLACEHOLDER = "{{BUGGY}}";
    public static final String CODEX_API_TOKEN_ENV_NAME = "CODEX_API_TOKEN";
    public static final String CODEX_NONCOMPLIANT_KEYWORD = "Noncompliant";
    public static final String CODEX_COMPLIANT_KEYWORD = "Compliant";
    public static final String CODEX_NONCOMPLIANT_HEADER = "### " + CODEX_NONCOMPLIANT_KEYWORD;
    public static final String CODEX_COMPLIANT_HEADER = "### " + CODEX_COMPLIANT_KEYWORD;
    public static final String CODEX_PROMPT_ENDING = CODEX_NONCOMPLIANT_HEADER + System.lineSeparator()
            + CODEX_PROMPT_BUGGY_CODE_PLACEHOLDER + System.lineSeparator()
            + CODEX_COMPLIANT_HEADER + System.lineSeparator();
    public static final int CODEX_TIMEOUT = 50;
    public static final int CODEX_MAX_TOKENS = 2048;
    public static final String CODEX_MODEL = "code-cushman-001";
//    public static final String CODEX_MODEL = "code-davinci-002";

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
