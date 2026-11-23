# GetFit — Native Android Application

The native Android version of **GetFit** is built using modern Android engineering standards: Kotlin, Jetpack Compose, Material 3, Room Database, and MVVM architecture. It is 100% offline-first, requiring zero network permissions for its core fitness and nutrition capabilities.

---

## 🏗️ Architecture & Tech Stack

```
android/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/getfit/
│       │   │   ├── data/            # Room Database, DAOs, Entities, SharedPreferences, Repositories
│       │   │   │   ├── dao/         # DiaryDao, ExerciseDao, MealScheduleDao, TrashDao, BodyweightDao
│       │   │   │   ├── entity/      # Room database entities
│       │   │   │   └── repository/  # Single source of truth data access
│       │   │   ├── notification/    # Meal & water reminder alarms and channels
│       │   │   ├── ui/
│       │   │   │   ├── components/  # Reusable Compose widgets (cards, modals, charts)
│       │   │   │   ├── navigation/  # Navigation bar, routes, backstack management
│       │   │   │   ├── screens/     # Screen composables (Dashboard, Food, Workout, Profile, Analytics)
│       │   │   │   ├── theme/       # Color palette, Typography, Dark/Light theme system
│       │   │   │   ├── util/        # StreakManager, AvatarManager, ThemePreferences
│       │   │   │   └── viewmodel/   # StateFlow MVVM ViewModels
│       │   │   └── MainActivity.kt  # Single-Activity entry point
│       │   └── res/                 # Vector drawables, launcher icons, values
│       └── test/                    # Comprehensive unit tests (AppStabilityAndStreakTest.kt)
├── build.gradle.kts                 # Project-level build configuration
├── settings.gradle.kts              # Module dependencies (:app)
└── gradlew / gradlew.bat            # Gradle wrapper
```

### Core Libraries
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Language**: Kotlin 1.9.22 (Coroutines, StateFlow, Flow)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) 2.6.1 + KSP
- **Image Loading**: Coil Compose 2.5.0
- **Navigation**: Jetpack Compose Navigation 2.7.7

---

## 🛠️ Building & Running

### Requirements
- Android Studio Iguana (2023.2.1) or later
- JDK 17 (recommended: Android Studio bundled OpenJDK)
- Android SDK with Platform 34 installed

### Build via Command Line
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Clean Project
./gradlew clean
```

### Pre-built APK
If you just want to install and test GetFit without building, the pre-compiled APK is available in the [`/apk`](../apk/) folder.
