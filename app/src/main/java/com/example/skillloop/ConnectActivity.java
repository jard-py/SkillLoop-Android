package com.example.skillloop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

public class ConnectActivity extends AppCompatActivity {

    private static final String DISCOVER_PREFS = "skillloop_discover";
    private static final String LIKED_KEY = "liked_students";
    private static final String PHOTO_PREFS = "skillloop_student_photos";

    private LinearLayout requestsContainer;
    private TextView requestCount;
    private TextView emptyTitle, emptySubtitle;
    private View requestsEmpty;

    private final List<SkillLoopData.Student> people = new ArrayList<>();

    private Set<String> likedIds = new HashSet<>();
    private Set<String> acceptedIds = new HashSet<>();
    private Set<String> passedIds = new HashSet<>();
    private boolean remoteMode = false;
    private ListenerRegistration requestListener;
    private final Map<String, FirebaseSkillLoopRepository.ConnectionRequest> requestForStudent = new HashMap<>();
    private final Map<String, String> introductionForStudent = new HashMap<>();
    private final Map<String, FirebaseSkillLoopRepository.ConnectionRequest> remoteRequests = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_connect);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));

        View root = findViewById(R.id.connect_root);

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

        requestsContainer = findViewById(R.id.requests_container);
        requestCount = findViewById(R.id.request_count);
        requestsEmpty = findViewById(R.id.connect_empty);
        emptyTitle = findViewById(R.id.connect_empty_title);
        emptySubtitle = findViewById(R.id.connect_empty_subtitle);

        setupNavigation();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestsContainer != null && !remoteMode) loadData();
    }

    private void loadData() {
        remoteMode = FirebaseSkillLoopRepository.isConfigured(this)
                && FirebaseSkillLoopRepository.currentUid() != null;
        emptyTitle.setText(remoteMode ? "NO NEW REQUESTS." : "LOCAL DEMO MODE.");
        emptySubtitle.setText(remoteMode ? "Your next collaboration is out there."
                : "Incoming requests require Firebase and another signed-in student.");
        if (remoteMode) {
            if (requestListener == null) {
                requestListener = FirebaseSkillLoopRepository.listenIncomingRequests(
                        this::renderFirebaseRequests,
                        error -> Toast.makeText(this, "Couldn't load incoming requests: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show());
            }
            return;
        }

        // Local sample profiles are discovery examples, not real accounts. A request sent from
        // this single-device demo must never be reflected back as if it came from another student.
        people.clear();
        requestForStudent.clear();
        introductionForStudent.clear();
        requestsContainer.removeAllViews();
        requestCount.setText("00 REQUESTS");
        requestsEmpty.setVisibility(View.VISIBLE);
    }

    private void renderFirebaseRequests(List<FirebaseSkillLoopRepository.ConnectionRequest> requests) {
        remoteRequests.clear();
        for (FirebaseSkillLoopRepository.ConnectionRequest request : requests) remoteRequests.put(request.id, request);
        requestForStudent.clear();
        introductionForStudent.clear();
        people.clear();
        requestsContainer.removeAllViews();
        requestCount.setText(String.format(Locale.ROOT, "%02d REQUESTS", requests.size()));
        requestsEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
        emptyTitle.setText("NO NEW REQUESTS.");
        emptySubtitle.setText("Your next collaboration is out there.");
        for (FirebaseSkillLoopRepository.ConnectionRequest request : requests) {
            FirebaseSkillLoopRepository.loadStudent(request.senderId, student -> {
                if (student == null || !remoteRequests.containsKey(request.id)) return;
                if (requestForStudent.containsKey(student.id)) return;
                requestForStudent.put(student.id, request);
                introductionForStudent.put(student.id, request.introductionMessage);
                people.add(student);
                requestsContainer.addView(createRequestCard(student));
            }, error -> Toast.makeText(this, "A requester's profile couldn't be loaded.", Toast.LENGTH_SHORT).show());
        }
    }

    private void renderRequests() {
        requestsContainer.removeAllViews();

        int pending = 0;

        for (SkillLoopData.Student person : people) {
            if (!likedIds.contains(person.id)) {
                continue;
            }

            if (acceptedIds.contains(person.id)
                    || passedIds.contains(person.id)) {
                continue;
            }

            requestsContainer.addView(createRequestCard(person));
            pending++;
        }

        requestCount.setText(String.format(
                Locale.ROOT, "%02d REQUESTS", pending));

        requestsEmpty.setVisibility(
                pending == 0 ? View.VISIBLE : View.GONE);
    }

    private View createRequestCard(SkillLoopData.Student person) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(background(
                Color.rgb(245, 242, 235), 26));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);

        // Large photo panel, matching the Discover hero.
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(background(
                Color.rgb(215, 200, 181), 20));
        hero.setClipToOutline(true);

        LinearLayout.LayoutParams heroParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(210));

        card.addView(hero, heroParams);

        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);

        FrameLayout.LayoutParams photoParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);

        hero.addView(photo, photoParams);

        TextView initials = new TextView(this);
        initials.setText(getInitials(person.name));
        initials.setTextColor(Color.rgb(117, 102, 87));
        initials.setTextSize(64);
        initials.setGravity(Gravity.CENTER);
        initials.setTypeface(Typeface.create(
                getResources().getFont(R.font.big_shoulders_extrabold), Typeface.BOLD));

        FrameLayout.LayoutParams initialsParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);

        hero.addView(initials, initialsParams);

        boolean hasPhoto = false;
        if (!remoteMode) hasPhoto = loadStudentPhoto(person.id, photo);
        if (!hasPhoto && person.photoUrl != null && !person.photoUrl.trim().isEmpty()) {
            SkillLoopImageLoader.load(this, person.photoUrl, photo, () -> runOnUiThread(() -> {
                if (person.photoUrl.equals(photo.getTag())) {
                    photo.setVisibility(View.VISIBLE);
                    initials.setVisibility(View.GONE);
                }
            }));
        }
        initials.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        photo.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);

        TextView domain = new TextView(this);
        domain.setText(safe(person.domain).toUpperCase(Locale.ROOT));
        domain.setTextColor(Color.WHITE);
        domain.setTextSize(10);
        domain.setGravity(Gravity.CENTER);
        domain.setPadding(dp(12), dp(7), dp(12), dp(7));
        domain.setBackground(background(Color.rgb(23, 23, 23), 12));

        FrameLayout.LayoutParams domainParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.START);

        domainParams.setMargins(dp(12), 0, 0, dp(12));
        hero.addView(domain, domainParams);

        // Course and year.
        TextView course = text(
                safe(person.course).toUpperCase(Locale.ROOT)
                        + " · "
                        + safe(person.year).toUpperCase(Locale.ROOT),
                10, Color.rgb(105, 101, 94), false);

        course.setPadding(0, dp(10), 0, 0);
        card.addView(course);

        // Name.
        TextView name = text(
                safe(person.name).toUpperCase(Locale.ROOT) + ".",
                32, Color.rgb(23, 23, 23), true);

        name.setPadding(0, dp(3), 0, dp(3));
        card.addView(name);

        // Bio.
        TextView bio = text(
                safe(person.bio), 13,
                Color.rgb(45, 45, 45), false);

        bio.setMaxLines(2);
        bio.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(bio);

        addDivider(card);
        addDetail(card, "CAN TEACH", person.teaches);
        addDetail(card, "WANTS TO LEARN", person.wants);

        // The message written on Discover.
        String message = remoteMode
                ? introductionForStudent.getOrDefault(person.id, "")
                : getSharedPreferences(DISCOVER_PREFS, MODE_PRIVATE)
                    .getString("connection_message_" + person.id, "");

        if (message != null && !message.trim().isEmpty()) {
            addDivider(card);

            TextView messageLabel = text(
                    "INTRODUCTION MESSAGE",
                    10, Color.rgb(105, 101, 94), false);

            messageLabel.setLetterSpacing(0.06f);
            card.addView(messageLabel);

            TextView messageText = text(
                    "“" + message.trim() + "”",
                    14, Color.rgb(23, 23, 23), false);

            messageText.setPadding(0, dp(5), 0, dp(2));
            card.addView(messageText);
        }

        // Actions.
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView pass = actionButton(
                "PASS",
                Color.rgb(245, 242, 235),
                Color.rgb(23, 23, 23));

        TextView connect = actionButton(
                "CONNECT  ↗",
                Color.rgb(255, 90, 0),
                Color.rgb(23, 23, 23));

        LinearLayout.LayoutParams actionParams =
                new LinearLayout.LayoutParams(0, dp(46), 1);

        actionParams.topMargin = dp(12);
        actionParams.rightMargin = dp(6);
        actions.addView(pass, actionParams);

        LinearLayout.LayoutParams connectParams =
                new LinearLayout.LayoutParams(0, dp(46), 1);

        connectParams.topMargin = dp(12);
        connectParams.leftMargin = dp(6);
        actions.addView(connect, connectParams);

        card.addView(actions);

        pass.setOnClickListener(v ->
                decideRequest(card, person, false));

        connect.setOnClickListener(v ->
                decideRequest(card, person, true));

        // Soft entrance animation.
        card.setAlpha(0f);
        card.setTranslationY(dp(12));
        card.post(() -> card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240)
                .start());

        SkillLoopMotion.attachToClickableViews(card);
        return card;
    }

    private boolean loadStudentPhoto(String studentId, ImageView target) {
        SharedPreferences prefs =
                getSharedPreferences(PHOTO_PREFS, MODE_PRIVATE);

        String path = prefs.getString("photo_path_" + studentId, "");

        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        File file = new File(path);

        if (!file.exists()) {
            return false;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(path);

        if (bitmap == null) {
            return false;
        }

        target.setImageBitmap(bitmap);
        return true;
    }

    private void decideRequest(View card, SkillLoopData.Student person, boolean accepted) {
        if (card.getTag() != null) return;
        card.setTag("processing");
        if (remoteMode) {
            FirebaseSkillLoopRepository.ConnectionRequest request = requestForStudent.get(person.id);
            if (request == null) {
                card.setTag(null);
                Toast.makeText(this, "This request is no longer available.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (accepted) {
                FirebaseSkillLoopRepository.acceptRequest(request.id, conversationId -> finishRemoteDecision(card, person, true),
                        error -> remoteDecisionFailed(card, error.getLocalizedMessage()));
            } else {
                FirebaseSkillLoopRepository.declineRequest(request.id, unused -> finishRemoteDecision(card, person, false),
                        error -> remoteDecisionFailed(card, error.getLocalizedMessage()));
            }
            return;
        }
        card.setTag(null);
        Toast.makeText(this, "Incoming requests require Firebase and a second account.", Toast.LENGTH_LONG).show();
    }

    private void finishRemoteDecision(View card, SkillLoopData.Student person, boolean accepted) {
        runOnUiThread(() -> {
            card.animate().alpha(0f).translationY(-dp(16)).setDuration(190)
                    .withEndAction(() -> {
                        requestsContainer.removeView(card);
                        requestForStudent.remove(person.id);
                        introductionForStudent.remove(person.id);
                        requestCount.setText(String.format(Locale.ROOT, "%02d REQUESTS", requestsContainer.getChildCount()));
                        requestsEmpty.setVisibility(requestsContainer.getChildCount() == 0 ? View.VISIBLE : View.GONE);
                    }).start();
            Toast.makeText(this, accepted ? "Connected with " + person.name + ". Chat is now open." : "Request declined", Toast.LENGTH_SHORT).show();
        });
    }

    private void remoteDecisionFailed(View card, String message) {
        runOnUiThread(() -> {
            card.setTag(null);
            card.setAlpha(1f);
            card.setTranslationX(0f);
            card.setTranslationY(0f);
            Toast.makeText(this, message == null ? "Couldn't update this request." : message, Toast.LENGTH_LONG).show();
        });
    }

    private void saveDecisions() {
        getSharedPreferences(DISCOVER_PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(LIKED_KEY, new HashSet<>(likedIds))
                .apply();
    }

    private void addDetail(
            LinearLayout parent,
            String label,
            String value) {

        TextView heading = text(
                label, 10, Color.rgb(105, 101, 94), false);

        heading.setLetterSpacing(0.06f);
        heading.setPadding(0, dp(9), 0, dp(3));
        parent.addView(heading);

        TextView content = text(
                safe(value), 14, Color.rgb(23, 23, 23), false);

        content.setMaxLines(2);
        content.setEllipsize(android.text.TextUtils.TruncateAt.END);
        parent.addView(content);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(216, 210, 200));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));

        params.topMargin = dp(10);
        params.bottomMargin = dp(9);
        parent.addView(divider, params);
    }

    private TextView actionButton(
            String label, int backgroundColor, int textColor) {

        TextView button = new TextView(this);
        button.setText(label);
        button.setTextSize(12);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(background(backgroundColor, 28));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView text(
            String value, int size, int color, boolean bold) {

        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(getResources().getFont(R.font.big_shoulders_extrabold), Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private String getInitials(String fullName) {
        String[] parts = safe(fullName).trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }

        return (parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty()
                ? "Not added yet" : value.trim();
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void setupNavigation() {
        SkillLoopTabNavigation.setup(this, 1);
    }

    private void navigateTo(Class<?> destination) {
        startActivity(new Intent(this, destination));
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        if (requestListener != null) requestListener.remove();
        super.onDestroy();
    }
}
