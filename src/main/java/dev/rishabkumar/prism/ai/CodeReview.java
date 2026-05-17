package dev.rishabkumar.ai;

import java.util.List;

public class CodeReview {

    private String summary;
    private int score;
    private String severity;
    private int bugCount;
    private int securityCount;
    private int performanceCount;
    private int codeQualityCount;
    private List<String> highlights;
    private String recommendation;
    private String fullReview;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getBugCount() {
        return bugCount;
    }

    public void setBugCount(int bugCount) {
        this.bugCount = bugCount;
    }

    public int getSecurityCount() {
        return securityCount;
    }

    public void setSecurityCount(int securityCount) {
        this.securityCount = securityCount;
    }

    public int getPerformanceCount() {
        return performanceCount;
    }

    public void setPerformanceCount(int performanceCount) {
        this.performanceCount = performanceCount;
    }

    public int getCodeQualityCount() {
        return codeQualityCount;
    }

    public void setCodeQualityCount(int codeQualityCount) {
        this.codeQualityCount = codeQualityCount;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getFullReview() {
        return fullReview;
    }

    public void setFullReview(String fullReview) {
        this.fullReview = fullReview;
    }
}
