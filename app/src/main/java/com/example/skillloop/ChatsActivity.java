package com.example.skillloop;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatsActivity extends AppCompatActivity {

    private static final String PREFS = "skillloop_local_chats";
    private static final String MESSAGES_KEY = "messages_";

    private LinearLayout conversationContainer;
    private LinearLayout messagesContainer;
    private ScrollView messagesScroll;
    private LinearLayout listScreen;
    private LinearLayout detailScreen;
    private EditText messageInput;
    private TextView chatPersonName;
    private TextView chatsEmpty;
    private TextView chatModeLabel;

    private final List<Person> people = new ArrayList<>();
    private Person activePerson;
    private boolean remoteMode = false;
    private ListenerRegistration conversationsListener;
    private ListenerRegistration messagesListener;
    private boolean loadingConversations = false;
    private boolean conversationLoadError = false;
    private int pendingProfileLoads = 0;
    private String activeConversationId;
    private int conversationsRevision = 0;
    private final List<FirebaseSkillLoopRepository.ChatMessage> remoteMessages = new ArrayList<>();

    private static class Person {
        String id;
        String name;
        String course;
        String teaches;
        String wants;
        String preview;
        String conversationId;
        String photoUrl;
        String lastMessageSenderId;
        long lastMessageAtMillis;

        Person(String id, String name, String course,
               String teaches, String wants, String preview) {
            this(id, name, course, teaches, wants, preview, "", "", "", 0L);
        }

        Person(String id, String name, String course, String teaches, String wants,
               String preview, String conversationId, String photoUrl,
               String lastMessageSenderId, long lastMessageAtMillis) {
            this.id = id;
            this.name = name;
            this.course = course;
            this.teaches = teaches;
            this.wants = wants;
            this.preview = preview;
            this.conversationId = conversationId;
            this.photoUrl = photoUrl;
            this.lastMessageSenderId = lastMessageSenderId;
            this.lastMessageAtMillis = lastMessageAtMillis;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_chats);
        findViewById(android.R.id.content).post(() -> SkillLoopMotion.attachToClickableViews(findViewById(android.R.id.content)));
        View root = findViewById(R.id.chats_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            view.setPadding(0, top, 0, imeVisible ? 0 : bottom);
            View island = findViewById(R.id.nav_island);
            if (island != null) island.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            int reservedBottom = imeVisible ? 0 : dp(80);
            View list = findViewById(R.id.chat_list_screen);
            View detail = findViewById(R.id.chat_detail_screen);
            if (list != null) list.setPadding(list.getPaddingLeft(), list.getPaddingTop(),
                    list.getPaddingRight(), reservedBottom);
            if (detail != null) detail.setPadding(detail.getPaddingLeft(), detail.getPaddingTop(),
                    detail.getPaddingRight(), reservedBottom);
            return insets;
        });

        conversationContainer = findViewById(R.id.conversation_container);
        messagesContainer = findViewById(R.id.messages_container);
        messagesScroll = findViewById(R.id.messages_scroll);
        listScreen = findViewById(R.id.chat_list_screen);
        detailScreen = findViewById(R.id.chat_detail_screen);
        messageInput = findViewById(R.id.message_input);
        chatPersonName = findViewById(R.id.chat_person_name);
        chatsEmpty = findViewById(R.id.chats_empty);
        chatModeLabel = findViewById(R.id.chat_mode_label);

        remoteMode = FirebaseSkillLoopRepository.isConfigured(this)
                && FirebaseSkillLoopRepository.currentUid() != null;
        chatModeLabel.setText(remoteMode ? "SKILLLOOP / MESSAGES" : "LOCAL DEMO · NOT SYNCED");
        if (remoteMode) {
            loadingConversations = true;
            chatsEmpty.setText("LOADING ACTIVE EXCHANGES...");
            loadRemoteConversations();
        } else {
            seedPeople();
            renderConversations();
        }
        setupNavigation();

        findViewById(R.id.chat_back).setOnClickListener(
                v -> showConversationList());

        findViewById(R.id.send_message).setOnClickListener(
                v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (conversationContainer != null && !remoteMode) {
            seedPeople();
            renderConversations();
        } else if (conversationContainer != null && remoteMode && conversationsListener == null) {
            loadingConversations = true;
            conversationLoadError = false;
            loadRemoteConversations();
        }
    }

    private void seedPeople() {
        people.clear();

        for (SkillLoopData.Student student :
                SkillLoopData.getConnections(this)) {

            String preview = "Start a conversation.";

            people.add(new Person(
                    student.id,
                    student.name,
                    student.course + " · " + student.year,
                    student.teaches,
                    student.wants,
                    preview
            ));
        }
    }

    private void loadRemoteConversations() {
        if (conversationsListener != null) conversationsListener.remove();
        conversationsListener = FirebaseSkillLoopRepository.listenConversations(conversations -> {
            int revision = ++conversationsRevision;
            people.clear();
            conversationContainer.removeAllViews();
            conversationLoadError = false;
            pendingProfileLoads = conversations.size();
            if (conversations.isEmpty()) {
                loadingConversations = false;
                renderConversations();
                return;
            }
            loadingConversations = true;
            renderConversations();
            for (FirebaseSkillLoopRepository.Conversation conversation : conversations) {
                FirebaseSkillLoopRepository.loadStudent(conversation.otherUid, student -> {
                    if (revision != conversationsRevision) return;
                    if (student != null) {
                        String course = student.course + " · " + student.year;
                        people.removeIf(p -> p.conversationId.equals(conversation.id));
                        people.add(new Person(conversation.otherUid, student.name, course,
                                student.teaches, student.wants, conversation.lastMessage,
                                conversation.id, student.photoUrl, conversation.lastMessageSenderId,
                                conversation.lastMessageAtMillis));
                        people.sort((a, b) -> Long.compare(b.lastMessageAtMillis, a.lastMessageAtMillis));
                    }
                    pendingProfileLoads = Math.max(0, pendingProfileLoads - 1);
                    if (!people.isEmpty()) {
                        loadingConversations = false;
                    } else if (pendingProfileLoads == 0) {
                        loadingConversations = false;
                        conversationLoadError = true;
                    }
                    renderConversations();
                }, error -> {
                    if (revision != conversationsRevision) return;
                    pendingProfileLoads = Math.max(0, pendingProfileLoads - 1);
                    if (pendingProfileLoads == 0 && people.isEmpty()) {
                        loadingConversations = false;
                        conversationLoadError = true;
                    }
                    renderConversations();
                });
            }
        }, error -> {
            conversationsListener = null;
            loadingConversations = false;
            conversationLoadError = true;
            people.clear();
            renderConversations();
            Toast.makeText(this, "Couldn't load chats: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        });
    }

    // ---------------- CONVERSATION LIST ----------------

    private void renderConversations() {
        conversationContainer.removeAllViews();

        boolean hasConversations = !people.isEmpty();
        if (hasConversations) {
            chatsEmpty.setOnClickListener(null);
            chatsEmpty.setVisibility(View.GONE);
        } else {
            if (loadingConversations) {
                chatsEmpty.setText("LOADING ACTIVE EXCHANGES...");
            } else if (conversationLoadError) {
                chatsEmpty.setText("COULDN'T LOAD YOUR CHATS.\nTap here to retry.");
                chatsEmpty.setOnClickListener(v -> {
                    if (remoteMode) {
                        loadingConversations = true;
                        conversationLoadError = false;
                        renderConversations();
                        loadRemoteConversations();
                    }
                });
            } else if (remoteMode) {
                chatsEmpty.setText("NO ACTIVE EXCHANGES YET.\nAccept a request to start a conversation.");
            } else {
                chatsEmpty.setText("NO CONVERSATIONS YET.\nYour next connection is out there.");
            }
            chatsEmpty.setVisibility(View.VISIBLE);
        }

        for (Person person : people) {
            conversationContainer.addView(createConversationRow(person));
        }
    }

    private View createConversationRow(Person person) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15), dp(15), dp(15), dp(15));
        row.setBackground(makeBackground(Color.rgb(245, 242, 235), 20, 0));

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        rowParams.bottomMargin = dp(12);
        row.setLayoutParams(rowParams);

        // Avatar: remote Firebase photo when available, initials as a robust fallback.
        FrameLayout avatar = new FrameLayout(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(52), dp(52));
        avatarParams.rightMargin = dp(13);
        ImageView avatarPhoto = new ImageView(this);
        avatarPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarPhoto.setBackground(makeBackground(Color.rgb(216, 200, 181), 40, 0));
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(dp(52), dp(52));
        avatar.addView(avatarPhoto, imageParams);

        TextView initials = new TextView(this);
        String[] parts = person.name.trim().split("\\s+");
        String letters = parts.length == 0 || parts[0].isEmpty() ? "S" : parts[0].substring(0, 1);
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) letters += parts[parts.length - 1].substring(0, 1);
        initials.setText(letters.toUpperCase(Locale.ROOT));
        initials.setTextColor(Color.rgb(23, 23, 23));
        initials.setTextSize(18);
        initials.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        initials.setGravity(Gravity.CENTER);
        initials.setBackground(makeBackground(Color.rgb(216, 200, 181), 40, 0));
        avatar.addView(initials, new FrameLayout.LayoutParams(dp(52), dp(52)));
        avatarPhoto.setVisibility(View.GONE);
        if (remoteMode && person.photoUrl != null && !person.photoUrl.isEmpty()) {
            SkillLoopImageLoader.load(this, person.photoUrl, avatarPhoto, () -> runOnUiThread(() -> {
                if (person.photoUrl.equals(avatarPhoto.getTag())) {
                    avatarPhoto.setVisibility(View.VISIBLE);
                    initials.setVisibility(View.GONE);
                }
            }));
        }
        row.addView(avatar, avatarParams);

        // Conversation details
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams detailsParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1);

        row.addView(details, detailsParams);

        TextView name = text(
                person.name.toUpperCase(Locale.ROOT) + ".",
                23,
                Color.rgb(23, 23, 23),
                true);

        details.addView(name);

        TextView course = text(
                person.course,
                11,
                Color.rgb(105, 101, 94),
                false);

        course.setPadding(0, dp(2), 0, dp(5));
        details.addView(course);

        String lastMessage = getLastMessage(person);

        TextView preview = text(
                lastMessage,
                12,
                Color.rgb(75, 75, 75),
                false);

        preview.setMaxLines(1);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        details.addView(preview);

        // Time of most recent saved message
        String lastTime = getLastMessageTime(person);

        if (!lastTime.isEmpty()) {
            TextView time = text(
                    lastTime,
                    10,
                    Color.rgb(140, 135, 127),
                    false);

            time.setGravity(Gravity.END);
            time.setPadding(0, dp(5), 0, 0);
            details.addView(time);
        }

        TextView arrow = text(
                "↗",
                22,
                Color.rgb(23, 23, 23),
                false);

        arrow.setPadding(dp(8), 0, 0, 0);
        row.addView(arrow);

        row.setClickable(true);
        row.setFocusable(true);
        SkillLoopMotion.attachToClickableViews(row);
        row.setOnClickListener(v -> openChat(person));

        return row;
    }

    private void openChat(Person person) {
        activePerson = person;

        chatPersonName.setText(
                person.name.toUpperCase(Locale.ROOT) + ".");

        messageInput.setText("");

        listScreen.setVisibility(View.GONE);
        detailScreen.setVisibility(View.VISIBLE);

        if (messagesListener != null) { messagesListener.remove(); messagesListener = null; }
        if (remoteMode) {
            activeConversationId = person.conversationId;
            String openedConversationId = activeConversationId;
            remoteMessages.clear();
            showMessageNotice("LOADING MESSAGES...", Color.rgb(105, 101, 94));
            messagesListener = FirebaseSkillLoopRepository.listenMessages(activeConversationId, messages -> {
                if (!openedConversationId.equals(activeConversationId)) return;
                remoteMessages.clear();
                remoteMessages.addAll(messages);
                renderRemoteMessages();
            }, error -> runOnUiThread(() -> {
                if (!openedConversationId.equals(activeConversationId)) return;
                showMessageNotice("MESSAGES COULDN'T LOAD.\nCheck your connection and try again.", Color.rgb(130, 45, 35));
                Toast.makeText(this, "Couldn't load messages: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }));
        } else {
            activeConversationId = null;
            renderMessages();
        }
    }

    private void showConversationList() {
        hideKeyboard();
        if (messagesListener != null) { messagesListener.remove(); messagesListener = null; }
        activeConversationId = null;
        detailScreen.setVisibility(View.GONE);
        listScreen.setVisibility(View.VISIBLE);
        renderConversations();
    }

    // ---------------- MESSAGE DISPLAY ----------------

    private void renderMessages() {
        if (activePerson == null) return;

        messagesContainer.removeAllViews();

        JSONArray messages = loadMessages(activePerson.id);

        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.optJSONObject(i);

            if (message == null) continue;

            String body = message.optString("body", "");
            boolean mine = message.optBoolean("mine", false);
            long time = message.optLong("time", 0L);

            messagesContainer.addView(
                    createMessageBubble(body, mine, time));
        }

        messagesScroll.post(
                () -> messagesScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void showMessageNotice(String message, int color) {
        messagesContainer.removeAllViews();
        TextView notice = text(message, 12, color, true);
        notice.setGravity(Gravity.CENTER);
        notice.setPadding(dp(20), dp(24), dp(20), dp(24));
        messagesContainer.addView(notice, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void renderRemoteMessages() {
        if (!remoteMode || activeConversationId == null) return;
        messagesContainer.removeAllViews();
        String uid = FirebaseSkillLoopRepository.currentUid();
        if (remoteMessages.isEmpty()) {
            TextView empty = text("SAY HELLO. YOUR SKILL EXCHANGE STARTS HERE.", 12, Color.rgb(105, 101, 94), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(24), dp(20), dp(24));
            messagesContainer.addView(empty);
        }
        for (FirebaseSkillLoopRepository.ChatMessage message : remoteMessages) {
            boolean mine = uid != null && uid.equals(message.senderId);
            messagesContainer.addView(createMessageBubble(message.text, mine, message.createdAtMillis));
        }
        messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN));
    }

    private View createMessageBubble(
            String body, boolean mine, long timestamp) {

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(mine ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams wrapperParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        wrapperParams.bottomMargin = dp(12);
        wrapper.setLayoutParams(wrapperParams);

        // Bubble
        TextView bubble = new TextView(this);
        bubble.setText(body);
        bubble.setTextSize(14);
        bubble.setTextColor(
                mine ? Color.WHITE : Color.rgb(23, 23, 23));

        bubble.setPadding(dp(15), dp(12), dp(15), dp(12));

        bubble.setMaxWidth((int) (
                getResources().getDisplayMetrics().widthPixels * 0.76f));

        bubble.setBackground(makeBackground(
                mine ? Color.rgb(23, 23, 23) : Color.WHITE,
                18,
                0));

        wrapper.addView(bubble);

        // Timestamp below the bubble
        if (timestamp > 0L) {
            TextView time = text(
                    formatMessageTime(timestamp),
                    10,
                    Color.rgb(125, 120, 112),
                    false);

            LinearLayout.LayoutParams timeParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);

            timeParams.topMargin = dp(4);
            timeParams.leftMargin = dp(4);
            timeParams.rightMargin = dp(4);

            wrapper.addView(time, timeParams);
        }

        return wrapper;
    }

    // ---------------- SEND AND SAVE ----------------

    private void sendMessage() {
        if (activePerson == null) return;

        String body = messageInput.getText().toString().trim();

        if (body.isEmpty()) {
            Toast.makeText(
                    this,
                    "Write a message first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (remoteMode) {
            View sendButton = findViewById(R.id.send_message);
            sendButton.setEnabled(false);
            FirebaseSkillLoopRepository.sendMessage(activeConversationId, body, unused -> runOnUiThread(() -> {
                sendButton.setEnabled(true);
                messageInput.setText("");
                hideKeyboard();
                // Firestore's message listener and conversation listener update the UI.
            }), error -> runOnUiThread(() -> {
                sendButton.setEnabled(true);
                Toast.makeText(this, "Message failed to send: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }));
            return;
        }

        JSONArray messages = loadMessages(activePerson.id);

        try {
            JSONObject message = new JSONObject();

            message.put("body", body);
            message.put("mine", true);
            message.put("time", System.currentTimeMillis());

            messages.put(message);

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(
                            MESSAGES_KEY + activePerson.id,
                            messages.toString())
                    .apply();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Couldn't save message",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        messageInput.setText("");
        hideKeyboard();

        renderMessages();
        renderConversations();
    }

    private JSONArray loadMessages(String personId) {
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(MESSAGES_KEY + personId, "[]");

        try {
            return new JSONArray(saved);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private String getLastMessage(Person person) {
        if (remoteMode) {
            if (person.preview == null || person.preview.trim().isEmpty()) return "Start a conversation.";
            String uid = FirebaseSkillLoopRepository.currentUid();
            return uid != null && uid.equals(person.lastMessageSenderId) ? "You: " + person.preview : person.preview;
        }
        JSONArray messages = loadMessages(person.id);

        if (messages.length() == 0) {
            return person.preview;
        }

        JSONObject last =
                messages.optJSONObject(messages.length() - 1);

        if (last == null) return person.preview;

        String body = last.optString("body", person.preview);

        return last.optBoolean("mine", false)
                ? "You: " + body
                : body;
    }

    private String getLastMessageTime(Person person) {
        if (remoteMode) return person.lastMessageAtMillis > 0 ? formatMessageTime(person.lastMessageAtMillis) : "";
        JSONArray messages = loadMessages(person.id);

        if (messages.length() == 0) {
            return "";
        }

        JSONObject last =
                messages.optJSONObject(messages.length() - 1);

        if (last == null) return "";

        long timestamp = last.optLong("time", 0L);

        if (timestamp <= 0L) return "";

        return formatMessageTime(timestamp);
    }

    private String formatMessageTime(long timestamp) {
        SimpleDateFormat formatter =
                new SimpleDateFormat("HH:mm", Locale.getDefault());

        return formatter.format(new Date(timestamp));
    }


    @Override
    public void onBackPressed() {
        if (detailScreen != null && detailScreen.getVisibility() == View.VISIBLE) {
            showConversationList();
        } else {
            super.onBackPressed();
        }
    }

    // ---------------- NAVIGATION ----------------

    private void setupNavigation() {
        SkillLoopTabNavigation.setup(this, 2);
    }

    // ---------------- UI HELPERS ----------------

    private TextView text(
            String value, int size, int color, boolean bold) {

        TextView view = new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(
                    Typeface.create(
                            getResources().getFont(R.font.big_shoulders_extrabold),
                            Typeface.BOLD));
        }

        return view;
    }

    private GradientDrawable makeBackground(
            int color, int radius, int strokeColor) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }

        return drawable;
    }

    private void hideKeyboard() {
        View current = getCurrentFocus();

        if (current != null) {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);

            if (manager != null) {
                manager.hideSoftInputFromWindow(
                        current.getWindowToken(), 0);
            }

            current.clearFocus();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}
