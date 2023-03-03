package se.kth.assertgroup.codar.sorald;

import org.apache.commons.lang3.tuple.Pair;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtMethod;

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

    private Set<Pair<Integer, Integer>> extractScopes(String srcPath){
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.addInputResource(srcPath);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        ElementScopeProcessor scopeProcessor =
                new ElementScopeProcessor(new HashSet<>(), Arrays.asList(CtMethod.class, CtStatement.class));
        model.processWith(scopeProcessor);

        return scopeProcessor.getOuterScopes();
    }

    public static void main(String[] args) {
        Set<Pair<Integer, Integer>> scopes = new ViolationScopeFinder()
                .extractScopes("/home/khaes/tmp/sds/sds-admin/src/main/java/com/didiglobal/sds/admin/service/impl/HeartbeatServiceImpl.java");
    }
}
