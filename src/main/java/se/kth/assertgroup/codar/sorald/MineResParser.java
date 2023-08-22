package se.kth.assertgroup.codar.sorald;

import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import se.kth.assertgroup.codar.sorald.models.ViolationScope;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MineResParser {
    public Map<ViolationScope, Map<String, Set<Integer>>> getCodeScopeToViolations
            (
                    File srcRoot,
                    File mineResFile
            )
            throws IOException, ParseException {
        Map<String, Map<String, Set<Integer>>> ruleToViolations = getRuleToViolations(mineResFile);

        final Map<String, Set<Pair<Integer, Integer>>> fileToScopes =
                getRelevantFileScopesForAllRules(srcRoot, ruleToViolations);

        final Map<ViolationScope, Map<String, Set<Integer>>> res = new HashMap<>();

        ruleToViolations.forEach((rule, fileToLines) -> {
            fileToLines.forEach((file, lines) -> {
                lines.forEach(l -> {
                    Pair<Integer, Integer> coveringScope = findCoveringScope(l, fileToScopes.get(file));

                    if (coveringScope == null) {
                        return;
                    }

                    ViolationScope vs = new ViolationScope(file, coveringScope.getLeft(), coveringScope.getRight());

                    Map<String, Set<Integer>> ruleToLines = res.getOrDefault(vs, new HashMap<>());

                    Set<Integer> curLines = ruleToLines.getOrDefault(rule, new HashSet<>());
                    curLines.add(l);

                    ruleToLines.put(rule, curLines);

                    res.put(vs, ruleToLines);
                });
            });
        });

        return res;
    }

    public Map<String, Map<ViolationScope, Set<Integer>>> getRuleToScopeViolations
            (
                    File srcRoot,
                    File mineResFile
            ) throws IOException, ParseException {
        Map<ViolationScope, Map<String, Set<Integer>>> scopeToViolations =
                getCodeScopeToViolations(srcRoot, mineResFile);

        Map<String, Map<ViolationScope, Set<Integer>>> res = new HashMap<>();

        scopeToViolations.forEach((scope, ruleToLines) -> {
            ruleToLines.forEach((rule, lines) -> {
                ViolationScope newScope =
                        new ViolationScope(scope.getSrcPath(), scope.getStartLine(), scope.getEndLine());
                Map<ViolationScope, Set<Integer>> ruleToViolations = res.getOrDefault(rule, new HashMap<>());

                Set<Integer> curScopeToViolations = ruleToViolations.getOrDefault(newScope, new HashSet<>());

                curScopeToViolations.addAll(lines);

                ruleToViolations.put(newScope, curScopeToViolations);

                res.put(rule, ruleToViolations);
            });
        });

        return res;
    }

    private Map<String, Set<Pair<Integer, Integer>>> getRelevantFileScopesForAllRules
            (
                    File srcRoot,
                    Map<String, Map<String, Set<Integer>>> ruleToViolations
            ) {
        final Map<String, Set<Pair<Integer, Integer>>> fileToScopes = new HashMap<>();


        final ViolationScopeFinder scopeFinder = new ViolationScopeFinder();
        ruleToViolations.forEach((rule, fileToLines) -> {
            fileToScopes.putAll(getRelevantFileScopes(srcRoot, fileToLines));
        });
        return fileToScopes;
    }

    private Map<String, Set<Pair<Integer, Integer>>> getRelevantFileScopes
            (
                    File srcRoot,
                    Map<String, Set<Integer>> fileToViolationLInes
            ) {
        final Map<String, Set<Pair<Integer, Integer>>> fileToScopes = new HashMap<>();

        final ViolationScopeFinder scopeFinder = new ViolationScopeFinder();
        fileToViolationLInes.forEach((file, lines) -> {
            if (!fileToScopes.containsKey(file)) {
                fileToScopes.put(file, scopeFinder.extractScopes(srcRoot.getPath() + File.separatorChar + file));
            }
        });
        return fileToScopes;
    }

    public Map<ViolationScope, Set<Integer>> getCodeScopeToViolations
            (
                    File srcRoot,
                    File mineResFile,
                    String rule
            )
            throws IOException, ParseException {
        Map<String, Set<Integer>> fileToViolationLines = getRuleToViolations(mineResFile).get(rule);

        if (fileToViolationLines == null)
            return new HashMap<>();

        final Map<String, Set<Pair<Integer, Integer>>> fileToScopes =
                getRelevantFileScopes(srcRoot, fileToViolationLines);

        final Map<ViolationScope, Set<Integer>> res = new HashMap<>();

        fileToViolationLines.forEach((file, lines) -> {
            lines.forEach(l -> {
                Pair<Integer, Integer> coveringScope = findCoveringScope(l, fileToScopes.get(file));

                if (coveringScope == null) {
                    return;
                }

                ViolationScope vs = new ViolationScope(file, coveringScope.getLeft(), coveringScope.getRight());
                Set<Integer> curLines = res.getOrDefault(vs, new HashSet<>());
                curLines.add(l);

                res.put(vs, curLines);
            });
        });

        return res;
    }

    public long countViolations(File srcRoot, ViolationScope targetScope, String rule, File mineResFile)
            throws IOException, ParseException {
        Map<ViolationScope, Set<Integer>> scopesToViolations = getCodeScopeToViolations(srcRoot, mineResFile, rule);
        Optional<ViolationScope> intersectingBuggyScope =
                scopesToViolations.keySet().stream().filter(sc -> targetScope.getStartLine().equals(sc.getStartLine()))
                        .findFirst();

        return intersectingBuggyScope.isEmpty() ? 0L : scopesToViolations.get(intersectingBuggyScope.get()).size();
    }

    private Pair<Integer, Integer> findCoveringScope(Integer targetLine, Set<Pair<Integer, Integer>> scopes) {
        Optional<Pair<Integer, Integer>> targetScope =
                scopes.stream().filter(s -> s.getKey() <= targetLine && s.getValue() >= targetLine).findFirst();
        if (!targetScope.isEmpty()) {
            return targetScope.get();
        } else {
            return null;
        }
    }

    // Returns a map from rule to a map of relative-file-path and violation lines set
    private Map<String, Map<String, Set<Integer>>> getRuleToViolations(File mineResFile)
            throws IOException, ParseException {
        Map<String, Map<String, Set<Integer>>> res = new HashMap<>();

        JSONParser parser = new JSONParser();
        JSONArray ja = (JSONArray) ((JSONObject) parser.parse(new FileReader(mineResFile))).get("minedRules");

        for (int i = 0; i < ja.size(); i++) {
            JSONObject jo = (JSONObject) ja.get(i);
            String rule = jo.get("ruleKey").toString();
            Map<String, Set<Integer>> filePathToViolationLines = new HashMap<>();

            JSONArray warnings = (JSONArray) jo.get("warningLocations");
            for (int j = 0; j < warnings.size(); j++) {
                JSONObject warning = (JSONObject) warnings.get(j);
                String filePath = warning.get("filePath").toString();

                if (isNotSourceDir(filePath)) {
                    continue;
                }

                int startLine = Integer.parseInt(warning.get("startLine").toString());

                if (!filePathToViolationLines.containsKey(filePath)) {
                    filePathToViolationLines.put(filePath, new HashSet<>());
                }
                filePathToViolationLines.get(filePath).add(startLine);
            }

            res.put(rule, filePathToViolationLines);
        }

        return res;
    }

    private boolean isNotSourceDir(String filePath) {
        return filePath.contains("src/test/java") || filePath.contains("/target/") || filePath.startsWith("target/");
    }

    public static void main(String[] args) throws IOException, ParseException {
        Map<ViolationScope, Map<String, Set<Integer>>> ruleToViolations =
                new MineResParser().getCodeScopeToViolations(
                        new File("/home/khaes/tmp/sds/"),
                        new File("/home/khaes/tmp/mine-sds.json"));
    }

}
