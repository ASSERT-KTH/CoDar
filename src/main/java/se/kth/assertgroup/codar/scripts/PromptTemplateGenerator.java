package se.kth.assertgroup.codar.scripts;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import se.kth.assertgroup.codar.Constants;
import se.kth.assertgroup.codar.sonar.SonarDocumentInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PromptTemplateGenerator {

    public static final String RESOURCE_PATH = "/home/khaes/phd/projects/sonar-codex/src/main/resources/";

    public void generatePromptTemplates(String rule) throws IOException, URISyntaxException {
        String url = Constants.SONAR_DOC_URL_TEMPLATE.replace(Constants.SONAR_DOC_URL_RULE_PLACEHOLDER,
                rule.substring(1)); // remove S at the beginning of the rule
        Document doc = Jsoup.connect(url).get();

        SonarDocumentInfo docInfo = new SonarDocumentInfo();
        docInfo.setRule(rule);

        docInfo.setTitle(getRuleDocTitle(doc));

        docInfo.setFullDescription(extractFullDescription(doc));

        docInfo.moveToUnifiedFullDescriptionFormat();

        docInfo.updateExampleIndices();

        if (!docInfo.includesProperExample())
            throw new IllegalArgumentException("Documentation doesn't have correct examples");

        docInfo.cleanDescriptions();

        docInfo.addExamplesToDocInfo(doc);

        docInfo.removeCompliantCommentsFromDescription();

        docInfo.cleanData();

        printPromptTemplates(docInfo);
    }

    private void printPromptTemplates(SonarDocumentInfo docInfo) throws IOException, URISyntaxException {
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

        writeToResource(Constants.FULL_DESCRIPTION_PROMPTS_DIR + docInfo.getRule(), prompt);
    }

    private void printTitleDescriptionExamplePromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = docInfo.getTitle() + System.lineSeparator() + docInfo.getDescriptionWithNoEx()
                + System.lineSeparator() + getPromptExampleSection(docInfo);

        writeToResource(Constants.TITLE_DESCRIPTION_Example_PROMPTS_DIR + docInfo.getRule(), prompt);
    }

    private void printTitleDescriptionPromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = docInfo.getTitle() + System.lineSeparator() + docInfo.getDescriptionWithNoEx()
                + System.lineSeparator() + Constants.PROMPT_ENDING;

        writeToResource(Constants.TITLE_DESCRIPTION_PROMPTS_DIR + docInfo.getRule(), prompt);
    }

    private void printTitlePromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = docInfo.getTitle() + System.lineSeparator()
                + Constants.PROMPT_ENDING;

        writeToResource(Constants.TITLE_PROMPTS_DIR + docInfo.getRule(), prompt);
    }

    private void printExamplePromptTemplate(SonarDocumentInfo docInfo) throws IOException {
        String prompt = getPromptExampleSection(docInfo);

        writeToResource(Constants.EXAMPLE_PROMPTS_DIR + docInfo.getRule(), prompt);
    }

    private void writeToResource(String rscPath, String text) throws IOException{
        FileUtils.write(new File(RESOURCE_PATH + rscPath), text,
                "UTF-8");
    }

    private String getPromptExampleSection(SonarDocumentInfo docInfo) {
        return Constants.PROMPT_NONCOMPLIANT_HEADER + System.lineSeparator()
                + docInfo.getExamples().get(0).getNonCompliant() + System.lineSeparator()
                +  Constants.PROMPT_COMPLIANT_HEADER + System.lineSeparator()
                + docInfo.getExamples().get(0).getCompliant() + System.lineSeparator()
                + Constants.PROMPT_ENDING;
    }

    private void printZeroShotPromptTemplate(SonarDocumentInfo docInfo) throws IOException, URISyntaxException {
        writeToResource(Constants.ZERO_SHOT_PROMPTS_DIR + docInfo.getRule(), Constants.PROMPT_ENDING);
    }

    private String extractFullDescription(Document doc){
        Element descriptionSection = doc.getElementsByTag("section").get(2);
        String description = descriptionSection.text();
        return description;
    }

    private String getRuleDocTitle(Document doc) throws IOException {
        Element headerSection = doc.getElementsByTag("section").get(0);
        Element header = headerSection.getElementsByTag("h1").get(0);
        return header.text();
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        PromptTemplateGenerator ptg = new PromptTemplateGenerator();

        List<String> rules = ptg.fetchListOfRules();

//        ptg.generatePromptTemplates("S4682");
//
//        List<String> rules =
//                FileUtils.readLines(new File("/home/khaes/tmp/rules.txt"), "UTF-8");
//
        rules.forEach(r -> {
            try {
                System.out.println("Check for: " + r);
                ptg.generatePromptTemplates(r);
            } catch (Exception e) {
                System.out.println("Didn't work for " + r);
            }
        });
    }

    private List<String> fetchListOfRules() throws IOException {
        String url = "https://rules.sonarsource.com/java/";
        Document doc = Jsoup.connect(url).get();

        Elements rules = doc.getElementsByClass("RulesListstyles__StyledLi-sc-6thbbv-1");
        return rules.stream().collect(ArrayList::new,
                (list, rule) -> list.add("S" +
                        rule.getElementsByTag("a").get(0)
                                .attr("href").split("-")[1].split("/")[0]),
                ArrayList::addAll);
    }
}
