package com.quizmaster.security;

import com.quizmaster.model.Role;
import com.quizmaster.model.UserSession;

/** Holds the in-memory user session and enforces role checks at service boundaries. */
public final class SessionManager {
    private UserSession activeSession;

    public synchronized void startStudent(com.quizmaster.model.StudentProfile profile) {
        activeSession = UserSession.student(profile);
    }

    public synchronized void startTeacher() { activeSession = UserSession.teacher(); }
    public synchronized void startTeacher(com.quizmaster.model.TeacherProfile profile) { activeSession = UserSession.teacher(profile); }
    public synchronized void clear() { activeSession = null; }
    public synchronized UserSession current() { return activeSession; }

    public synchronized UserSession require(Role role) {
        if (activeSession == null || activeSession.getRole() != role) {
            throw new SecurityException("This action requires an active " + role.name().toLowerCase() + " session.");
        }
        return activeSession;
    }
}
