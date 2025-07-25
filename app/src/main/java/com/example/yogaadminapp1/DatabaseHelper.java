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
        db.execSQL("CREATE TABLE IF NOT EXISTS teachers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT, phone TEXT, bio TEXT, photoUri TEXT)");
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

    // Clear all data from all tables
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("courses", null, null);
        db.delete("teachers", null, null);
        db.delete("instances", null, null);
    }

    // Upsert (insert or replace) teacher by ID
    public void upsertTeacher(Teacher teacher) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("id", Integer.parseInt(teacher.id));
        values.put("name", teacher.name);
        values.put("email", teacher.email);
        values.put("phone", teacher.phone);
        values.put("bio", teacher.bio);
        values.put("photoUri", teacher.photoUri);
        db.insertWithOnConflict("teachers", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Upsert (insert or replace) course by ID
    public void upsertCourse(Course course) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("id", course.localId);
        values.put("dayOfWeek", course.dayOfWeek);
        values.put("time", course.time);
        values.put("capacity", course.capacity);
        values.put("durationMinutes", course.durationMinutes);
        values.put("price", course.price);
        values.put("classType", course.classType);
        values.put("description", course.description);
        db.insertWithOnConflict("courses", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Upsert (insert or replace) class instance by ID
    public void upsertInstance(ClassInstance instance) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("id", instance.id);
        values.put("courseId", instance.courseId);
        values.put("teacherId", instance.teacherId);
        values.put("date", instance.date);
        values.put("comments", instance.comments);
        db.insertWithOnConflict("instances", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
} 