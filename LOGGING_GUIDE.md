# Logging Control Guide

## Quick Start - Disable All Logs

To disable all logging in the application:

1. Open `app/src/main/java/com/example/bms/utils/Logger.java`
2. Change line 14 from:
   ```java
   private static final boolean ENABLE_LOGGING = true;
   ```
   to:
   ```java
   private static final boolean ENABLE_LOGGING = false;
   ```
3. Rebuild the app

## Current Logging State

The application currently uses both:
- **Standard Android Log** (`android.util.Log`)
- **Custom Logger** (`com.example.bms.utils.Logger`) - Created in the recent fixes

## Option 1: Quick Disable (Using BuildConfig)

The easiest way to automatically disable logs in production builds is to use BuildConfig.

### Step 1: Modify Logger.java

Change this line in `Logger.java`:
```java
private static final boolean ENABLE_LOGGING = true;
```

To:
```java
private static final boolean ENABLE_LOGGING = BuildConfig.DEBUG;
```

Then add this import at the top:
```java
import com.example.bms.BuildConfig;
```

**Result:** Logs will be automatically disabled in release builds, enabled in debug builds.

---

## Option 2: Manual Control

Keep the current setup and manually change `ENABLE_LOGGING = false` when you want to disable logs.

### In Logger.java you can also control individual log levels:

```java
private static final boolean ENABLE_DEBUG = false;    // Disable debug logs only
private static final boolean ENABLE_INFO = true;      // Keep info logs
private static final boolean ENABLE_WARNING = true;   // Keep warnings
private static final boolean ENABLE_ERROR = true;     // Keep errors (recommended for production)
```

**Recommendation:** In production, keep ERROR logs enabled for troubleshooting.

---

## Option 3: Replace All Log Calls (Most Complete)

To completely centralize logging control, you need to replace all `Log.*` calls with `Logger.*` calls.

### Files that need updating:

**Recently Modified Files (from bug fixes):**
- ✅ `App.java` - Partially updated
- `TimeEntryRegister.java` - Uses Log.d, Log.e
- `InitFacePassHandler.java` - Uses Log.d, Log.e
- `LoginActivity.java` - Uses Log.e, Log.d
- `SplashScreen.java` - Uses Log.e, Log.d

**Other Core Files:**
- All files in `com.example.bms.time_entry.*`
- All files in `com.example.bms.ui.login.*`
- All files in `com.example.bms.enrollment.*`
- `FingerPrintScanActivity.java`
- Other activities and repositories

### Manual Find & Replace:

Use Android Studio's "Replace in Path" feature:

1. Press `Ctrl+Shift+R` (Windows/Linux) or `Cmd+Shift+R` (Mac)
2. Find: `Log\.d\(`
3. Replace: `Logger.d(`
4. Click "Replace All"

5. Repeat for:
   - `Log.e(` → `Logger.e(`
   - `Log.i(` → `Logger.i(`
   - `Log.w(` → `Logger.w(`
   - `Log.v(` → `Logger.v(`

6. Add import to each file:
   ```java
   import com.example.bms.utils.Logger;
   ```

7. You can remove the `android.util.Log` import if no longer used

---

## Option 4: ProGuard/R8 (Automatic for Release Builds)

Android's build system can automatically remove all Log statements in release builds.

### Step 1: Add to `app/proguard-rules.pro`:

```proguard
# Remove all logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep error logs for crash reporting (optional)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
```

### Step 2: Ensure ProGuard is enabled in `app/build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Note:** This only works for release builds with minification enabled.

---

## Recommended Approach

**For your situation, I recommend Option 1 (BuildConfig):**

1. It's the simplest
2. Automatically handles debug vs release
3. No need to manually replace thousands of Log calls
4. Can still use Logger for new code

### Implementation:

**File: `app/src/main/java/com/example/bms/utils/Logger.java`**

```java
package com.example.bms.utils;

import android.util.Log;
import com.example.bms.BuildConfig;

public class Logger {
    // Automatically disabled in release builds
    private static final boolean ENABLE_LOGGING = BuildConfig.DEBUG;

    // Keep errors enabled even in production (for crash reports)
    private static final boolean ENABLE_ERROR = true;

    public static void d(String tag, String message) {
        if (ENABLE_LOGGING) {
            Log.d(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (ENABLE_ERROR) {
            Log.e(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable throwable) {
        if (ENABLE_ERROR) {
            Log.e(tag, message, throwable);
        }
    }

    // ... rest of methods
}
```

Then gradually replace `Log.*` with `Logger.*` in new code.

---

## Testing

### To verify logs are disabled:

1. Set `ENABLE_LOGGING = false` in Logger.java
2. Build debug APK: `./gradlew assembleDebug`
3. Install and run
4. Open Logcat in Android Studio
5. Filter by package: `com.example.bms`
6. Perform actions - you should see no logs from Logger class

### To verify release build strips logs (Option 4):

1. Build release APK: `./gradlew assembleRelease`
2. Install release APK
3. Check Logcat - should see no log statements

---

## Summary

| Option | Effort | Control | Recommendation |
|--------|--------|---------|----------------|
| 1. BuildConfig | Low | Automatic | ✅ **Best for most cases** |
| 2. Manual Flag | Low | Manual | Good for testing |
| 3. Replace All | High | Complete | Best long-term |
| 4. ProGuard | Medium | Automatic | Only for release builds |

**Recommended:** Start with Option 1 (BuildConfig) now, gradually move to Option 3 (replace all) over time.

---

## Current Status

**Files Updated with Logger:**
- ✅ Logger.java created
- ✅ App.java partially updated (1 location)

**To Complete Migration:**
Run these commands in project root:

```bash
# Count how many Log statements exist
grep -r "Log\.[diwev]" app/src/main/java --include="*.java" | wc -l

# Find all files using Log
grep -r "import android.util.Log" app/src/main/java --include="*.java"
```

This will show you how many log statements need updating.
