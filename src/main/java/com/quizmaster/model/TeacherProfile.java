package com.quizmaster.model;

/** Public teacher identity; credential material is never part of this model. */
public final class TeacherProfile {
    private final String teacherId;
    private final String displayName;
    public TeacherProfile(String teacherId, String displayName) {
        if (teacherId == null || teacherId.isBlank() || displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Teacher ID and display name are required.");
        this.teacherId=teacherId.trim(); this.displayName=displayName.trim();
    }
    public String getTeacherId(){return teacherId;}
    public String getDisplayName(){return displayName;}
}
