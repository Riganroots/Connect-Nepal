# Connect

Connect is a native Android activity-based social app originally prototyped with Google AI Studio.

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

## Environment and Firebase

`.env` is ignored by Git. `.env.example` contains placeholders only.

Do not place private server credentials, service-account keys, signing keys, or production secrets in the repository or Android APK.

Firebase-dependent functionality needs the appropriate Firebase project/app configuration when it is enabled. The build currently allows the Google Services configuration file to be absent so baseline CI can validate the Android project.

## Release signing

Release signing is configured only when all required environment variables are present:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- optional `KEY_ALIAS` (defaults to `upload`)

Keystore/private signing files must not be committed.

## CI

GitHub Actions validates the project with JDK 17, Gradle 9.3.1, Android SDK 36.1, JVM tests, and a debug APK build.
