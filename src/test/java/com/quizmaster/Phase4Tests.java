package com.quizmaster;

import com.quizmaster.model.Question;
import com.quizmaster.model.QuizAttempt;
import com.quizmaster.model.QuizResult;
import com.quizmaster.model.Role;
import com.quizmaster.model.StudentProfile;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.model.UnitQuiz;
import com.quizmaster.security.SessionManager;
import com.quizmaster.service.StudentQuizService;
import com.quizmaster.storage.QuestionStore;
import com.quizmaster.storage.ResultLoadResult;
import com.quizmaster.storage.ResultStore;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free behavioral checks for Phase 4. */
public final class Phase4Tests {
    private static int passed;
    private Phase4Tests() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "verify-result-restart".equals(args[0])) {
            verifyResultRestart(Path.of(args[1]));
            return;
        }
        Path root = Files.createTempDirectory(Path.of("build"), "phase4-test-");
        try {
            testSelectionAuthorizationAndAttempt(root.resolve("attempt"));
            testPercentRounding(root.resolve("rounding"));
            testEmptyUnit(root.resolve("empty"));
            System.out.println("Phase 4 checks passed: " + passed);
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private static void testSelectionAuthorizationAndAttempt(Path directory) throws Exception {
        QuestionStore bank = new QuestionStore(directory);
        Question first = question("Q1", SyllabusUnit.UNIT_I, "What is the right answer?");
        Question second = question("Q2", SyllabusUnit.UNIT_I, "Which answer is wrong?");
        Question third = question("Q3", SyllabusUnit.UNIT_I, "Leave this unanswered.");
        first = new Question(first.getId(), first.getUnit(), first.getTopic(), first.getText(), first.getOptions(), 0, first.getExplanation());
        second = new Question(second.getId(), second.getUnit(), second.getTopic(), second.getText(), second.getOptions(), 2, second.getExplanation());
        third = new Question(third.getId(), third.getUnit(), third.getTopic(), third.getText(), third.getOptions(), 3, third.getExplanation());
        bank.add(first); bank.add(second); bank.add(third);
        bank.add(question("Q4", SyllabusUnit.UNIT_II, "A second unit question."));

        SessionManager sessions = new SessionManager();
        ResultStore results = new ResultStore(directory);
        StudentQuizService service = new StudentQuizService(bank, results, sessions);
        expect(SecurityException.class, service::availableQuizzes, "quiz selection requires a student session");
        expect(SecurityException.class, () -> service.startQuiz(SyllabusUnit.UNIT_I), "starting a quiz requires a student session");
        expect(SecurityException.class, () -> service.submitAttempt(null), "attempt submission requires a student session");

        sessions.startTeacher();
        expect(SecurityException.class, service::availableQuizzes, "teacher cannot open student quiz selection");
        expect(SecurityException.class, () -> service.startQuiz(SyllabusUnit.UNIT_I), "teacher cannot start a student quiz");

        StudentProfile student = new StudentProfile("STU-4", "Maya, \"MJ\"");
        sessions.startStudent(student);
        List<UnitQuiz> available = service.availableQuizzes();
        UnitQuiz unitOne = available.stream().filter(q -> q.getUnit() == SyllabusUnit.UNIT_I).findFirst().orElseThrow();
        UnitQuiz unitTwo = available.stream().filter(q -> q.getUnit() == SyllabusUnit.UNIT_II).findFirst().orElseThrow();
        check(unitOne.getQuestionCount() == 3 && unitOne.getQuizId().equals("PCC202-UNIT-I"), "unit quiz identity is stable and count uses saved questions");
        check(unitTwo.getQuestionCount() == 1, "selection count reflects the saved question bank");

        QuizAttempt attempt = service.startQuiz(SyllabusUnit.UNIT_I);
        check(attempt.getQuestionCount() == 3 && attempt.getDuration().equals(Duration.ofMinutes(3)), "quiz uses every saved unit question and one minute per question");
        check(attempt.getQuestions().get(0).getId().equals("Q1"), "question order remains stable without shuffling");
        Instant deadline = attempt.getStartedAt().plus(attempt.getDuration());
        check(!attempt.isExpired(deadline.minusMillis(1)) && attempt.isExpired(deadline)
                        && attempt.secondsRemaining(deadline) == 0,
                "countdown expiry is detected exactly at the attempt deadline");
        check(results.loadAll().getResults().isEmpty(), "an unsubmitted attempt is not persisted");

        attempt.selectAnswer("Q1", 0);
        attempt.nextQuestion();
        attempt.selectAnswer("Q2", 0);
        attempt.previousQuestion();
        check(attempt.getSelectedAnswer("Q1") == 0, "answer selection survives previous and next navigation");
        attempt.goToQuestion(2);
        check(attempt.getSelectedAnswer("Q3") == null, "unanswered question remains unanswered");
        expect(IllegalArgumentException.class, () -> attempt.selectAnswer("Q3", 4), "invalid answer index is rejected");

        QuizResult result = service.submitAttempt(attempt);
        check(result.getCorrectCount() == 1 && result.getIncorrectCount() == 1 && result.getUnansweredCount() == 1,
                "scoring separates correct, incorrect, and unanswered answers");
        check(result.getPercentage().compareTo(new BigDecimal("33.33")) == 0, "score percentage rounds to two decimal places");
        check(result.getStudentName().equals(student.getDisplayName()) && result.getStudentId().equals(student.getStudentId()),
                "result captures the authenticated student identity");
        check(result.getQuizId().equals("PCC202-UNIT-I") && result.getQuizName().equals("Unit I Practice Quiz"),
                "result captures the stable quiz identity");
        check(attempt.isSubmitted(), "successful persistence marks the attempt submitted");
        expect(SecurityException.class, () -> service.submitAttempt(attempt), "duplicate submission is prevented");
        check(results.loadAll().getResults().size() == 1, "duplicate submission does not create another result row");

        String csv = Files.readString(directory.resolve("results.csv"), StandardCharsets.UTF_8);
        check(csv.contains("\"Maya, \"\"MJ\"\"\"") && csv.contains("\"Maya, \"\"MJ\"\"\""),
                "result CSV correctly escapes commas and quotation marks in student names");
        check(!csv.contains("correct_answer_index") && !csv.contains("Option A") && !csv.contains("Option B"),
                "result records contain no answer keys or answer options");
        check(runFreshJvm(directory), "saved result reloads in a fresh JVM");

        QuizAttempt abandoned = service.startQuiz(SyllabusUnit.UNIT_II);
        service.abandonAttempt(abandoned);
        check(results.loadAll().getResults().size() == 1, "abandoned attempt is not saved");

        QuizAttempt foreign = service.startQuiz(SyllabusUnit.UNIT_II);
        sessions.startTeacher();
        expect(SecurityException.class, () -> service.submitAttempt(foreign), "teacher cannot submit a student attempt");
        check(results.loadAll().getResults().size() == 1, "denied submission does not create a result");
    }

    private static void testPercentRounding(Path directory) throws Exception {
        QuestionStore bank = new QuestionStore(directory);
        for (int i = 0; i < 6; i++) {
            Question q = question("ROUND-" + i, SyllabusUnit.UNIT_V, "Rounding case " + i);
            int correct = i < 4 ? 1 : 2;
            bank.add(new Question(q.getId(), q.getUnit(), q.getTopic(), q.getText(), q.getOptions(), correct, ""));
        }
        SessionManager sessions = new SessionManager();
        sessions.startStudent(new StudentProfile("ROUND-STUDENT", "Rounder"));
        StudentQuizService service = new StudentQuizService(bank, new ResultStore(directory), sessions);
        QuizAttempt attempt = service.startQuiz(SyllabusUnit.UNIT_V);
        for (int i = 0; i < 4; i++) attempt.selectAnswer(attempt.getQuestions().get(i).getId(), 1);
        for (int i = 4; i < 6; i++) attempt.selectAnswer(attempt.getQuestions().get(i).getId(), 0);
        QuizResult result = service.submitAttempt(attempt);
        check(result.getPercentage().compareTo(new BigDecimal("66.67")) == 0, "non-terminating score percentage rounds half up");
    }

    private static void testEmptyUnit(Path directory) throws Exception {
        QuestionStore bank = new QuestionStore(directory);
        bank.add(question("ONLY-I", SyllabusUnit.UNIT_I, "Only unit I has a question."));
        SessionManager sessions = new SessionManager();
        sessions.startStudent(new StudentProfile("EMPTY-STUDENT", "Empty Unit"));
        StudentQuizService service = new StudentQuizService(bank, new ResultStore(directory), sessions);
        UnitQuiz empty = service.availableQuizzes().stream().filter(q -> q.getUnit() == SyllabusUnit.UNIT_II).findFirst().orElseThrow();
        check(empty.getQuestionCount() == 0, "empty unit is represented with zero available questions");
        expect(IllegalStateException.class, () -> service.startQuiz(SyllabusUnit.UNIT_II), "empty unit cannot start a quiz");
    }

    private static Question question(String id, SyllabusUnit unit, String text) {
        return new Question(id, unit, "Quiz test topic", text,
                List.of("Option A", "Option B", "Option C", "Option D"), 0, "Saved explanation.");
    }

    private static boolean runFreshJvm(Path directory) throws Exception {
        String executable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        Process child = new ProcessBuilder(executable, "-cp", System.getProperty("java.class.path"),
                Phase4Tests.class.getName(), "verify-result-restart", directory.toAbsolutePath().toString())
                .redirectErrorStream(true).start();
        String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = child.waitFor();
        if (exit != 0) System.out.print(output);
        return exit == 0;
    }

    private static void verifyResultRestart(Path directory) throws Exception {
        ResultLoadResult loaded = new ResultStore(directory).loadAll();
        if (!loaded.isClean() || loaded.getResults().size() != 1) throw new AssertionError("Expected exactly one clean saved result.");
        QuizResult result = loaded.getResults().get(0);
        if (!result.getStudentName().equals("Maya, \"MJ\"") || result.getCorrectCount() != 1
                || result.getIncorrectCount() != 1 || result.getUnansweredCount() != 1
                || result.getPercentage().compareTo(new BigDecimal("33.33")) != 0) {
            throw new AssertionError("Saved result fields did not survive a fresh process.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAILED: " + message);
        passed++;
        System.out.println("PASS: " + message);
    }

    private static void expect(Class<? extends Throwable> type, ThrowingAction action, String message) {
        try { action.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) { check(true, message); return; }
            throw new AssertionError("FAILED: " + message + " (unexpected " + thrown + ")", thrown);
        }
        throw new AssertionError("FAILED: " + message + " (no exception)");
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
}
