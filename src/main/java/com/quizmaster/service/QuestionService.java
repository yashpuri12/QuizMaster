package com.quizmaster.service;

import com.quizmaster.model.Question;
import com.quizmaster.model.Role;
import com.quizmaster.security.SessionManager;
import com.quizmaster.storage.QuestionLoadResult;
import com.quizmaster.storage.QuestionStore;
import com.quizmaster.model.Subject;

import java.io.IOException;

/** Question-bank operations. Every read or mutation requires a teacher session. */
public final class QuestionService {
    private final QuestionStore store;
    private final SessionManager sessions;
    private final SubjectService subjects;

    public QuestionService(QuestionStore store, SessionManager sessions) {
        this.store = store;
        this.sessions = sessions;
        this.subjects = null;
    }

    public QuestionService(QuestionStore store, SessionManager sessions, SubjectService subjects) {
        this.store=store; this.sessions=sessions; this.subjects=subjects;
    }

    public QuestionLoadResult listQuestions() throws IOException {
        sessions.require(Role.TEACHER);
        if (subjects == null) return store.load();
        String subjectId=subjects.requireSelectedOwnedByActiveSession().getId();
        var all=store.load();
        return new QuestionLoadResult(all.getQuestions().stream().filter(q->q.getSubjectId().equals(subjectId)).toList(),all.getIssues());
    }
    public String selectedSubjectId() throws IOException { sessions.require(Role.TEACHER);return subjects==null?Subject.OOP_ID:subjects.requireSelectedOwnedByActiveSession().getId(); }
    public String selectedSubjectName() throws IOException { sessions.require(Role.TEACHER);return subjects==null?Subject.OOP_NAME:subjects.requireSelectedOwnedByActiveSession().getName(); }

    public void addQuestion(Question question) throws IOException {
        var session=sessions.require(Role.TEACHER);
        if(subjects!=null && !question.getSubjectId().equals(subjects.requireSelectedOwnedByActiveSession().getId()))throw new SecurityException("Question does not belong to the selected subject.");
        if(subjects==null && !question.getSubjectId().equals(Subject.OOP_ID) && session.getSubjectId()!=null)throw new SecurityException("Question is outside the selected subject.");
        store.add(question);
    }

    public void updateQuestion(Question question) throws IOException {
        var session=sessions.require(Role.TEACHER);
        if(subjects!=null) { var subject=subjects.requireSelectedOwnedByActiveSession(); var loaded=store.load(); if(loaded.getQuestions().stream().noneMatch(q->q.getId().equalsIgnoreCase(question.getId())&&q.getSubjectId().equals(subject.getId())))throw new SecurityException("You cannot edit a question outside your selected subject."); if(!question.getSubjectId().equals(subject.getId()))throw new SecurityException("Question subject cannot be changed during editing."); }
        store.update(question);
    }

    public void deleteQuestion(String questionId) throws IOException {
        sessions.require(Role.TEACHER);
        if(subjects!=null) { var subject=subjects.requireSelectedOwnedByActiveSession(); var loaded=store.load(); if(loaded.getQuestions().stream().noneMatch(q->q.getId().equalsIgnoreCase(questionId)&&q.getSubjectId().equals(subject.getId())))throw new SecurityException("You cannot delete a question outside your selected subject."); }
        store.delete(questionId);
    }
}
