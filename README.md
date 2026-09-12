# progress day

A minimalist Android home-screen widget inspired by the Age in Motion concept.

## Widget modes
- **Original counter:** live age in years to 9 decimal places. On wider 3x1/4x1 layouts it also shows years, months, days and hours.
- **Today in motion:** the same age counter plus a segmented 24-hour day progress indicator, percentage, and on wider layouts hours/minutes/seconds/milliseconds.

## Sizes
The widget is intentionally one row tall and horizontally resizable.
- **2x1:** compact
- **3x1:** detailed
- **4x1:** full-width detailed

## Build without Android Studio
Use the included GitHub Actions workflow:
1. Create a GitHub repository.
2. Upload the contents of this folder (not the outer ZIP folder).
3. Open **Actions** and select **Build APK**.
4. Run the workflow.
5. Download the `progress-day-debug-apk` artifact.
6. Extract it and install the APK on your Android phone.

The workflow installs JDK 17 and uses the Gradle wrapper.

## Notes
Android launchers can throttle widget refreshes. The app schedules a minute-level refresh, but the launcher/OS ultimately controls exact redraw timing.
