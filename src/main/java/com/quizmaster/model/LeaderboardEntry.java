package com.quizmaster.model;

/** A saved attempt with its competition rank inside one quiz identity. */
public final class LeaderboardEntry {
    private final int rank;
    private final QuizResult result;

    public LeaderboardEntry(int rank, QuizResult result) {
        if (rank < 1 || result == null) throw new IllegalArgumentException("A positive rank and saved result are required.");
        this.rank = rank;
        this.result = result;
    }

    public int getRank() { return rank; }
    public QuizResult getResult() { return result; }
}
