# Music Player

A customized, modern, high-performance offline music player for Android.

---

## Project Context & Rationale

**Music Player** is a personal project developed by **HrshD1eux** designed to serve specific daily-driver requirements and workflow enhancements that do not fit within the upstream project's design philosophy or contributing guidelines.

Upstream [Auxio](https://github.com/OxygenCobalt/Auxio) is intentionally built around strict minimalism and a tightly defined feature set. Features such as kernel-level recursive filesystem monitoring (`inotify`), instant pull-to-refresh gestures, custom 3x2 Material 3 card widgets, specialized UI accents, and proactive background storage synchronization lie outside upstream's scope. Rather than proposing changes that diverge from Auxio's design goals, this personalized fork was created to implement these features independently while maintaining complete compatibility with modern Android versions (Android 14 through Android 17).

---

## Attribution & Lineage

> **Music Player** is a personalized fork and extension of **[Auxio](https://github.com/OxygenCobalt/Auxio)**, originally created by **Alexander Capehart (OxygenCobalt)**. This project contains substantial modifications and original contributions by **HrshD1eux**. The applicable portions of this project remain licensed under the **GNU General Public License v3 or later**.

* **Original Codebase & Architecture**: Copyright &copy; 2021–2024 Alexander Capehart (OxygenCobalt) and Auxio Contributors.
* **Modifications, Enhancements & Additions**: Copyright &copy; 2026 HrshD1eux.

---

## Key Modifications & Enhancements

This project introduces several enhancements and stability fixes over upstream:

### 1. Modern 3x2 Material 3 Card Widget
* **Container Design**: Built using a Material 3 rounded card container (`@drawable/ui_widget_bg_round`) with system dynamic color tinting (`?attr/colorSurface` / `?attr/colorSurfaceContainer`).
* **Cover Artwork**: Centered, rounded-corner album cover preview.
* **Metadata Hierarchy**: Prominent bold song title and secondary artist subtitle.
* **Streamlined Control Bar**: Responsive control row featuring **Previous**, a circular filled FAB-style **Play/Pause** toggle button, and **Next**.

### 2. Real-Time Kernel-Level Storage Synchronization
* **Recursive inotify File Observer (`DirectoryFileObserver`)**:
  * Monitors music directories directly at the Linux kernel level across all storage volumes (Internal Storage, MicroSD, OTG).
  * Immediately detects `CLOSE_WRITE`, `CREATE`, `MOVED_TO`, and `DELETE` events without waiting for periodic system scans.
  * 500ms debouncing window to gracefully handle multi-file batch transfers and album extractions.
  * Automatically filters hidden directories and `.nomedia` folders.
* **Instant MediaScanner Ingestion (`MediaScanner`)**:
  * Directly triggers `MediaScannerConnection.scanFile()` upon detected filesystem changes, bypassing the typical 1–2 minute delay from Android's background media provider.
* **Pull-to-Refresh Gesture**:
  * Added `SwipeRefreshLayout` integration across all library tabs (Songs, Albums, Artists, Genres, Playlists) with dynamic theme palette matching.

### 3. Stability, Memory Leak & Power Optimizations
* **Native JNI Memory Leak Fix**:
  * Fixed unreleased `GetStringUTFChars` calls in `JInputStream.cpp` during TagLib scanning, ensuring all native heap memory is safely reclaimed.
* **ViewBinding Lifecycle Nulling**:
  * Enforced strict nulling of `_binding` references during `onDestroyView()` across all dialog and fragment base classes.
* **Paused CPU Drain Elimination**:
  * Added active-playback state guards to `PlaybackViewModel` ticker coroutines, allowing the CPU to enter deep sleep immediately when playback is paused.
* **Atomic Room Database Transactions**:
  * Wrapped queue and playback state saving operations in atomic Room `@Transaction` methods (`replaceState`, `replaceQueue`) with WAL journaling to prevent disk churn and state corruption.
* **Android 14/15/16/17 Foreground Service Hardening**:
  * Configured `FOREGROUND_SERVICE_DATA_SYNC` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` service types in `AndroidManifest.xml` to prevent `ForegroundServiceTypeNotAllowedException` during background library indexing.

### 4. Hardware & Modern Platform Readiness
* **16 KB Page Size Alignment**:
  * Built with `-Wl,-z,max-page-size=16384` using NDK r27/r28, ensuring out-of-the-box compatibility with 16 KB memory page kernels on Google Tensor devices (Pixel 8, 9, 10 series) and Android 15+.
* **Windows Build Environment Compatibility**:
  * Added cross-platform shell script detection and fixed Groovy buildscript syntax to enable native C++ builds on Windows environments without WSL.

---

## Privacy & Network Policy

* **100% Offline**: The app does **not** request or use `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE`.
* **Zero Telemetry**: No analytics, tracking SDKs, remote error reporters, or network calls of any kind exist in this application.

---

## Building from Source

### Prerequisites
* Android Studio (Ladybug / Iguana or later)
* Android SDK (API 36, Build Tools 36.0.0, NDK 27+)
* Java Development Kit (JDK 21)
* CMake & Ninja

### Build Instructions
```bash
# Clone the repository recursively with submodules
git clone --recurse-submodules https://github.com/HrshD1eux/Music_Player.git

cd Music_Player

# If submodules were not cloned initially:
git submodule update --init --recursive

# Build debug APK
./gradlew assembleDebug

# Install on a connected Android device
./gradlew installDebug
```

---

## License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License** as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but **WITHOUT ANY WARRANTY**; without even the implied warranty of **MERCHANTABILITY** or **FITNESS FOR A PARTICULAR PURPOSE**. See the [GNU General Public License](https://www.gnu.org/licenses/gpl-3.0.en.html) for more details.

A full copy of the GNU General Public License is available in the [`LICENSE`](LICENSE) file.

* Original Auxio code &copy; 2021–2024 Alexander Capehart (OxygenCobalt) and contributors.
* Modifications and additions &copy; 2026 HrshD1eux.
