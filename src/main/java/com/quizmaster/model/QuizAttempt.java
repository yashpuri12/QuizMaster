package com.quizmaster.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** In-memory attempt state. Answers are keyed by stable question ID. */
public final class QuizAttempt {
    private final String attemptId = UUID.randomUUID().toString();
    private final StudentProfile student;
    private final UnitQuiz quiz;
    private final List<Question> questions;
    private final Map<String, Integer> selectedAnswers = new LinkedHashMap<>();
    private final Instant startedAt;
    private final Duration duration;
    private int currentIndex;
    private QuizResult pendingResult;
    private boolean submitted;

    public QuizAttempt(StudentProfile student, UnitQuiz quiz, List<Question> questions, Instant startedAt, Duration duration) {
        if (student == null) throw new IllegalArgumentException("Student profile is required.");
        if (quiz == null) throw new IllegalArgumentException("Quiz identity is required.");
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("A quiz must contain at least one question.");
        if (startedAt == null || duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("A quiz start time and positive duration are required.");
        }
        List<Question> copied = new ArrayList<>(questions);
        for (Question question : copied) {
            if (question == null || question.getUnit() != quiz.getUnit()) {
                throw new IllegalArgumentException("Quiz questions must belong to the selected syllabus unit.");
            }
        }
        if (copied.size() != quiz.getQuestionCount()) throw new IllegalArgumentException("Quiz count does not match its questions.");
        this.student = student;
        this.quiz = quiz;
        this.questions = List.copyOf(copied);
        this.startedAt = startedAt;
        this.duration = duration;
    }

    public String getAttemptId() { return attemptId; }
    public StudentProfile getStudent() { return student; }
    public UnitQuiz getQuiz() { return quiz; }
    public List<Question> getQuestions() { return questions; }
    public int getQuestionCount() { return questions.size(); }
    public int getCurrentIndex() { return currentIndex; }
    public Question getCurrentQuestion() { return questions.get(currentIndex); }
    public Instant getStartedAt() { return startedAt; }
    public Duration getDuration() { return duration; }
    public synchronized boolean isSubmitted() { return submitted; }

    public synchronized void goToQuestion(int index) {
        requireActive();
        if (index < 0 || index >= questions.size()) throw new IndexOutOfBoundsException("Question number is outside this quiz.");
        currentIndex = index;
    }

    public synchronized void previousQuestion() {
        requireActive();
        if (currentIndex > 0) currentIndex--;
    }

    public synchronized void nextQuestion() {
        requireActive();
        if (currentIndex < questions.size() - 1) currentIndex++;
    }

    public synchronized void selectAnswer(String questionId, Integer optionIndex) {
        requireActive();
        Question question = questionById(questionId);
        if (optionIndex == null) selectedAnswers.remove(question.getId());
        else {
            if (optionIndex < 0 || optionIndex > 3) throw new IllegalArgumentException("Answer option must be between 0 and 3.");
            selectedAnswers.put(question.getId(), optionIndex);
        }
    }

    public synchronized Integer getSelectedAnswer(String questionId) {
        questionById(questionId);
        return selectedAnswers.get(questionId);
    }

    public synchronized Map<String, Integer> getSelectedAnswers() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(selectedAnswers));
    }

    public boolean isExpired(Instant now) { return !now.isBefore(startedAt.plus(duration)); }

    public long secondsRemaining(Instant now) {
        long millis = Duration.between(now, startedAt.plus(duration)).toMillis();
        if (millis <= 0) return 0;
        return (millis + 999) / 1000;
    }

    /** Calculates once and caches the pending record until it is persisted. */
    public synchronized QuizResult prepareSubmission(Instant completedAt) {
        if (submitted) throw new IllegalStateException("This attempt has already been submitted.");
        if (pendingResult != null) return pendingResult;
        int correct = 0;
        int incorrect = 0;
        for (Question question : questions) {
            Integer selected = selectedAnswers.get(question.getId());
            if (selected == null) continue;
            if (selected == question.getCorrectAnswerIndex()) correct++;
            else incorrect++;
        }
        int unanswered = questions.size() - correct - incorrect;
        BigDecimal percentage = BigDecimal.valueOf(correct).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP);
        pendingResult = new QuizResult(attemptId, student.getStudentId(), student.getDisplayName(),
                quiz.getQuizId(), quiz.getQuizName(), quiz.getUnit(), questions.size(), correct,
                incorrect, unanswered, percentage, completedAt, quiz.getSubjectId());
        return pendingResult;
    }

    public synchronized void markSubmitted() {
        if (pendingResult == null) throw new IllegalStateException("Calculate the result before marking the attempt submitted.");
        if (submitted) throw new IllegalStateException("This attempt has already been submitted.");
        submitted = true;
    }

    private Question questionById(String id) {
        for (Question question : questions) if (question.getId().equals(id)) return question;
        throw new IllegalArgumentException("Question is not part of this quiz.");
    }

    private void requireActive() {
        if (submitted) throw new IllegalStateException("This attempt is already submitted.");
    }
}
