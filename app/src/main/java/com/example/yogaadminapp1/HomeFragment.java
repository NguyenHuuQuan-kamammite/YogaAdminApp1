package com.example.yogaadminapp1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import org.json.JSONObject;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import java.io.IOException;

public class HomeFragment extends Fragment {
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        // Navigation logic will be added later
        view.findViewById(R.id.btnCourseManagement).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CourseManagementFragment())
                .addToBackStack(null)
                .commit();
        });
        view.findViewById(R.id.btnTeacherManagement).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new TeacherManagementFragment())
                .addToBackStack(null)
                .commit();
        });
        view.findViewById(R.id.btnSync).setOnClickListener(v -> {
            if (isConnected()) {
                uploadAllCoursesToFirestore();
                uploadAllTeachersToFirestore();
                /*uploadAllInstancesToFirestore();*/
                Toast.makeText(getContext(), "Sync started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void uploadAllCoursesToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Course course : dbHelper.getAllCourses()) {
            java.util.Map<String, Object> courseMap = new java.util.HashMap<>();
            courseMap.put("dayOfWeek", course.dayOfWeek);
            courseMap.put("time", course.time);
            courseMap.put("capacity", course.capacity);
            courseMap.put("durationMinutes", course.durationMinutes);
            courseMap.put("price", course.price);
            courseMap.put("classType", course.classType);
            courseMap.put("description", course.description);
            db.collection("courses")
                .document(String.valueOf(course.localId))
                .set(courseMap)
                .addOnSuccessListener(aVoid -> Log.d("Sync", "Course uploaded: " + course.localId))
                .addOnFailureListener(e -> Log.e("Sync", "Failed to upload course: " + course.localId, e));
        }
    }

    private void uploadAllTeachersToFirestore() {
        // Instead of direct Firestore upload, send to backend for account creation
        OkHttpClient client = new OkHttpClient();
        for (Teacher teacher : dbHelper.getAllTeachers()) {
            okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
            org.json.JSONObject json = new org.json.JSONObject();
            try {
                json.put("name", teacher.name);
                json.put("email", teacher.email);
                json.put("phone", teacher.phone);
                json.put("bio", teacher.bio);
                json.put("photoUri", teacher.photoUri);
            } catch (org.json.JSONException e) {
                Log.e("Sync", "JSON error for teacher: " + teacher.id, e);
                continue;
            }
            okhttp3.RequestBody body = okhttp3.RequestBody.create(json.toString(), JSON);
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://26.202.205.190:3000/add-teacher")
                .post(body)
                .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Teacher sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                    Log.e("Sync", "Teacher sync failed", e);
                }
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    String responseBody = response.body() != null ? response.body().string() : "<empty>";
                    requireActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Teacher synced to cloud", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Teacher sync error: " + response.code() + ", " + responseBody, Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.d("Sync", "Teacher sync response: " + response.code() + ", body: " + responseBody);
                }
            });
        }
    }

    /*private void uploadAllInstancesToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (ClassInstance instance : dbHelper.getAllInstances()) {
            java.util.Map<String, Object> instanceMap = new java.util.HashMap<>();
            instanceMap.put("courseId", instance.courseId);
            instanceMap.put("teacherId", instance.teacherId);
            instanceMap.put("date", instance.date);
            instanceMap.put("comments", instance.comments);
            db.collection("instances")
                .document(String.valueOf(instance.id))
                .set(instanceMap)
                .addOnSuccessListener(aVoid -> Log.d("Sync", "Instance uploaded: " + instance.id))
                .addOnFailureListener(e -> Log.e("Sync", "Failed to upload instance: " + instance.id, e));
        }
    }*/
} 