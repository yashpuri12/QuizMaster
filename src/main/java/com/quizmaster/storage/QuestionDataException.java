package com.quizmaster.storage;

import java.io.IOException;

/** Signals that a question-bank change was refused to protect malformed source data. */
public final class QuestionDataException extends IOException {
    public QuestionDataException(String message) { super(message); }
}
