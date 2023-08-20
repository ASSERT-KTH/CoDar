package se.kth.assertgroup.codar.sonar;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import se.kth.assertgroup.codar.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SonarDocumentInfo {
    private String rule, title, fullDescription;
    private List<ViolationExample> examples;
    private List<Integer> nonCompliantIndices, compliantIndices;

    public SonarDocumentInfo(){
        this.examples = new ArrayList<>();
    }

    public void cleanDescriptions() {

        // Replacing example labels
        fullDescription = fullDescription.replaceAll("Noncompliant Code Example ",
                System.lineSeparator() + "Noncompliant Code Example" + System.lineSeparator())
                .replaceAll("Compliant Solution ",
                        System.lineSeparator() + "Compliant Solution" + System.lineSeparator())
                .replaceAll("Noncompliant code example ",
                        System.lineSeparator() + "Noncompliant Code Example" + System.lineSeparator())
                .replaceAll("Compliant solution ",
                        System.lineSeparator() + "Compliant Solution" + System.lineSeparator());

        // Replacing example labels
        fullDescription = fullDescription.replaceAll("Noncompliant Code Example", Constants.PROMPT_NONCOMPLIANT_HEADER)
                .replaceAll("Compliant Solution", Constants.PROMPT_COMPLIANT_HEADER);
    }

    public void removeCompliantCommentsFromDescription(){
        fullDescription = removeCompliantComment(fullDescription);
    }

    public void addExamplesToDocInfo(Document jsoupDoc) {
        Elements snippets = jsoupDoc.getElementsByTag("pre");
        Element firstSnippetAfterNonCompliant = null, firstSnippetAfterCompliant = null;

        for (int i = 0; i < snippets.size(); i++) {
            String txt = snippets.get(i).text();
            if(fullDescription.indexOf(txt) >= nonCompliantIndices.get(0)
                    && (firstSnippetAfterNonCompliant == null
                    || fullDescription.indexOf(txt) <= fullDescription.indexOf(firstSnippetAfterNonCompliant.text())))
                firstSnippetAfterNonCompliant = snippets.get(i);

            if(fullDescription.indexOf(txt) >= compliantIndices.get(0)
                    && (firstSnippetAfterCompliant == null
                    || fullDescription.indexOf(txt) <= fullDescription.indexOf(firstSnippetAfterCompliant.text())))
                firstSnippetAfterCompliant = snippets.get(i);
        }

        SonarDocumentInfo.ViolationExample ex = new SonarDocumentInfo.ViolationExample();
        ex.setNonCompliant(removeCompliantComment(firstSnippetAfterNonCompliant.text()));
        ex.setCompliant(removeCompliantComment(firstSnippetAfterCompliant.text()));
        examples.add(ex);
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
            ex.setCompliant(removeCompliantComment(rawCompliant));
        }
    }

    @NotNull
    private static String removeCompliantComment(String rawCompliant) {
        String[] lines = rawCompliant.split(System.lineSeparator());
        for (int i = 0; i < lines.length; i++) {
            if(lines[i].contains("//") && lines[i].contains("Compliant")
                    && lines[i].indexOf("//") < lines[i].indexOf("Compliant"))
            lines[i] = lines[i].substring(0, lines[i].indexOf("//"));
        }
        rawCompliant = Arrays.asList(lines).stream().collect(Collectors.joining(System.lineSeparator()));
        return rawCompliant;
    }

    public void moveToUnifiedFullDescriptionFormat() {
        this.fullDescription = this.fullDescription.replaceAll("Sensitive Code Example",
                "Noncompliant Code Example");
    }

    public List<Integer> getNonCompliantIndices() {
        return nonCompliantIndices;
    }

    public void setNonCompliantIndices(List<Integer> nonCompliantIndices) {
        this.nonCompliantIndices = nonCompliantIndices;
    }

    public List<Integer> getCompliantIndices() {
        return compliantIndices;
    }

    public void setCompliantIndices(List<Integer> compliantIndices) {
        this.compliantIndices = compliantIndices;
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

    public void updateExampleIndices() {
        nonCompliantIndices = findWord(fullDescription, "Noncompliant Code Example ");
        compliantIndices = findWord(fullDescription, "Compliant Solution ");
    }

    public boolean includesProperExample() {
        return nonCompliantIndices.size() > 0 && compliantIndices.size() > 0;
    }

    private List<Integer> findWord(String textString, String word) {
        List<Integer> indexes = new ArrayList<Integer>();
        String lowerCaseTextString = textString.toLowerCase();
        String lowerCaseWord = word.toLowerCase();

        int index = 0;
        while (index != -1) {
            index = lowerCaseTextString.indexOf(lowerCaseWord, index);
            if (index != -1) {
                indexes.add(index);
                index++;
            }
        }
        return indexes;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }
}
