package se.kth.assertgroup.codar.sorald;

import org.apache.commons.lang3.tuple.Pair;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ElementScopeProcessor extends AbstractProcessor<CtElement> {
    private Set<Pair<Integer, Integer>> outerScopes;
    List<Class> classes;

    public ElementScopeProcessor(Set<Pair<Integer, Integer>> outerScopes, List<Class> classes) {
        this.outerScopes = outerScopes;
        this.classes = classes;
    }

    @Override
    public void process(CtElement element) {
        if(classes.stream().noneMatch(c -> c.isInstance(element))
                || element instanceof CtClass || element instanceof CtJavaDoc)
            return;

        if (!element.getPosition().isValidPosition())
            return;

        int startLine = element.getPosition().getLine(),
                endLine = element.getPosition().getEndLine();

        updateOuterScopes(Pair.of(startLine, endLine));
    }

    public void updateOuterScopes(Pair<Integer, Integer> newOuterScope) {
        if (outerScopes.stream().noneMatch(s -> s.getKey() <= newOuterScope.getKey()
                && s.getValue() >= newOuterScope.getValue())) {
            outerScopes = outerScopes.stream()
                    .filter(s -> !(s.getKey() >= newOuterScope.getKey() && s.getValue() <= newOuterScope.getValue()))
                    .collect(Collectors.toSet());
            outerScopes.add(newOuterScope);
        }
    }

    public Set<Pair<Integer, Integer>> getOuterScopes() {
        return outerScopes;
    }
}
