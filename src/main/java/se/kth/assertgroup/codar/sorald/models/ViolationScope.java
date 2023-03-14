package se.kth.assertgroup.codar.sorald.models;

import java.util.Objects;

public class ViolationScope {
    private String srcPath;
    private Integer startLine, endLine;

    public ViolationScope(String srcPath, Integer startLine, Integer endLine){
        this.srcPath = srcPath;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViolationScope that = (ViolationScope) o;
        return Objects.equals(getSrcPath(), that.getSrcPath()) &&
                Objects.equals(getStartLine(), that.getStartLine()) &&
                Objects.equals(getEndLine(), that.getEndLine());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSrcPath(), getStartLine(), getEndLine());
    }

    public String getSrcPath() {
        return srcPath;
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }
}
