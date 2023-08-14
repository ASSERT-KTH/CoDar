package se.kth.assertgroup.codar.scripts;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import se.kth.assertgroup.codar.Constants;
import se.kth.assertgroup.codar.sonar.SonarDocumentInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PromptTemplateGenerator {
    public void generatePromptTemplates(String rule) throws IOException {
        String url = Constants.SONAR_DOC_URL_TEMPLATE.replace(Constants.SONAR_DOC_URL_RULE_PLACEHOLDER,
                rule.substring(1)); // remove S at the beginning of the rule
        Document doc = Jsoup.connect(url).get();

        SonarDocumentInfo docInfo = new SonarDocumentInfo();
        docInfo.setRule(rule);

        docInfo.setTitle(getRuleDocTitle(doc));

        docInfo.setFullDescription(extractFullDescription(doc));

        docInfo.moveToUnifiedFullDescriptionFormat();

        if (!areExamplesCorrectlyOrdered(doc, docInfo.getFullDescription()))
            throw new IllegalArgumentException("Documentation doesn't have correct examples");

        docInfo.moveToUnifiedFullDescriptionFormat();

        docInfo.cleanDescriptions();

        addExamplesToDocInfo(doc, docInfo);

        docInfo.cleanData();

        printPromptTemplates(docInfo);
    }

    private void printPromptTemplates(SonarDocumentInfo docInfo) throws IOException {
        printZeroShotPromptTemplate(docInfo);
        printExamplePromptTemplate(docInfo);
        printTitlePromptTemplate(docInfo);
        printTitleDescriptionPromptTemplate(docInfo);
        printTitleDescriptionExamplePromptTemplate(docInfo);
        printFullDescriptionPromptTemplate(docInfo);
    }

    private void printFullDescriptionPromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = docInfo.getTitle() + System.lineSeparator()
                + docInfo.getFullDescription() + System.lineSeparator() + Constants.PROMPT_ENDING;

        FileUtils.write(new File(Constants.FULL_DESCRIPTION_PROMPTS_DIR + docInfo.getRule()),
                prompt, "UTF-8");
    }

    private void printTitleDescriptionExamplePromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = docInfo.getTitle() + System.lineSeparator() + docInfo.getDescriptionWithNoEx()
                + System.lineSeparator() + getPromptExampleSection(docInfo);

        FileUtils.write(new File(Constants.TITLE_DESCRIPTION_Example_PROMPTS_DIR + docInfo.getRule()),
                prompt, "UTF-8");
    }

    private void printTitleDescriptionPromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = docInfo.getTitle() + System.lineSeparator() + docInfo.getDescriptionWithNoEx()
                + System.lineSeparator() + Constants.PROMPT_ENDING;

        FileUtils.write(new File(Constants.TITLE_DESCRIPTION_PROMPTS_DIR + docInfo.getRule()),
                prompt, "UTF-8");
    }

    private void printTitlePromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = docInfo.getTitle() + System.lineSeparator()
                + Constants.PROMPT_ENDING;

        FileUtils.write(new File(Constants.TITLE_PROMPTS_DIR + docInfo.getRule()),
                prompt, "UTF-8");
    }

    private void printExamplePromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = getPromptExampleSection(docInfo);

        FileUtils.write(new File(Constants.EXAMPLE_PROMPTS_DIR + docInfo.getRule()),
                prompt, "UTF-8");
    }

    private String getPromptExampleSection(SonarDocumentInfo docInfo) {
        return Constants.PROMPT_NONCOMPLIANT_HEADER + System.lineSeparator()
                + docInfo.getExamples().get(0).getNonCompliant() + System.lineSeparator()
                +  Constants.PROMPT_COMPLIANT_HEADER + System.lineSeparator()
                + docInfo.getExamples().get(0).getCompliant() + System.lineSeparator()
                + Constants.PROMPT_ENDING;
    }

    private void printZeroShotPromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        FileUtils.write(new File(Constants.ZERO_SHOT_PROMPTS_DIR + docInfo.getRule()),
                Constants.PROMPT_ENDING, "UTF-8");
    }

    private void addExamplesToDocInfo(Document jsoupDoc, SonarDocumentInfo docInfo) {
        Elements snippets = jsoupDoc.getElementsByTag("pre");
        for (int i = 0; i < snippets.size(); i += 2) {
            SonarDocumentInfo.ViolationExample ex = new SonarDocumentInfo.ViolationExample();
            ex.setNonCompliant(snippets.get(i).text());
            ex.setCompliant(snippets.get(i + 1).text());
            docInfo.getExamples().add(ex);
        }
    }

    private String extractFullDescription(Document doc){
        Element descriptionSection = doc.getElementsByTag("section").get(2);
        String description = descriptionSection.text();
        return description;
    }

    private boolean areExamplesCorrectlyOrdered(Document doc, String description) {
        List<Integer> nonCompliantIndices = findWord(description, "Noncompliant Code Example "),
                compliantIndices = findWord(description, "Compliant Solution ");

        if (nonCompliantIndices.size() != compliantIndices.size())
            return false;

        for (int i = 1; i < nonCompliantIndices.size(); i++) {
            if (nonCompliantIndices.get(i) > compliantIndices.get(i)
                    || nonCompliantIndices.get(i) < compliantIndices.get(i - 1))
                return false;
        }

        if (nonCompliantIndices.get(0) > compliantIndices.get(0))
            return false;

        if (doc.getElementsByTag("pre").size() != nonCompliantIndices.size() * 2
                || nonCompliantIndices.size() != 1) // only consider rules with one example
            return false;

        return true;
    }

    private String getRuleDocTitle(Document doc) throws IOException {
        Element headerSection = doc.getElementsByTag("section").get(0);
        Element header = headerSection.getElementsByTag("h1").get(0);
        return header.text();
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

    public static void main(String[] args) throws IOException {
        PromptTemplateGenerator ptg = new PromptTemplateGenerator();

//        ptg.generatePromptTemplates("S5042");

        List<String> rules =
                FileUtils.readLines(new File("/home/khaes/tmp/rules.txt"), "UTF-8");

        rules.forEach(r -> {
            try {
                System.out.println("Check for: " + r);
                ptg.generatePromptTemplates(r);
            } catch (Exception e) {
                System.out.println("Didn't work for " + r);
            }
        });
    }
}
