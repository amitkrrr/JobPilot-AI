package com.jobpilot.dto.resume;

import java.util.List;

public class ResumeOptimizeResponse {

    private int overallScore;
    private String summary;
    private List<Suggestion> suggestions;
    private List<String> missingKeywords;

    public ResumeOptimizeResponse() {
    }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<Suggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Suggestion> suggestions) { this.suggestions = suggestions; }

    public List<String> getMissingKeywords() { return missingKeywords; }
    public void setMissingKeywords(List<String> missingKeywords) { this.missingKeywords = missingKeywords; }

    public static class Suggestion {
        private String category;
        private String suggestion;
        private String priority;

        public Suggestion() {}

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }
}
