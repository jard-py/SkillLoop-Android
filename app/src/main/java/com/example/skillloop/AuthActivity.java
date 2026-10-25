package com.example.skillloop;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class AuthActivity extends AppCompatActivity {
    private EditText nameInput, emailInput, passwordInput;
    private TextView modeLabel, submit, forgot, switchMode, passwordToggle;
    private ProgressBar progress;
    private boolean registering = false;
    private boolean showPassword = false;
    private FirebaseAuth auth;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!FirebaseSkillLoopRepository.isConfigured(this)) {
            Toast.makeText(this, "Firebase isn't configured yet. Add google-services.json first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            routeSignedInUser(auth.getCurrentUser().getUid());
            return;
        }
        setContentView(R.layout.activity_auth);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));
        nameInput = findViewById(R.id.auth_name);
        emailInput = findViewById(R.id.auth_email);
        passwordInput = findViewById(R.id.auth_password);
        modeLabel = findViewById(R.id.auth_mode_label);
        submit = findViewById(R.id.auth_submit);
        forgot = findViewById(R.id.auth_forgot);
        switchMode = findViewById(R.id.auth_switch);
        passwordToggle = findViewById(R.id.auth_toggle_password);
        progress = findViewById(R.id.auth_progress);

        submit.setOnClickListener(v -> submitAuth());
        switchMode.setOnClickListener(v -> setRegistering(!registering));
        forgot.setOnClickListener(v -> resetPassword());
        passwordToggle.setOnClickListener(v -> togglePassword());
        setRegistering(getIntent().getBooleanExtra("start_registering", false));
    }

    private void setRegistering(boolean enabled) {
        registering = enabled;
        modeLabel.setText(enabled ? "CREATE ACCOUNT" : "SIGN IN");
        submit.setText(enabled ? "CREATE ACCOUNT  ↗" : "SIGN IN  ↗");
        switchMode.setText(enabled ? "Already have an account? Sign in" : "New here? Create an account");
        nameInput.setVisibility(enabled ? View.VISIBLE : View.GONE);
        forgot.setVisibility(enabled ? View.GONE : View.VISIBLE);
        if (enabled) nameInput.setHint("Full name");
    }

    private void togglePassword() {
        showPassword = !showPassword;
        int cursor = passwordInput.getSelectionStart();
        passwordInput.setTransformationMethod(showPassword
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        passwordToggle.setText(showPassword ? "HIDE" : "SHOW");
        passwordInput.setSelection(Math.max(0, cursor));
    }

    private void submitAuth() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String fullName = nameInput.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address"); emailInput.requestFocus(); return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Use at least 6 characters"); passwordInput.requestFocus(); return;
        }
        if (registering && fullName.isEmpty()) {
            nameInput.setError("Enter your name"); nameInput.requestFocus(); return;
        }
        setLoading(true);
        if (registering) {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        Intent intent = new Intent(this, ProfileSetupActivity.class);
                        intent.putExtra("displayName", fullName);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(error -> showError(error.getMessage()));
        } else {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> routeSignedInUser(result.getUser().getUid()))
                    .addOnFailureListener(error -> showError(error.getMessage()));
        }
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter your email first"); emailInput.requestFocus(); return;
        }
        setLoading(true);
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(error -> showError(error.getMessage()));
    }

    private void routeSignedInUser(String uid) {
        setLoading(true);
        FirebaseSkillLoopRepository.loadProfile(uid, this::continueFromProfile,
                error -> showError("Couldn't verify your profile. Check your connection and try again."));
    }

    private void continueFromProfile(DocumentSnapshot snapshot) {
        setLoading(false);
        if (snapshot == null || !snapshot.exists()
                || !Boolean.TRUE.equals(snapshot.getBoolean("profileComplete"))) {
            startActivity(new Intent(this, ProfileSetupActivity.class));
        } else {
            startActivity(new Intent(this, DiscoverActivity.class));
        }
        finish();
    }

    private void showError(String message) {
        setLoading(false);
        hideKeyboard();
        String safe = message == null || message.trim().isEmpty()
                ? "Something went wrong. Please try again." : message;
        Toast.makeText(this, safe, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        if (progress == null || submit == null) return;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        submit.setEnabled(!loading);
        submit.setAlpha(loading ? 0.65f : 1f);
        switchMode.setEnabled(!loading);
        forgot.setEnabled(!loading);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) return;
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
