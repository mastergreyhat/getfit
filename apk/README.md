# GetFit — Android APK Release

This directory contains the ready-to-install Android Application Package (`.apk`) for **GetFit**, allowing you to test, review, and sideload the application immediately on any physical Android device or emulator without needing to build from source.

---

## 📦 Package Details

| Attribute | Specification |
|:---|:---|
| **File Name** | `GetFit.apk` |
| **Application ID** | `com.getfit` |
| **Version Name** | `1.0.0` |
| **Version Code** | `1` |
| **Min Android Version** | Android 8.0 Oreo (API Level 26) |
| **Target Android Version** | Android 14 UpsideDownCake (API Level 34) |
| **Architecture** | Universal (`arm64-v8a`, `armeabi-v7a`, `x86_64`) |
| **Build Type** | Debug (Optimized with ProGuard/R8 rules preserved) |
| **Permissions** | `POST_NOTIFICATIONS` (Meal & Consistency reminders) |

---

## 📲 How to Install on Any Android Device

1. **Transfer the APK to your phone:**
   - Download `GetFit.apk` directly to your phone via USB, Google Drive, WhatsApp, Telegram, or email.
2. **Enable Unknown App Installation:**
   - When you tap the downloaded `GetFit.apk`, Android may ask: *"For your security, your phone is not allowed to install unknown apps from this source."*
   - Tap **Settings** and toggle **"Allow from this source"**.
3. **Install & Launch:**
   - Tap **Install**.
   - Once installation completes, tap **Open** to experience GetFit!

---

## 🛠️ Building from Source

If you prefer to compile a fresh APK from source code:

```bash
# Navigate to the native Android directory
cd ../android

# Build debug APK
./gradlew assembleDebug

# Output APK will be generated at:
# android/app/build/outputs/apk/debug/app-debug.apk
```
