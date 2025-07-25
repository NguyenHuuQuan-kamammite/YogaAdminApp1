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
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.app.Activity;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import android.content.Context;
import android.widget.Button;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

public class TeacherManagementFragment extends Fragment {
    private List<Teacher> teacherList = new ArrayList<>();
    private TeacherListAdapter adapter;
    private DatabaseHelper dbHelper;
    private Uri selectedImageUri = null;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_IMAGE_PICK = 1002;
    private ImageView imgTeacherPhoto;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_management, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerTeachers);
        FloatingActionButton fabAddTeacher = view.findViewById(R.id.fabAddTeacher);
        Spinner spinnerSearchTeacher = view.findViewById(R.id.spinnerSearchTeacher);
        ImageButton btnSearchTeacher = view.findViewById(R.id.btnSearchTeacher);

        dbHelper = new DatabaseHelper(requireContext());
        teacherList = dbHelper.getAllTeachers();
        adapter = new TeacherListAdapter(teacherList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fabAddTeacher.setOnClickListener(v -> showAddTeacherDialog());

        // Populate spinner
        List<Teacher> allTeachers = dbHelper.getAllTeachers();
        List<String> teacherNames = new ArrayList<>();
        teacherNames.add("All");
        for (Teacher t : allTeachers) teacherNames.add(t.name);
        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, teacherNames);
        spinnerSearchTeacher.setAdapter(teacherAdapter);

        btnSearchTeacher.setOnClickListener(v -> {
            String selected = spinnerSearchTeacher.getSelectedItem().toString();
            List<Teacher> filtered = new ArrayList<>();
            if (selected.equals("All")) {
                filtered.addAll(allTeachers);
            } else {
                for (Teacher t : allTeachers) {
                    if (t.name.equals(selected)) filtered.add(t);
                }
            }
            teacherList.clear();
            teacherList.addAll(filtered);
            adapter.notifyDataSetChanged();
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                if (imgTeacherPhoto != null) imgTeacherPhoto.setImageURI(selectedImageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                selectedImageUri = getImageUriFromBitmap(requireContext(), imageBitmap);
                if (imgTeacherPhoto != null) imgTeacherPhoto.setImageURI(selectedImageUri);
            }
        }
    }
    private Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "TeacherPhoto", null);
        return Uri.parse(path);
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
        imgTeacherPhoto = dialogView.findViewById(R.id.imgTeacherPhoto);
        Button btnTakePhoto = dialogView.findViewById(R.id.btnTakePhoto);
        Button btnChoosePhoto = dialogView.findViewById(R.id.btnChoosePhoto);
        selectedImageUri = null;
        btnTakePhoto.setOnClickListener(v2 -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });
        btnChoosePhoto.setOnClickListener(v2 -> {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
        });
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
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedImageUri != null) {
                    uploadTeacherPhotoAndSaveTeacher(name, email, phone, bio, selectedImageUri, dialog);
                } else {
                    saveTeacherToLocal(name, email, phone, bio, null, dialog);
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                editName.setText("");
                editEmail.setText("");
                editPhone.setText("");
                editBio.setText("");
                selectedImageUri = null;
                if (imgTeacherPhoto != null) imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
            });
        });
        dialog.show();
    }
    private void uploadTeacherPhotoAndSaveTeacher(String name, String email, String phone, String bio, Uri imageUri, AlertDialog dialog) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("teacher_photos/" + System.currentTimeMillis() + ".jpg");
        storageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                saveTeacherToLocal(name, email, phone, bio, uri.toString(), dialog);
            }))
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    private void saveTeacherToLocal(String name, String email, String phone, String bio, String photoUri, AlertDialog dialog) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("INSERT INTO teachers (name, email, phone, bio, photoUri) VALUES (?, ?, ?, ?, ?)",
                new Object[]{name, email, phone, bio, photoUri});
        refreshTeacherList();
        Toast.makeText(getContext(), "Teacher added locally", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    private void showEditTeacherDialog(Teacher teacher) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_teacher, null, false);
        EditText editName = dialogView.findViewById(R.id.editName);
        EditText editEmail = dialogView.findViewById(R.id.editEmail);
        EditText editPhone = dialogView.findViewById(R.id.editPhone);
        EditText editBio = dialogView.findViewById(R.id.editBio);
        imgTeacherPhoto = dialogView.findViewById(R.id.imgTeacherPhoto);
        Button btnTakePhoto = dialogView.findViewById(R.id.btnTakePhoto);
        Button btnChoosePhoto = dialogView.findViewById(R.id.btnChoosePhoto);
        selectedImageUri = null;
        // Pre-fill fields
        editName.setText(teacher.name);
        editEmail.setText(teacher.email);
        editPhone.setText(teacher.phone);
        editBio.setText(teacher.bio);
        if (teacher.photoUri != null && !teacher.photoUri.isEmpty()) {
            Glide.with(this).load(teacher.photoUri).placeholder(android.R.drawable.ic_menu_camera).into(imgTeacherPhoto);
        } else {
            imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
        }
        btnTakePhoto.setOnClickListener(v2 -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });
        btnChoosePhoto.setOnClickListener(v2 -> {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
        });
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
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedImageUri != null) {
                    uploadTeacherPhotoAndUpdateTeacher(teacher, name, email, phone, bio, selectedImageUri, dialog);
                } else {
                    updateTeacherInLocal(teacher, name, email, phone, bio, teacher.photoUri, dialog);
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                editName.setText("");
                editEmail.setText("");
                editPhone.setText("");
                editBio.setText("");
                selectedImageUri = null;
                if (imgTeacherPhoto != null) imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
            });
        });
        dialog.show();
    }
    private void uploadTeacherPhotoAndUpdateTeacher(Teacher teacher, String name, String email, String phone, String bio, Uri imageUri, AlertDialog dialog) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("teacher_photos/" + System.currentTimeMillis() + ".jpg");
        storageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                updateTeacherInLocal(teacher, name, email, phone, bio, uri.toString(), dialog);
            }))
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    private void updateTeacherInLocal(Teacher teacher, String name, String email, String phone, String bio, String photoUri, AlertDialog dialog) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE teachers SET name=?, email=?, phone=?, bio=?, photoUri=? WHERE id=?",
                new Object[]{name, email, phone, bio, photoUri, teacher.id});
        Toast.makeText(getContext(), "Teacher updated", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        refreshTeacherList();
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
        private final TextView tvName, tvEmail, tvPhone, tvBio;
        private final ImageButton btnEdit, btnDelete;
        private final ImageView imgTeacherPhoto;
        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvBio = itemView.findViewById(R.id.tvBio);
            imgTeacherPhoto = itemView.findViewById(R.id.imgTeacherPhoto);
            btnEdit = itemView.findViewById(R.id.btnEditTeacher);
            btnDelete = itemView.findViewById(R.id.btnDeleteTeacher);
        }
        public void bind(Teacher teacher) {
            tvName.setText(teacher.name);
            tvEmail.setText(teacher.email);
            tvPhone.setText(teacher.phone);
            tvBio.setText(teacher.bio);
            if (teacher.photoUri != null && !teacher.photoUri.isEmpty()) {
                Glide.with(itemView.getContext()).load(teacher.photoUri).placeholder(android.R.drawable.ic_menu_camera).into(imgTeacherPhoto);
            } else {
                imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
            btnEdit.setOnClickListener(v -> showEditTeacherDialog(teacher));
            btnDelete.setOnClickListener(v -> confirmDeleteTeacher(teacher));
        }
    }
} 