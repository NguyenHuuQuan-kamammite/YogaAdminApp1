package com.example.yogaadminapp1;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yogaadminapp1.HomeFragment;
import com.example.yogaadminapp1.databinding.FragmentAdminLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AdminLoginFragment extends Fragment {
    private static final String TAG = "AdminLoginFragment";
    private FragmentAdminLoginBinding binding;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            checkAdminRole(currentUser.getUid());
            return;
        }

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        

    }

    private void attemptLogin() {
        String email = binding.editTextUsername.getText() != null ? binding.editTextUsername.getText().toString().trim() : "";
        String password = binding.editTextPassword.getText() != null ? binding.editTextPassword.getText().toString().trim() : "";

        Log.d(TAG, "Attempting login with email: " + email);
        
        if (TextUtils.isEmpty(email)) {
            binding.editTextUsername.setError("Email required");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextUsername.setError("Please enter a valid email address");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.editTextPassword.setError("Password required");
            return;
        }

        binding.btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    binding.btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Authentication successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User authenticated with UID: " + user.getUid());
                            // Check if email is verified (optional security measure)
                            if (user.isEmailVerified() || email.equals("admin@example.com") || email.toLowerCase().contains("admin")) {
                                Log.d(TAG, "Email verified or bypassing verification for admin user");
                                checkAdminRole(user.getUid());
                            } else {
                                Log.d(TAG, "Email not verified. Signing out.");
                                mAuth.signOut();
                                Toast.makeText(getContext(), "Please verify your email address before logging in.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        String errorMessage = "Authentication failed.";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            Log.e("AdminLoginFragment", "Login failed", task.getException());
                            if (exceptionMessage != null && exceptionMessage.contains("INVALID_LOGIN_CREDENTIALS")) {
                                errorMessage = "Invalid email or password. Please try again.";
                            } else if (exceptionMessage != null && exceptionMessage.contains("TOO_MANY_ATTEMPTS")) {
                                errorMessage = "Too many failed attempts. Please try again later.";
                            } else {
                                errorMessage = "Login failed: " + exceptionMessage;
                            }
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    } 
                });
    }

    private void checkAdminRole(String uid) {
        Log.d(TAG, "Checking admin role for user ID: " + uid);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Firestore query successful. Document exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Log.d(TAG, "User role: " + role);
                        if ("admin".equals(role)) {
                            Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Admin role verified. Navigating to HomeFragment.");
                            // Navigate to HomeFragment
                            requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new HomeFragment())
                                .addToBackStack(null)
                                .commit();
                        } else {
                            Log.d(TAG, "User does not have admin role. Signing out.");
                            mAuth.signOut();
                            Toast.makeText(getContext(), "Access denied: Admins only. Your role is: " + (role != null ? role : "undefined"), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(TAG, "User document not found in Firestore.");
                        // Special case for initial admin setup
                        if (uid.equals("admin@example.com") || uid.startsWith("admin") || uid.toLowerCase().contains("admin")) {
                            Log.d(TAG, "Creating new admin user.");
                            // Create admin user in database
                            createAdminUser(uid);
                        } else {
                            Log.d(TAG, "Non-admin user not found. Signing out.");
                            mAuth.signOut();
                            Toast.makeText(getContext(), "User account not found. Please contact administrator.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminLoginFragment", "Failed to verify admin role", e);
                    mAuth.signOut();
                    Toast.makeText(getContext(), "Failed to verify admin role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createAdminUser(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        

        Map<String, Object> adminData = new HashMap<>();
        adminData.put("role", "admin");
        adminData.put("createdAt", new Date());
        adminData.put("lastLogin", new Date());
        
        db.collection("users").document(uid)
                .set(adminData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Admin account created successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate to HomeFragment
                    requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .addToBackStack(null)
                        .commit();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create admin user", e);
                    mAuth.signOut();
                    Toast.makeText(getContext(), "Failed to create admin account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 