package se.kth.assertgroup.codar.sorald;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import se.kth.assertgroup.codar.repair.FixScale;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ViolationScopeFinder {
//    public Map<Pair<Integer, Integer>, Map<String, Set<WarningLocation>>> getViolationScope(WarningLocation location) {
//        extractScopes();
//        Launcher launcher = new Launcher();
//        launcher.getEnvironment().setCommentEnabled(true);
//        launcher.addInputResource();
//        launcher.buildModel();
//        CtModel model;
//    }

    public Set<Pair<Integer, Integer>> extractScopes(String srcPath, FixScale fixScale) {
        if(fixScale.equals(FixScale.FILE)){
            try {
                return new HashSet<>(Arrays.asList(Pair.of(0, FileUtils.readLines(new File(srcPath)).size())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Launcher launcher = new Launcher();
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.addInputResource(srcPath);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        ElementScopeProcessor scopeProcessor =
                new ElementScopeProcessor(new HashSet<>(), Arrays.asList(CtMethod.class, CtStatement.class, CtField.class));
        model.processWith(scopeProcessor);

        return scopeProcessor.getOuterScopes();
    }

    public static void main(String[] args) throws IOException {
        Set<Pair<Integer, Integer>> scopes = new ViolationScopeFinder()
                .extractScopes("/home/khaes/tmp/missinglink/core/src/main/java/com/spotify/missinglink/datamodel/AccessedField.java",
                        FixScale.FILE);
    }
}
