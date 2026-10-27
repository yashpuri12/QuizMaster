package com.quizmaster.service;

import com.quizmaster.model.Question;
import com.quizmaster.model.QuizAttempt;
import com.quizmaster.model.QuizResult;
import com.quizmaster.model.Role;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.model.UnitQuiz;
import com.quizmaster.model.UserSession;
import com.quizmaster.security.SessionManager;
import com.quizmaster.storage.QuestionLoadResult;
import com.quizmaster.storage.QuestionStore;
import com.quizmaster.storage.ResultStore;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Student-only quiz selection, attempt creation, and result submission. */
public final class StudentQuizService {
    private static final long SECONDS_PER_QUESTION = 60;
    private final QuestionStore questions;
    private final ResultStore results;
    private final SessionManager sessions;
    private final Clock clock;
    private final SubjectService subjects;
    private final Map<String, QuizAttempt> activeAttempts = new HashMap<>();

    public StudentQuizService(QuestionStore questions, ResultStore results, SessionManager sessions) {
        this(questions, results, sessions, Clock.systemUTC());
    }

    public StudentQuizService(QuestionStore questions, ResultStore results, SessionManager sessions, Clock clock) {
        this(questions,results,sessions,clock,null);
    }
    public StudentQuizService(QuestionStore questions, ResultStore results, SessionManager sessions, Clock clock,SubjectService subjects) {
        this.questions = questions;
        this.results = results;
        this.sessions = sessions;
        this.clock = clock;
        this.subjects=subjects;
    }

    public List<UnitQuiz> availableQuizzes() throws IOException {
        sessions.require(Role.STUDENT);
        QuestionLoadResult loaded = loadQuestionBank();
        Map<SyllabusUnit, Integer> counts = new EnumMap<>(SyllabusUnit.class);
        for (SyllabusUnit unit : SyllabusUnit.values()) counts.put(unit, 0);
        String subjectId=subjects==null?com.quizmaster.model.Subject.OOP_ID:subjects.requireSelectedOwnedByActiveSession().getId();
        for (Question question : loaded.getQuestions()) if(question.getSubjectId().equals(subjectId)) counts.merge(question.getUnit(), 1, Integer::sum);
        List<UnitQuiz> quizzes = new ArrayList<>();
        var subject=subjects==null?null:subjects.requireSelectedOwnedByActiveSession();
        for (SyllabusUnit unit : SyllabusUnit.values()) quizzes.add(new UnitQuiz(unit, counts.get(unit),subject==null?com.quizmaster.model.Subject.OOP_ID:subject.getId(),subject==null?com.quizmaster.model.Subject.OOP_NAME:subject.getName()));
        return quizzes;
    }

    public synchronized QuizAttempt startQuiz(SyllabusUnit unit) throws IOException {
        UserSession session = sessions.require(Role.STUDENT);
        if (unit == null) throw new IllegalArgumentException("Choose a syllabus unit.");
        QuestionLoadResult loaded = loadQuestionBank();
        var subject=subjects==null?null:subjects.requireSelectedOwnedByActiveSession();
        String subjectId=subject==null?com.quizmaster.model.Subject.OOP_ID:subject.getId();
        List<Question> selected = loaded.getQuestions().stream().filter(question -> question.getUnit() == unit&&question.getSubjectId().equals(subjectId)).toList();
        if (selected.isEmpty()) throw new IllegalStateException("There are no questions available for " + unit.getTitle() + " yet.");
        UnitQuiz quiz = new UnitQuiz(unit, selected.size(),subjectId,subject==null?com.quizmaster.model.Subject.OOP_NAME:subject.getName());
        Duration duration = Duration.ofSeconds(SECONDS_PER_QUESTION * selected.size());
        QuizAttempt attempt = new QuizAttempt(session.getStudent(), quiz, selected, clock.instant(), duration);
        activeAttempts.put(attempt.getAttemptId(), attempt);
        return attempt;
    }

    public synchronized QuizResult submitAttempt(QuizAttempt attempt) throws IOException {
        UserSession session = sessions.require(Role.STUDENT);
        requireOwnedActiveAttempt(attempt, session);
        if (attempt.isSubmitted()) throw new IllegalStateException("This attempt has already been submitted.");
        QuizResult result = attempt.prepareSubmission(clock.instant());
        results.saveCompleted(result);
        attempt.markSubmitted();
        activeAttempts.remove(attempt.getAttemptId());
        return result;
    }

    public synchronized void abandonAttempt(QuizAttempt attempt) {
        UserSession session = sessions.require(Role.STUDENT);
        requireOwnedActiveAttempt(attempt, session);
        activeAttempts.remove(attempt.getAttemptId());
    }

    private QuestionLoadResult loadQuestionBank() throws IOException {
        QuestionLoadResult loaded = questions.load();
        if (!loaded.isClean()) {
            throw new IOException("The question bank contains invalid CSV rows. Ask the teacher to correct questions.csv before starting a quiz. "
                    + String.join("; ", loaded.getIssues()));
        }
        return loaded;
    }

    private void requireOwnedActiveAttempt(QuizAttempt attempt, UserSession session) {
        if (attempt == null || activeAttempts.get(attempt.getAttemptId()) != attempt) {
            throw new SecurityException("This quiz attempt is not active in this session.");
        }
        if (!attempt.getStudent().getStudentId().equalsIgnoreCase(session.getStudent().getStudentId())) {
            throw new SecurityException("This attempt belongs to a different student.");
        }
    }
}
