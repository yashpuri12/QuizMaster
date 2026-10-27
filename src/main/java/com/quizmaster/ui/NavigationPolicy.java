package com.quizmaster.ui;

import com.quizmaster.model.Role;

/** Central names and mandatory post-login routes for the card-based UI. */
public final class NavigationPolicy {
    public static final String WELCOME="home",STUDENT_LOGIN="student-login",TEACHER_LOGIN="teacher-login";
    public static final String STUDENT_REGISTER="student-register",TEACHER_REGISTER="teacher-register",LEGACY_CLAIM="legacy-claim";
    public static final String STUDENT_SUBJECTS="student-subjects",TEACHER_SUBJECTS="teacher-subjects";
    private NavigationPolicy(){}
    public static String afterLogin(Role role){if(role==Role.STUDENT)return STUDENT_SUBJECTS;if(role==Role.TEACHER)return TEACHER_SUBJECTS;throw new IllegalArgumentException("An authenticated role is required.");}
    public static String loginFor(Role role){if(role==Role.STUDENT)return STUDENT_LOGIN;if(role==Role.TEACHER)return TEACHER_LOGIN;throw new IllegalArgumentException("An authenticated role is required.");}
    public static String registrationFor(Role role){if(role==Role.STUDENT)return STUDENT_REGISTER;if(role==Role.TEACHER)return TEACHER_REGISTER;throw new IllegalArgumentException("An authenticated role is required.");}
}
