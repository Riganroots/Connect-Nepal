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

Firebase-dependent functionality (group chat sync, interest sync, and the AI Local Guide) needs a Firebase project with an Android app registered for `com.connectnepal.app`. Download its `google-services.json` into `app/`. The build allows that file to be absent so CI can validate the project; without it these features stay local-only or show an "unavailable" message.

The AI Local Guide calls Gemini through [Firebase AI Logic](https://firebase.google.com/docs/ai-logic), so no Gemini API key is compiled into the app. Enable AI Logic (Gemini Developer API) in the Firebase console, and turn on App Check before a public release.

## Beta build settings

- Debug builds seed sample accounts you can log into with the demo button (password `password123`). Release builds keep the sample content but lock those accounts, hide the demo login, and hide the developer map/OAuth settings.
- Passwords are stored as salted PBKDF2 hashes.
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
