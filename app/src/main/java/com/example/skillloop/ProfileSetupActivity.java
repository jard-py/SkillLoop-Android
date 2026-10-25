package com.example.skillloop;

import android.content.Intent;
import android.net.Uri;
import android.content.DialogInterface;
import java.io.File;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {
    private EditText name, course, year, domain, bio, teaches, learn, project, interests;
    private ImageView photo;
    private TextView choosePhoto, save;
    private ProgressBar progress;
    private Uri selectedPhoto;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraOutputUri;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!FirebaseSkillLoopRepository.isConfigured(this)
                || FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in before creating a profile.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        setContentView(R.layout.activity_profile_setup);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));
        name = findViewById(R.id.setup_name);
        course = findViewById(R.id.setup_course);
        year = findViewById(R.id.setup_year);
        domain = findViewById(R.id.setup_domain);
        bio = findViewById(R.id.setup_bio);
        teaches = findViewById(R.id.setup_teaches);
        learn = findViewById(R.id.setup_learn);
        project = findViewById(R.id.setup_project);
        interests = findViewById(R.id.setup_interests);
        photo = findViewById(R.id.setup_photo);
        choosePhoto = findViewById(R.id.setup_choose_photo);
        save = findViewById(R.id.setup_save);
        progress = findViewById(R.id.setup_progress);

        name.setText(getIntent().getStringExtra("displayName"));
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            selectedPhoto = uri;
            photo.setImageURI(uri);
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraOutputUri != null) {
                selectedPhoto = cameraOutputUri;
                photo.setImageURI(cameraOutputUri);
            } else {
                cameraOutputUri = null;
            }
        });
        choosePhoto.setOnClickListener(v -> showPhotoOptions());
        photo.setOnClickListener(v -> showPhotoOptions());
        save.setOnClickListener(v -> saveProfile());
        loadExistingProfile();
    }


    private void showPhotoOptions() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Profile photo")
                .setItems(new String[]{"Choose from Gallery", "Take a Photo"}, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else {
                        openCamera();
                    }
                }).show();
    }

    private void openCamera() {
        try {
            File output = new File(getFilesDir(), "skillloop_setup_camera.jpg");
            cameraOutputUri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", output);
            cameraLauncher.launch(cameraOutputUri);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open the camera. Please check camera access and try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadExistingProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseSkillLoopRepository.loadProfile(uid, snapshot -> {
            if (snapshot == null || !snapshot.exists()) return;
            putIfEmpty(name, snapshot.getString("displayName"));
            course.setText(snapshot.getString("course"));
            year.setText(snapshot.getString("year"));
            domain.setText(snapshot.getString("domain"));
            bio.setText(snapshot.getString("bio"));
            teaches.setText(snapshot.getString("skillsToTeach"));
            learn.setText(snapshot.getString("skillsToLearn"));
            project.setText(snapshot.getString("currentProject"));
            interests.setText(snapshot.getString("interests"));
        }, error -> { /* An empty profile is expected for a new account. */ });
    }

    private void putIfEmpty(EditText field, String value) {
        if (TextUtils.isEmpty(field.getText().toString()) && value != null) field.setText(value);
    }

    private void saveProfile() {
        if (!validate(name, "Enter your name") || !validate(course, "Enter your course/branch")
                || !validate(year, "Enter your year of study") || !validate(domain, "Enter your domain")
                || !validate(teaches, "Add at least one skill you can teach")
                || !validate(learn, "Add a skill you want to learn")) return;
        setLoading(true);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (selectedPhoto != null) {
            FirebaseSkillLoopRepository.uploadProfilePhoto(uid, selectedPhoto,
                    uri -> writeProfile(uri.toString()), error -> {
                        setLoading(false);
                        Toast.makeText(this, "Photo upload failed: " + friendly(error.getMessage()), Toast.LENGTH_LONG).show();
                    });
        } else {
            FirebaseSkillLoopRepository.loadProfile(uid, snapshot -> {
                String oldPhoto = snapshot == null ? "" : snapshot.getString("photoUrl");
                writeProfile(oldPhoto == null ? "" : oldPhoto);
            }, error -> writeProfile(""));
        }
    }

    private boolean validate(EditText field, String message) {
        if (TextUtils.isEmpty(field.getText().toString().trim())) {
            field.setError(message); field.requestFocus(); return false;
        }
        return true;
    }

    private void writeProfile(String photoUrl) {
        Map<String, Object> values = new HashMap<>();
        values.put("displayName", clean(name));
        values.put("course", clean(course));
        values.put("year", clean(year));
        values.put("domain", clean(domain));
        values.put("bio", clean(bio));
        values.put("skillsToTeach", clean(teaches));
        values.put("skillsToLearn", clean(learn));
        values.put("currentProject", clean(project));
        values.put("interests", clean(interests));
        values.put("photoUrl", photoUrl == null ? "" : photoUrl);
        FirebaseSkillLoopRepository.saveProfile(FirebaseAuth.getInstance().getCurrentUser().getUid(), values,
                unused -> {
                    setLoading(false);
                    startActivity(new Intent(this, DiscoverActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                }, error -> {
                    setLoading(false);
                    Toast.makeText(this, "Couldn't save profile: " + friendly(error.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private String clean(EditText field) { return field.getText().toString().trim(); }
    private String friendly(String message) { return message == null ? "Please try again." : message; }

    private void setLoading(boolean loading) {
        save.setEnabled(!loading);
        save.setAlpha(loading ? 0.65f : 1f);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        choosePhoto.setEnabled(!loading);
    }
}
