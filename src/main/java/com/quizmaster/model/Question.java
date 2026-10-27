package com.quizmaster.model;

import java.util.ArrayList;
import java.util.List;

/** Immutable, validated four-option question. The correct-answer index is zero-based. */
public final class Question {
    private final String id;
    private final SyllabusUnit unit;
    private final String topic;
    private final String text;
    private final List<String> options;
    private final int correctAnswerIndex;
    private final String explanation;
    private final String subjectId;

    public Question(String id, SyllabusUnit unit, String topic, String text,
                    List<String> options, int correctAnswerIndex, String explanation) {
        this(id, unit, topic, text, options, correctAnswerIndex, explanation, Subject.OOP_ID);
    }

    public Question(String id, SyllabusUnit unit, String topic, String text,
                    List<String> options, int correctAnswerIndex, String explanation, String subjectId) {
        this.id = required(id, "Question ID");
        if (!this.id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
            throw new IllegalArgumentException("Question ID must use 1–64 letters, digits, dots, underscores, or hyphens.");
        }
        if (unit == null) throw new IllegalArgumentException("Select a syllabus unit.");
        this.unit = unit;
        this.topic = required(topic, "Topic");
        this.text = required(text, "Question text");
        if (this.topic.length() > 120) throw new IllegalArgumentException("Topic must be 120 characters or fewer.");
        if (this.text.length() > 1000) throw new IllegalArgumentException("Question text must be 1000 characters or fewer.");
        if (options == null || options.size() != 4) throw new IllegalArgumentException("Enter exactly four answer options.");
        List<String> checkedOptions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String option = required(options.get(i), "Option " + (i + 1));
            if (option.length() > 300) throw new IllegalArgumentException("Each answer option must be 300 characters or fewer.");
            checkedOptions.add(option);
        }
        if (correctAnswerIndex < 0 || correctAnswerIndex > 3) {
            throw new IllegalArgumentException("Choose which of the four options is correct.");
        }
        this.options = List.copyOf(checkedOptions);
        this.correctAnswerIndex = correctAnswerIndex;
        this.explanation = explanation == null ? "" : explanation.trim();
        if (subjectId == null || subjectId.trim().isEmpty()) throw new IllegalArgumentException("Subject is required.");
        this.subjectId = subjectId.trim();
        if (this.explanation.length() > 2000) throw new IllegalArgumentException("Explanation must be 2000 characters or fewer.");
        if (this.explanation.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Explanation cannot contain control characters or line breaks.");
        }
        rejectLineBreaks(this.id, "Question ID");
        rejectLineBreaks(this.topic, "Topic");
        rejectLineBreaks(this.text, "Question text");
        for (int i = 0; i < this.options.size(); i++) rejectLineBreaks(this.options.get(i), "Option " + (i + 1));
        rejectLineBreaks(this.explanation, "Explanation");
    }

    public String getId() { return id; }
    public SyllabusUnit getUnit() { return unit; }
    public String getTopic() { return topic; }
    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public String getExplanation() { return explanation; }
    public String getSubjectId() { return subjectId; }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        String result = value.trim();
        if (result.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException(label + " cannot contain control characters or line breaks.");
        return result;
    }

    private static void rejectLineBreaks(String value, String label) {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) throw new IllegalArgumentException(label + " must fit on one line.");
    }
}
