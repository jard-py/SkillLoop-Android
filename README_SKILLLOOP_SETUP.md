# SkillLoop — Android Studio project

Native Android application in **Java + XML**. The existing project structure and activities are retained. The Stitch screens used as the visual reference are stored in `design_reference/stitch/`.

## Open and run

1. Extract the ZIP and open the folder containing `settings.gradle.kts` in Android Studio.
2. Use a recent Android Studio/JDK setup compatible with this project's Gradle wrapper and AGP versions, and install Android SDK Platform 36 / Build Tools as prompted.
3. Let Gradle sync complete. The first sync may need network access to download the wrapper distribution and Maven dependencies.
4. Without a `google-services.json`, the app intentionally remains in local demo mode. The demo mode is labelled and does not write sample students into Firebase or pretend local requests are real incoming requests.

## Connect your own Firebase project

No `google-services.json`, project ID, credentials, or service-account key has been fabricated or included.

1. In the [Firebase Console](https://console.firebase.google.com/), create or open a Firebase project.
2. Register an **Android app** with package name exactly `com.example.skillloop`.
3. Download the generated `google-services.json` and place it at `app/google-services.json` (not in the repository root). The Gradle build applies the Google Services plugin only when this real file exists.
4. Under **Authentication → Sign-in method**, enable **Email/Password**.
5. Create a **Cloud Firestore** database and a **Cloud Storage** bucket for this project. **Current Firebase guidance requires the pay-as-you-go Blaze plan to provision/use Cloud Storage buckets**; no-cost usage may still apply within quotas, but storage can generate charges. Review [Firebase's Storage billing requirements](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024) before enabling Storage.
6. Install the Firebase CLI if needed, sign in, select your Firebase project (for example with `firebase use --add`), and deploy the included rules/indexes:

   ```bash
   firebase deploy --only firestore:rules,firestore:indexes,storage
   ```

   Or publish the contents of `firestore.rules` and `storage.rules` in the corresponding Firebase Console rules editors, and add the composite index definitions from `firestore.indexes.json` if the console requests them.
7. Sync Gradle, run the app, create a first account, and complete profile onboarding. To exercise connection and messaging, create a second test account and complete its profile too.

Official docs: [Add Firebase to Android](https://firebase.google.com/docs/android/setup), [Email/password authentication](https://firebase.google.com/docs/auth/android/password-auth), [Firestore security rules and queries](https://firebase.google.com/docs/firestore/security/rules-query), and [Storage security rules](https://firebase.google.com/docs/storage/security).

## Firebase data model

- `profiles/{uid}` — discoverable student profile only: display name, course, year, domain, bio, teaching/learning skills, project, interests, photo URL, profile-completion state, and timestamps.
- `connectionRequests/{requestId}` — sender UID, recipient UID, introduction (1–300 characters), status, deterministic conversation ID, and timestamps. The incoming Connect query filters both `recipientId == current UID` and `status == pending`.
- `conversations/{conversationId}` — exactly two participant UIDs, accepted request ID, last-message preview/sender/time, and timestamps.
- `conversations/{conversationId}/messages/{messageId}` — sender UID, message text, timestamp, and introduction marker. The introduction message uses a deterministic document ID to avoid duplicate insertion on retries.
- `profilePhotos/{uid}/profile.jpg` in Storage — owner-scoped uploads, restricted by the included rules to image MIME types under 5 MiB.

Request state: pending requests do not create active conversations. Only the intended recipient can accept or decline. Acceptance atomically changes the request and creates/reuses the stable two-person conversation along with the original introduction message; decline does not create a conversation. Chats query conversations containing the signed-in user's UID. Firestore rules restrict profile writes to their owner, request access to sender/recipient, and conversation/message access to participants.

## Validation status and limitations

- XML well-formedness and static references to Android resources are checked as part of this handoff.
- The Android build could not be completed in the generation environment because Gradle attempted to download its wrapper distribution and the environment could not resolve `services.gradle.org`. Android SDK environment variables were also not configured here. This is an environment blocker, **not a claim that the APK built successfully**.
- Firebase Console setup was not available to this run. The Firestore and Storage rules have not been deployed or emulator-tested, and two-account authentication/request/chat tests have not been run. Treat the rules as a restrictive starting point and test them with the Firebase Emulator Suite or a non-production Firebase project before launch.
- Existing local profile/sample data is retained in the codebase and is not automatically imported into a new Firebase account. Firebase accounts use their own UID-scoped Firestore profile and decision preferences.
- The four primary activities share a common navigation-island component and a 250 ms gliding active-pill animation. The destinations remain separate activities; the pill is animated before navigating rather than implemented as one persistent Fragment-hosted view.
