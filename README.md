# Connect Nepal

Connect Nepal is a native Android activity-based social app for discovering things to do in Nepal and meeting people to do them with. It was originally prototyped with Google AI Studio.

- Application ID: `com.connectnepal.app`
- Current version: `0.1.0-beta` (versionCode 1)

AI Studio project: https://ai.studio/apps/a3244f92-3601-4e08-a39b-c826a0135fc1

## Development stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room
- Firebase dependencies for AI, Firestore, and App Check
- Retrofit / OkHttp / Moshi
- Coil
- StateFlow / coroutines

## Local setup

### Requirements

- Android Studio compatible with Android Gradle Plugin 9.1.1
- JDK 17
- Android SDK Platform 36.1
- Android SDK Build-Tools 36.0.0

### Run

1. Clone this repository.
2. Open it in Android Studio.
3. Select JDK 17 for Gradle.
4. Install Android SDK Platform 36.1 if prompted.
5. Sync Gradle.
6. Run the `app` configuration on an emulator or Android device.

The repository includes the Gradle 9.3.1 wrapper. A custom debug keystore is not required; Android's normal debug signing is used.

## Firebase

Do not place private server credentials, service-account keys, signing keys, or production secrets in the repository or Android APK.

The app has two modes, chosen automatically at startup:

- **Online (Firebase configured):** accounts use Firebase Auth, and profiles, activities, participants and group chats are shared through Firestore. Room is used as the local cache. No sample content is seeded.
- **Offline demo (no `google-services.json`):** everything is stored on the device and sample people and activities are seeded. CI and the unit tests run in this mode.

### Firebase setup

1. In the Firebase console, register an Android app with package name `com.connectnepal.app` and put its `google-services.json` in `app/`.
2. **Authentication → Sign-in method:** enable **Email/Password**.
3. **Firestore Database:** create the database, then deploy the security rules from this repo:
   ```sh
   npm install -g firebase-tools
   firebase login
   firebase use --add            # pick your project
   firebase deploy --only firestore:rules
   ```
4. **AI Logic:** enable the Gemini Developer API for the AI Local Guide. It calls Gemini through [Firebase AI Logic](https://firebase.google.com/docs/ai-logic), so no Gemini API key is compiled into the app.
5. Before a public release, turn on App Check.

Firestore layout (enforced by `firestore.rules`):

| Path | Contents | Who can write |
| --- | --- | --- |
| `users/{uid}` | public profile and interests | that user |
| `activities/{id}` | activity details, `organizerId`, `participants` map | organizer; others may only add/remove themselves |
| `activities/{id}/messages/{id}` | group chat | participants only (also the only readers) |
| `reports/{id}` | abuse reports | any signed-in user (create only); review them in the console |

## Beta build settings

- Debug builds without Firebase seed sample accounts you can log into with the demo button (password `password123`). Release builds hide the demo login and the developer map/OAuth settings.
- Settings has **Delete Account**, which removes the user's profile, the activities they organise, and their place in activities they joined. Google Play requires in-app account deletion for apps with sign-up.
- Offline-mode passwords are stored as salted PBKDF2 hashes; online mode uses Firebase Auth.
- Set `connect.feedbackEmail` in `gradle.properties` (or pass `-Pconnect.feedbackEmail=...`) to show a "Send Beta Feedback" button in Settings.
- Bump `versionCode` in `app/build.gradle.kts` for every upload to Google Play.

## Release signing

Release signing is configured only when all required environment variables are present:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- optional `KEY_ALIAS` (defaults to `upload`)

Keystore/private signing files must not be committed.

## CI

GitHub Actions validates the project with JDK 17, Gradle 9.3.1, Android SDK 36.1, JVM tests, a debug APK build, and an (unsigned) release App Bundle build. Both are uploaded as workflow artifacts.
