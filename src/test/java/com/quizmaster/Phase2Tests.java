package com.quizmaster;

import com.quizmaster.model.StudentProfile;
import com.quizmaster.security.SessionManager;
import com.quizmaster.security.TeacherCredentialStore;
import com.quizmaster.service.StudentProfileService;
import com.quizmaster.service.TeacherService;
import com.quizmaster.storage.StudentProfileStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free behavioral checks for Phase 2. Run through test.bat. */
public final class Phase2Tests {
    private static int passed;
    private static final String TEST_TEACHER_PASSWORD = "QuizMaster-Teacher-2026";
    private Phase2Tests() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "verify-restart".equals(args[0])) {
            verifyRestart(Path.of(args[1]));
            return;
        }
        Path temp = Files.createTempDirectory(Path.of("build"), "phase2-test-");
        try {
            StudentProfileStore store = new StudentProfileStore(temp.resolve("store"));
            check(store.loadAll().isEmpty(), "missing profile file creates an empty store");
            expect(IllegalArgumentException.class, () -> store.create("  ", "STU01"), "empty name rejected");
            expect(IllegalArgumentException.class, () -> store.create("A Student", "bad/id"), "invalid ID rejected");
            StudentProfile first = store.create("Riya, \"RJ\"", "STU-01");
            expect(IllegalArgumentException.class, () -> store.create("Another", "stu-01"), "duplicate IDs rejected case-insensitively");
            StudentProfileStore afterRestart = new StudentProfileStore(temp.resolve("store"));
            StudentProfile restored = afterRestart.find("stu-01");
            check(restored != null && restored.getDisplayName().equals(first.getDisplayName()), "profile persists and quoted CSV fields reload");

            SessionManager sessions = new SessionManager();
            StudentProfileService studentService = new StudentProfileService(afterRestart, sessions);
            TeacherCredentialStore credentials = new TeacherCredentialStore(temp.resolve("store"));
            TeacherService teacherService = new TeacherService(credentials, afterRestart, sessions);
            expect(SecurityException.class, teacherService::listStudentProfiles, "teacher operation denied without a session");
            studentService.activate(restored);
            expect(SecurityException.class, teacherService::listStudentProfiles, "student cannot call teacher-only operation");

            expect(IllegalArgumentException.class,
                    () -> credentials.initialize("short1".toCharArray(), "short1".toCharArray()), "weak password rejected");
            char[] teacherPassword = TEST_TEACHER_PASSWORD.toCharArray();
            expect(IllegalArgumentException.class,
                    () -> credentials.initialize(teacherPassword, "Different-Password-1".toCharArray()), "mismatched password rejected");
            teacherService.initialize(teacherPassword, teacherPassword.clone());
            check(runRestartProcess(temp.resolve("store")), "student profile and teacher credential persist across a fresh JVM");
            check(!credentials.verify("Wrong-Teacher-Password-1".toCharArray()), "incorrect teacher password rejected");
            check(!teacherService.login("Wrong-Teacher-Password-1".toCharArray()), "incorrect teacher login rejected");
            check(sessions.current() == null, "failed teacher login leaves no active session");
            TeacherService afterRestartTeacher = new TeacherService(
                    new TeacherCredentialStore(temp.resolve("store")), afterRestart, sessions);
            check(afterRestartTeacher.login(teacherPassword), "correct teacher password accepted after restart");
            check(afterRestartTeacher.listStudentProfiles().size() == 1, "teacher session can access protected operation");
            String credentialFile = Files.readString(temp.resolve("store/teacher.properties"), StandardCharsets.UTF_8);
            check(!credentialFile.contains(new String(teacherPassword)), "teacher password is not stored in plain text");
            sessions.clear();
            check(sessions.current() == null, "logout clears active session");
            expect(SecurityException.class, afterRestartTeacher::listStudentProfiles, "cleared session loses teacher access");
            System.out.println("Phase 2 checks passed: " + passed);
        } finally {
            try (var paths = Files.walk(temp)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private static boolean runRestartProcess(Path dataDirectory) throws Exception {
        String executable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        Process process = new ProcessBuilder(executable, "-cp", System.getProperty("java.class.path"),
                Phase2Tests.class.getName(), "verify-restart", dataDirectory.toAbsolutePath().toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) System.out.print(output);
        return exit == 0;
    }

    private static void verifyRestart(Path dataDirectory) throws Exception {
        StudentProfile profile = new StudentProfileStore(dataDirectory).find("STU-01");
        if (profile == null || !profile.getDisplayName().equals("Riya, \"RJ\"")) throw new AssertionError("Student profile did not persist across process restart.");
        if (!new TeacherCredentialStore(dataDirectory).verify(TEST_TEACHER_PASSWORD.toCharArray())) {
            throw new AssertionError("Teacher credential did not persist across process restart.");
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void expect(Class<? extends Throwable> type, ThrowingAction action, String name) throws Exception {
        try { action.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) { check(true, name); return; }
            throw new AssertionError("FAILED: " + name + " (unexpected " + thrown + ")", thrown);
        }
        throw new AssertionError("FAILED: " + name + " (no exception)");
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
}
