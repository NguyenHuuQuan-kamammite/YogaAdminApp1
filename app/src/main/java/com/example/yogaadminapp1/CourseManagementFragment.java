package com.example.yogaadminapp1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import androidx.annotation.Nullable;
import java.util.HashMap;
import android.widget.ImageButton;
import android.database.sqlite.SQLiteDatabase;

public class CourseManagementFragment extends Fragment {
    private List<Course> courseList = new ArrayList<>();
    private CourseListAdapter adapter;
    private DatabaseHelper dbHelper;
    private int nextId = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_management, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerCourses);
        FloatingActionButton fabAddCourse = view.findViewById(R.id.fabAddCourse);

        dbHelper = new DatabaseHelper(requireContext());
        courseList = dbHelper.getAllCourses();
        adapter = new CourseListAdapter(courseList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fabAddCourse.setOnClickListener(v -> showAddCourseDialog());

        return view;
    }

    private void refreshCourseList() {
        courseList.clear();
        courseList.addAll(dbHelper.getAllCourses());
        adapter.notifyDataSetChanged();
    }

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

    private void showEditCourseDialog(Course course) {
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

        // Pre-fill fields
        spinnerDayOfWeek.setSelection(dayAdapter.getPosition(course.dayOfWeek));
        spinnerClassType.setSelection(typeAdapter.getPosition(course.classType));
        spinnerTime.setSelection(timeAdapter.getPosition(course.time));
        editCapacity.setText(String.valueOf(course.capacity));
        editDuration.setText(String.valueOf(course.durationMinutes));
        editPrice.setText(String.valueOf(course.price));
        editDescription.setText(course.description != null ? course.description : "");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle("Edit Course")
            .setView(dialogView)
            .setPositiveButton("Save", null)
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
                // Update SQLite
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("UPDATE courses SET dayOfWeek=?, time=?, capacity=?, durationMinutes=?, price=?, classType=?, description=? WHERE id=?",
                        new Object[]{dayOfWeek, time, capacity, duration, price, classType, description, course.localId});
                Toast.makeText(getContext(), "Course updated", Toast.LENGTH_SHORT).show();
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

    private void confirmDeleteCourse(Course course) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete this course?")
            .setPositiveButton("Delete", (dialog, which) -> {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM courses WHERE id=?", new Object[]{course.localId});
                Toast.makeText(getContext(), "Course deleted", Toast.LENGTH_SHORT).show();
                refreshCourseList();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // Update adapter to use Course objects
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
            btnEdit.setOnClickListener(v -> showEditCourseDialog(course));
            btnDelete.setOnClickListener(v -> confirmDeleteCourse(course));
        }
    }
} 