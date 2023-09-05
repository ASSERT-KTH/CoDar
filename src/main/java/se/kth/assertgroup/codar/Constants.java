package se.kth.assertgroup.codar;

import java.io.File;

public class Constants {
    public static final String CODAR_COMMAND_NAME = "CoDar";

    public static final String ARG_ROOT_DIR = "--root";
    public static final String ARG_MINE_RES = "--mine-res";
    public static final String ARG_RULE = "--rule";
    public static final String ARG_PROMPT_TYPE = "--prompt-type";
    public static final String ARG_FIX_SCALE = "--fix-scale";

    public static final String PROMPT_TEMPLATE_BASE = "repair_prompts/";
    public static final String PROMPT_SETUP_PATH = "other_prompts/repair_setup_prompt";
    public static final String PROMPT_BUGGY_CODE_PLACEHOLDER = "{{BUGGY}}";
    public static final String OPENAI_API_TOKEN_ENV_NAME = "CODEX_API_TOKEN";
    public static final String PROMPT_NONCOMPLIANT_KEYWORD = "Noncompliant";
    public static final String PROMPT_COMPLIANT_KEYWORD = "Compliant";
    public static final String PROMPT_NONCOMPLIANT_HEADER = "### " + PROMPT_NONCOMPLIANT_KEYWORD;
    public static final String PROMPT_COMPLIANT_HEADER = "### " + PROMPT_COMPLIANT_KEYWORD;
    public static final String PROMPT_ENDING = PROMPT_NONCOMPLIANT_HEADER + System.lineSeparator()
            + PROMPT_BUGGY_CODE_PLACEHOLDER + System.lineSeparator()
            + PROMPT_COMPLIANT_HEADER + System.lineSeparator();
    public static final String PROMPT_SETUP_RULE_PLACEHOLDER = "{{RULE}}";
    public static final int MAX_CONVERSATION_LENGTH = 3;
    public static final String COMPILE_CHECK_PROMPT_TEMPLATE_PATH =
            "files/other_prompts/compilability_check_prompt";
    public static final String COMPILABILITY_PROMPT_TEXT_PLACEHOLDER = "{{TEXT}}";
    public static final String COMPILABILITY_POSITIVE_RES = "Option 1";
    public static final String COMPILABILITY_NEGATIVE_RES = "Option 2";
    public static final int OPENAI_REQUEST_TIMEOUT = 50;
    public static final double OPENAI_LOW_TEMPERATURE = 0.0;
    public static final double OPENAI_MAX_TEMPERATURE = 1.0;
    public static final double TEMPERATURE_INCREASE_STEP = 0.3;
    public static final int OPENAI_REQUEST_MAX_TOKENS = 2048;
    public static final String OPENAI_RESPONSE_SNIPPET_SEPARATOR = "```";
    public static final String CODEX_MODEL = "code-cushman-001";
    public static final String TURBO_MODEL = "gpt-3.5-turbo";
    public static final String GPT4_MODEL = "gpt-4";
//    public static final String CODEX_MODEL = "code-davinci-002";

    public static final int MAX_FEEDBACK_LINES = 30;

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
    public static final String SORALD_PATH_ENV_NAME = "SORALD_JAR_PATH";
}
