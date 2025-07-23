package com.example.yogaadminapp1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.database.sqlite.SQLiteDatabase;
import android.widget.ImageButton;
import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;

public class TeacherManagementFragment extends Fragment {
    private List<Teacher> teacherList = new ArrayList<>();
    private TeacherListAdapter adapter;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_management, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerTeachers);
        FloatingActionButton fabAddTeacher = view.findViewById(R.id.fabAddTeacher);

        dbHelper = new DatabaseHelper(requireContext());
        teacherList = dbHelper.getAllTeachers();
        adapter = new TeacherListAdapter(teacherList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fabAddTeacher.setOnClickListener(v -> showAddTeacherDialog());

        return view;
    }

    private void refreshTeacherList() {
        teacherList.clear();
        teacherList.addAll(dbHelper.getAllTeachers());
        adapter.notifyDataSetChanged();
    }

    private void showAddTeacherDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_teacher, null, false);
        EditText editName = dialogView.findViewById(R.id.editName);
        EditText editEmail = dialogView.findViewById(R.id.editEmail);
        EditText editPhone = dialogView.findViewById(R.id.editPhone);
        EditText editBio = dialogView.findViewById(R.id.editBio);
        EditText editPhotoUri = dialogView.findViewById(R.id.editPhotoUri);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle("Add Teacher")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = editName.getText().toString().trim();
                String email = editEmail.getText().toString().trim();
                String phone = editPhone.getText().toString().trim();
                String bio = editBio.getText().toString().trim();
                String photoUri = editPhotoUri.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Save to SQLite only
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("INSERT INTO teachers (name, email, phone, bio, photoUri) VALUES (?, ?, ?, ?, ?)",
                        new Object[]{name, email, phone, bio, photoUri});
                refreshTeacherList();
                Toast.makeText(getContext(), "Teacher added locally", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                editName.setText("");
                editEmail.setText("");
                editPhone.setText("");
                editBio.setText("");
                editPhotoUri.setText("");
            });
        });
        dialog.show();
    }

    private void showEditTeacherDialog(Teacher teacher) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_teacher, null, false);
        EditText editName = dialogView.findViewById(R.id.editName);
        EditText editEmail = dialogView.findViewById(R.id.editEmail);
        EditText editPhone = dialogView.findViewById(R.id.editPhone);
        EditText editBio = dialogView.findViewById(R.id.editBio);
        EditText editPhotoUri = dialogView.findViewById(R.id.editPhotoUri);

        // Pre-fill fields
        editName.setText(teacher.name);
        editEmail.setText(teacher.email);
        editPhone.setText(teacher.phone);
        editBio.setText(teacher.bio);
        editPhotoUri.setText(teacher.photoUri);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle("Edit Teacher")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = editName.getText().toString().trim();
                String email = editEmail.getText().toString().trim();
                String phone = editPhone.getText().toString().trim();
                String bio = editBio.getText().toString().trim();
                String photoUri = editPhotoUri.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Update SQLite
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("UPDATE teachers SET name=?, email=?, phone=?, bio=?, photoUri=? WHERE id=?",
                        new Object[]{name, email, phone, bio, photoUri, teacher.id});
                Toast.makeText(getContext(), "Teacher updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshTeacherList();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                editName.setText("");
                editEmail.setText("");
                editPhone.setText("");
                editBio.setText("");
                editPhotoUri.setText("");
            });
        });
        dialog.show();
    }

    private void confirmDeleteTeacher(Teacher teacher) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Teacher")
            .setMessage("Are you sure you want to delete this teacher?")
            .setPositiveButton("Delete", (dialog, which) -> {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM teachers WHERE id=?", new Object[]{teacher.id});
                Toast.makeText(getContext(), "Teacher deleted", Toast.LENGTH_SHORT).show();
                refreshTeacherList();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private class TeacherListAdapter extends RecyclerView.Adapter<TeacherViewHolder> {
        private final List<Teacher> teachers;
        TeacherListAdapter(List<Teacher> teachers) { this.teachers = teachers; }
        @NonNull
        @Override
        public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher, parent, false);
            return new TeacherViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
            holder.bind(teachers.get(position));
        }
        @Override
        public int getItemCount() { return teachers.size(); }
    }

    private class TeacherViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvEmail, tvPhone, tvBio, tvPhotoUri;
        private final ImageButton btnEdit, btnDelete;
        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvBio = itemView.findViewById(R.id.tvBio);
            tvPhotoUri = itemView.findViewById(R.id.tvPhotoUri);
            btnEdit = itemView.findViewById(R.id.btnEditTeacher);
            btnDelete = itemView.findViewById(R.id.btnDeleteTeacher);
        }
        public void bind(Teacher teacher) {
            tvName.setText(teacher.name);
            tvEmail.setText(teacher.email);
            tvPhone.setText(teacher.phone);
            tvBio.setText(teacher.bio);
            tvPhotoUri.setText(teacher.photoUri);
            btnEdit.setOnClickListener(v -> showEditTeacherDialog(teacher));
            btnDelete.setOnClickListener(v -> confirmDeleteTeacher(teacher));
        }
    }
} 