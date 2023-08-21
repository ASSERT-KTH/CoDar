package se.kth.assertgroup.codar.scripts;

import java.io.File;

public class ExtractHandledRules {
    public static void main(String[] args) {
        File promptsDir = new File(PromptTemplateGenerator.RESOURCE_PATH + "/repair_prompts");
        File[] promptTypes = promptsDir.listFiles();
        for (File promptType : promptTypes) {
            if (promptType.isDirectory()) {
                File[] rules = promptType.listFiles();
                for (File rule : rules) {
                    if (rule.isFile()) {
                        System.out.println(promptType.getName() + "," + rule.getName().split("\\.")[0]);
                    }
                }
            }
        }
    }
}
