package com.example.skillloop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Firebase data boundary for SkillLoop. The application deliberately keeps a local demo mode
 * when no Firebase app has been configured; local sample records are never uploaded as users.
 */
public final class FirebaseSkillLoopRepository {
    private static final String PROFILES = "profiles";
    private static final String REQUESTS = "connectionRequests";
    private static final String CONVERSATIONS = "conversations";
    private static final String MESSAGES = "messages";

    private FirebaseSkillLoopRepository() { }

    public static boolean isConfigured(@NonNull Context context) {
        return !FirebaseApp.getApps(context).isEmpty();
    }

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    public static String currentUid() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user == null ? null : user.getUid();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void loadProfile(String uid,
                                   OnSuccessListener<DocumentSnapshot> success,
                                   OnFailureListener failure) {
        db().collection(PROFILES).document(uid).get().addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    public static void saveProfile(@NonNull String uid,
                                   @NonNull Map<String, Object> fields,
                                   @NonNull OnSuccessListener<Void> success,
                                   @NonNull OnFailureListener failure) {
        Map<String, Object> profile = new HashMap<>(fields);
        profile.put("uid", uid);
        profile.put("updatedAt", FieldValue.serverTimestamp());
        profile.put("profileComplete", isProfileComplete(profile));
        DocumentReference ref = db().collection(PROFILES).document(uid);
        ref.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) profile.put("createdAt", FieldValue.serverTimestamp());
            ref.set(profile, SetOptions.merge()).addOnSuccessListener(success).addOnFailureListener(failure);
        }).addOnFailureListener(failure);
    }

    public static boolean isProfileComplete(@NonNull Map<String, Object> profile) {
        return !TextUtils.isEmpty(asString(profile.get("displayName")))
                && !TextUtils.isEmpty(asString(profile.get("course")))
                && !TextUtils.isEmpty(asString(profile.get("year")))
                && !TextUtils.isEmpty(asString(profile.get("domain")))
                && !TextUtils.isEmpty(asString(profile.get("skillsToTeach")))
                && !TextUtils.isEmpty(asString(profile.get("skillsToLearn")));
    }

    public static void uploadProfilePhoto(@NonNull String uid,
                                          @NonNull Uri localUri,
                                          @NonNull OnSuccessListener<Uri> success,
                                          @NonNull OnFailureListener failure) {
        final Context appContext;
        try {
            appContext = FirebaseApp.getInstance().getApplicationContext();
        } catch (Exception e) {
            failure.onFailure(e);
            return;
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Thread worker = new Thread(() -> {
            final File preparedPhoto;
            try {
                preparedPhoto = prepareProfilePhoto(appContext, localUri);
            } catch (Exception e) {
                mainHandler.post(() -> failure.onFailure(e));
                return;
            }

            StorageReference ref = FirebaseStorage.getInstance().getReference()
                    .child("profilePhotos").child(uid).child("profile.jpg");
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg").build();
            ref.putFile(Uri.fromFile(preparedPhoto), metadata)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e != null) throw e;
                            throw new IllegalStateException("Photo upload failed");
                        }
                        return ref.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        // The upload is complete; remove the temporary compressed local copy.
                        // The original selected photo is never deleted or modified.
                        //noinspection ResultOfMethodCallIgnored
                        preparedPhoto.delete();
                        if (task.isSuccessful()) {
                            success.onSuccess(task.getResult());
                        } else {
                            Exception error = task.getException();
                            failure.onFailure(error == null
                                    ? new IllegalStateException("Photo upload failed") : error);
                        }
                    });
        }, "skillloop-profile-photo-upload");
        worker.start();
    }

    /** Downsamples and re-encodes picked/captured images so the Storage 5 MiB limit is practical. */
    private static File prepareProfilePhoto(Context context, Uri source) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IOException("Unable to open the selected photo");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("The selected file is not a supported image");
        }

        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > 2200) {
            sample *= 2;
        }
        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IOException("Unable to read the selected photo");
            bitmap = BitmapFactory.decodeStream(input, null, decode);
        }
        if (bitmap == null) throw new IOException("The selected photo could not be decoded");

        Bitmap outputBitmap = bitmap;
        int longestSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longestSide > 1600) {
            float scale = 1600f / longestSide;
            outputBitmap = Bitmap.createScaledBitmap(bitmap,
                    Math.max(1, Math.round(bitmap.getWidth() * scale)),
                    Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
        }

        File output = File.createTempFile("skillloop_profile_", ".jpg", context.getCacheDir());
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!outputBitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)) {
                throw new IOException("Unable to prepare the selected photo");
            }
            stream.flush();
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            output.delete();
            throw e;
        } finally {
            if (outputBitmap != bitmap) outputBitmap.recycle();
            bitmap.recycle();
        }
        if (output.length() >= 5L * 1024L * 1024L) {
            // One more lower-quality encode is safer than a predictable Storage-rule rejection.
            Bitmap retryBitmap = BitmapFactory.decodeFile(output.getAbsolutePath());
            if (retryBitmap == null) {
                //noinspection ResultOfMethodCallIgnored
                output.delete();
                throw new IOException("Unable to compress the selected photo enough");
            }
            try (FileOutputStream stream = new FileOutputStream(output, false)) {
                if (!retryBitmap.compress(Bitmap.CompressFormat.JPEG, 65, stream)) {
                    throw new IOException("Unable to compress the selected photo enough");
                }
                stream.flush();
            } catch (IOException e) {
                //noinspection ResultOfMethodCallIgnored
                output.delete();
                throw e;
            } finally {
                retryBitmap.recycle();
            }
        }
        if (output.length() >= 5L * 1024L * 1024L) {
            //noinspection ResultOfMethodCallIgnored
            output.delete();
            throw new IOException("Please choose a smaller photo (under 5 MiB)");
        }
        return output;
    }

    /**
     * Live discover feed. Profiles are shuffled on refresh and people who already have a pending
     * request in either direction, or an accepted conversation with the signed-in user, are kept
     * out of Discover. Incoming requests belong in Connect, not in the suggestion deck.
     */
    public static ListenerRegistration listenDiscoverableProfiles(
            @NonNull Consumer<List<SkillLoopData.Student>> success,
            @NonNull Consumer<Exception> failure) {
        String uid = currentUid();
        if (TextUtils.isEmpty(uid)) {
            failure.accept(new IllegalStateException("Sign in to discover students."));
            return new CompositeListenerRegistration();
        }
        final class State {
            final List<SkillLoopData.Student> profiles = new ArrayList<>();
            final Set<String> incomingSenders = new HashSet<>();
            final Set<String> outgoingRecipients = new HashSet<>();
            final Set<String> connectedUsers = new HashSet<>();
            boolean profilesLoaded = false;
            void publish() {
                if (!profilesLoaded) return;
                List<SkillLoopData.Student> available = new ArrayList<>();
                for (SkillLoopData.Student student : profiles) {
                    if (student == null || uid.equals(student.id)) continue;
                    if (incomingSenders.contains(student.id)
                            || outgoingRecipients.contains(student.id)
                            || connectedUsers.contains(student.id)) continue;
                    available.add(student);
                }
                Collections.shuffle(available);
                success.accept(available);
            }
        }
        State state = new State();
        CompositeListenerRegistration composite = new CompositeListenerRegistration();
        composite.add(db().collection(PROFILES).whereEqualTo("profileComplete", true)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { failure.accept(error); return; }
                    state.profiles.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot d : snapshots.getDocuments()) {
                            SkillLoopData.Student student = toStudent(d);
                            if (student != null) state.profiles.add(student);
                        }
                    }
                    state.profilesLoaded = true;
                    state.publish();
                }));
        composite.add(db().collection(REQUESTS).whereEqualTo("recipientId", uid)
                .whereEqualTo("status", "pending").addSnapshotListener((snapshots, error) -> {
                    if (error != null) { failure.accept(error); return; }
                    state.incomingSenders.clear();
                    if (snapshots != null) for (DocumentSnapshot d : snapshots.getDocuments()) {
                        String sender = d.getString("senderId");
                        if (sender != null) state.incomingSenders.add(sender);
                    }
                    state.publish();
                }));
        composite.add(db().collection(REQUESTS).whereEqualTo("senderId", uid)
                .whereEqualTo("status", "pending").addSnapshotListener((snapshots, error) -> {
                    if (error != null) { failure.accept(error); return; }
                    state.outgoingRecipients.clear();
                    if (snapshots != null) for (DocumentSnapshot d : snapshots.getDocuments()) {
                        String recipient = d.getString("recipientId");
                        if (recipient != null) state.outgoingRecipients.add(recipient);
                    }
                    state.publish();
                }));
        composite.add(db().collection(CONVERSATIONS).whereArrayContains("participantIds", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { failure.accept(error); return; }
                    state.connectedUsers.clear();
                    if (snapshots != null) for (DocumentSnapshot d : snapshots.getDocuments()) {
                        List<String> participants = (List<String>) d.get("participantIds");
                        if (participants == null) continue;
                        for (String participant : participants) {
                            if (participant != null && !uid.equals(participant)) state.connectedUsers.add(participant);
                        }
                    }
                    state.publish();
                }));
        return composite;
    }

    private static final class CompositeListenerRegistration implements ListenerRegistration {
        private final List<ListenerRegistration> registrations = new ArrayList<>();
        void add(ListenerRegistration registration) { if (registration != null) registrations.add(registration); }
        @Override public void remove() {
            for (ListenerRegistration registration : registrations) registration.remove();
            registrations.clear();
        }
    }

    public static ListenerRegistration listenIncomingRequests(
            @NonNull Consumer<List<ConnectionRequest>> success,
            @NonNull Consumer<Exception> failure) {
        String uid = currentUid();
        if (TextUtils.isEmpty(uid)) throw new IllegalStateException("Sign in to view requests.");
        return db().collection(REQUESTS)
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { failure.accept(error); return; }
                    List<ConnectionRequest> results = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot d : snapshots.getDocuments()) {
                            ConnectionRequest r = toRequest(d);
                            if (r != null && uid.equals(r.recipientId) && "pending".equals(r.status)) results.add(r);
                        }
                    }
                    results.sort((a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));
                    success.accept(results);
                });
    }

    public static void loadStudent(String uid,
                                   Consumer<SkillLoopData.Student> success,
                                   Consumer<Exception> failure) {
        db().collection(PROFILES).document(uid).get().addOnSuccessListener(d -> success.accept(toStudent(d)))
                .addOnFailureListener(failure::accept);
    }

    public static void sendRequest(@NonNull String recipientUid,
                                   @NonNull String introduction,
                                   @NonNull OnSuccessListener<String> success,
                                   @NonNull OnFailureListener failure) {
        String senderUid = currentUid();
        if (TextUtils.isEmpty(senderUid)) { failure.onFailure(new IllegalStateException("Sign in to connect with students.")); return; }
        if (senderUid.equals(recipientUid)) { failure.onFailure(new IllegalArgumentException("You cannot connect with yourself.")); return; }
        String cleanMessage = introduction.trim();
        if (cleanMessage.isEmpty() || cleanMessage.length() > 300) {
            failure.onFailure(new IllegalArgumentException("Introductions must contain 1–300 characters.")); return;
        }
        String requestId = requestId(senderUid, recipientUid);
        DocumentReference req = db().collection(REQUESTS).document(requestId);
        DocumentReference convo = db().collection(CONVERSATIONS).document(conversationId(senderUid, recipientUid));
        db().runTransaction(transaction -> {
            DocumentSnapshot oldRequest = transaction.get(req);
            DocumentSnapshot existingConversation = transaction.get(convo);
            if (existingConversation.exists()) throw new IllegalStateException("You are already connected.");
            if (oldRequest.exists()) {
                String oldStatus = oldRequest.getString("status");
                if ("pending".equals(oldStatus)) {
                    throw new IllegalStateException("You have already sent a request to this student.");
                }
                if (!"declined".equals(oldStatus) && !"cancelled".equals(oldStatus)) {
                    throw new IllegalStateException("This student is already connected to you.");
                }
                transaction.update(req, "introductionMessage", cleanMessage,
                        "status", "pending", "updatedAt", FieldValue.serverTimestamp());
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("senderId", senderUid);
                data.put("recipientId", recipientUid);
                data.put("introductionMessage", cleanMessage);
                data.put("conversationId", convo.getId());
                data.put("status", "pending");
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(req, data);
            }
            return requestId;
        }).addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public static void acceptRequest(@NonNull String requestId,
                                     @NonNull OnSuccessListener<String> success,
                                     @NonNull OnFailureListener failure) {
        String uid = currentUid();
        if (TextUtils.isEmpty(uid)) { failure.onFailure(new IllegalStateException("Sign in to accept requests.")); return; }
        DocumentReference requestRef = db().collection(REQUESTS).document(requestId);
        db().runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(requestRef);
            if (!request.exists()) throw new IllegalStateException("This request no longer exists.");
            String recipient = request.getString("recipientId");
            String sender = request.getString("senderId");
            String status = request.getString("status");
            String intro = asString(request.get("introductionMessage"));
            if (!uid.equals(recipient)) throw new SecurityException("Only the recipient can accept this request.");
            if (!"pending".equals(status)) throw new IllegalStateException("This request has already been handled.");
            String convoId = conversationId(sender, recipient);
            DocumentReference convoRef = db().collection(CONVERSATIONS).document(convoId);
            DocumentSnapshot existingConvo = transaction.get(convoRef);
            boolean alreadyConnected = existingConvo.exists();
            if (!alreadyConnected) {
                Map<String, Object> convo = new HashMap<>();
                convo.put("participantIds", Arrays.asList(sender, recipient));
                convo.put("acceptedRequestId", requestId);
                convo.put("createdAt", FieldValue.serverTimestamp());
                convo.put("updatedAt", FieldValue.serverTimestamp());
                convo.put("lastMessage", intro);
                convo.put("lastMessageSenderId", sender);
                convo.put("lastMessageAt", FieldValue.serverTimestamp());
                transaction.set(convoRef, convo);
                if (!intro.trim().isEmpty()) {
                    DocumentReference introRef = convoRef.collection(MESSAGES).document("intro_" + requestId);
                    Map<String, Object> first = new HashMap<>();
                    first.put("senderId", sender);
                    first.put("text", intro);
                    first.put("createdAt", FieldValue.serverTimestamp());
                    first.put("isIntroduction", true);
                    transaction.set(introRef, first);
                }
            }
            transaction.update(requestRef, "status", alreadyConnected ? "cancelled" : "accepted",
                    "updatedAt", FieldValue.serverTimestamp());
            return convoId;
        }).addOnSuccessListener(conversationId -> {
            // If both students had sent a request, cancel the other pending direction. The
            // stable conversation ID makes repeated/near-simultaneous acceptance idempotent.
            String senderUid = currentUid();
            db().collection(REQUESTS).document(requestId).get().addOnSuccessListener(r -> {
                if (!r.exists()) { success.onSuccess(conversationId); return; }
                String from = r.getString("senderId");
                String to = r.getString("recipientId");
                if (from == null || to == null) { success.onSuccess(conversationId); return; }
                String inverseId = requestId(to, from);
                DocumentReference inverse = db().collection(REQUESTS).document(inverseId);
                db().runTransaction(tx -> {
                    DocumentSnapshot opposite = tx.get(inverse);
                    if (opposite.exists() && "pending".equals(opposite.getString("status"))
                            && senderUid != null && senderUid.equals(opposite.getString("senderId"))) {
                        tx.update(inverse, "status", "cancelled", "updatedAt", FieldValue.serverTimestamp());
                    }
                    return null;
                }).addOnCompleteListener(ignored -> success.onSuccess(conversationId));
            }).addOnFailureListener(e -> success.onSuccess(conversationId));
        }).addOnFailureListener(failure);
    }

    public static void declineRequest(@NonNull String requestId,
                                     @NonNull OnSuccessListener<Void> success,
                                     @NonNull OnFailureListener failure) {
        String uid = currentUid();
        if (TextUtils.isEmpty(uid)) { failure.onFailure(new IllegalStateException("Sign in to decline requests.")); return; }
        DocumentReference ref = db().collection(REQUESTS).document(requestId);
        db().<Void>runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(ref);
            if (!request.exists()) throw new IllegalStateException("This request no longer exists.");
            if (!uid.equals(request.getString("recipientId"))) throw new SecurityException("Only the recipient can decline this request.");
            if (!"pending".equals(request.getString("status"))) throw new IllegalStateException("This request has already been handled.");
            transaction.update(ref, "status", "declined", "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public static void cancelRequest(@NonNull String recipientUid,
                                     @NonNull OnSuccessListener<Void> success,
                                     @NonNull OnFailureListener failure) {
        String uid = currentUid();
        if (TextUtils.isEmpty(uid)) {
            failure.onFailure(new IllegalStateException("Sign in to cancel this request."));
            return;
        }
        DocumentReference ref = db().collection(REQUESTS).document(requestId(uid, recipientUid));
        db().<Void>runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(ref);
            if (!request.exists()) throw new IllegalStateException("This request no longer exists.");
            if (!uid.equals(request.getString("senderId"))) {
                throw new SecurityException("Only the sender can cancel this request.");
            }
            if (!"pending".equals(request.getString("status"))) {
                throw new IllegalStateException("Only pending requests can be cancelled.");
            }
            transaction.update(ref, "status", "cancelled", "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public static ListenerRegistration listenConversations(
            @NonNull Consumer<List<Conversation>> success,
            @NonNull Consumer<Exception> failure) {
        String uid = currentUid();
        if (TextUtils.isEmpty(uid)) throw new IllegalStateException("Sign in to view chats.");
        return db().collection(CONVERSATIONS).whereArrayContains("participantIds", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { failure.accept(error); return; }
                    List<Conversation> results = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot d : snapshots.getDocuments()) {
                            List<String> participants = (List<String>) d.get("participantIds");
                            if (participants == null || participants.size() != 2 || !participants.contains(uid)) continue;
                            String other = participants.get(0).equals(uid) ? participants.get(1) : participants.get(0);
                            Timestamp ts = d.getTimestamp("lastMessageAt");
                            results.add(new Conversation(d.getId(), other,
                                    asString(d.get("lastMessage")),
                                    asString(d.get("lastMessageSenderId")),
                                    ts == null ? 0L : ts.toDate().getTime()));
                        }
                    }
                    results.sort((a, b) -> Long.compare(b.lastMessageAtMillis, a.lastMessageAtMillis));
                    success.accept(results);
                });
    }

    public static ListenerRegistration listenMessages(
            @NonNull String conversationId,
            @NonNull Consumer<List<ChatMessage>> success,
            @NonNull Consumer<Exception> failure) {
        return db().collection(CONVERSATIONS).document(conversationId).collection(MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { failure.accept(error); return; }
                    List<ChatMessage> results = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot d : snapshots.getDocuments()) {
                            Timestamp ts = d.getTimestamp("createdAt");
                            results.add(new ChatMessage(d.getId(), asString(d.get("senderId")),
                                    asString(d.get("text")), ts == null ? 0L : ts.toDate().getTime(),
                                    Boolean.TRUE.equals(d.getBoolean("isIntroduction"))));
                        }
                    }
                    success.accept(results);
                });
    }

    public static void sendMessage(@NonNull String conversationId,
                                   @NonNull String body,
                                   @NonNull OnSuccessListener<Void> success,
                                   @NonNull OnFailureListener failure) {
        String uid = currentUid();
        String clean = body.trim();
        if (TextUtils.isEmpty(uid)) { failure.onFailure(new IllegalStateException("Sign in to send messages.")); return; }
        if (clean.isEmpty() || clean.length() > 2000) { failure.onFailure(new IllegalArgumentException("Messages must contain 1–2000 characters.")); return; }
        DocumentReference convo = db().collection(CONVERSATIONS).document(conversationId);
        DocumentReference message = convo.collection(MESSAGES).document();
        db().<Void>runTransaction(tx -> {
            DocumentSnapshot conversation = tx.get(convo);
            if (!conversation.exists()) throw new IllegalStateException("Conversation not found.");
            List<String> participants = (List<String>) conversation.get("participantIds");
            if (participants == null || !participants.contains(uid) || participants.size() != 2) {
                throw new SecurityException("You are not a participant in this conversation.");
            }
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("senderId", uid);
            messageData.put("text", clean);
            messageData.put("createdAt", FieldValue.serverTimestamp());
            messageData.put("isIntroduction", false);
            tx.set(message, messageData);
            tx.update(convo, "lastMessage", clean, "lastMessageSenderId", uid,
                    "lastMessageAt", FieldValue.serverTimestamp(), "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public static String conversationId(String a, String b) {
        if (a == null || b == null) throw new IllegalArgumentException("Missing participant UID");
        String first = a.compareTo(b) <= 0 ? a : b;
        String second = a.compareTo(b) <= 0 ? b : a;
        return "c_" + first + "__" + second;
    }

    public static String requestId(String sender, String recipient) {
        if (sender == null || recipient == null) throw new IllegalArgumentException("Missing participant UID");
        return "r_" + sender + "__" + recipient;
    }

    public static SkillLoopData.Student toStudent(DocumentSnapshot d) {
        if (d == null || !d.exists()) return null;
        String name = asString(d.get("displayName"));
        if (name.isEmpty()) return null;
        return new SkillLoopData.Student(d.getId(), name,
                fallback(asString(d.get("course")), "Course not added"),
                fallback(asString(d.get("year")), "Year not added"),
                asString(d.get("skillsToTeach")), asString(d.get("skillsToLearn")),
                fallback(asString(d.get("domain")), "Other"), asString(d.get("bio")),
                asString(d.get("currentProject")), joinList(d.get("interests")), asString(d.get("photoUrl")));
    }

    public static ConnectionRequest toRequest(DocumentSnapshot d) {
        if (d == null || !d.exists()) return null;
        Timestamp ts = d.getTimestamp("createdAt");
        return new ConnectionRequest(d.getId(), asString(d.get("senderId")),
                asString(d.get("recipientId")), asString(d.get("introductionMessage")),
                asString(d.get("status")), ts == null ? 0L : ts.toDate().getTime());
    }

    private static String joinList(Object value) {
        if (value instanceof List<?>) {
            List<String> items = new ArrayList<>();
            for (Object item : (List<?>) value) if (item != null) items.add(String.valueOf(item));
            return TextUtils.join(", ", items);
        }
        return asString(value);
    }

    private static String asString(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String fallback(String value, String fallback) { return value.isEmpty() ? fallback : value; }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException("Unable to make stable Firebase ID", e); }
    }

    public static final class ConnectionRequest {
        public final String id, senderId, recipientId, introductionMessage, status;
        public final long createdAtMillis;
        ConnectionRequest(String id, String senderId, String recipientId, String introductionMessage, String status, long createdAtMillis) {
            this.id = id; this.senderId = senderId; this.recipientId = recipientId;
            this.introductionMessage = introductionMessage; this.status = status; this.createdAtMillis = createdAtMillis;
        }
    }

    public static final class Conversation {
        public final String id, otherUid, lastMessage, lastMessageSenderId;
        public final long lastMessageAtMillis;
        Conversation(String id, String otherUid, String lastMessage, String lastMessageSenderId, long lastMessageAtMillis) {
            this.id = id; this.otherUid = otherUid; this.lastMessage = lastMessage;
            this.lastMessageSenderId = lastMessageSenderId; this.lastMessageAtMillis = lastMessageAtMillis;
        }
    }

    public static final class ChatMessage {
        public final String id, senderId, text;
        public final long createdAtMillis;
        public final boolean isIntroduction;
        ChatMessage(String id, String senderId, String text, long createdAtMillis, boolean isIntroduction) {
            this.id = id; this.senderId = senderId; this.text = text;
            this.createdAtMillis = createdAtMillis; this.isIntroduction = isIntroduction;
        }
    }
}
