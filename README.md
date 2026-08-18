# Silent Call Recorder for Android

A modern, simple, and silent Android call recorder app written in **Kotlin** with **Jetpack Compose**, **Room Database**, and **Hilt Dependency Injection**.

Designed to operate seamlessly on **Android 12+ (API 31+)**.

---

## 🌟 Key Features

1. **Silent Background Recording**:
   - Automatically detects incoming and outgoing phone calls using an **Accessibility Service** and telephony state callbacks.
   - Records audio via `MediaRecorder` using `VOICE_COMMUNICATION` with fallback to `MIC`.
   - Runs a foreground service with a low-priority, silent notification channel to keep the recording service active without sound or vibrations.

2. **Metadata & Organization**:
   - Saves recordings as **MP3 files** in the public `/sdcard/Music/CallRecordings/` directory.
   - Persists caller/callee phone number, resolved contact name, timestamp, duration, call direction, and file size in a local **Room Database**.

3. **Built-in Playback**:
   - Clean Jetpack Compose UI with interactive playback bar, seekable progress slider, elapsed/total timers, and play/pause controls.

4. **Master Toggle & Persistence**:
   - Master switch to pause or resume recording at any time (stored in Jetpack DataStore).
   - Auto-start on boot support (`BootReceiver`) ensuring monitoring persists across device restarts.

5. **Material 3 Design**:
   - Dark mode & light mode support with Material You dynamic color theming.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Dependency Injection**: Dagger Hilt
- **Local Persistence**: Room Database (SQLite) + Jetpack DataStore Preferences
- **Audio Engine**: Android `MediaRecorder` (128 kbps AAC/MP3 stream) & `MediaPlayer`
- **Services**:
  - `CallDetectorAccessibilityService`: Monitors call state transitions and InCall UI events.
  - `CallRecorderService`: Foreground recording service with microphone permissions.
  - `BootReceiver`: Listens for `BOOT_COMPLETED` to initialize background monitoring.

---

## 📱 How to Run and Test

1. **Prerequisites**:
   - Android Studio Jellyfish / Koala or newer
   - JDK 17+
   - Physical Android device running Android 12 (API 31) or higher

2. **Installation & Setup**:
   - Open the project in Android Studio.
   - Build and install the APK on your device:
     ```bash
     ./gradlew assembleDebug
     ```
   - On first launch:
     1. Accept the **Legal Notice & Disclaimer**.
     2. Grant runtime permissions (**Microphone**, **Phone State**, **Call Logs**, **Contacts**).
     3. Grant **All Files Access** (`MANAGE_EXTERNAL_STORAGE`) to allow saving MP3s to your public storage.
     4. Enable the **Call Recorder Accessibility Service** in **Settings → Accessibility → Downloaded apps → Call Recorder**.

3. **Verify Recording**:
   - Make or receive a phone call.
   - The app will automatically begin recording silently in the background.
   - When the call ends, open the app to see the recording in the list, play it back, or manage your files.
