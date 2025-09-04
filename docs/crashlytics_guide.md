# Comprehensive Firebase Crashlytics Implementation Guide

This guide provides a complete, copy-paste-ready solution for implementing Firebase Crashlytics to capture crashes and non-fatal errors across **all Activities, functions, and UI buttons** in your Android application.

---

# Why Crashlytics is Sufficient

*   Crashlytics automatically captures most uncaught crashes and displays their stack traces in the Firebase Console. This allows you to identify the cause of a crash even without a physical device or emulator.
*   In addition to fatal crashes, you can log *non-fatal* exceptions (`recordException`), custom logs, and custom keys to gather context about the execution flow leading up to an error. This enables you to track issues within functions or button clicks, even if they don't cause a complete application failure.

---

# 1) Prerequisites (Summary)

1.  Create a Firebase project and add your Android app → Download `google-services.json` and place it in the `app/` directory.
2.  Add the Crashlytics Gradle plugin and SDK (e.g., plugin `com.google.firebase:firebase-crashlytics-gradle` and the library `com.google.firebase:firebase-crashlytics`). Always refer to the official Firebase documentation for the latest versions.

---

# 2) `build.gradle` Examples (Ready to Use)

**Project-level `build.gradle` (Groovy):**

```groovy
buildscript {
    repositories { google(); mavenCentral() }
    dependencies {
        // Use the version corresponding to your Android Gradle Plugin
        classpath 'com.android.tools.build:gradle:8.1.0'
        // Google services plugin
        classpath 'com.google.gms:google-services:4.3.15'
        // Crashlytics plugin (check release notes for the latest version)
        classpath 'com.google.firebase:firebase-crashlytics-gradle:3.0.6'
    }
}
```

**App-level `build.gradle` (Groovy)** — Place this inside your `app` module:

```groovy
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
    id 'com.google.firebase.crashlytics'
}

android {
    // ... sdk, defaultConfig, buildTypes ...
    buildTypes {
        release {
            // Ensure mapping file upload is enabled for deobfuscation
            firebaseCrashlytics {
                mappingFileUploadEnabled true
            }
            // If you use ProGuard/R8
            // minifyEnabled true
        }
    }
}

dependencies {
    // Option: Use the Firebase BOM to manage library versions
    implementation platform('com.google.firebase:firebase-bom:32.0.0') // Choose the latest BoM version
    implementation 'com.google.firebase:firebase-crashlytics'
    // Recommended for better breadcrumb logging
    implementation 'com.google.firebase:firebase-analytics'
}
```

> Always check the official Crashlytics documentation for the most up-to-date versions of the BoM and plugin.

---

# 3) The `Application` Class — The Central Hub (Paste this entire code)

This code initializes Crashlytics, sets a `DefaultUncaughtExceptionHandler`, registers `ActivityLifecycleCallbacks` to log the current screen as a custom key, and prepares a simple integration with Timber (if you use it). It also provides helper functions to log errors from any function or button.

```kotlin
// MyApp.kt
package com.example.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Optional: Control report collection (true = enable)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // 1) Activity lifecycle: Set the current Activity name as a custom key (helps in filtering crashes)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                FirebaseCrashlytics.getInstance().setCustomKey("current_activity",
                    activity.javaClass.simpleName)
            }

            override fun onActivityPaused(activity: Activity) {
                // The value can be cleared or left as is
            }

            // We leave the rest of the callbacks empty:
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })

        // 2) Default UncaughtExceptionHandler for all threads
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Add some context before sending
            FirebaseCrashlytics.getInstance().log("UncaughtException on thread: ${thread.name}")
            // Log the exception (Crashlytics captures uncaught crashes automatically,
            // but logging it ensures a non-fatal copy exists if needed)
            FirebaseCrashlytics.getInstance().recordException(throwable)
            // Pass to the default handler (which closes the app as expected)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 3) (If you use Kotlin Coroutines) A general handler that can be attached to important Scopes
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            FirebaseCrashlytics.getInstance().log("CoroutineExceptionHandler fired")
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
        // Use this handler when creating important Scopes:
        // CoroutineScope(Dispatchers.Default + coroutineExceptionHandler)

        // 4) (Optional) Redirect Timber to Crashlytics — useful for collecting breadcrumbs/logs
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    FirebaseCrashlytics.getInstance().log("${priority}/${tag ?: "NoTag"}: $message")
                    t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                }
            })
        }
    }
}
```

> This setup ensures that the name of the active Activity will appear as a `customKey` in your crash reports, making it easier to identify which screen was active during a crash.

---

# 4) General Helper for Logging Errors in Functions and Buttons

Place this code in a utility file. It simplifies wrapping any function or button click to automatically log exceptions.

```kotlin
// CrashlyticsUtils.kt
package com.example.app.util

import android.os.SystemClock
import android.view.View
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashlyticsUtils {
    fun log(msg: String) = FirebaseCrashlytics.getInstance().log(msg)

    fun setKey(key: String, value: Any) = when (value) {
        is String -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Int -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Long -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Boolean -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Float -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        else -> FirebaseCrashlytics.getInstance().setCustomKey(key, value.toString())
    }

    fun record(e: Throwable) = FirebaseCrashlytics.getInstance().recordException(e)

    /**
     * Runs a block safely. If an Exception occurs, it will be recorded and then re-thrown.
     * (You can set rethrow to false to keep it non-fatal).
     */
    inline fun safeRun(rethrow: Boolean = true, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            record(t)
            if (rethrow) throw t
        }
    }
}

/**
 * Extension for Views — replace setOnClickListener with setSafeOnClickListener everywhere.
 * Includes debouncing to prevent rapid clicks.
 */
fun View.setSafeOnClickListener(debounceMs: Long = 500L, rethrow: Boolean = true, block: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { v ->
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < debounceMs) return@setOnClickListener
        lastClickTime = now
        try {
            block(v)
        } catch (t: Throwable) {
            CrashlyticsUtils.record(t)
            if (rethrow) throw t
        }
    }
}
```

**How to quickly apply this to all buttons?**

*   Use your IDE's "Replace in Path" feature (Refactor → Replace in Path) to replace all instances of `setOnClickListener { ... }` with `setSafeOnClickListener { ... }`.
*   Alternatively, start by applying it incrementally to your most critical screens.

> Logging exceptions with `recordException` sends them to the Firebase Console as **non-fatal** issues. If you set `rethrow = true`, the event will cause an actual crash, which helps group similar crashes together in the Console.

---

# 5) Tips for Covering Coroutine and Background Task Errors

*   There is no single magic bullet for catching all coroutine exceptions if their scopes are not provided with an exception handler. Therefore:
    *   Create a single `CoroutineExceptionHandler` (as shown in the `Application` class) and attach it to your most important root-level scopes (e.g., `ViewModelScope`).
    *   Review all `launch {}` call sites in your code. If they are numerous, consider gradually refactoring them to include the handler or use a `supervisorScope` so that one failure doesn't bring down the entire scope.

---

# 6) Uploading Mapping Files (Deobfuscation)

If you enable code shrinking (minify/R8) in your release builds, ensure you upload the mapping files. This allows Crashlytics to deobfuscate your stack traces so they are human-readable. The Crashlytics Gradle plugin handles this automatically when you set `mappingFileUploadEnabled true`.

---

# 7) Testing (Essential)

*   To test your setup, add a "Test Crash" button that throws a `RuntimeException("Test Crash")` or call `FirebaseCrashlytics.getInstance().crash()` in a debug build.
*   **Important Note:** When your app is connected to the debugger, Crashlytics may be inhibited from sending reports. **Run the app without the debugger attached** when testing the final crash reporting flow. After reopening the app post-crash, the report should appear in the Firebase Console within a few minutes.

---

# 8) Final Notes and Security Practices

*   **Never log sensitive information** (passwords, auth tokens, full credit card numbers) in any custom key or log. This could violate user privacy.
*   Remember that `recordException` generates a **non-fatal** report. If you re-throw the exception after logging it, it becomes a **fatal** crash. Choose the behavior based on your needs: non-fatal for gathering information without terminating the app, or re-throw for capturing a true crash.
