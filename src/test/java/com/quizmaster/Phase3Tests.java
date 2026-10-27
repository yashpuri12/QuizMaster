package com.quizmaster;

import com.quizmaster.model.Question;
import com.quizmaster.model.Role;
import com.quizmaster.model.StudentProfile;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.security.SessionManager;
import com.quizmaster.service.QuestionService;
import com.quizmaster.service.StarterQuestionBank;
import com.quizmaster.storage.Csv;
import com.quizmaster.storage.QuestionDataException;
import com.quizmaster.storage.QuestionLoadResult;
import com.quizmaster.storage.QuestionStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free checks for the Phase 3 question bank and authorization. */
public final class Phase3Tests {
    private static int passed;
    private static final String PERSISTED_TEXT = "Which value contains a comma, and a \"quote\"?";
    private Phase3Tests() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "verify-question-restart".equals(args[0])) {
            verifyRestart(Path.of(args[1]));
            return;
        }
        Path temp = Files.createTempDirectory(Path.of("build"), "phase3-test-");
        try {
            testValidation();
            testTeacherCrudAndRestart(temp.resolve("crud"));
            testMalformedRows(temp.resolve("malformed"));
            testStarterInitialization(temp.resolve("starter"));
            System.out.println("Phase 3 checks passed: " + passed);
        } finally {
            try (var paths = Files.walk(temp)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private static void testValidation() {
        Question valid = question("VALID-01", "What is a, \"quoted\" field?");
        check(valid.getOptions().size() == 4 && valid.getCorrectAnswerIndex() == 0, "valid question model accepts four options and answer index");
        expect(IllegalArgumentException.class, () -> new Question("", SyllabusUnit.UNIT_I, "Topic", "Text", options(), 0, ""), "missing question ID rejected");
        expect(IllegalArgumentException.class, () -> new Question("Q1", null, "Topic", "Text", options(), 0, ""), "missing unit rejected");
        expect(IllegalArgumentException.class, () -> new Question("Q1", SyllabusUnit.UNIT_I, " ", "Text", options(), 0, ""), "missing topic rejected");
        expect(IllegalArgumentException.class, () -> new Question("Q1", SyllabusUnit.UNIT_I, "Topic", " ", options(), 0, ""), "missing question text rejected");
        expect(IllegalArgumentException.class, () -> new Question("Q1", SyllabusUnit.UNIT_I, "Topic", "Text", List.of("a", "b", "c"), 0, ""), "wrong number of options rejected");
        expect(IllegalArgumentException.class, () -> new Question("Q1", SyllabusUnit.UNIT_I, "Topic", "Text", List.of("a", "b", "c", " "), 0, ""), "empty answer option rejected");
        expect(IllegalArgumentException.class, () -> new Question("Q1", SyllabusUnit.UNIT_I, "Topic", "Text", options(), 4, ""), "out-of-range correct answer rejected");
        expect(IllegalArgumentException.class, () -> new Question("Q1", SyllabusUnit.UNIT_I, "Topic", "Text\nmore", options(), 0, ""), "line breaks rejected for safe CSV records");
        expect(IllegalArgumentException.class, () -> new QuestionStore(Path.of("build", "unused")).add(null), "null question rejected by storage");
    }

    private static void testTeacherCrudAndRestart(Path directory) throws Exception {
        QuestionStore store = new QuestionStore(directory);
        check(store.load().getQuestions().isEmpty(), "missing question file is created as an empty bank");
        Path explicitlyEmpty = directory.resolve("empty-file");
        Files.createDirectories(explicitlyEmpty);
        Files.writeString(explicitlyEmpty.resolve("questions.csv"), "", StandardCharsets.UTF_8);
        check(new QuestionStore(explicitlyEmpty).load().getQuestions().isEmpty(), "zero-byte question file is safely initialized");
        SessionManager sessions = new SessionManager();
        QuestionService service = new QuestionService(store, sessions);
        Question q = question("PERSIST-01", PERSISTED_TEXT);
        expect(SecurityException.class, () -> service.addQuestion(q), "unauthenticated add denied");
        expect(SecurityException.class, () -> service.updateQuestion(q), "unauthenticated update denied");
        expect(SecurityException.class, () -> service.deleteQuestion(q.getId()), "unauthenticated delete denied");
        sessions.startStudent(new StudentProfile("STU-1", "Student"));
        expect(SecurityException.class, () -> service.addQuestion(q), "student add denied");
        expect(SecurityException.class, () -> service.updateQuestion(q), "student update denied");
        expect(SecurityException.class, () -> service.deleteQuestion(q.getId()), "student delete denied");

        sessions.startTeacher();
        service.addQuestion(q);
        check(service.listQuestions().getQuestions().size() == 1, "teacher can add and list a question");
        Question escaped = new QuestionStore(directory).load().getQuestions().get(0);
        check(escaped.getText().equals(PERSISTED_TEXT) && escaped.getOptions().get(0).equals("Option \"A\"")
                        && escaped.getOptions().get(1).equals("Option, B") && escaped.getExplanation().contains("\"quoted text\""),
                "CSV commas and quotation marks round-trip in all text fields");
        expect(IllegalArgumentException.class, () -> service.addQuestion(question("persist-01", "duplicate ID")), "duplicate IDs rejected without regard to case");
        Question changed = new Question(q.getId(), q.getUnit(), q.getTopic(), "Teacher-edited question",
                q.getOptions(), 2, q.getExplanation());
        service.updateQuestion(changed);
        Question reloaded = new QuestionStore(directory).load().getQuestions().get(0);
        check(reloaded.getText().equals("Teacher-edited question") && reloaded.getCorrectAnswerIndex() == 2,
                "teacher edit persists to disk");
        check(runRestartProcess(directory), "question data reloads in a fresh JVM");
        service.deleteQuestion(q.getId());
        check(service.listQuestions().getQuestions().isEmpty(), "teacher can delete a question");
    }

    private static void testMalformedRows(Path directory) throws Exception {
        QuestionStore store = new QuestionStore(directory);
        store.load();
        Question good = question("GOOD-01", "Valid row before and after damaged data");
        store.add(good);
        Path file = directory.resolve("questions.csv");
        String malformed = "\"BROKEN\"trailing,UNIT_I,Topic,Question,a,b,c,d,0,Explanation";
        Question afterBad = question("GOOD-02", "A valid row after the malformed row");
        Files.writeString(file, malformed + System.lineSeparator() + encodeQuestion(afterBad) + System.lineSeparator(),
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        QuestionLoadResult result = store.load();
        check(result.getQuestions().size() == 2, "valid question rows remain available around a malformed row");
        check(result.getIssues().size() == 1 && result.getIssues().get(0).contains("Line 3"), "malformed CSV row is reported with its line number");
        byte[] before = Files.readAllBytes(file);
        expect(QuestionDataException.class, () -> store.add(question("NEW-01", "Must not overwrite the malformed bank")),
                "malformed question bank refuses writes");
        check(java.util.Arrays.equals(before, Files.readAllBytes(file)), "refused change leaves the entire malformed CSV untouched");

        Path badHeaderDirectory = directory.resolve("bad-header");
        Files.createDirectories(badHeaderDirectory);
        Question headerSurvivor = question("HEADER-OK", "Valid data after a malformed header");
        Files.writeString(badHeaderDirectory.resolve("questions.csv"), "\"unclosed header\n"
                + encodeQuestion(headerSurvivor) + System.lineSeparator(), StandardCharsets.UTF_8);
        QuestionLoadResult badHeader = new QuestionStore(badHeaderDirectory).load();
        check(badHeader.getQuestions().size() == 1 && !badHeader.isClean(),
                "valid data rows remain readable when the header is malformed");
    }

    private static void testStarterInitialization(Path directory) throws Exception {
        QuestionStore store = new QuestionStore(directory);
        List<Question> starters = StarterQuestionBank.questions();
        check(starters.size() == 5, "starter set has one original sample for each syllabus unit");
        check(store.initializeIfEmpty(starters), "starter bank initializes an empty question file");
        check(!store.initializeIfEmpty(starters), "repeated starter initialization does not add duplicates");
        check(store.load().getQuestions().size() == starters.size(), "starter count remains unchanged after repeated initialization");
        Question edited = starters.get(0);
        Question teacherEdit = new Question(edited.getId(), edited.getUnit(), edited.getTopic(),
                "Teacher's revised starter question", edited.getOptions(), edited.getCorrectAnswerIndex(), edited.getExplanation());
        store.update(teacherEdit);
        check(!store.initializeIfEmpty(starters), "starter initialization leaves a non-empty bank alone");
        check(store.load().getQuestions().get(0).getText().equals("Teacher's revised starter question"),
                "starter initialization never overwrites a teacher edit");
    }

    private static String encodeQuestion(Question q) {
        return Csv.encodeRow(List.of(q.getId(), q.getUnit().name(), q.getTopic(), q.getText(),
                q.getOptions().get(0), q.getOptions().get(1), q.getOptions().get(2), q.getOptions().get(3),
                Integer.toString(q.getCorrectAnswerIndex()), q.getExplanation()));
    }

    private static boolean runRestartProcess(Path directory) throws Exception {
        String executable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        Process process = new ProcessBuilder(executable, "-cp", System.getProperty("java.class.path"),
                Phase3Tests.class.getName(), "verify-question-restart", directory.toAbsolutePath().toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) System.out.print(output);
        return exit == 0;
    }

    private static void verifyRestart(Path directory) throws Exception {
        Question question = new QuestionStore(directory).load().getQuestions().stream()
                .filter(item -> item.getId().equals("PERSIST-01")).findFirst().orElseThrow();
        if (!question.getText().equals("Teacher-edited question") || question.getCorrectAnswerIndex() != 2) {
            throw new AssertionError("The edited question did not persist across a process restart.");
        }
        if (!question.getOptions().get(0).equals("Option \"A\"") || !question.getExplanation().contains("\"quoted text\"")) {
            throw new AssertionError("Quoted options or explanation did not persist across process restart.");
        }
    }

    private static Question question(String id, String text) {
        return new Question(id, SyllabusUnit.UNIT_IV, "CSV, escaping", text,
                List.of("Option \"A\"", "Option, B", "Option C", "Option D"), 0,
                "An explanation with a comma, and \"quoted text\".");
    }

    private static List<String> options() { return List.of("one", "two", "three", "four"); }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void expect(Class<? extends Throwable> type, ThrowingAction action, String name) {
        try { action.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) { check(true, name); return; }
            throw new AssertionError("FAILED: " + name + " (unexpected " + thrown + ")", thrown);
        }
        throw new AssertionError("FAILED: " + name + " (no exception)");
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
}
