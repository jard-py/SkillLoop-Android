
package com.example.skillloop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MyProfileActivity extends AppCompatActivity {

    private static final String PREFS = "skillloop_my_profile";

    private EditText nameInput, courseInput, yearInput;
    private EditText teachesInput, wantsInput, projectInput;
    private EditText domainInput, bioInput;

    private TextView viewName, viewCourseYear, viewDomain;
    private TextView viewBio, viewTeaches, viewWants, viewProject;
    private TextView profileHeading, profileSubtitle;

    private View editCourseYear, changePhoto;

    private ImageView profilePhoto;
    private TextView profileInitials;
    private ImageView editButton, saveIcon;

    private boolean editing = false;
    private String photoPath = "";
    private String remotePhotoUrl = "";
    private Uri pendingPhotoUri;
    private boolean remoteMode = false;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraOutputUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_my_profile);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));

        View root = findViewById(R.id.profile_root);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int top = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()).top;

            int bottom = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()).bottom;
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            view.setPadding(0, top, 0, imeVisible ? 0 : bottom);
            View navigationIsland = findViewById(R.id.nav_island);
            if (navigationIsland != null) {
                navigationIsland.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            }
            return insets;
        });

        bindViews();
        setupPhotoLaunchers();
        setupButtons();
        setupNavigation();
        remoteMode = FirebaseSkillLoopRepository.isConfigured(this)
                && FirebaseSkillLoopRepository.currentUid() != null;
        loadProfile();
        setEditing(false);
        if (remoteMode) loadRemoteProfile();
        View signOut = findViewById(R.id.sign_out_button);
        if (signOut != null) {
            signOut.setVisibility(remoteMode ? View.VISIBLE : View.GONE);
            signOut.setOnClickListener(v -> confirmSignOut());
        }
    }

    private void bindViews() {
        nameInput = findViewById(R.id.input_name);
        courseInput = findViewById(R.id.input_course);
        yearInput = findViewById(R.id.input_year);
        teachesInput = findViewById(R.id.input_teaches);
        wantsInput = findViewById(R.id.input_wants);
        projectInput = findViewById(R.id.input_project);
        domainInput = findViewById(R.id.input_domain);
        bioInput = findViewById(R.id.input_bio);

        viewName = findViewById(R.id.view_name);
        viewCourseYear = findViewById(R.id.view_course_year);
        viewDomain = findViewById(R.id.view_domain);
        viewBio = findViewById(R.id.view_bio);
        viewTeaches = findViewById(R.id.view_teaches);
        viewWants = findViewById(R.id.view_wants);
        viewProject = findViewById(R.id.view_project);

        profileHeading = findViewById(R.id.profile_heading);
        profileSubtitle = findViewById(R.id.profile_subtitle);

        editCourseYear = findViewById(R.id.edit_course_year);
        changePhoto = findViewById(R.id.change_photo);

        profilePhoto = findViewById(R.id.profile_photo);
        profileInitials = findViewById(R.id.profile_initials);

        editButton = findViewById(R.id.edit_profile);
        saveIcon = findViewById(R.id.save_profile_icon);
    }

    private void setupPhotoLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveSelectedPhoto(uri);
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraOutputUri != null) {
                        saveSelectedPhoto(cameraOutputUri);
                    } else {
                        cameraOutputUri = null;
                    }
                });
    }

    private void setupButtons() {
        editButton.setOnClickListener(v -> setEditing(true));

        // The square-check icon saves the profile.
        saveIcon.setOnClickListener(v -> saveProfile());

        changePhoto.setOnClickListener(v -> showPhotoOptions());

        profilePhoto.setOnClickListener(v -> {
            if (editing) {
                showPhotoOptions();
            }
        });
    }

    private void loadProfile() {
        if (remoteMode) {
            // Never attach a previous device-local profile to a different Firebase account.
            nameInput.setText("");
            courseInput.setText("");
            yearInput.setText("");
            teachesInput.setText("");
            wantsInput.setText("");
            projectInput.setText("");
            domainInput.setText("");
            bioInput.setText("");
            photoPath = "";
            remotePhotoUrl = "";
        } else {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            nameInput.setText(prefs.getString("name", ""));
            courseInput.setText(prefs.getString("course", ""));
            yearInput.setText(prefs.getString("year", ""));
            teachesInput.setText(prefs.getString("teaches", ""));
            wantsInput.setText(prefs.getString("wants", ""));
            projectInput.setText(prefs.getString("project", ""));
            domainInput.setText(prefs.getString("domain", ""));
            bioInput.setText(prefs.getString(
                    "bio", "Learning, sharing, and building together."));
            photoPath = prefs.getString("photo_path", "");
        }
        displayPhoto();
        refreshProfileText();
    }

    private void loadRemoteProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseSkillLoopRepository.loadProfile(uid, snapshot -> {
            if (snapshot == null || !snapshot.exists()) return;
            nameInput.setText(snapshot.getString("displayName"));
            courseInput.setText(snapshot.getString("course"));
            yearInput.setText(snapshot.getString("year"));
            domainInput.setText(snapshot.getString("domain"));
            bioInput.setText(snapshot.getString("bio"));
            teachesInput.setText(snapshot.getString("skillsToTeach"));
            wantsInput.setText(snapshot.getString("skillsToLearn"));
            projectInput.setText(snapshot.getString("currentProject"));
            remotePhotoUrl = snapshot.getString("photoUrl");
            if (remotePhotoUrl == null) remotePhotoUrl = "";
            refreshProfileText();
            displayPhoto();
        }, error -> Toast.makeText(this, "Couldn't sync profile: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show());
    }

    private String value(EditText input) { return input.getText().toString().trim(); }

    private void confirmSignOut() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign out of SkillLoop?")
                .setMessage("Your profile, connections and messages stay in your account.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }).show();
    }

    private void refreshProfileText() {
        String name = nameInput.getText().toString().trim();
        String course = courseInput.getText().toString().trim();
        String year = yearInput.getText().toString().trim();
        String domain = domainInput.getText().toString().trim();

        viewName.setText(name.isEmpty()
                ? "YOUR NAME"
                : name.toUpperCase());

        String courseYear = course.isEmpty()
                ? "COURSE NOT ADDED"
                : course;

        if (!year.isEmpty()) {
            courseYear += " · " + year.toUpperCase();
        }

        viewCourseYear.setText(courseYear);

        viewDomain.setText(domain.isEmpty()
                ? "Add your domain"
                : domain);

        viewBio.setText(valueOrPlaceholder(
                bioInput.getText().toString(),
                "Add a short introduction"));

        viewTeaches.setText(valueOrPlaceholder(
                teachesInput.getText().toString(),
                "Add the skills you can share"));

        viewWants.setText(valueOrPlaceholder(
                wantsInput.getText().toString(),
                "Add skills you'd like to learn"));

        viewProject.setText(valueOrPlaceholder(
                projectInput.getText().toString(),
                "Tell people what you're building"));

        profileHeading.setText(name.isEmpty()
                ? "THIS IS YOU."
                : "YOUR SPACE.");

        profileSubtitle.setText(editing
                ? "Make it yours. Save when you're ready."
                : "Your skills are your introduction.");
    }

    private String valueOrPlaceholder(
            String value, String placeholder) {

        String cleaned = value == null ? "" : value.trim();

        return cleaned.isEmpty() ? placeholder : cleaned;
    }

    private void setEditing(boolean enabled) {
        editing = enabled;

        int editVisibility = enabled ? View.VISIBLE : View.GONE;
        int viewVisibility = enabled ? View.GONE : View.VISIBLE;

        nameInput.setVisibility(editVisibility);
        editCourseYear.setVisibility(editVisibility);
        domainInput.setVisibility(editVisibility);
        bioInput.setVisibility(editVisibility);
        teachesInput.setVisibility(editVisibility);
        wantsInput.setVisibility(editVisibility);
        projectInput.setVisibility(editVisibility);

        viewName.setVisibility(viewVisibility);
        viewCourseYear.setVisibility(viewVisibility);
        viewDomain.setVisibility(viewVisibility);
        viewBio.setVisibility(viewVisibility);
        viewTeaches.setVisibility(viewVisibility);
        viewWants.setVisibility(viewVisibility);
        viewProject.setVisibility(viewVisibility);

        editButton.setVisibility(
                enabled ? View.GONE : View.VISIBLE);

        saveIcon.setVisibility(
                enabled ? View.VISIBLE : View.GONE);

        changePhoto.setVisibility(
                enabled ? View.VISIBLE : View.GONE);

        refreshProfileText();
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) { nameInput.setError("Enter your name"); nameInput.requestFocus(); return; }
        String course = courseInput.getText().toString().trim();
        String year = yearInput.getText().toString().trim();
        String teaches = teachesInput.getText().toString().trim();
        String wants = wantsInput.getText().toString().trim();
        String project = projectInput.getText().toString().trim();
        String domain = domainInput.getText().toString().trim();
        String bio = bioInput.getText().toString().trim();
        if (course.isEmpty()) { courseInput.setError("Enter your course/branch"); courseInput.requestFocus(); return; }
        if (year.isEmpty()) { yearInput.setError("Enter your year of study"); yearInput.requestFocus(); return; }
        if (domain.isEmpty()) { domainInput.setError("Enter your domain"); domainInput.requestFocus(); return; }
        if (teaches.isEmpty()) { teachesInput.setError("Add at least one skill you can teach"); teachesInput.requestFocus(); return; }
        if (wants.isEmpty()) { wantsInput.setError("Add a skill you want to learn"); wantsInput.requestFocus(); return; }

        if (remoteMode) {
            saveIcon.setEnabled(false);
            saveIcon.setAlpha(0.5f);
            if (pendingPhotoUri != null) {
                FirebaseSkillLoopRepository.uploadProfilePhoto(FirebaseAuth.getInstance().getCurrentUser().getUid(), pendingPhotoUri,
                        uri -> saveRemoteProfile(uri.toString()), error -> {
                            saveIcon.setEnabled(true);
                            saveIcon.setAlpha(1f);
                            Toast.makeText(this, "Photo upload failed: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                saveRemoteProfile(remotePhotoUrl);
            }
            return;
        }

        SkillLoopData.saveMyProfile(this, name, course, year, teaches, wants, project, domain, bio, photoPath);
        refreshProfileText();
        setEditing(false);
        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
    }

    private void saveRemoteProfile(String photoUrl) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("displayName", value(nameInput));
        fields.put("course", value(courseInput));
        fields.put("year", value(yearInput));
        fields.put("domain", value(domainInput));
        fields.put("bio", value(bioInput));
        fields.put("skillsToTeach", value(teachesInput));
        fields.put("skillsToLearn", value(wantsInput));
        fields.put("currentProject", value(projectInput));
        fields.put("photoUrl", photoUrl == null ? "" : photoUrl);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseSkillLoopRepository.saveProfile(uid, fields, unused -> runOnUiThread(() -> {
            remotePhotoUrl = photoUrl == null ? "" : photoUrl;
            pendingPhotoUri = null;
            saveIcon.setEnabled(true);
            saveIcon.setAlpha(1f);
            refreshProfileText();
            displayPhoto();
            setEditing(false);
            Toast.makeText(this, "Profile synced to your account.", Toast.LENGTH_SHORT).show();
        }), error -> runOnUiThread(() -> {
            saveIcon.setEnabled(true);
            saveIcon.setAlpha(1f);
            Toast.makeText(this, "Couldn't sync profile: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }));
    }

    private void showPhotoOptions() {
        String[] options = {
                "Choose from Gallery",
                "Take a Photo"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Profile photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openCamera() {
        try {
            File photoFile = new File(
                    getFilesDir(),
                    "skillloop_profile_camera.jpg");

            cameraOutputUri =
                    androidx.core.content.FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".fileprovider",
                            photoFile);

            cameraLauncher.launch(cameraOutputUri);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Couldn't open the camera. Check FileProvider setup.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void saveSelectedPhoto(Uri sourceUri) {
        pendingPhotoUri = sourceUri;
        if (remoteMode) {
            remotePhotoUrl = "";
        } else {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove("photo_url").apply();
        }
        try (InputStream input =
                     getContentResolver().openInputStream(sourceUri)) {

            if (input == null) {
                throw new Exception("Unable to open selected image");
            }

            Bitmap bitmap = BitmapFactory.decodeStream(input);

            if (bitmap == null) {
                throw new Exception("Invalid image");
            }

            File destination = new File(
                    getFilesDir(),
                    "skillloop_profile.jpg");

            try (FileOutputStream output =
                         new FileOutputStream(destination)) {

                bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        90,
                        output);

                output.flush();
            }

            bitmap.recycle();

            photoPath = destination.getAbsolutePath();

            displayPhoto();

            Toast.makeText(
                    this,
                    "Photo selected!",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Couldn't save that photo. Please try another image.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void displayPhoto() {
        String source;
        if (remoteMode) {
            source = pendingPhotoUri != null && photoPath != null && !photoPath.isEmpty()
                    ? photoPath : remotePhotoUrl;
        } else {
            String remotePhoto = getSharedPreferences(PREFS, MODE_PRIVATE).getString("photo_url", "");
            source = remotePhoto == null || remotePhoto.trim().isEmpty() ? photoPath : remotePhoto;
        }
        if (source == null || source.isEmpty()) {
            profilePhoto.setImageDrawable(null);
            profilePhoto.setVisibility(View.GONE);
            profileInitials.setVisibility(View.VISIBLE);
            return;
        }
        profileInitials.setVisibility(View.VISIBLE);
        profilePhoto.setVisibility(View.GONE);
        SkillLoopImageLoader.load(this, source, profilePhoto, () -> runOnUiThread(() -> {
            profilePhoto.setVisibility(View.VISIBLE);
            profileInitials.setVisibility(View.GONE);
        }));
    }

    private void setupNavigation() {
        SkillLoopTabNavigation.setup(this, 3);
    }
}
