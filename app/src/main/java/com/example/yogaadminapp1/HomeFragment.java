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
import android.widget.LinearLayout;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.PopupMenu;
import com.google.firebase.auth.FirebaseAuth;

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
        btnTeacherManagement.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new TeacherManagementFragment())
                .addToBackStack(null)
                .commit();
        });
        // Setup More Actions dropdown
        ImageButton btnMoreActions = view.findViewById(R.id.btnMoreActions);
        btnMoreActions.setOnClickListener(v -> showMoreActionsMenu(v));
        // In onCreateView, after setting up the search bar and spinners:
        Spinner spinnerSearchDay = view.findViewById(R.id.spinnerSearchDay);
        Spinner spinnerSearchType = view.findViewById(R.id.spinnerSearchType);
        Spinner spinnerSearchTeacher = view.findViewById(R.id.spinnerSearchTeacher);
        ImageButton btnSearch = view.findViewById(R.id.btnSearch);

        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.days_of_week, android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchDay.setAdapter(dayAdapter);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.class_types, android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchType.setAdapter(typeAdapter);
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
            boolean noDay = selectedDay.equals("Select Day");
            boolean noType = selectedType.equals("Select Type");
            boolean noTeacher = selectedTeacher.equals("Select Teacher");
            for (Course c : dbHelper.getAllCourses()) {
                boolean match = true;
                if (!noDay && !c.dayOfWeek.equalsIgnoreCase(selectedDay)) match = false;
                if (!noType && !c.classType.equalsIgnoreCase(selectedType)) match = false;
                if (!noTeacher) {
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
            // If no filter is selected, show all
            if (noDay && noType && noTeacher) {
                filtered = dbHelper.getAllCourses();
            }
            courseList.clear();
            courseList.addAll(filtered);
            adapter.setHighlight(selectedDay, selectedType, selectedTeacher);
            adapter.notifyDataSetChanged();
        });
        return view;
    }

    private void showMoreActionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenu().add("Sync to Cloud");
        popup.getMenu().add("Restore from Cloud");
        popup.getMenu().add("Reset Database");
        popup.getMenu().add("Logout");
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Sync to Cloud")) {
                uploadAllCoursesToFirestore();
                uploadAllTeachersToFirestore();
                uploadAllInstancesToFirestore();
                Toast.makeText(getContext(), "Sync to cloud started", Toast.LENGTH_SHORT).show();
                return true;
            } else if (title.equals("Restore from Cloud")) {
                restoreFromCloud();
                return true;
            } else if (title.equals("Reset Database")) {
                new AlertDialog.Builder(getContext())
                    .setTitle("Reset Local Database")
                    .setMessage("Are you sure you want to delete ALL local data? This cannot be undone and will not affect cloud data.")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        dbHelper.clearAllData();
                        refreshCourseList();
                        Toast.makeText(getContext(), "Local database reset.", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
            } else if (title.equals("Logout")) {
                new AlertDialog.Builder(getContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        // Navigate back to login screen
                        requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new AdminLoginFragment())
                            .commit();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
            }
            return false;
        });
        popup.show();
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

                // Show confirmation dialog before saving
                StringBuilder confirmMsg = new StringBuilder();
                confirmMsg.append("Day: ").append(dayOfWeek).append("\n");
                confirmMsg.append("Time: ").append(time).append("\n");
                confirmMsg.append("Capacity: ").append(capacity).append("\n");
                confirmMsg.append("Duration: ").append(duration).append(" minutes\n");
                confirmMsg.append("Price: ").append(price).append("\n");
                confirmMsg.append("Type: ").append(classType).append("\n");
                if (!description.isEmpty()) {
                    confirmMsg.append("Description: ").append(description).append("\n");
                }

                new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Course Details")
                    .setMessage(confirmMsg.toString())
                    .setPositiveButton("Confirm", (dialogInterface, which) -> {
                        // Save to SQLite
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.execSQL("INSERT INTO courses (dayOfWeek, time, capacity, durationMinutes, price, classType, description) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                new Object[]{dayOfWeek, time, capacity, duration, price, classType, description});
                        Toast.makeText(getContext(), "Course added", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        refreshCourseList();
                    })
                    .setNegativeButton("Edit", null)
                    .show();
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
        for (Teacher teacher : dbHelper.getAllTeachers()) {
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

    // Restore all data from Firestore, replacing local data
    private void restoreFromCloud() {
        dbHelper.clearAllData();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Restore teachers
        db.collection("teachers").get().addOnSuccessListener(teacherSnap -> {
            for (com.google.firebase.firestore.DocumentSnapshot doc : teacherSnap.getDocuments()) {
                Teacher t = new Teacher(
                    doc.getId(),
                    doc.getString("name"),
                    doc.getString("email"),
                    doc.getString("phone"),
                    doc.getString("bio"),
                    doc.getString("photoUri")
                );
                dbHelper.upsertTeacher(t);
            }
            // Restore courses
            db.collection("courses").get().addOnSuccessListener(courseSnap -> {
                for (com.google.firebase.firestore.DocumentSnapshot doc : courseSnap.getDocuments()) {
                    Course c = new Course(
                        Integer.parseInt(doc.getId()),
                        doc.getString("dayOfWeek"),
                        doc.getString("time"),
                        doc.getLong("capacity").intValue(),
                        doc.getLong("durationMinutes").intValue(),
                        doc.getDouble("price"),
                        doc.getString("classType"),
                        doc.getString("description")
                    );
                    dbHelper.upsertCourse(c);
                }
                // Restore instances
                db.collection("instances").get().addOnSuccessListener(instanceSnap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : instanceSnap.getDocuments()) {
                        ClassInstance ci = new ClassInstance(
                            Integer.parseInt(doc.getId()),
                            doc.getLong("courseId").intValue(),
                            doc.getLong("teacherId").intValue(),
                            doc.getString("date"),
                            doc.getString("comments")
                        );
                        dbHelper.upsertInstance(ci);
                    }
                    Toast.makeText(getContext(), "Restore from cloud complete!", Toast.LENGTH_LONG).show();
                    refreshCourseList();
                });
            });
        });
    }

    // Inner adapter and viewholder for course list
    private class CourseListAdapter extends RecyclerView.Adapter<CourseViewHolder> {
        private final List<Course> courses;
        private String highlightDay = null, highlightType = null, highlightTeacher = null;
        CourseListAdapter(List<Course> courses) { this.courses = courses; }
        void setHighlight(String day, String type, String teacher) {
            highlightDay = day;
            highlightType = type;
            highlightTeacher = teacher;
        }
        @NonNull
        @Override
        public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
            return new CourseViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
            holder.bind(courses.get(position), highlightDay, highlightType, highlightTeacher);
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
        private android.text.Spanned highlight(String text, String query) {
            if (query == null || query.startsWith("Select") || query.isEmpty()) return android.text.Html.fromHtml(text);
            String regex = java.util.regex.Pattern.quote(query);
            return android.text.Html.fromHtml(text.replaceAll("(?i)"+regex, "<b><font color='#FF8800'>"+query+"</font></b>"));
        }
        public void bind(Course course, String highlightDay, String highlightType, String highlightTeacher) {
            tvCourseName.setText(highlight(course.classType, highlightType));
            tvTime.setText(course.time);
            tvDayOfWeek.setText(highlight(course.dayOfWeek, highlightDay));
            tvDescription.setText(course.description == null || course.description.isEmpty() ? "-" : course.description);
            tvType.setText(highlight(course.classType, highlightType));
            tvPrice.setText(String.format("$%.2f", course.price));
            tvCapacity.setText(String.valueOf(course.capacity));
            tvDuration.setText(course.durationMinutes + " min");
            // Detail view on click
            itemView.setOnClickListener(v -> {
                String msg = "Type: " + course.classType + "\n" +
                        "Day: " + course.dayOfWeek + "\n" +
                        "Time: " + course.time + "\n" +
                        "Capacity: " + course.capacity + "\n" +
                        "Duration: " + course.durationMinutes + " min\n" +
                        "Price: $" + course.price + "\n" +
                        "Description: " + (course.description == null ? "-" : course.description);
                new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Course Details")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show();
            });
            btnAddClass.setOnClickListener(v -> {
                getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, ClassInstanceFragment.newInstance(course.localId))
                    .addToBackStack(null)
                    .commit();
            });
            btnEdit.setOnClickListener(v -> {
                showEditCourseDialog(course);
            });
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Delete Course")
                    .setMessage("Are you sure you want to delete this course? This cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.execSQL("DELETE FROM courses WHERE id = ?", new Object[]{course.localId});
                        Toast.makeText(itemView.getContext(), "Course deleted", Toast.LENGTH_SHORT).show();
                        refreshCourseList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    // Show the add/edit course dialog pre-filled for editing
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

        // Set current values
        spinnerDayOfWeek.setSelection(dayAdapter.getPosition(course.dayOfWeek));
        spinnerClassType.setSelection(typeAdapter.getPosition(course.classType));
        spinnerTime.setSelection(timeAdapter.getPosition(course.time));
        editCapacity.setText(String.valueOf(course.capacity));
        editDuration.setText(String.valueOf(course.durationMinutes));
        editPrice.setText(String.valueOf(course.price));
        editDescription.setText(course.description == null ? "" : course.description);

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

                // Show confirmation dialog before saving
                StringBuilder confirmMsg = new StringBuilder();
                confirmMsg.append("Day: ").append(dayOfWeek).append("\n");
                confirmMsg.append("Time: ").append(time).append("\n");
                confirmMsg.append("Capacity: ").append(capacity).append("\n");
                confirmMsg.append("Duration: ").append(duration).append(" minutes\n");
                confirmMsg.append("Price: ").append(price).append("\n");
                confirmMsg.append("Type: ").append(classType).append("\n");
                if (!description.isEmpty()) {
                    confirmMsg.append("Description: ").append(description).append("\n");
                }

                new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Edit Course")
                    .setMessage(confirmMsg.toString())
                    .setPositiveButton("Confirm", (dialogInterface, which) -> {
                        // Update in SQLite
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.execSQL("UPDATE courses SET dayOfWeek=?, time=?, capacity=?, durationMinutes=?, price=?, classType=?, description=? WHERE id=?",
                                new Object[]{dayOfWeek, time, capacity, duration, price, classType, description, course.localId});
                        Toast.makeText(getContext(), "Course updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        refreshCourseList();
                    })
                    .setNegativeButton("Edit", null)
                    .show();
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
} 