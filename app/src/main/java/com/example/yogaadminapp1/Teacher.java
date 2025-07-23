package com.example.yogaadminapp1;

public class Teacher {
    public String id; // Firestore document ID
    public String name;
    public String email;
    public String phone;
    public String bio;
    public String photoUri;

    public Teacher() {}

    public Teacher(String id, String name, String email, String phone, String bio, String photoUri) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.bio = bio;
        this.photoUri = photoUri;
    }
} 