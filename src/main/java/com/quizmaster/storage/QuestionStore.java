package com.quizmaster.storage;

import com.quizmaster.model.Question;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.model.Subject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** UTF-8 CSV repository for questions. Malformed source data blocks writes. */
public final class QuestionStore {
    private static final List<String> HEADER = List.of("question_id", "unit", "topic", "question_text",
            "option_1", "option_2", "option_3", "option_4", "correct_answer_index", "explanation", "subject_id");
    private static final List<String> LEGACY_HEADER = HEADER.subList(0, 10);
    private final Path file;

    public QuestionStore(Path dataDirectory) { this.file = dataDirectory.resolve("questions.csv"); }

    public synchronized QuestionLoadResult load() throws IOException {
        ensureFile();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Question> questions = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        boolean legacy = false;
        if (lines.isEmpty()) return new QuestionLoadResult(questions, issues);
        try {
            List<String> header = Csv.parse(lines.get(0));
            legacy = header.equals(LEGACY_HEADER);
            if (!header.equals(HEADER) && !legacy) issues.add("Line 1: header does not match the expected question bank columns.");
        } catch (IllegalArgumentException ex) {
            issues.add("Line 1: malformed CSV header: " + ex.getMessage());
        }

        Map<String, Question> byId = new LinkedHashMap<>();
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (line.isBlank()) continue;
            try {
                List<String> row = Csv.parse(line);
                if (row.size() != LEGACY_HEADER.size() && row.size() != HEADER.size()) throw new IllegalArgumentException("unexpected question column count");
                Question question = fromRow(row);
                String key = normalize(question.getId());
                if (byId.containsKey(key)) throw new IllegalArgumentException("duplicate question ID '" + question.getId() + "'");
                byId.put(key, question);
            } catch (IllegalArgumentException ex) {
                issues.add("Line " + (lineIndex + 1) + ": " + ex.getMessage());
            }
        }
        questions.addAll(byId.values());
        return new QuestionLoadResult(questions, issues);
    }

    public synchronized void add(Question question) throws IOException {
        if (question == null) throw new IllegalArgumentException("Question is required.");
        List<Question> questions = cleanQuestionsForWrite();
        String id = normalize(question.getId());
        for (Question existing : questions) if (normalize(existing.getId()).equals(id)) {
            throw new IllegalArgumentException("Question ID '" + question.getId() + "' already exists.");
        }
        questions.add(question);
        save(questions);
    }

    public synchronized void update(Question question) throws IOException {
        if (question == null) throw new IllegalArgumentException("Question is required.");
        List<Question> questions = cleanQuestionsForWrite();
        for (int i = 0; i < questions.size(); i++) {
            if (normalize(questions.get(i).getId()).equals(normalize(question.getId()))) {
                questions.set(i, question);
                save(questions);
                return;
            }
        }
        throw new IllegalArgumentException("Question ID '" + question.getId() + "' no longer exists.");
    }

    public synchronized void delete(String questionId) throws IOException {
        List<Question> questions = cleanQuestionsForWrite();
        boolean removed = questions.removeIf(question -> normalize(question.getId()).equals(normalize(questionId)));
        if (!removed) throw new IllegalArgumentException("Question ID '" + questionId + "' was not found.");
        save(questions);
    }

    /** Seeds only a clean, empty bank; it never replaces an existing question. */
    public synchronized boolean initializeIfEmpty(List<Question> starterQuestions) throws IOException {
        List<Question> current = cleanQuestionsForWrite();
        if (!current.isEmpty()) return false;
        Map<String, Question> unique = new LinkedHashMap<>();
        for (Question question : starterQuestions) {
            String key = normalize(question.getId());
            if (unique.putIfAbsent(key, question) != null) {
                throw new IllegalArgumentException("Starter question IDs must be unique: " + question.getId());
            }
        }
        save(new ArrayList<>(unique.values()));
        return true;
    }

    private List<Question> cleanQuestionsForWrite() throws IOException {
        QuestionLoadResult result = load();
        if (!result.isClean()) {
            throw new QuestionDataException("Question bank was not changed because it contains invalid data:\n"
                    + String.join("\n", result.getIssues()));
        }
        return new ArrayList<>(result.getQuestions());
    }

    private void ensureFile() throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file) || Files.size(file) == 0) {
            Files.writeString(file, Csv.encodeRow(HEADER) + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    private void save(List<Question> questions) throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.exists(file)) {
            List<String> existing = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (!existing.isEmpty() && Csv.parse(existing.get(0)).equals(LEGACY_HEADER)) {
                Path backup=file.resolveSibling("questions.csv.pre-subject-migration.bak");
                if(!Files.exists(backup)) Files.copy(file,backup);
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add(Csv.encodeRow(HEADER));
        for (Question question : questions) lines.add(Csv.encodeRow(toRow(question)));
        Path temporary = file.resolveSibling("questions.csv.tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Question fromRow(List<String> row) {
        SyllabusUnit unit;
        try { unit = SyllabusUnit.fromId(row.get(1)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("unknown syllabus unit '" + row.get(1) + "'"); }
        int correct;
        try { correct = Integer.parseInt(row.get(8)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("correct answer index must be from 0 to 3"); }
        return new Question(row.get(0), unit, row.get(2), row.get(3),
                List.of(row.get(4), row.get(5), row.get(6), row.get(7)), correct, row.get(9), row.size()>10?row.get(10):Subject.OOP_ID);
    }

    private static List<String> toRow(Question question) {
        List<String> row = new ArrayList<>();
        row.add(question.getId());
        row.add(question.getUnit().name());
        row.add(question.getTopic());
        row.add(question.getText());
        row.addAll(question.getOptions());
        row.add(Integer.toString(question.getCorrectAnswerIndex()));
        row.add(question.getExplanation());
        row.add(question.getSubjectId());
        return row;
    }

    private static String normalize(String id) { return id.toLowerCase(Locale.ROOT); }
}
