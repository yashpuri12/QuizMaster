package com.quizmaster.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** Persistable summary of a submitted attempt; it deliberately contains no answers. */
public final class QuizResult {
    private final String attemptId;
    private final String studentId;
    private final String studentName;
    private final String quizId;
    private final String quizName;
    private final SyllabusUnit unit;
    private final int questionCount;
    private final int correctCount;
    private final int incorrectCount;
    private final int unansweredCount;
    private final BigDecimal percentage;
    private final Instant completedAt;
    private final String subjectId;

    public QuizResult(String attemptId, String studentId, String studentName, String quizId, String quizName,
                      SyllabusUnit unit, int questionCount, int correctCount, int incorrectCount,
                      int unansweredCount, BigDecimal percentage, Instant completedAt) {
        this(attemptId,studentId,studentName,quizId,quizName,unit,questionCount,correctCount,incorrectCount,unansweredCount,percentage,completedAt,Subject.OOP_ID);
    }
    public QuizResult(String attemptId, String studentId, String studentName, String quizId, String quizName,
                      SyllabusUnit unit, int questionCount, int correctCount, int incorrectCount,
                      int unansweredCount, BigDecimal percentage, Instant completedAt,String subjectId) {
        this.attemptId = required(attemptId, "Attempt ID");
        this.studentId = required(studentId, "Student ID");
        this.studentName = required(studentName, "Student name");
        this.quizId = required(quizId, "Quiz ID");
        this.quizName = required(quizName, "Quiz name");
        if (unit == null) throw new IllegalArgumentException("Syllabus unit is required.");
        if (questionCount <= 0 || correctCount < 0 || incorrectCount < 0 || unansweredCount < 0
                || correctCount + incorrectCount + unansweredCount != questionCount) {
            throw new IllegalArgumentException("Result counts must be non-negative and add up to the question count.");
        }
        this.unit = unit;
        this.questionCount = questionCount;
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
        this.unansweredCount = unansweredCount;
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }
        this.percentage = percentage.setScale(2, RoundingMode.HALF_UP);
        if (completedAt == null) throw new IllegalArgumentException("Completion timestamp is required.");
        this.completedAt = completedAt;
        this.subjectId=required(subjectId,"Subject ID");
    }

    public String getAttemptId() { return attemptId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getQuizId() { return quizId; }
    public String getQuizName() { return quizName; }
    public SyllabusUnit getUnit() { return unit; }
    public int getQuestionCount() { return questionCount; }
    public int getCorrectCount() { return correctCount; }
    public int getIncorrectCount() { return incorrectCount; }
    public int getUnansweredCount() { return unansweredCount; }
    public BigDecimal getPercentage() { return percentage; }
    public Instant getCompletedAt() { return completedAt; }
    public String getSubjectId(){return subjectId;}

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }
}
