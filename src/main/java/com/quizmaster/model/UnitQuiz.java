package com.quizmaster.model;

/** Stable quiz identity and the number of currently available unit questions. */
public final class UnitQuiz {
    private final SyllabusUnit unit;
    private final int questionCount;
    private final String subjectId, subjectName;

    public UnitQuiz(SyllabusUnit unit, int questionCount) {
        this(unit,questionCount,Subject.OOP_ID,Subject.OOP_NAME);
    }
    public UnitQuiz(SyllabusUnit unit,int questionCount,String subjectId,String subjectName) {
        if (unit == null) throw new IllegalArgumentException("Syllabus unit is required.");
        if (questionCount < 0) throw new IllegalArgumentException("Question count cannot be negative.");
        this.unit = unit;
        this.questionCount = questionCount;
        this.subjectId=subjectId;this.subjectName=subjectName;
    }

    public SyllabusUnit getUnit() { return unit; }
    public int getQuestionCount() { return questionCount; }
    public String getSubjectId(){return subjectId;} public String getSubjectName(){return subjectName;}
    public String getQuizId() { return subjectId.equals(Subject.OOP_ID)?"PCC202-" + unit.name().replace('_', '-'):subjectId+"-"+unit.name().replace('_','-'); }
    public String getQuizName() {
        String unitName = switch (unit) {
            case UNIT_I -> "Unit I";
            case UNIT_II -> "Unit II";
            case UNIT_III -> "Unit III";
            case UNIT_IV -> "Unit IV";
            case UNIT_V -> "Unit V";
        };
        return subjectName.equals(Subject.OOP_NAME)?unitName + " Practice Quiz":subjectName+" — "+unitName+" Practice Quiz";
    }
}
