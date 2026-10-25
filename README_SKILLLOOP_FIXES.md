# SkillLoop update notes

This update fixes the welcome/returning-user flow, tab back-stack behaviour, random Discover ordering, filtering of pending/accepted people out of Discover, and tactile button feedback. It also adjusts Firestore conversation-list rules so a `whereArrayContains(participantIds, uid)` query can be authorized by the deployed rules.

## Apply the updated Firestore configuration

Open PowerShell in this project root (the folder containing `firebase.json`) and run:

```powershell
firebase deploy --only firestore:rules,firestore:indexes
```

This deploys the updated rules and the composite indexes used by incoming and outgoing request listeners. It does not deploy Storage rules.

## Test flow

1. Open the app. If signed in with a complete profile, the welcome screen should show the saved profile name and `CONTINUE LEARNING`.
2. Tap Continue Learning to open Discover.
3. With two complete Firebase accounts, send a request from A to B. A should no longer see B in Discover while the request is pending; B should see A in Connect.
4. Accept from B. Both accounts should see the conversation in Chats and be able to send messages.
5. Verify the profile photo URL appears in Discover, Connect, and the Chats list.

If Firestore still returns `PERMISSION_DENIED`, capture the exact Logcat error. Do not switch to open/public rules.
