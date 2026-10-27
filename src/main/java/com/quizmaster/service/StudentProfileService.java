package com.quizmaster.service;

import com.quizmaster.model.StudentProfile;
import com.quizmaster.security.SessionManager;
import com.quizmaster.storage.StudentProfileStore;

import java.io.IOException;
import java.util.List;

/** Profile operations and student session authorization. */
public final class StudentProfileService {
    private final StudentProfileStore store;
    private final SessionManager sessions;

    public StudentProfileService(StudentProfileStore store, SessionManager sessions) {
        this.store = store;
        this.sessions = sessions;
    }

    public StudentProfile create(String name, String id) throws IOException { return store.create(name, id); }
    public StudentProfile register(String name, String id, char[] password, char[] confirmation) throws IOException {
        StudentProfile profile = store.register(name, id, password, confirmation);
        sessions.clear();
        return profile;
    }
    public StudentProfile registerGenerated(String name,char[] password,char[] confirmation)throws IOException{return store.registerGenerated(name,password,confirmation);}
    public StudentProfile claimLegacyProfile(String name,String id,char[] password,char[] confirmation)throws IOException{return store.claimLegacyProfile(name,id,password,confirmation);}
    public boolean login(String id, char[] password) throws IOException {
        sessions.clear();
        StudentProfile profile = store.authenticate(id, password);
        if (profile == null) return false;
        sessions.startStudent(profile);
        return true;
    }
    public void changePassword(char[] current,char[] replacement,char[] confirmation)throws IOException{
        var session=sessions.require(com.quizmaster.model.Role.STUDENT);store.changePassword(session.getStudent().getStudentId(),current,replacement,confirmation);
    }
    public StudentProfile find(String id) throws IOException { return store.find(id); }
    public List<StudentProfile> list() throws IOException { return store.loadAll(); }
    public void activate(StudentProfile profile) { sessions.startStudent(profile); }
}
