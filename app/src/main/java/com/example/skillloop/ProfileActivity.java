package com.example.skillloop;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private View hero;
    private View overlay;
    private ImageView photo;
    private TextView initials;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));

        hero = findViewById(R.id.profile_hero);
        overlay = findViewById(R.id.profile_photo_overlay);
        photo = findViewById(R.id.profile_photo);
        initials = findViewById(R.id.profile_initials);
        scrollView = findViewById(R.id.profile_scroll);

        Intent intent = getIntent();

        String name = intent.getStringExtra("name");
        String course = intent.getStringExtra("course");
        String year = intent.getStringExtra("year");
        String teaches = intent.getStringExtra("teaches");
        String wants = intent.getStringExtra("wants");
        String bio = intent.getStringExtra("bio");
        String project = intent.getStringExtra("project");
        String interests = intent.getStringExtra("interests");
        String photoUrl = intent.getStringExtra("photoUrl");
        String studentId = intent.getStringExtra("studentId");

        if (name == null || name.trim().isEmpty()) {
            name = "AARAV MEHTA";
        }

        if (course == null) course = "Computer Engineering";
        if (year == null) year = "Year 3";
        if (teaches == null) teaches = "Python · Machine Learning · Excel";
        if (wants == null) wants = "UI/UX · Motion Design";
        if (bio == null) bio = "Building things. Learning constantly.";
        if (project == null) project = "Not added yet";
        if (interests == null) interests = "Not added yet";

        setText(R.id.profile_name, name.toUpperCase() + ".");
        setText(R.id.profile_course,
                course.toUpperCase() + " · " + year.toUpperCase());
        setText(R.id.profile_teaches, teaches);
        setText(R.id.profile_wants, wants);
        setText(R.id.profile_bio, bio);
        setText(R.id.profile_project_description, project);
        setText(R.id.profile_interests, interests);

        String[] parts = name.trim().split("\\s+");
        String initialsText = parts[0].substring(0, 1);

        if (parts.length > 1) {
            initialsText += parts[parts.length - 1].substring(0, 1);
        }

        initials.setText(initialsText.toUpperCase());
        photo.setVisibility(View.GONE);
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            SkillLoopImageLoader.load(this, photoUrl, photo, () -> runOnUiThread(() -> {
                photo.setVisibility(View.VISIBLE);
                initials.setVisibility(View.GONE);
            }));
        }

        ImageButton back = findViewById(R.id.profile_back);
        back.setOnClickListener(v -> finish());

        setupScrollEffect();

        findViewById(R.id.connect_button).setOnClickListener(v -> {
            if (!FirebaseSkillLoopRepository.isConfigured(this)
                    || FirebaseSkillLoopRepository.currentUid() == null
                    || studentId == null || studentId.trim().isEmpty()) {
                Toast.makeText(this,
                        "Connection requests between real accounts require Firebase setup.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            EditText intro = new EditText(this);
            intro.setHint("Say what you'd like to learn or share...");
            intro.setMinLines(2);
            intro.setMaxLines(4);
            intro.setPadding(dp(14), dp(12), dp(14), dp(12));
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Introduce yourself")
                    .setView(intro)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Send request", (dialog, which) -> {
                        String message = intro.getText().toString().trim();
                        if (message.isEmpty()) {
                            Toast.makeText(this, "Write a short introduction first.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseSkillLoopRepository.sendRequest(studentId, message,
                                requestId -> runOnUiThread(() -> Toast.makeText(this, "Request sent. Chat unlocks after acceptance.", Toast.LENGTH_LONG).show()),
                                error -> runOnUiThread(() -> Toast.makeText(this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show()));
                    }).show();
        });
    }

    private void setText(int id, String value) {
        TextView textView = findViewById(id);
        textView.setText(value);
    }

    private void setupScrollEffect() {
        scrollView.setOnScrollChangeListener(
                (View v, int scrollX, int scrollY,
                 int oldScrollX, int oldScrollY) -> {

                    float progress = Math.min(
                            1f,
                            scrollY / (float) dp(240)
                    );

                    // Slightly scale down the hero as it scrolls away.
                    float scale = 1f - (progress * 0.035f);
                    photo.setScaleX(scale);
                    photo.setScaleY(scale);

                    // Increase the dark overlay as the user scrolls.
                    overlay.setAlpha(0.25f + (progress * 0.55f));

                    // Keep the placeholder initials subtle.
                    initials.setAlpha(1f - (progress * 0.7f));

                    // Add subtle blur on Android 12 and newer.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        float blurRadius = progress * 7f;

                        if (blurRadius > 0.1f) {
                            photo.setRenderEffect(
                                    android.graphics.RenderEffect
                                            .createBlurEffect(
                                                    blurRadius,
                                                    blurRadius,
                                                    android.graphics.Shader.TileMode.CLAMP
                                            )
                            );
                        } else {
                            photo.setRenderEffect(null);
                        }
                    }
                }
        );
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}
