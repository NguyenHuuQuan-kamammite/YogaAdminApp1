package com.example.yogaadminapp1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "yoga_admin.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS courses (id INTEGER PRIMARY KEY AUTOINCREMENT, dayOfWeek TEXT, time TEXT, capacity INTEGER, durationMinutes INTEGER, price REAL, classType TEXT, description TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS teachers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT, phone TEXT, bio TEXT, photoUri TEXT, isSynced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS instances (id INTEGER PRIMARY KEY AUTOINCREMENT, courseId INTEGER, teacherId INTEGER, date TEXT, comments TEXT)");
    }

    // Migration for existing DBs: add isSynced column if not present
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS courses");
        db.execSQL("DROP TABLE IF EXISTS teachers");
        db.execSQL("DROP TABLE IF EXISTS instances");
        onCreate(db);
    }

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM courses", null);
        while (cursor.moveToNext()) {
            Course course = new Course(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("dayOfWeek")),
                cursor.getString(cursor.getColumnIndexOrThrow("time")),
                cursor.getInt(cursor.getColumnIndexOrThrow("capacity")),
                cursor.getInt(cursor.getColumnIndexOrThrow("durationMinutes")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                cursor.getString(cursor.getColumnIndexOrThrow("classType")),
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
            );
            courses.add(course);
        }
        cursor.close();
        return courses;
    }

    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM teachers", null);
        while (cursor.moveToNext()) {
            Teacher teacher = new Teacher(
                String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("id"))),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("email")),
                cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("bio")),
                cursor.getString(cursor.getColumnIndexOrThrow("photoUri"))
            );
            teachers.add(teacher);
        }
        cursor.close();
        return teachers;
    }

    public List<Teacher> getUnsyncedTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM teachers WHERE isSynced = 0", null);
        while (cursor.moveToNext()) {
            Teacher teacher = new Teacher(
                String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("id"))),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("email")),
                cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("bio")),
                cursor.getString(cursor.getColumnIndexOrThrow("photoUri"))
            );
            teachers.add(teacher);
        }
        cursor.close();
        return teachers;
    }

    public void markTeacherAsSynced(int localId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE teachers SET isSynced = 1 WHERE id = ?", new Object[]{localId});
    }

    // --- ClassInstance CRUD ---
    public long addInstance(ClassInstance instance) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("courseId", instance.courseId);
        values.put("teacherId", instance.teacherId);
        values.put("date", instance.date);
        values.put("comments", instance.comments);
        return db.insert("instances", null, values);
    }

    public List<ClassInstance> getAllInstances() {
        List<ClassInstance> instances = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM instances", null);
        while (cursor.moveToNext()) {
            ClassInstance instance = new ClassInstance(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("courseId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("teacherId")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                cursor.getString(cursor.getColumnIndexOrThrow("comments"))
            );
            instances.add(instance);
        }
        cursor.close();
        return instances;
    }

    public int updateInstance(ClassInstance instance) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("courseId", instance.courseId);
        values.put("teacherId", instance.teacherId);
        values.put("date", instance.date);
        values.put("comments", instance.comments);
        return db.update("instances", values, "id = ?", new String[]{String.valueOf(instance.id)});
    }

    public int deleteInstance(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("instances", "id = ?", new String[]{String.valueOf(id)});
    }

    public Course getCourseById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM courses WHERE id = ?", new String[]{String.valueOf(id)});
        Course course = null;
        if (cursor.moveToFirst()) {
            course = new Course(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("dayOfWeek")),
                cursor.getString(cursor.getColumnIndexOrThrow("time")),
                cursor.getInt(cursor.getColumnIndexOrThrow("capacity")),
                cursor.getInt(cursor.getColumnIndexOrThrow("durationMinutes")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                cursor.getString(cursor.getColumnIndexOrThrow("classType")),
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
            );
        }
        cursor.close();
        return course;
    }
} 