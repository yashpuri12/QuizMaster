package com.quizmaster.service;

import com.quizmaster.model.Role;
import com.quizmaster.model.Subject;
import com.quizmaster.security.SessionManager;
import com.quizmaster.storage.SubjectStore;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Subject selection and teacher-owned subject operations. */
public final class SubjectService {
    private final SubjectStore store;private final SessionManager sessions;
    public SubjectService(SubjectStore store,SessionManager sessions){this.store=store;this.sessions=sessions;}
    public List<Subject> activeForStudent()throws IOException{
        sessions.require(Role.STUDENT);return store.loadAll().stream().filter(Subject::isActive).toList();
    }
    public List<Subject> ownedByTeacher()throws IOException{
        var teacher=sessions.require(Role.TEACHER).getTeacher();return store.loadAll().stream().filter(s->s.getOwnerTeacherId().equalsIgnoreCase(teacher.getTeacherId())).toList();
    }
    public Subject createOwned(String name,String description)throws IOException{
        var teacher=sessions.require(Role.TEACHER).getTeacher();String subjectName=name==null?"":name.trim();
        if(subjectName.isEmpty()||subjectName.length()>100)throw new IllegalArgumentException("Subject name must be 1–100 characters.");
        if(description!=null&&description.length()>500)throw new IllegalArgumentException("Description must be 500 characters or fewer.");
        String id="SUB-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(java.util.Locale.ROOT);
        return store.add(new Subject(id,subjectName,description,teacher.getTeacherId(),true,Instant.now()));
    }
    public Subject setOwnedActive(String id,boolean active)throws IOException{
        var teacher=sessions.require(Role.TEACHER).getTeacher();Subject subject=store.find(id);
        if(subject==null||!subject.getOwnerTeacherId().equalsIgnoreCase(teacher.getTeacherId()))throw new SecurityException("This teacher cannot modify that subject.");
        return store.setActive(id,active);
    }
    public void select(String subjectId)throws IOException{
        var session=sessions.current();if(session==null)throw new SecurityException("Sign in before selecting a subject.");
        Subject subject=store.find(subjectId);
        if(subject==null)throw new IllegalArgumentException("Subject is no longer available.");
        if(session.getRole()==Role.STUDENT&&!subject.isActive())throw new SecurityException("This subject is inactive.");
        if(session.getRole()==Role.TEACHER&&!subject.getOwnerTeacherId().equalsIgnoreCase(session.getTeacher().getTeacherId()))throw new SecurityException("You can only select a subject you own.");
        session.selectSubject(subject);
    }
    public Subject requireSelectedOwnedByActiveSession()throws IOException{
        var session=sessions.current();if(session==null||session.getSubjectId()==null)throw new SecurityException("Select a subject before continuing.");
        Subject subject=store.find(session.getSubjectId());if(subject==null)throw new SecurityException("The selected subject is no longer available.");
        if(session.getRole()==Role.TEACHER&&!subject.getOwnerTeacherId().equalsIgnoreCase(session.getTeacher().getTeacherId()))throw new SecurityException("You do not own the selected subject.");
        if(session.getRole()==Role.STUDENT&&!subject.isActive())throw new SecurityException("The selected subject is inactive.");return subject;
    }
}
