package com.quizmaster.model;

/** A locally saved student identity used by the practice application. */
public final class StudentProfile {
    private final String studentId;
    private final String displayName;

    public StudentProfile(String studentId, String displayName) {
        this.studentId = studentId;
        this.displayName = displayName;
    }

    public String getStudentId() { return studentId; }
    public String getDisplayName() { return displayName; }
}
