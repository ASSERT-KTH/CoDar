package se.kth.assertgroup.codar.repair;

import java.io.File;
import java.io.IOException;

public interface RepairTool {
    void repairSingleLine(File inputSrc, File outputSrc,
                          int bugStartLine, int bugEndLine, int nonCompliantLine, String ruleKey) throws IOException;
}
