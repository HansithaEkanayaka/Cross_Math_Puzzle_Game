# Cross Math Puzzle Game

An Android puzzle game built with Kotlin and Jetpack Compose, where players fill in a math-themed crossword-style grid using numbers and operators to satisfy arithmetic equations.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (ViewModel + State)
- **Build System:** Gradle (Kotlin DSL)
- **Min SDK:** 24
- **Target/Compile SDK:** 36

## Project Structure

```
20231946_cw/
├── app/
│   └── src/main/java/com/example/a20231946_cw/
│       ├── MainActivity.kt        # App entry point
│       ├── PuzzleGenerator.kt     # Logic for generating puzzles
│       ├── GameScreen.kt          # Compose UI for the game
│       └── viewmodel/
│           └── GameViewModel.kt   # Game state management
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## How to Run in Android Studio

1. **Install Android Studio**
   Download the latest version from [developer.android.com/studio](https://developer.android.com/studio) and install it.

2. **Open the Project**
   - Launch Android Studio.
   - Click **Open** (not "New Project").
   - Navigate to and select the `20231946_cw` folder (the folder containing `build.gradle.kts` and `settings.gradle.kts` — this is the actual Gradle project root, not the outer `Cross_Math_Puzzle_Game` folder).

3. **Let Gradle Sync**
   - Android Studio will automatically start syncing the project and downloading dependencies.
   - This can take a few minutes on the first run (requires internet access).
   - If prompted to update Gradle/AGP, accept the recommended versions or keep as-is — the project is configured for AGP 8.13.2 and Kotlin 2.0.21.

4. **Set Up a Device to Run On**
   - **Option A — Emulator:**
     - Go to **Tools > Device Manager**.
     - Click **Create Device**, pick a phone (e.g., Pixel 6), select a system image with API level 24 or higher, and finish setup.
   - **Option B — Physical Device:**
     - Enable **Developer Options** and **USB Debugging** on your Android phone (Settings > About Phone > tap "Build Number" 7 times, then Settings > Developer Options).
     - Connect via USB and allow the debugging prompt.

5. **Run the App**
   - Select your device/emulator from the device dropdown in the toolbar.
   - Click the green **Run ▶** button (or press `Shift + F10`).
   - The app will build, install, and launch automatically.

## Troubleshooting

- **Gradle sync fails:** Check your internet connection (dependencies are downloaded from Google's and Maven Central repositories) and ensure you have JDK 11 or higher configured (File > Settings > Build Tools > Gradle).
- **"SDK location not found":** Go to **File > Project Structure > SDK Location** and set the path to your installed Android SDK.
- **Outdated AGP/Kotlin warnings:** Safe to accept Android Studio's suggested upgrades.

## App Details

- **Package name:** `com.example.a20231946_cw`
- **Application ID:** `com.example.mycw`
- **Main Activity:** `MainActivity.kt`
