package com.quizmaster.service;

import com.quizmaster.model.LeaderboardEntry;
import com.quizmaster.model.QuizResult;
import com.quizmaster.model.Role;
import com.quizmaster.security.SessionManager;
import com.quizmaster.storage.ResultLoadResult;
import com.quizmaster.storage.ResultStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Read-only, role-checked queries over completed results saved on disk. */
public final class ResultService {
    private final ResultStore store;
    private final SessionManager sessions;
    private final SubjectService subjects;

    public ResultService(ResultStore store, SessionManager sessions) {
        this.store = Objects.requireNonNull(store);
        this.sessions = Objects.requireNonNull(sessions);
        this.subjects=null;
    }
    public ResultService(ResultStore store,SessionManager sessions,SubjectService subjects){this.store=Objects.requireNonNull(store);this.sessions=Objects.requireNonNull(sessions);this.subjects=subjects;}

    /** Student history is limited to the active student's ID. */
    public ResultLoadResult studentHistory() throws IOException {
        var student = sessions.require(Role.STUDENT).getStudent();
        ResultLoadResult loaded = store.loadAll();
        List<QuizResult> own = new ArrayList<>();
        for (QuizResult result : loaded.getResults()) {
            if (result.getStudentId().equalsIgnoreCase(student.getStudentId())&&inSelectedSubject(result)) own.add(result);
        }
        own.sort(Comparator.comparing(QuizResult::getCompletedAt).reversed()
                .thenComparing(QuizResult::getAttemptId, String.CASE_INSENSITIVE_ORDER));
        return new ResultLoadResult(own, loaded.getIssues());
    }

    /** Teacher view of all real persisted results. */
    public ResultLoadResult teacherResults() throws IOException {
        sessions.require(Role.TEACHER);String subjectId=subjects==null?null:subjects.requireSelectedOwnedByActiveSession().getId();
        ResultLoadResult loaded = store.loadAll();
        List<QuizResult> sorted = new ArrayList<>();for(QuizResult r:loaded.getResults())if(subjectId==null||r.getSubjectId().equals(subjectId))sorted.add(r);
        sorted.sort(Comparator.comparing(QuizResult::getCompletedAt).reversed()
                .thenComparing(QuizResult::getAttemptId, String.CASE_INSENSITIVE_ORDER));
        return new ResultLoadResult(sorted, loaded.getIssues());
    }

    /** The category selector contains only quiz identities found in saved attempts. */
    public Map<String, String> availableStudentQuizCategories() throws IOException {
        sessions.require(Role.STUDENT);
        Map<String, String> categories = new LinkedHashMap<>();
        String subjectId=subjects==null?null:subjects.requireSelectedOwnedByActiveSession().getId();
        for (QuizResult result : store.loadAll().getResults()) {
            if((subjectId==null||result.getSubjectId().equals(subjectId))&&inSelectedSubject(result))categories.putIfAbsent(result.getQuizId(), result.getQuizName());
        }
        return categories;
    }

    /** Ranks each attempt independently, comparing only attempts with the selected stable quiz ID. */
    public List<LeaderboardEntry> studentLeaderboard(String quizId) throws IOException {
        sessions.require(Role.STUDENT);String subjectId=subjects==null?null:subjects.requireSelectedOwnedByActiveSession().getId();
        if (quizId == null || quizId.isBlank()) return List.of();
        ResultLoadResult loaded = store.loadAll();
        List<QuizResult> matching = new ArrayList<>();
        for (QuizResult result : loaded.getResults()) {
            if (result.getQuizId().equals(quizId)&&(subjectId==null||result.getSubjectId().equals(subjectId))&&inSelectedSubject(result)) matching.add(result);
        }
        Comparator<QuizResult> order = Comparator.comparingInt(QuizResult::getCorrectCount).reversed()
                .thenComparing(QuizResult::getPercentage, Comparator.reverseOrder())
                .thenComparing(QuizResult::getCompletedAt)
                .thenComparing(QuizResult::getStudentId, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(QuizResult::getAttemptId, String.CASE_INSENSITIVE_ORDER);
        matching.sort(order);
        List<LeaderboardEntry> ranked = new ArrayList<>();
        QuizResult previous = null;
        int rank = 0;
        for (int i = 0; i < matching.size(); i++) {
            QuizResult current = matching.get(i);
            if (previous == null || !sameRank(previous, current)) rank = i + 1;
            ranked.add(new LeaderboardEntry(rank, current));
            previous = current;
        }
        return List.copyOf(ranked);
    }
    private boolean inSelectedSubject(QuizResult result){if(subjects==null)return true;try{return result.getSubjectId().equals(subjects.requireSelectedOwnedByActiveSession().getId());}catch(IOException|SecurityException ex){throw new SecurityException("Select an authorized subject first.",ex);}}

    private static boolean sameRank(QuizResult first, QuizResult second) {
        return first.getCorrectCount() == second.getCorrectCount()
                && first.getPercentage().compareTo(second.getPercentage()) == 0
                && first.getCompletedAt().equals(second.getCompletedAt());
    }
}
