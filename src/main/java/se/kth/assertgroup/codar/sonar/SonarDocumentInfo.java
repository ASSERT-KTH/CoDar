package se.kth.assertgroup.codar.sonar;

import se.kth.assertgroup.codar.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SonarDocumentInfo {
    private String rule, title, fullDescription;
    private List<ViolationExample> examples;

    public SonarDocumentInfo(){
        this.examples = new ArrayList<>();
    }

    public void cleanDescriptions() {
        boolean isCode = false, isCompliant = false;

        // Replacing example labels
        fullDescription = fullDescription.replaceAll("Noncompliant Code Example ",
                System.lineSeparator() + "Noncompliant Code Example" + System.lineSeparator())
                .replaceAll("Compliant Solution ",
                        System.lineSeparator() + "Compliant Solution" + System.lineSeparator());

        String[] desLines = fullDescription.split(System.lineSeparator());

        for (int i = 0; i < desLines.length; i++) {
            if (desLines[i].contains("Noncompliant Code Example")) {
                isCode = true;
                isCompliant = false;
            }
            if (desLines[i].contains("Compliant Solution")) {
                isCompliant = true;
            }

            if (isCode && !isCompliant && desLines[i].contains("//") && desLines[i].contains("Noncompliant")
                    && desLines[i].indexOf("//") < desLines[i].indexOf("Noncompliant"))
                desLines[i] = desLines[i].substring(0, desLines[i].indexOf("//"));
        }

        Arrays.asList(desLines).stream().collect(Collectors.joining(System.lineSeparator()));

        // Replacing example labels
        fullDescription = fullDescription.replaceAll("Noncompliant Code Example", Constants.PROMPT_NONCOMPLIANT_HEADER)
                .replaceAll("Compliant Solution", Constants.PROMPT_COMPLIANT_HEADER);
    }

    public List<ViolationExample> getExamples() {
        return examples;
    }

    public void setExamples(List<ViolationExample> examples) {
        this.examples = examples;
    }

    public void cleanData(){
        cleanExamples();
    }

    public String getDescriptionWithNoEx(){
        return fullDescription.substring(0, fullDescription.indexOf(System.lineSeparator() + Constants.PROMPT_NONCOMPLIANT_HEADER));
    }

    private void cleanExamples(){
        for(ViolationExample ex : examples){
            String rawCompliant = ex.getCompliant();
            String[] lines = rawCompliant.split(System.lineSeparator());
            for (int i = 0; i < lines.length; i++) {
                if(lines[i].contains("//") && lines[i].contains("Compliant")
                        && lines[i].indexOf("//") < lines[i].indexOf("Compliant"))
                lines[i] = lines[i].substring(0, lines[i].indexOf("//"));
            }
            rawCompliant = Arrays.asList(lines).stream().collect(Collectors.joining(System.lineSeparator()));
            ex.setCompliant(rawCompliant);
        }
    }

    public void moveToUnifiedFullDescriptionFormat() {
        this.fullDescription = this.fullDescription.replaceAll("Sensitive Code Example",
                "Noncompliant Code Example");
    }

    public static class ViolationExample {
        private String nonCompliant, compliant;

        public String getNonCompliant() {
            return nonCompliant;
        }

        public void setNonCompliant(String nonCompliant) {
            this.nonCompliant = nonCompliant;
        }

        public String getCompliant() {
            return compliant;
        }

        public void setCompliant(String compliant) {
            this.compliant = compliant;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }
}
