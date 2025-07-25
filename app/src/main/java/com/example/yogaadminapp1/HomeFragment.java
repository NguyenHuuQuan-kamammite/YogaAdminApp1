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
import android.widget.Spinner;
import android.widget.EditText;
import android.app.AlertDialog;
import android.database.sqlite.SQLiteDatabase;
import android.widget.ArrayAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;
import java.util.ArrayList;
import android.widget.TextView;
import android.widget.ImageButton;

public class HomeFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private List<Course> courseList = new ArrayList<>();
    private CourseListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        // Setup RecyclerView for courses
        RecyclerView recyclerView = view.findViewById(R.id.recyclerCoursesHome);
        courseList = dbHelper.getAllCourses();
        adapter = new CourseListAdapter(courseList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        // Use FAB for adding course
        view.findViewById(R.id.fabAddCourseHome).setOnClickListener(v -> showAddCourseDialog());
        ImageButton btnTeacherManagement = view.findViewById(R.id.btnTeacherManagement);
        ImageButton btnSync = view.findViewById(R.id.btnSync);
        btnTeacherManagement.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new TeacherManagementFragment())
                .addToBackStack(null)
                .commit();
        });
        btnSync.setOnClickListener(v -> {
            if (isConnected()) {
                uploadAllCoursesToFirestore();
                uploadAllTeachersToFirestore();
                uploadAllInstancesToFirestore();
                Toast.makeText(getContext(), "Sync started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });
        // Setup search spinners
        Spinner spinnerSearchDay = view.findViewById(R.id.spinnerSearchDay);
        Spinner spinnerSearchType = view.findViewById(R.id.spinnerSearchType);
        Spinner spinnerSearchTeacher = view.findViewById(R.id.spinnerSearchTeacher);
        ImageButton btnSearch = view.findViewById(R.id.btnSearch);

        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.days_of_week, android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchDay.setAdapter(dayAdapter);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.class_types, android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchType.setAdapter(typeAdapter);
        // Teacher spinner
        List<Teacher> teacherList = dbHelper.getAllTeachers();
        List<String> teacherNames = new ArrayList<>();
        teacherNames.add("Select Teacher");
        for (Teacher t : teacherList) teacherNames.add(t.name);
        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, teacherNames);
        spinnerSearchTeacher.setAdapter(teacherAdapter);

        btnSearch.setOnClickListener(v -> {
            String selectedDay = spinnerSearchDay.getSelectedItem().toString();
            String selectedType = spinnerSearchType.getSelectedItem().toString();
            String selectedTeacher = spinnerSearchTeacher.getSelectedItem().toString();
            List<Course> filtered = new ArrayList<>();
            for (Course c : dbHelper.getAllCourses()) {
                boolean match = true;
                if (!selectedDay.equals("Select Day") && !c.dayOfWeek.equalsIgnoreCase(selectedDay)) match = false;
                if (!selectedType.equals("Select Type") && !c.classType.equalsIgnoreCase(selectedType)) match = false;
                if (!selectedTeacher.equals("Select Teacher")) {
                    // Check if any instance of this course is taught by the selected teacher
                    boolean found = false;
                    for (ClassInstance inst : dbHelper.getAllInstances()) {
                        if (inst.courseId == c.localId) {
                            for (Teacher t : teacherList) {
                                if (t.name.equals(selectedTeacher) && Integer.parseInt(t.id) == inst.teacherId) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) break;
                    }
                    if (!found) match = false;
                }
                if (match) filtered.add(c);
            }
            courseList.clear();
            courseList.addAll(filtered);
            adapter.notifyDataSetChanged();
        });
        return view;
    }

    private void refreshCourseList() {
        courseList.clear();
        courseList.addAll(dbHelper.getAllCourses());
        adapter.notifyDataSetChanged();
    }

    // Add the add course dialog logic (copied from CourseManagementFragment)
    private void showAddCourseDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_course, null, false);
        Spinner spinnerDayOfWeek = dialogView.findViewById(R.id.spinnerDayOfWeek);
        Spinner spinnerClassType = dialogView.findViewById(R.id.spinnerClassType);
        Spinner spinnerTime = dialogView.findViewById(R.id.spinnerTime);
        EditText editCapacity = dialogView.findViewById(R.id.editCapacity);
        EditText editDuration = dialogView.findViewById(R.id.editDuration);
        EditText editPrice = dialogView.findViewById(R.id.editPrice);
        EditText editDescription = dialogView.findViewById(R.id.editDescription);

        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.days_of_week, android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.class_types, android.R.layout.simple_spinner_dropdown_item);
        spinnerClassType.setAdapter(typeAdapter);
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.course_times, android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(timeAdapter);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle("Add Course")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int dayPos = spinnerDayOfWeek.getSelectedItemPosition();
                int typePos = spinnerClassType.getSelectedItemPosition();
                int timePos = spinnerTime.getSelectedItemPosition();
                String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
                String time = spinnerTime.getSelectedItem().toString();
                String capacityStr = editCapacity.getText().toString().trim();
                String durationStr = editDuration.getText().toString().trim();
                String priceStr = editPrice.getText().toString().trim();
                String classType = spinnerClassType.getSelectedItem().toString();
                String description = editDescription.getText().toString().trim();

                if (dayPos == 0 || typePos == 0 || timePos == 0 || capacityStr.isEmpty() || durationStr.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                int capacity, duration;
                double price;
                try {
                    capacity = Integer.parseInt(capacityStr);
                    duration = Integer.parseInt(durationStr);
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid number input", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Save to SQLite
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("INSERT INTO courses (dayOfWeek, time, capacity, durationMinutes, price, classType, description) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        new Object[]{dayOfWeek, time, capacity, duration, price, classType, description});
                Toast.makeText(getContext(), "Course added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshCourseList();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                spinnerDayOfWeek.setSelection(0);
                spinnerClassType.setSelection(0);
                spinnerTime.setSelection(0);
                editCapacity.setText("");
                editDuration.setText("");
                editPrice.setText("");
                editDescription.setText("");
            });
        });
        dialog.show();
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Teacher teacher : dbHelper.getUnsyncedTeachers()) {
            java.util.Map<String, Object> teacherMap = new java.util.HashMap<>();
            teacherMap.put("name", teacher.name);
            teacherMap.put("email", teacher.email);
            teacherMap.put("phone", teacher.phone);
            teacherMap.put("bio", teacher.bio);
            teacherMap.put("photoUri", teacher.photoUri == null ? "" : teacher.photoUri);
            db.collection("teachers")
                .document(String.valueOf(teacher.id)) // Use local ID as Firestore doc ID
                .set(teacherMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Sync", "Teacher uploaded: " + teacher.id);
                    try {
                        dbHelper.markTeacherAsSynced(Integer.parseInt(teacher.id));
                    } catch (Exception e) {
                        Log.e("Sync", "Failed to mark teacher as synced", e);
                    }
                })
                .addOnFailureListener(e -> Log.e("Sync", "Failed to upload teacher: " + teacher.id, e));
        }
    }

    private void uploadAllInstancesToFirestore() {
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
    }

    // Inner adapter and viewholder for course list
    private class CourseListAdapter extends RecyclerView.Adapter<CourseViewHolder> {
        private final List<Course> courses;
        CourseListAdapter(List<Course> courses) { this.courses = courses; }
        @NonNull
        @Override
        public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
            return new CourseViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
            holder.bind(courses.get(position));
        }
        @Override
        public int getItemCount() { return courses.size(); }
    }

    private class CourseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCourseName, tvTime, tvDayOfWeek, tvDescription, tvType, tvPrice, tvCapacity, tvDuration;
        private final ImageButton btnEdit, btnDelete;
        private final View btnAddClass;
        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvType = itemView.findViewById(R.id.tvType);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnAddClass = itemView.findViewById(R.id.btnAddClass);
        }
        public void bind(Course course) {
            tvCourseName.setText(course.classType);
            tvTime.setText(course.time);
            tvDayOfWeek.setText(course.dayOfWeek);
            tvDescription.setText(course.description == null || course.description.isEmpty() ? "-" : course.description);
            tvType.setText(course.classType);
            tvPrice.setText(String.format("$%.2f", course.price));
            tvCapacity.setText(String.valueOf(course.capacity));
            tvDuration.setText(course.durationMinutes + " min");
            btnAddClass.setOnClickListener(v -> {
                getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, ClassInstanceFragment.newInstance(course.localId))
                    .addToBackStack(null)
                    .commit();
            });
        }
    }
} 