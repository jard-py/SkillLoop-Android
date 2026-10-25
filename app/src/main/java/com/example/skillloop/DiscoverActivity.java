package com.example.skillloop;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DiscoverActivity extends AppCompatActivity {

    private static final String PREFS = "skillloop_discover";
    private static final String LIKED = "liked_students";
    private static final String PASSED = "passed_students";

    private final List<SkillLoopData.Student> allStudents =
            new ArrayList<>();
    private final List<SkillLoopData.Student> availableStudents = new ArrayList<>();

    private final List<SkillLoopData.Student> history =
            new ArrayList<>();

    private Set<String> likedIds = new HashSet<>();
    private Set<String> passedIds = new HashSet<>();

    private int currentIndex = 0;

    private TextView initials, domain, counter, course, name, bio;
    private TextView teaches, wants, emptyMessage, likeFeedback;
    private TextView discoverStatus, filtersButton;
    private String selectedDomain = "All domains";
    private String preferredAfterReload;

    private View personCard, actionButtons, connectComposer;
    private EditText connectMessage;
    private TextView connectHint;

    private SkillLoopData.Student currentStudent;
    private ImageView personPhoto;
    private boolean remoteMode = false;
    private ListenerRegistration profilesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_discover_cards);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));

        View root = findViewById(R.id.discover_root);

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
        remoteMode = FirebaseSkillLoopRepository.isConfigured(this)
                && FirebaseSkillLoopRepository.currentUid() != null;
        loadDecisions();
        discoverStatus.setText(remoteMode ? "MATCH ENGINE · LIVE" : "LOCAL DEMO · SAMPLE STUDENTS");
        filtersButton.setOnClickListener(v -> showDomainFilter());
        setupButtons();
        setupNavigation();
        if (remoteMode) {
            personCard.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
            emptyMessage.setText("FINDING YOUR NEXT SKILL EXCHANGE...\nLoading students on SkillLoop.");
            loadRemoteStudents();
        } else {
            loadStudents();
        }
    }

    private void bindViews() {
        initials = findViewById(R.id.person_initials);
        personPhoto = findViewById(R.id.person_photo);
        domain = findViewById(R.id.person_domain);
        counter = findViewById(R.id.person_counter);
        course = findViewById(R.id.person_course);
        name = findViewById(R.id.person_name);
        bio = findViewById(R.id.person_bio);
        teaches = findViewById(R.id.person_teaches);
        wants = findViewById(R.id.person_wants);

        emptyMessage = findViewById(R.id.discover_empty);
        likeFeedback = findViewById(R.id.like_feedback);
        discoverStatus = findViewById(R.id.discover_status);
        filtersButton = findViewById(R.id.discover_filters_button);

        personCard = findViewById(R.id.discover_person_card);
        actionButtons = findViewById(R.id.discover_actions);

        connectComposer = findViewById(R.id.connect_composer);
        connectMessage = findViewById(R.id.connect_message);
        connectHint = findViewById(R.id.connect_hint);
    }

    private void loadDecisions() {
        SharedPreferences prefs =
                getSharedPreferences(decisionPrefsName(), MODE_PRIVATE);

        likedIds = new HashSet<>(
                prefs.getStringSet(LIKED, new HashSet<>()));

        passedIds = new HashSet<>(
                prefs.getStringSet(PASSED, new HashSet<>()));
    }

    private String decisionPrefsName() {
        if (remoteMode) {
            String uid = FirebaseSkillLoopRepository.currentUid();
            if (uid != null && !uid.isEmpty()) return PREFS + "_" + uid;
        }
        return PREFS + "_demo";
    }

    private void saveDecisions() {
        getSharedPreferences(decisionPrefsName(), MODE_PRIVATE)
                .edit()
                .putStringSet(LIKED, new HashSet<>(likedIds))
                .putStringSet(PASSED, new HashSet<>(passedIds))
                .apply();
    }

    private void loadStudents() {
        availableStudents.clear();
        SkillLoopData.Student me = SkillLoopData.getMyProfile(this);
        for (SkillLoopData.Student student : SkillLoopData.getStudents()) {
            if (me != null && student.id.equals(me.id)) continue;
            availableStudents.add(student);
        }
        applyStudentFilters(true, null);
    }

    private void loadRemoteStudents() {
        if (profilesListener != null) profilesListener.remove();
        final String preferred = preferredAfterReload != null
                ? preferredAfterReload : (currentStudent == null ? null : currentStudent.id);
        preferredAfterReload = null;
        profilesListener = FirebaseSkillLoopRepository.listenDiscoverableProfiles(students -> {
            availableStudents.clear();
            availableStudents.addAll(students);
            applyStudentFilters(false, preferred);
        }, error -> {
            emptyMessage.setVisibility(View.VISIBLE);
            emptyMessage.setText("WE COULDN'T LOAD STUDENTS.\nCheck your connection and try again.");
            Toast.makeText(this, "Couldn't load students: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void applyStudentFilters(boolean restart, String keepStudentId) {
        allStudents.clear();
        for (SkillLoopData.Student student : availableStudents) {
            if (likedIds.contains(student.id) || passedIds.contains(student.id)) continue;
            if (!"All domains".equalsIgnoreCase(selectedDomain)
                    && !selectedDomain.equalsIgnoreCase(student.domain)) continue;
            allStudents.add(student);
        }
        if (restart) {
            currentIndex = 0;
            history.clear();
        } else if (keepStudentId != null) {
            int found = -1;
            for (int i = 0; i < allStudents.size(); i++) {
                if (keepStudentId.equals(allStudents.get(i).id)) { found = i; break; }
            }
            currentIndex = found >= 0 ? found : Math.min(currentIndex, allStudents.size());
        } else {
            currentIndex = Math.min(currentIndex, allStudents.size());
        }
        showCurrentStudent();
    }

    private void showDomainFilter() {
        String[] domains = {"All domains", "Technology", "Business", "Design", "Engineering", "Media", "Research", "Other"};
        int selected = 0;
        for (int i = 0; i < domains.length; i++) {
            if (domains[i].equalsIgnoreCase(selectedDomain)) { selected = i; break; }
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter by domain")
                .setSingleChoiceItems(domains, selected, (dialog, which) -> {
                    selectedDomain = domains[which];
                    filtersButton.setText("FILTERS  ·  " + (which == 0 ? "ALL" : selectedDomain.toUpperCase(Locale.ROOT)) + "  ⌄");
                    applyStudentFilters(true, null);
                    dialog.dismiss();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void setupButtons() {
        findViewById(R.id.pass_person).setOnClickListener(v -> {
            if (connectComposer.getVisibility() == View.VISIBLE) {
                closeComposer(false);
            }
            passCurrentStudent();
        });

        findViewById(R.id.like_person).setOnClickListener(v ->
                openComposer());

        findViewById(R.id.undo_person).setOnClickListener(v -> {
            if (connectComposer.getVisibility() == View.VISIBLE) {
                closeComposer(false);
            }
            undoLastAction();
        });

        findViewById(R.id.view_profile_button).setOnClickListener(v ->
                openFullProfile());

        findViewById(R.id.cancel_connect).setOnClickListener(v ->
                closeComposer(true));

        findViewById(R.id.send_connect).setOnClickListener(v ->
                sendConnectionRequest());
    }

    private void openComposer() {
        if (currentStudent == null
                || connectComposer.getVisibility() == View.VISIBLE) {
            return;
        }

        String firstName = currentStudent.name.split("\\s+")[0];

        connectHint.setText(
                "Introduce yourself to " + firstName
                        + ". Mention a skill or shared interest.");

        connectMessage.setText("");
        connectMessage.setHint(
                "Hey " + firstName
                        + "! I noticed you're interested in...");

        actionButtons.animate()
                .alpha(0f)
                .translationY(-12f)
                .setDuration(160)
                .withEndAction(() -> {
                    actionButtons.setVisibility(View.GONE);
                    actionButtons.setAlpha(1f);
                    actionButtons.setTranslationY(0f);

                    connectComposer.setVisibility(View.VISIBLE);
                    connectComposer.setAlpha(0f);
                    connectComposer.setTranslationY(18f);

                    connectComposer.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(240)
                            .start();

                    connectMessage.requestFocus();

                    connectMessage.postDelayed(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        InputMethodManager imm =
                                (InputMethodManager) getSystemService(
                                        Context.INPUT_METHOD_SERVICE);

                        if (imm != null) {
                            imm.showSoftInput(
                                    connectMessage,
                                    InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 250);
                })
                .start();
    }

    private void closeComposer(boolean animate) {
        if (connectComposer == null
                || connectComposer.getVisibility() != View.VISIBLE) {
            return;
        }

        InputMethodManager imm =
                (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(
                    connectMessage.getWindowToken(), 0);
        }

        connectMessage.clearFocus();

        if (!animate) {
            connectComposer.animate().cancel();
            actionButtons.animate().cancel();

            connectComposer.setVisibility(View.GONE);
            connectComposer.setAlpha(0f);
            connectComposer.setTranslationY(18f);

            actionButtons.setVisibility(View.VISIBLE);
            actionButtons.setAlpha(1f);
            actionButtons.setTranslationY(0f);
            return;
        }

        connectComposer.animate()
                .alpha(0f)
                .translationY(18f)
                .setDuration(160)
                .withEndAction(() -> {
                    connectComposer.setVisibility(View.GONE);
                    connectComposer.setAlpha(0f);
                    connectComposer.setTranslationY(18f);

                    actionButtons.setVisibility(View.VISIBLE);
                    actionButtons.setAlpha(0f);
                    actionButtons.setTranslationY(-12f);

                    actionButtons.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }

    private void sendConnectionRequest() {
        if (currentStudent == null) {
            return;
        }

        String message = connectMessage.getText()
                .toString()
                .trim();

        if (message.isEmpty()) {
            connectMessage.setError(
                    "Write a short introduction first");
            connectMessage.requestFocus();
            return;
        }

        String studentId = currentStudent.id;
        SkillLoopData.Student sentStudent = currentStudent;
        if (remoteMode) {
            View sendButton = findViewById(R.id.send_connect);
            sendButton.setEnabled(false);
            FirebaseSkillLoopRepository.sendRequest(studentId, message, requestId -> runOnUiThread(() -> {
                sendButton.setEnabled(true);
                likedIds.add(studentId);
                saveDecisions();
                history.add(sentStudent);
                closeComposer(false);
                currentIndex++;
                showCurrentStudent();
                Toast.makeText(this, "Request sent. Chat unlocks when they accept.", Toast.LENGTH_SHORT).show();
            }), error -> runOnUiThread(() -> {
                sendButton.setEnabled(true);
                Toast.makeText(this, error.getLocalizedMessage() == null ? "Couldn't send request." : error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }));
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString("connection_message_" + studentId, message).apply();
        likedIds.add(studentId);
        saveDecisions();
        history.add(currentStudent);
        closeComposer(false);
        currentIndex++;
        showCurrentStudent();
        Toast.makeText(this,
                "Demo only: another student receives requests after Firebase is configured.",
                Toast.LENGTH_LONG).show();
    }

    private void showCurrentStudent() {
        if (currentIndex >= allStudents.size()) {
            currentStudent = null;

            personCard.setVisibility(View.GONE);
            actionButtons.setVisibility(View.GONE);
            connectComposer.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
            counter.setText("END");
            likeFeedback.setText(remoteMode
                    ? "Complete your profile and check back as more students join."
                    : "You've seen everyone in this demo pool.");
            if (remoteMode && allStudents.isEmpty()) {
                emptyMessage.setText("NO STUDENTS YET.\nBe one of the first to build your campus network.");
            } else if (!remoteMode) {
                emptyMessage.setText("YOU'VE MET EVERYONE.\nCheck back for your next connection.");
            }
            return;
        }

        currentStudent = allStudents.get(currentIndex);

        personCard.setVisibility(View.VISIBLE);
        actionButtons.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        initials.setText(getInitials(currentStudent.name));
        initials.setVisibility(View.VISIBLE);
        personPhoto.setVisibility(View.GONE);
        personPhoto.setImageDrawable(null);
        if (currentStudent.photoUrl != null && !currentStudent.photoUrl.trim().isEmpty()) {
            SkillLoopImageLoader.load(this, currentStudent.photoUrl, personPhoto, () -> runOnUiThread(() -> {
                if (currentStudent != null && currentStudent.photoUrl.equals(personPhoto.getTag())) {
                    personPhoto.setVisibility(View.VISIBLE);
                    initials.setVisibility(View.GONE);
                }
            }));
        }
        domain.setText(
                currentStudent.domain.toUpperCase(Locale.ROOT));

        counter.setText(String.format(
                Locale.ROOT,
                "%02d / %02d",
                currentIndex + 1,
                allStudents.size()));

        course.setText(
                currentStudent.course.toUpperCase(Locale.ROOT)
                        + " · "
                        + currentStudent.year.toUpperCase(Locale.ROOT));

        name.setText(
                currentStudent.name.toUpperCase(Locale.ROOT) + ".");

        bio.setText(currentStudent.bio);

        teaches.setText(orFallback(
                currentStudent.teaches, "Not added yet"));

        wants.setText(orFallback(
                currentStudent.wants, "Open to learning"));

        likeFeedback.setText("");
    }

    private void passCurrentStudent() {
        if (currentStudent == null) {
            return;
        }

        passedIds.add(currentStudent.id);
        saveDecisions();

        history.add(currentStudent);
        currentIndex++;

        showCurrentStudent();
    }

    private void undoLastAction() {
        if (history.isEmpty()) {
            Toast.makeText(this, "Nothing to undo yet", Toast.LENGTH_SHORT).show();
            return;
        }
        SkillLoopData.Student previous = history.get(history.size() - 1);
        if (remoteMode && likedIds.contains(previous.id)) {
            FirebaseSkillLoopRepository.cancelRequest(previous.id,
                    unused -> runOnUiThread(() -> {
                        history.remove(history.size() - 1);
                        likedIds.remove(previous.id);
                        passedIds.remove(previous.id);
                        saveDecisions();
                        preferredAfterReload = previous.id;
                        loadRemoteStudents();
                        Toast.makeText(this, "Connection request cancelled.", Toast.LENGTH_SHORT).show();
                    }), error -> Toast.makeText(this,
                            error.getLocalizedMessage() == null ? "Couldn't undo this request." : error.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show());
            return;
        }
        history.remove(history.size() - 1);
        likedIds.remove(previous.id);
        passedIds.remove(previous.id);
        if (!remoteMode) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove("connection_message_" + previous.id).apply();
        }
        saveDecisions();
        if (remoteMode) {
            preferredAfterReload = previous.id;
            loadRemoteStudents();
            return;
        }
        applyStudentFilters(false, previous.id);
    }

    private void openFullProfile() {
        if (currentStudent == null) {
            return;
        }

        Intent intent = new Intent(this, ProfileActivity.class);

        intent.putExtra("name", currentStudent.name);
        intent.putExtra("course", currentStudent.course);
        intent.putExtra("year", currentStudent.year);
        intent.putExtra("teaches", currentStudent.teaches);
        intent.putExtra("wants", currentStudent.wants);
        intent.putExtra("bio", currentStudent.bio);
        intent.putExtra("project", currentStudent.project);
        intent.putExtra("domain", currentStudent.domain);
        intent.putExtra("interests", currentStudent.interests);
        intent.putExtra("studentId", currentStudent.id);
        intent.putExtra("photoUrl", currentStudent.photoUrl);

        startActivity(intent);
        overridePendingTransition(R.anim.tab_enter, R.anim.tab_exit);
    }

    private String getInitials(String fullName) {
        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1)
                    .toUpperCase(Locale.ROOT);
        }

        return (parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    private String orFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback : value;
    }

    private void setupNavigation() {
        SkillLoopTabNavigation.setup(this, 0);
    }

    private void navigateTo(Class<?> destination) {
        startActivity(new Intent(this, destination));
        overridePendingTransition(R.anim.tab_enter, R.anim.tab_exit);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (personCard != null && !remoteMode) {
            loadDecisions();
            loadStudents();
        }
    }

    @Override
    protected void onDestroy() {
        if (profilesListener != null) profilesListener.remove();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (connectComposer != null
                && connectComposer.getVisibility() == View.VISIBLE) {
            closeComposer(true);
        } else {
            super.onBackPressed();
        }
    }
}
