package com.quizmaster.model;

import java.time.Instant;

/** A teacher-owned, student-visible subject. */
public final class Subject {
    public static final String OOP_ID = "PCC202-OOP-CG";
    public static final String OOP_NAME = "OOP & Computer Graphics";
    private final String id, name, description, ownerTeacherId;
    private final boolean active;
    private final Instant createdAt;
    public Subject(String id,String name,String description,String ownerTeacherId,boolean active,Instant createdAt) {
        this.id=required(id,"Subject ID"); this.name=required(name,"Subject name"); this.description=description==null?"":description.trim();
        this.ownerTeacherId=required(ownerTeacherId,"Owning teacher ID"); this.active=active;
        if(createdAt==null)throw new IllegalArgumentException("Creation timestamp is required."); this.createdAt=createdAt;
    }
    public String getId(){return id;} public String getName(){return name;} public String getDescription(){return description;}
    public String getOwnerTeacherId(){return ownerTeacherId;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;}
    @Override public String toString(){return name;}
    private static String required(String v,String label){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(label+" is required.");return v.trim();}
}
