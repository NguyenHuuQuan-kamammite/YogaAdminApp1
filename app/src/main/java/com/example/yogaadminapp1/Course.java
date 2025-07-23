package com.example.yogaadminapp1;

public class Course {
    public String id; // Firestore document ID
    public int localId; // for legacy/local use, not used for Firestore
    public String dayOfWeek;
    public String time;
    public int capacity;
    public int durationMinutes;
    public double price;
    public String classType;
    public String description;

    public Course(String id, String dayOfWeek, String time, int capacity, int durationMinutes, double price, String classType, String description) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.capacity = capacity;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.classType = classType;
        this.description = description;
    }

    // Legacy constructor for local use (not used for Firestore)
    public Course(int localId, String dayOfWeek, String time, int capacity, int durationMinutes, double price, String classType, String description) {
        this.localId = localId;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.capacity = capacity;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.classType = classType;
        this.description = description;
    }
} 