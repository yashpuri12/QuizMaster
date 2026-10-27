package com.quizmaster.storage;

import com.quizmaster.model.QuizResult;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.model.Subject;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** UTF-8 CSV storage for completed quiz summaries. Answer data is never written. */
public final class ResultStore {
    private static final List<String> HEADER = List.of("attempt_id", "student_id", "student_display_name",
            "quiz_id", "quiz_name", "unit", "question_count", "correct_count", "incorrect_count",
            "unanswered_count", "percentage", "completed_at", "subject_id");
    private static final List<String> LEGACY_HEADER=HEADER.subList(0,12);
    private final Path file;

    public ResultStore(Path dataDirectory) { this.file = dataDirectory.resolve("results.csv"); }

    public synchronized ResultLoadResult loadAll() throws IOException {
        ensureFile();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<QuizResult> results = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        if (lines.isEmpty()) return new ResultLoadResult(results, issues);
        boolean legacy=false;
        try {
            List<String> header=Csv.parse(lines.get(0)); legacy=header.equals(LEGACY_HEADER);
            if (!header.equals(HEADER)&&!legacy) issues.add("Line 1: results header does not match the expected columns.");
        } catch (IllegalArgumentException ex) {
            issues.add("Line 1: malformed results header: " + ex.getMessage());
        }

        Map<String, QuizResult> byAttempt = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            try {
                List<String> row = Csv.parse(line);
                if (row.size() != (legacy?LEGACY_HEADER.size():HEADER.size())) throw new IllegalArgumentException("unexpected result column count");
                QuizResult result = fromRow(row,legacy);
                String key = result.getAttemptId().toLowerCase(Locale.ROOT);
                if (byAttempt.containsKey(key)) throw new IllegalArgumentException("duplicate attempt ID '" + result.getAttemptId() + "'");
                byAttempt.put(key, result);
            } catch (IllegalArgumentException ex) {
                issues.add("Line " + (i + 1) + ": " + ex.getMessage());
        }
        }
        results.addAll(byAttempt.values());
        return new ResultLoadResult(results, issues);
    }

    public synchronized void saveCompleted(QuizResult result) throws IOException {
        if (result == null) throw new IllegalArgumentException("A completed result is required.");
        ResultLoadResult loaded = loadAll();
        if (!loaded.isClean()) {
            throw new IOException("Results were not changed because results.csv contains invalid records: "
                    + String.join("; ", loaded.getIssues()));
        }
        for (QuizResult existing : loaded.getResults()) {
            if (existing.getAttemptId().equalsIgnoreCase(result.getAttemptId())) {
                throw new IllegalStateException("This attempt ID is already saved.");
            }
        }
        List<QuizResult> updated = new ArrayList<>(loaded.getResults());
        updated.add(result);
        save(updated);
    }

    private void ensureFile() throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file) || Files.size(file) == 0) {
            Files.writeString(file, Csv.encodeRow(HEADER) + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    private void save(List<QuizResult> results) throws IOException {
        if(Files.exists(file)) { List<String> current=Files.readAllLines(file,StandardCharsets.UTF_8); if(!current.isEmpty()&&Csv.parse(current.get(0)).equals(LEGACY_HEADER)) { Path backup=file.resolveSibling("results.csv.pre-subject-migration.bak"); if(!Files.exists(backup))Files.copy(file,backup); } }
        List<String> lines = new ArrayList<>();
        lines.add(Csv.encodeRow(HEADER));
        for (QuizResult result : results) lines.add(Csv.encodeRow(toRow(result)));
        Path temporary = file.resolveSibling("results.csv.tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static QuizResult fromRow(List<String> row,boolean legacy) {
        try {
            SyllabusUnit unit = SyllabusUnit.fromId(row.get(5));
            int questionCount = Integer.parseInt(row.get(6));
            int correct = Integer.parseInt(row.get(7));
            int incorrect = Integer.parseInt(row.get(8));
            int unanswered = Integer.parseInt(row.get(9));
            BigDecimal percentage = new BigDecimal(row.get(10));
            Instant completedAt = Instant.parse(row.get(11));
            BigDecimal expected = BigDecimal.valueOf(correct).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(questionCount), 2, java.math.RoundingMode.HALF_UP);
            if (percentage.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(expected) != 0) {
                throw new IllegalArgumentException("percentage does not match correct answers and question count");
            }
            return new QuizResult(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4), unit,
                    questionCount, correct, incorrect, unanswered, percentage, completedAt,legacy?Subject.OOP_ID:row.get(12));
        } catch (NumberFormatException | java.time.DateTimeException ex) {
            throw new IllegalArgumentException("invalid numeric value or completion timestamp");
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("question count must be greater than zero");
        }
    }

    private static List<String> toRow(QuizResult result) {
        return List.of(result.getAttemptId(), result.getStudentId(), result.getStudentName(),
                result.getQuizId(), result.getQuizName(), result.getUnit().name(),
                Integer.toString(result.getQuestionCount()), Integer.toString(result.getCorrectCount()),
                Integer.toString(result.getIncorrectCount()), Integer.toString(result.getUnansweredCount()),
                result.getPercentage().toPlainString(), result.getCompletedAt().toString(),result.getSubjectId());
    }
}
