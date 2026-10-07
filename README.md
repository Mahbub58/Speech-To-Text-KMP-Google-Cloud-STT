# Speech To Text KMP (Google Cloud STT + TTS)

A Kotlin Multiplatform project targeting **Android** and **iOS**, built with Compose Multiplatform.

It provides two features:

- **Speech to Text** – streams microphone audio over a WebSocket to a Google Cloud Speech-to-Text relay (`wss://stt-ws.onrender.com/`) and shows partial/final transcripts live. No API key is needed on the client for this.
- **Text to Speech** – calls the [Google Cloud Text-to-Speech API](https://cloud.google.com/text-to-speech) directly from the app and plays the returned audio. **This feature requires your own Google Cloud API key.**

> [!IMPORTANT]
> Never put an API key in source code, commit it, or open a pull request containing one.
> Keys belong in `local.properties`, which is gitignored.

---

## Project structure

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that's common for all targets
    (speech recognition, the TTS client, and shared UI).
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, [androidMain](./composeApp/src/androidMain/kotlin) holds Android-only implementations
    (microphone recording, HTTP client) and [iosMain](./composeApp/src/iosMain/kotlin) holds their iOS equivalents.

* [/iosApp](./iosApp/iosApp) contains the iOS application entry point. Even if you're sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/CloudeSpeechToTextStreeming](./CloudeSpeechToTextStreeming) is a standalone Android-only sample of the
  streaming speech-to-text feature, if you want to see it without the multiplatform setup.

---

## 1. Prerequisites

- **JDK 11+**
- **Android Studio** (or IntelliJ IDEA) with the Kotlin Multiplatform plugin
- **Android SDK**
- **Xcode** (only for building/running the iOS target, macOS only)
- A **Google Cloud account** (only if you want the Text-to-Speech feature)

---

## 2. Get a Google Cloud API key

The Text-to-Speech feature needs a Google Cloud API key with the Cloud Text-to-Speech API enabled.

1. Open the [Google Cloud Console](https://console.cloud.google.com/) and create or select a project.
2. Enable the API:
   - Go to **APIs & Services → Library**
   - Search for **Cloud Text-to-Speech API**
   - Click **Enable**
3. Create an API key:
   - Go to **APIs & Services → Credentials**
   - Click **+ Create Credentials → API key**
   - Copy the generated key (it starts with `AIza`)
4. **Restrict the key** (strongly recommended, especially for a public repo):
   - Click the key you just created
   - Under **API restrictions**, select **Restrict key** and allow only
     **Cloud Text-to-Speech API**
   - Optionally also add an **Application restriction** (e.g. Android apps with your package name
     `com.mahbub.cloudspeechtotextkmp`, and/or iOS apps with your bundle ID)
   - Click **Save**
5. Enable billing: the Text-to-Speech API uses the free monthly quota first, then charges per character.
   See [pricing](https://cloud.google.com/text-to-speech/pricing).

---

## 3. Configure your API key

The key is read from `local.properties` at build time and injected into the code by Gradle.
`local.properties` is listed in [.gitignore](./.gitignore), so it is never committed.

1. Copy the template:

   ```shell
   cp local.properties.example local.properties
   ```

2. Open `local.properties` and set your values:

   ```properties
   sdk.dir=/path/to/Android/sdk
   GOOGLE_TTS_API_KEY=your-google-cloud-api-key-here
   ```

   If you already have a `local.properties` (Android Studio creates one with `sdk.dir`),
   just add the `GOOGLE_TTS_API_KEY=` line to it.

3. Done. Gradle generates `ApiKey.kt` at build time from this value — do not create that file yourself,
   and do not commit your real key.

Notes:

- If `GOOGLE_TTS_API_KEY` is missing or empty the project still **builds and runs**, but pressing
  **Play** on the Text-to-Speech screen fails with
  `Google TTS API key is not configured`.
- After changing the key, rebuild the app. The key is baked in at **build** time, not read at runtime.

---

## 4. Build and run the Android application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE's toolbar or build it directly from the terminal:

- on macOS/Linux

  ```shell
  ./gradlew :composeApp:assembleDebug
  ```

- on Windows

  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

Install the APK on a connected device or emulator:

```shell
./gradlew :composeApp:installDebug
```

## 5. Build and run the iOS application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE's toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

You can also link the shared framework manually:

```shell
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Google TTS API key is not configured` | Add `GOOGLE_TTS_API_KEY=...` to `local.properties` and rebuild. |
| `TTS request failed: HTTP 400/403` | The key is invalid, restricted, or the Cloud Text-to-Speech API is not enabled. Check the key restrictions in the Cloud Console. |
| `TTS request failed: HTTP 429` | You exceeded the free quota or billing is not enabled for the project. |
| Speech-to-Text never returns text | The app connects to `wss://stt-ws.onrender.com/`, a hosted relay. Make sure the device has internet access and the relay is running. |

---

## Security notes

- Keep `GOOGLE_TTS_API_KEY` only in `local.properties` (gitignored). Never hardcode it in `.kt`, `.xml`,
  `gradle.properties`, or any tracked file.
- Restrict the key in the Google Cloud Console (API restrictions + application restrictions) so it can only
  be used for the Text-to-Speech API from your app.
- If a key is ever committed or shared, **revoke it immediately** and create a new one — deleting it from the
  code does not remove it from git history.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
