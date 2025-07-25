package com.example.yogaadminapp1;

public class ClassInstance {
    public int id;
    public int courseId;
    public int teacherId;
    public String date;
    public String comments;

    public ClassInstance(int id, int courseId, int teacherId, String date, String comments) {
        this.id = id;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.date = date;
        this.comments = comments;
    }

    public ClassInstance(int courseId, int teacherId, String date, String comments) {
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.date = date;
        this.comments = comments;
    }
} 