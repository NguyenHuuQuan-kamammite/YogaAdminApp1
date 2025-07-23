package com.example.yogaadminapp1;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yogaadminapp1.databinding.FragmentAdminLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginFragment extends Fragment {
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

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = binding.editTextUsername.getText() != null ? binding.editTextUsername.getText().toString().trim() : "";
        String password = binding.editTextPassword.getText() != null ? binding.editTextPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.editTextUsername.setError("Email required");
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
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkAdminRole(user.getUid());
                        }
                    } else {
                        Toast.makeText(getContext(), "Authentication failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    } 
                });
    }

    private void checkAdminRole(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && "admin".equals(documentSnapshot.getString("role"))) {
                        Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                        // Navigate to HomeFragment
                        requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .addToBackStack(null)
                            .commit();
                    } else {
                        mAuth.signOut();
                        Toast.makeText(getContext(), "Access denied: Admins only.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    mAuth.signOut();
                    Toast.makeText(getContext(), "Failed to verify admin role.", Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 