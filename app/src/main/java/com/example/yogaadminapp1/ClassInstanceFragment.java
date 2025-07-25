package com.example.yogaadminapp1;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yogaadminapp1.databinding.FragmentClassInstanceBinding;
import com.example.yogaadminapp1.databinding.DialogAddInstanceBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.widget.ImageButton;

public class ClassInstanceFragment extends Fragment {
    private static final String ARG_COURSE_ID = "course_id";
    private int courseId;
    private DatabaseHelper dbHelper;
    private InstanceAdapter adapter;
    private List<ClassInstance> instanceList = new ArrayList<>();
    private HashMap<Integer, String> teacherIdNameMap = new HashMap<>();
    private List<Teacher> teacherList = new ArrayList<>();
    private Course course;
    private String highlightTeacher = null;
    private String highlightDate = null;

    public static ClassInstanceFragment newInstance(int courseId) {
        ClassInstanceFragment fragment = new ClassInstanceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COURSE_ID, courseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            courseId = getArguments().getInt(ARG_COURSE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_class_instance, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        course = dbHelper.getCourseById(courseId);
        setupRecyclerView(view);
        loadTeachers();
        loadInstances();
        // UI references
        Spinner spinnerTeacher = view.findViewById(R.id.spinnerSearchInstanceTeacher);
        EditText editDate = view.findViewById(R.id.editSearchInstanceDate);
        ImageButton btnSearch = view.findViewById(R.id.btnSearchInstance);
        // Populate teacher spinner
        List<String> teacherNames = new ArrayList<>();
        teacherNames.add("All");
        for (Teacher t : teacherList) teacherNames.add(t.name);
        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, teacherNames);
        spinnerTeacher.setAdapter(teacherAdapter);
        // Date picker logic
        editDate.setFocusable(false);
        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                String pickedDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(calendar.getTime());
                String pickedDay = new java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(calendar.getTime());
                if (course != null && !pickedDay.equalsIgnoreCase(course.dayOfWeek)) {
                    editDate.setError("Pick a " + course.dayOfWeek + " only");
                    editDate.setText("");
                    Toast.makeText(getContext(), "Date must be a " + course.dayOfWeek, Toast.LENGTH_SHORT).show();
                } else {
                    editDate.setError(null);
                    editDate.setText(pickedDate);
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });
        btnSearch.setOnClickListener(v -> {
            String selectedTeacher = spinnerTeacher.getSelectedItem().toString();
            String date = editDate.getText().toString().trim();
            List<ClassInstance> filtered = new ArrayList<>();
            for (ClassInstance ci : dbHelper.getAllInstances()) {
                if (ci.courseId != courseId) continue;
                boolean match = true;
                if (!selectedTeacher.equals("All")) {
                    String teacherName = teacherIdNameMap.get(ci.teacherId);
                    if (teacherName == null || !teacherName.equals(selectedTeacher)) match = false;
                }
                if (!date.isEmpty()) {
                    if (!ci.date.equals(date)) match = false;
                }
                if (match) filtered.add(ci);
            }
            instanceList.clear();
            instanceList.addAll(filtered);
            highlightTeacher = selectedTeacher.equals("All") ? null : selectedTeacher;
            highlightDate = date.isEmpty() ? null : date;
            adapter.notifyDataSetChanged();
        });
        // Enable the add button for class instance
        View fabAddInstance = view.findViewById(R.id.fabAddInstance);
        fabAddInstance.setOnClickListener(v -> showAddEditDialog(null));
        return view;
    }

    private void setupRecyclerView(View view) {
        adapter = new InstanceAdapter();
        RecyclerView recyclerView = view.findViewById(R.id.recyclerInstances);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadTeachers() {
        teacherList = dbHelper.getAllTeachers();
        teacherIdNameMap.clear();
        for (Teacher t : teacherList) {
            try {
                teacherIdNameMap.put(Integer.parseInt(t.id), t.name);
            } catch (Exception ignored) {}
        }
    }

    private void loadInstances() {
        instanceList = new ArrayList<>();
        for (ClassInstance ci : dbHelper.getAllInstances()) {
            if (ci.courseId == courseId) instanceList.add(ci);
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddEditDialog(@Nullable ClassInstance editInstance) {
        DialogAddInstanceBinding dialogBinding = DialogAddInstanceBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(dialogBinding.getRoot());
        AlertDialog dialog = builder.create();

        // Setup teacher spinner
        List<String> teacherNames = new ArrayList<>();
        for (Teacher t : teacherList) teacherNames.add(t.name);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, teacherNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerTeacher.setAdapter(spinnerAdapter);

        if (editInstance != null) {
            int teacherIndex = 0;
            for (int i = 0; i < teacherList.size(); i++) {
                if (Integer.parseInt(teacherList.get(i).id) == editInstance.teacherId) {
                    teacherIndex = i; break;
                }
            }
            dialogBinding.spinnerTeacher.setSelection(teacherIndex);
            dialogBinding.etDate.setText(editInstance.date);
            dialogBinding.etComments.setText(editInstance.comments);
        }

        // Date picker logic
        dialogBinding.etDate.setFocusable(false);
        dialogBinding.etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                String pickedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
                // Check if picked date matches course dayOfWeek
                String pickedDay = new SimpleDateFormat("EEEE", Locale.US).format(calendar.getTime());
                if (course != null && !pickedDay.equalsIgnoreCase(course.dayOfWeek)) {
                    dialogBinding.etDate.setError("Pick a " + course.dayOfWeek + " only");
                    Toast.makeText(getContext(), "Date must be a " + course.dayOfWeek, Toast.LENGTH_SHORT).show();
                    dialogBinding.etDate.setText("");
                } else {
                    dialogBinding.etDate.setError(null);
                    dialogBinding.etDate.setText(pickedDate);
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });

        dialogBinding.btnSave.setOnClickListener(v -> {
            int teacherPos = dialogBinding.spinnerTeacher.getSelectedItemPosition();
            if (teacherPos < 0) {
                Toast.makeText(getContext(), "Select a teacher", Toast.LENGTH_SHORT).show();
                return;
            }
            String date = dialogBinding.etDate.getText().toString().trim();
            if (TextUtils.isEmpty(date)) {
                dialogBinding.etDate.setError("Date required");
                return;
            }
            // Validate again on save
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date));
                String pickedDay = new SimpleDateFormat("EEEE", Locale.US).format(cal.getTime());
                if (course != null && !pickedDay.equalsIgnoreCase(course.dayOfWeek)) {
                    dialogBinding.etDate.setError("Pick a " + course.dayOfWeek + " only");
                    Toast.makeText(getContext(), "Date must be a " + course.dayOfWeek, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                dialogBinding.etDate.setError("Invalid date");
                return;
            }
            String comments = dialogBinding.etComments.getText().toString().trim();
            int teacherId = Integer.parseInt(teacherList.get(teacherPos).id);
            if (editInstance == null) {
                dbHelper.addInstance(new ClassInstance(courseId, teacherId, date, comments));
            } else {
                dbHelper.updateInstance(new ClassInstance(editInstance.id, courseId, teacherId, date, comments));
            }
            loadInstances();
            dialog.dismiss();
        });
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Remove filterInstances() or update to use findViewById if needed

    private class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.InstanceViewHolder> {
        @NonNull
        @Override
        public InstanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance, parent, false);
            return new InstanceViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull InstanceViewHolder holder, int position) {
            ClassInstance instance = instanceList.get(position);
            String teacherName = teacherIdNameMap.get(instance.teacherId) != null ? teacherIdNameMap.get(instance.teacherId) : "?";
            holder.tvDate.setText(highlight(instance.date, highlightDate));
            holder.tvTeacherName.setText("Teacher: " + highlight(teacherName, highlightTeacher));
            holder.tvComments.setText(instance.comments);
            holder.btnEdit.setOnClickListener(v -> showAddEditDialog(instance));
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Instance")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (d, w) -> {
                            dbHelper.deleteInstance(instance.id);
                            loadInstances();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
        @Override
        public int getItemCount() { return instanceList.size(); }
        private android.text.Spanned highlight(String text, String query) {
            if (query == null || query.isEmpty()) return android.text.Html.fromHtml(text);
            String regex = java.util.regex.Pattern.quote(query);
            return android.text.Html.fromHtml(text.replaceAll("(?i)"+regex, "<b><font color='#FF8800'>"+query+"</font></b>"));
        }
        class InstanceViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvTeacherName, tvComments;
            View btnEdit, btnDelete;
            InstanceViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
                tvComments = itemView.findViewById(R.id.tvComments);
                btnEdit = itemView.findViewById(R.id.btnEditInstance);
                btnDelete = itemView.findViewById(R.id.btnDeleteInstance);
            }
        }
    }
} 