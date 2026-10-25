package com.example.skillloop;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private TextView whiteText, blackText, subtitleText, continueButton, userName, userRole;
    private boolean firebaseConfigured;
    private final Random random = new Random();
    private final String[][] statements = {
            {"YOU DON'T\nHAVE TO", "KNOW IT ALL.\nJUST KEEP\nLEARNING.", "Someone on your campus knows what you want to learn."},
            {"YOU KNOW\nSOMETHING.", "SOMEONE\nNEEDS IT.", "Your skills could be someone else's next breakthrough."},
            {"YOUR NEXT\nSKILL IS", "CLOSER THAN\nYOU THINK.", "Discover the talent and knowledge already around you."},
            {"LEARN FROM\nPEOPLE.", "NOT JUST\nSCREENS.", "Real people. Shared knowledge. New possibilities."},
            {"YOU TEACH.", "I LEARN.\nTHEN WE\nSWITCH.", "The best exchanges work both ways."},
            {"GROW\nTOGETHER.", "NOT ALONE.", "Your next collaborator could be sitting across campus."},
            {"ONE CAMPUS.", "A THOUSAND\nTHINGS TO\nLEARN.", "Explore beyond your branch, course, and comfort zone."},
            {"SHARE WHAT\nYOU KNOW.", "DISCOVER\nWHAT'S NEXT.", "Every connection is a chance to grow."}
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseConfigured = FirebaseSkillLoopRepository.isConfigured(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(R.layout.activity_main);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return windowInsets;
        });
        whiteText = findViewById(R.id.manifesto_white);
        blackText = findViewById(R.id.manifesto_black);
        subtitleText = findViewById(R.id.manifesto_subtitle);
        continueButton = findViewById(R.id.continue_button);
        userName = findViewById(R.id.user_name);
        userRole = findViewById(R.id.user_role);
        int selected = random.nextInt(statements.length);
        whiteText.setText(statements[selected][0]);
        blackText.setText(statements[selected][1]);
        subtitleText.setText(statements[selected][2]);

        FirebaseUser current = firebaseConfigured ? FirebaseAuth.getInstance().getCurrentUser() : null;
        if (current == null) {
            userName.setText("YOUR CAMPUS");
            userRole.setText("Your next chapter starts here");
            continueButton.setText(firebaseConfigured ? "CREATE ACCOUNT  ↗" : "EXPLORE DEMO  ↗");
        } else {
            userName.setText("WELCOME BACK");
            userRole.setText("Your SkillLoop is ready");
            continueButton.setText("CONTINUE LEARNING  ↗");
            FirebaseSkillLoopRepository.loadProfile(current.getUid(), this::applyReturningProfile, error -> {
                // Keep a friendly welcome even when the profile request temporarily fails.
                if (!isFinishing()) userName.setText(current.getDisplayName() == null ? "WELCOME BACK" : current.getDisplayName());
            });
        }
        continueButton.setOnClickListener(v -> continueIntoApp());
    }

    private void applyReturningProfile(DocumentSnapshot profile) {
        if (isFinishing()) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (profile == null || !profile.exists() || !Boolean.TRUE.equals(profile.getBoolean("profileComplete"))) {
            Intent setup = new Intent(this, ProfileSetupActivity.class);
            if (user != null && user.getDisplayName() != null) setup.putExtra("displayName", user.getDisplayName());
            startActivity(setup); finish(); return;
        }
        String name = profile.getString("displayName");
        String course = profile.getString("course");
        String year = profile.getString("year");
        userName.setText(name == null || name.trim().isEmpty() ? "WELCOME BACK" : name.toUpperCase());
        String details = (course == null ? "" : course) + (year == null || year.isEmpty() ? "" : " · " + year);
        userRole.setText(details.trim().isEmpty() ? "Your SkillLoop is ready" : details);
        continueButton.setText("CONTINUE LEARNING  ↗");
    }

    private void continueIntoApp() {
        if (!firebaseConfigured) {
            startActivity(new Intent(this, DiscoverActivity.class));
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent authIntent = new Intent(this, AuthActivity.class);
            authIntent.putExtra("start_registering", true);
            startActivity(authIntent);
            return;
        }
        continueButton.setEnabled(false);
        FirebaseSkillLoopRepository.loadProfile(user.getUid(), profile -> {
            continueButton.setEnabled(true);
            if (profile == null || !profile.exists() || !Boolean.TRUE.equals(profile.getBoolean("profileComplete"))) {
                Intent setup = new Intent(this, ProfileSetupActivity.class);
                setup.putExtra("displayName", user.getDisplayName() == null ? "" : user.getDisplayName());
                startActivity(setup);
            } else {
                startActivity(new Intent(this, DiscoverActivity.class));
            }
        }, error -> {
            continueButton.setEnabled(true);
            Toast.makeText(this, "Couldn't load your profile. Check your connection and try again.", Toast.LENGTH_LONG).show();
        });
    }
}
