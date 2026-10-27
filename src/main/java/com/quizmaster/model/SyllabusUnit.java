package com.quizmaster.model;

/** PCC-202 syllabus areas used to categorize practice questions. */
public enum SyllabusUnit {
    UNIT_I("Unit I: OOP fundamentals and control structures"),
    UNIT_II("Unit II: Classes, objects, arrays, and input/output"),
    UNIT_III("Unit III: Inheritance, polymorphism, exception handling, and multithreading"),
    UNIT_IV("Unit IV: Computer Graphics primitives, scan conversion, polygon filling, and clipping"),
    UNIT_V("Unit V: 2D/3D transformations and projections");

    private final String title;

    SyllabusUnit(String title) { this.title = title; }
    public String getTitle() { return title; }
    public String getGenericTitle(){return switch(this){case UNIT_I->"Unit I";case UNIT_II->"Unit II";case UNIT_III->"Unit III";case UNIT_IV->"Unit IV";case UNIT_V->"Unit V";};}
    public String getTitleForSubject(String subjectName){return com.quizmaster.model.Subject.OOP_NAME.equals(subjectName)?title:getGenericTitle();}
    @Override public String toString() { return title; }

    public static SyllabusUnit fromId(String id) {
        for (SyllabusUnit unit : values()) if (unit.name().equals(id)) return unit;
        throw new IllegalArgumentException("Unknown syllabus unit: " + id);
    }
}
