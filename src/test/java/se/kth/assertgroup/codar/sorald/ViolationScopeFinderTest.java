package se.kth.assertgroup.codar.sorald;

import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import se.kth.assertgroup.codar.sorald.ViolationScopeFinder;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertTrue;


class ViolationScopeFinderTest {
    @Test
    void test_notEmpty() {
        Set<Pair<Integer, Integer>> scopes=
                new ViolationScopeFinder().extractScopes(
                        getClass().getClassLoader()
                                .getResource("sorald/src_ex/ClassLoadingUtil.java").getPath().toString()
                );
        assertTrue(scopes.size() > 0);
    }
}
