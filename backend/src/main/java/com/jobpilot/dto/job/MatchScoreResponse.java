package com.jobpilot.dto.job;

import java.util.List;

public class MatchScoreResponse {

    private int score;
    private List<String> strengths;
    private List<String> gaps;
    private String verdict;

    public MatchScoreResponse() {
    }

    public MatchScoreResponse(int score, List<String> strengths, List<String> gaps, String verdict) {
        this.score = score;
        this.strengths = strengths;
        this.gaps = gaps;
        this.verdict = verdict;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public void setGaps(List<String> gaps) {
        this.gaps = gaps;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }
}
