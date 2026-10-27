package com.quizmaster;

import com.quizmaster.model.LeaderboardEntry;
import com.quizmaster.model.QuizResult;
import com.quizmaster.model.Role;
import com.quizmaster.model.StudentProfile;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.security.SessionManager;
import com.quizmaster.service.ResultService;
import com.quizmaster.storage.ResultLoadResult;
import com.quizmaster.storage.ResultStore;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Dependency-free checks for Phase 5 saved history and leaderboard behavior. */
public final class Phase5Tests {
    private static int passed;
    private Phase5Tests() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "verify-restart".equals(args[0])) { verifyRestart(Path.of(args[1])); return; }
        Path root = Files.createTempDirectory(Path.of("build"), "phase5-test-");
        try {
            testEmptyAndMalformed(root.resolve("empty"));
            testHistoryRankingAndRoles(root.resolve("rank"));
            System.out.println("Phase 5 checks passed: " + passed);
        } finally {
            try (var paths = Files.walk(root)) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) { } }); }
        }
    }

    private static void testEmptyAndMalformed(Path dir) throws Exception {
        SessionManager sessions = new SessionManager(); ResultService service = new ResultService(new ResultStore(dir), sessions);
        expect(SecurityException.class, service::studentHistory, "history requires a student session");
        sessions.startStudent(new StudentProfile("S-EMPTY", "Empty"));
        check(service.studentHistory().getResults().isEmpty(), "missing result file creates no fabricated history");
        check(service.availableStudentQuizCategories().isEmpty(), "empty saved history has no leaderboard categories");
        Files.writeString(dir.resolve("results.csv"), "not,a,valid,header\nmalformed,row\n", StandardCharsets.UTF_8);
        ResultLoadResult loaded = service.studentHistory();
        check(loaded.getResults().isEmpty() && !loaded.getIssues().isEmpty(), "malformed results are reported and do not crash history");
        sessions.startTeacher();
        check(service.teacherResults().getResults().isEmpty(), "teacher can read an empty valid-record set beside malformed data");
        expect(SecurityException.class, () -> service.studentLeaderboard("Q"), "teacher cannot use student leaderboard service");
    }

    private static void testHistoryRankingAndRoles(Path dir) throws Exception {
        ResultStore store = new ResultStore(dir);
        Instant t1 = Instant.parse("2026-01-01T10:00:00Z"), t2 = Instant.parse("2026-01-01T10:05:00Z"), t3 = Instant.parse("2026-01-01T10:10:00Z");
        QuizResult first = result("A1", "STU-A", "A", "QUIZ-A", 4, 5, t1);
        QuizResult tie = result("B1", "STU-B", "B", "QUIZ-A", 4, 5, t1);
        QuizResult next = result("A2", "STU-A", "A", "QUIZ-A", 3, 5, t2);
        QuizResult other = result("C1", "STU-C", "C", "QUIZ-B", 5, 5, t3);
        store.saveCompleted(first); store.saveCompleted(tie); store.saveCompleted(next); store.saveCompleted(other);
        SessionManager sessions = new SessionManager(); ResultService service = new ResultService(store, sessions);
        expect(SecurityException.class, service::teacherResults, "teacher results require teacher authorization");
        sessions.startStudent(new StudentProfile("STU-A", "Current A"));
        List<QuizResult> history = service.studentHistory().getResults();
        check(history.size() == 2 && history.get(0).getAttemptId().equals("A2"), "student history is filtered and newest first");
        check(service.availableStudentQuizCategories().size() == 2, "leaderboard filter offers every quiz identity present in saved attempts");
        List<LeaderboardEntry> ranks = service.studentLeaderboard("QUIZ-A");
        check(ranks.size() == 3, "leaderboard includes each attempt, including multiple attempts by one student");
        check(ranks.get(0).getRank() == 1 && ranks.get(1).getRank() == 1 && ranks.get(2).getRank() == 3,
                "exact primary-field ties use competition ranks 1, 1, 3");
        check(ranks.stream().noneMatch(e -> e.getResult().getQuizId().equals("QUIZ-B")), "leaderboard excludes attempts from other quiz identities");
        check(service.studentLeaderboard("QUIZ-B").size() == 1, "specific quiz filter returns only that quiz's attempts");
        check(service.studentLeaderboard("UNKNOWN").isEmpty(), "unknown quiz identity returns no fabricated rows");
        sessions.startTeacher();
        check(service.teacherResults().getResults().size() == 4, "teacher overview includes all actual saved attempts");
        expect(SecurityException.class, service::studentHistory, "teacher cannot access student history service");
        check(runFreshJvm(dir), "result records reload in a fresh JVM");
        String csv = Files.readString(dir.resolve("results.csv"), StandardCharsets.UTF_8);
        check(!csv.contains("correct_answer_index") && !csv.contains("selected_answer") && !csv.contains("answer_options"), "result CSV contains no answer keys or selections");
    }

    private static QuizResult result(String attempt, String id, String name, String quiz, int correct, int total, Instant time) {
        int wrong = total - correct;
        BigDecimal percent = BigDecimal.valueOf(correct * 100L).divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
        return new QuizResult(attempt,id,name,quiz,"Practice " + quiz,SyllabusUnit.UNIT_I,total,correct,wrong,0,percent,time);
    }

    private static boolean runFreshJvm(Path dir) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        Process p = new ProcessBuilder(java,"-cp",System.getProperty("java.class.path"),Phase5Tests.class.getName(),"verify-restart",dir.toAbsolutePath().toString()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8); int code=p.waitFor(); if(code!=0)System.out.print(out); return code==0;
    }
    private static void verifyRestart(Path dir) throws Exception {
        ResultLoadResult loaded=new ResultStore(dir).loadAll(); if(loaded.getResults().size()!=4||!loaded.isClean())throw new AssertionError("Four saved results expected after restart.");
        var times=loaded.getResults().stream().map(QuizResult::getCompletedAt).toList(); if(times.size()!=4)throw new AssertionError("Saved results missing.");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("FAILED: "+message);passed++;System.out.println("PASS: "+message);}
    private static void expect(Class<? extends Throwable> type,Action action,String message){try{action.run();}catch(Throwable t){if(type.isInstance(t)){check(true,message);return;}throw new AssertionError("FAILED: "+message,t);}throw new AssertionError("FAILED: "+message+" (no exception)");}
    @FunctionalInterface private interface Action{void run()throws Exception;}
}
