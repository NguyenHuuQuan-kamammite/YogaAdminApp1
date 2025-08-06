package com.example.yogaadminapp1;

import android.app.AlertDialog;
import android.content.ContentValues;
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
import java.io.ByteArrayOutputStream;
import android.content.Context;
import android.widget.Button;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

// Permission handling imports
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Logging import
import android.util.Log;

// Image processing imports
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class TeacherManagementFragment extends Fragment {
    private List<Teacher> teacherList = new ArrayList<>();
    private TeacherListAdapter adapter;
    private DatabaseHelper dbHelper;
    private Uri selectedImageUri = null;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_IMAGE_PICK = 1002;
    private static final int PERMISSION_REQUEST_CAMERA = 1003;
    private static final int PERMISSION_REQUEST_STORAGE = 1004;
    private ImageView imgTeacherPhoto;
    private boolean isTakingPhoto = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_management, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerTeachers);
        FloatingActionButton fabAddTeacher = view.findViewById(R.id.fabAddTeacher);
        Spinner spinnerSearchTeacher = view.findViewById(R.id.spinnerSearchTeacher);
        ImageButton btnSearchTeacher = view.findViewById(R.id.btnSearchTeacher);
        ImageButton btnBackToHome = view.findViewById(R.id.btnBackToHome);

        dbHelper = new DatabaseHelper(requireContext());
        teacherList = dbHelper.getAllTeachers();
        adapter = new TeacherListAdapter(teacherList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fabAddTeacher.setOnClickListener(v -> showAddTeacherDialog());
        
        // Back button to navigate to HomeFragment
        btnBackToHome.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .addToBackStack(null)
                .commit();
        });

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
                if (imgTeacherPhoto != null) {
                    // Display the selected image
                    displayImageInImageView(selectedImageUri, imgTeacherPhoto);
                }
                Log.d("TeacherManagement", "Gallery image selected: " + selectedImageUri.toString());
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                selectedImageUri = getImageUriFromBitmap(requireContext(), imageBitmap);
                if (imgTeacherPhoto != null) {
                    // Display the captured image
                    displayImageInImageView(selectedImageUri, imgTeacherPhoto);
                }
                isTakingPhoto = false;
                Log.d("TeacherManagement", "Camera image captured");
            }
        } else {
            Log.d("TeacherManagement", "Activity result not OK, result code: " + resultCode);
        }
    }
    private Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "TeacherPhoto", null);
        return Uri.parse(path);
    }

    private void displayImageInImageView(Uri imageUri, ImageView imageView) {
        try {
            // Load the image from URI and convert to Base64 for consistent handling
            String base64Image = convertImageUriToBase64(imageUri);
            if (base64Image != null) {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
                // Force refresh the image view
                imageView.invalidate();
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } catch (Exception e) {
            Log.e("TeacherManagement", "Error displaying image", e);
            imageView.setImageResource(android.R.drawable.ic_menu_camera);
        }
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
            requestCameraPermission();
        });
        btnChoosePhoto.setOnClickListener(v2 -> {
            requestStoragePermission();
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
        try {
            // Convert image to Base64 string
            String base64Image = convertImageUriToBase64(imageUri);
            if (base64Image != null) {
                Log.d("TeacherManagement", "Image converted to Base64 successfully");
                saveTeacherToLocal(name, email, phone, bio, base64Image, dialog);
            } else {
                Log.e("TeacherManagement", "Failed to convert image to Base64");
                Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("TeacherManagement", "Exception during image processing", e);
            Toast.makeText(getContext(), "Image processing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void saveTeacherToLocal(String name, String email, String phone, String bio, String photoUri, AlertDialog dialog) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("phone", phone);
        values.put("bio", bio);
        values.put("photoUri", photoUri);
        db.insert("teachers", null, values);
        db.close();
        dialog.dismiss();
        // Refresh the teacher list
        teacherList.clear();
        teacherList.addAll(dbHelper.getAllTeachers());
        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Teacher added successfully", Toast.LENGTH_SHORT).show();
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
            // Check if it's a Base64 string or a URL
            if (teacher.photoUri.startsWith("data:image") || (teacher.photoUri.length() > 100 && !teacher.photoUri.contains("http"))) {
                // It's likely a Base64 string (either properly formatted or not)
                try {
                    String base64String = teacher.photoUri;
                    // If it's a properly formatted data URL, extract the Base64 part
                    if (base64String.startsWith("data:image")) {
                        base64String = base64String.substring(base64String.indexOf(",") + 1);
                    }
                    byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgTeacherPhoto.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    Log.e("TeacherManagement", "Error decoding Base64 image in edit dialog", e);
                    imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
                }
            } else if (!TextUtils.isEmpty(teacher.photoUri)) {
                // It's a URL, use Glide as before
                Glide.with(this).load(teacher.photoUri).placeholder(android.R.drawable.ic_menu_camera).into(imgTeacherPhoto);
            } else {
                // No valid image data
                imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
        }
        btnTakePhoto.setOnClickListener(v2 -> {
            requestCameraPermission();
        });
        btnChoosePhoto.setOnClickListener(v2 -> {
            requestStoragePermission();
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
        try {
            // Convert image to Base64 string
            String base64Image = convertImageUriToBase64(imageUri);
            if (base64Image != null) {
                Log.d("TeacherManagement", "Image converted to Base64 successfully");
                updateTeacherInLocal(teacher, name, email, phone, bio, base64Image, dialog);
            } else {
                Log.e("TeacherManagement", "Failed to convert image to Base64");
                Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("TeacherManagement", "Exception during image processing", e);
            Toast.makeText(getContext(), "Image processing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private String convertImageUriToBase64(Uri imageUri) {
        try {
            InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            
            // Compress the bitmap to reduce size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // 80% quality
            byte[] imageBytes = baos.toByteArray();
            
            // Convert to Base64 string
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("TeacherManagement", "Error converting image to Base64", e);
            return null;
        }
    }
    private void updateTeacherInLocal(Teacher teacher, String name, String email, String phone, String bio, String photoUri, AlertDialog dialog) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("phone", phone);
        values.put("bio", bio);
        values.put("photoUri", photoUri);
        db.update("teachers", values, "id = ?", new String[]{String.valueOf(teacher.id)});
        db.close();
        dialog.dismiss();
        // Refresh the teacher list
        teacherList.clear();
        teacherList.addAll(dbHelper.getAllTeachers());
        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Teacher updated successfully", Toast.LENGTH_SHORT).show();
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
                // Check if it's a Base64 string or a URL
                if (teacher.photoUri.startsWith("data:image") || (teacher.photoUri.length() > 100 && !teacher.photoUri.contains("http"))) {
                    // It's likely a Base64 string (either properly formatted or not)
                    try {
                        String base64String = teacher.photoUri;
                        // If it's a properly formatted data URL, extract the Base64 part
                        if (base64String.startsWith("data:image")) {
                            base64String = base64String.substring(base64String.indexOf(",") + 1);
                        }
                        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imgTeacherPhoto.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        Log.e("TeacherManagement", "Error decoding Base64 image", e);
                        imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
                    }
                } else if (!TextUtils.isEmpty(teacher.photoUri)) {
                    // It's a URL, use Glide as before
                    Glide.with(itemView.getContext()).load(teacher.photoUri).placeholder(android.R.drawable.ic_menu_camera).into(imgTeacherPhoto);
                } else {
                    // No valid image data
                    imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
                }
            } else {
                imgTeacherPhoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
            btnEdit.setOnClickListener(v -> showEditTeacherDialog(teacher));
            btnDelete.setOnClickListener(v -> confirmDeleteTeacher(teacher));
        }
    }
    
    private void requestCameraPermission() {
        Log.d("TeacherManagement", "Requesting camera permission");
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("TeacherManagement", "Camera permission not granted, requesting permission");
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        } else {
            Log.d("TeacherManagement", "Camera permission already granted, opening camera");
            openCamera();
        }
    }
    
    private void requestStoragePermission() {
        Log.d("TeacherManagement", "Requesting storage permission");
        // Check for newer Android versions that might need different permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), check READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("TeacherManagement", "READ_MEDIA_IMAGES permission not granted, requesting permission");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_STORAGE);
            } else {
                Log.d("TeacherManagement", "READ_MEDIA_IMAGES permission already granted, opening gallery");
                openGallery();
            }
        } else {
            // For older Android versions, check READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("TeacherManagement", "Storage permission not granted, requesting permission");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_STORAGE);
            } else {
                Log.d("TeacherManagement", "Storage permission already granted, opening gallery");
                openGallery();
            }
        }
    }
    
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            isTakingPhoto = true;
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    
    private void openGallery() {
        Log.d("TeacherManagement", "Opening gallery");
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("TeacherManagement", "Permission result received, requestCode: " + requestCode);
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TeacherManagement", "Camera permission granted");
                    openCamera();
                } else {
                    Log.d("TeacherManagement", "Camera permission denied");
                    Toast.makeText(getContext(), "Camera permission is required to take photos", Toast.LENGTH_LONG).show();
                }
                break;
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TeacherManagement", "Storage permission granted");
                    openGallery();
                } else {
                    Log.d("TeacherManagement", "Storage permission denied");
                    // Check if we're on Android 13+ and need to handle READ_MEDIA_IMAGES
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) 
                                == PackageManager.PERMISSION_GRANTED) {
                            Log.d("TeacherManagement", "READ_MEDIA_IMAGES permission granted");
                            openGallery();
                            return;
                        }
                    }
                    Toast.makeText(getContext(), "Storage permission is required to choose photos", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
} 