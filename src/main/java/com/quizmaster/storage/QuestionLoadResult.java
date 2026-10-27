package com.quizmaster.storage;

import com.quizmaster.model.Question;
import java.util.List;

/** Valid rows and diagnostics found while reading the local question bank. */
public final class QuestionLoadResult {
    private final List<Question> questions;
    private final List<String> issues;

    public QuestionLoadResult(List<Question> questions, List<String> issues) {
        this.questions = List.copyOf(questions);
        this.issues = List.copyOf(issues);
    }

    public List<Question> getQuestions() { return questions; }
    public List<String> getIssues() { return issues; }
    public boolean isClean() { return issues.isEmpty(); }
}
