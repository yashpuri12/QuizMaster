package com.quizmaster.model;

/** Immutable authenticated identity for the current application session. */
public final class UserSession {
    private final Role role;
    private final StudentProfile student;
    private final TeacherProfile teacher;
    private String subjectId;

    private UserSession(Role role, StudentProfile student, TeacherProfile teacher) {
        this.role = role;
        this.student = student;
        this.teacher = teacher;
    }

    public static UserSession student(StudentProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Student profile is required.");
        return new UserSession(Role.STUDENT, profile, null);
    }

    public static UserSession teacher() { return teacher(new TeacherProfile("LOCAL-TEACHER", "Local teacher")); }
    public static UserSession teacher(TeacherProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Teacher profile is required.");
        return new UserSession(Role.TEACHER, null, profile);
    }
    public Role getRole() { return role; }
    public StudentProfile getStudent() { return student; }
    public TeacherProfile getTeacher() { return teacher; }
    public synchronized String getSubjectId() { return subjectId; }
    public synchronized void clearSubject(){subjectId=null;}
    public synchronized void selectSubject(Subject subject) { if(subject==null)throw new IllegalArgumentException("Subject is required."); subjectId=subject.getId(); }
}
