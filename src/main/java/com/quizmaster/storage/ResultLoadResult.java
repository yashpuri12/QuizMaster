package com.quizmaster.storage;

import com.quizmaster.model.QuizResult;
import java.util.List;

/** Valid completed quiz records and any CSV issues found during loading. */
public final class ResultLoadResult {
    private final List<QuizResult> results;
    private final List<String> issues;

    public ResultLoadResult(List<QuizResult> results, List<String> issues) {
        this.results = List.copyOf(results);
        this.issues = List.copyOf(issues);
    }

    public List<QuizResult> getResults() { return results; }
    public List<String> getIssues() { return issues; }
    public boolean isClean() { return issues.isEmpty(); }
}
