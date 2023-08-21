package se.kth.assertgroup.codar.sorald;

import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ViolationScopeFinderTest {
    @Test
    public void test_notEmpty() {
        Set<Pair<Integer, Integer>> scopes=
                new ViolationScopeFinder().extractScopes(
                        getClass().getClassLoader()
                                .getResource("sorald/src_ex/ClassLoadingUtil.java").getPath().toString()
                );
        assertTrue(scopes.size() > 0);
    }
}
