package se.kth.assertgroup.codar.sorald;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import sorald.segment.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MineResParser {
    public Map<Pair<Integer, Integer>, Map<String, Set<Integer>>> getCodeScopeToViolations(File mineResFile)
            throws IOException, ParseException {
        Map<Pair<Integer, Integer>, Map<String, Set<Integer>>> res = new HashMap<>();
        JSONParser parser = new JSONParser();
        parser.parse(new FileReader(mineResFile));

        // TODO

        return res;
    }

}
