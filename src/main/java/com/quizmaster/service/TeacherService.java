package com.quizmaster.service;

import com.quizmaster.model.Role;
import com.quizmaster.model.StudentProfile;
import com.quizmaster.security.SessionManager;
import com.quizmaster.security.TeacherCredentialStore;
import com.quizmaster.storage.StudentProfileStore;
import com.quizmaster.storage.TeacherAccountStore;
import com.quizmaster.model.TeacherProfile;

import java.io.IOException;
import java.util.List;

/** Teacher login and protected teacher-only data access. */
public final class TeacherService {
    private final TeacherCredentialStore credentials;
    private final StudentProfileStore students;
    private final SessionManager sessions;
    private final TeacherAccountStore accounts;

    public TeacherService(TeacherCredentialStore credentials, StudentProfileStore students, SessionManager sessions) {
        this.credentials = credentials;
        this.students = students;
        this.sessions = sessions;
        this.accounts = null;
    }

    public TeacherService(TeacherAccountStore accounts, StudentProfileStore students, SessionManager sessions) {
        this.credentials = null; this.students = students; this.sessions = sessions; this.accounts = accounts;
    }

    public boolean isConfigured() { return accounts != null || credentials.isConfigured(); }
    public void initialize(char[] password, char[] confirmation) throws IOException { credentials.initialize(password, confirmation); }

    public boolean login(char[] password) throws IOException {
        sessions.clear();
        if (!credentials.verify(password)) return false;
        sessions.startTeacher();
        return true;
    }

    public boolean login(String teacherId, char[] password) throws IOException {
        if (accounts == null) return login(password);
        sessions.clear();
        TeacherProfile teacher = accounts.authenticate(teacherId, password);
        if (teacher == null) return false;
        sessions.startTeacher(teacher);
        return true;
    }

    /** Registration is invitation-like; only a signed-in teacher can issue its one-use code. */
    public String createTeacherRegistrationCode()throws IOException{var session=sessions.require(Role.TEACHER);if(accounts==null)throw new IllegalStateException("Teacher account storage is unavailable.");return accounts.issueRegistrationCode(session.getTeacher().getTeacherId());}
    public TeacherProfile registerTeacherWithCode(String name,char[] password,char[] confirmation,String code)throws IOException{
        if(accounts==null)throw new IllegalStateException("Teacher account storage is unavailable.");return accounts.registerWithCode(name,password,confirmation,code);
    }
    public void changePassword(char[] current,char[] replacement,char[] confirmation)throws IOException{
        var session=sessions.require(Role.TEACHER);if(accounts==null)throw new IllegalStateException("Teacher account storage is unavailable.");accounts.changePassword(session.getTeacher().getTeacherId(),current,replacement,confirmation);
    }

    /** Representative teacher-only operation; later phases will add question/result services here. */
    public List<StudentProfile> listStudentProfiles() throws IOException {
        sessions.require(Role.TEACHER);
        return students.loadAll();
    }
}
