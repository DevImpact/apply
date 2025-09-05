package com.crowdfunding

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.crowdfunding.ads.AppOpenManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber

class App : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var currentActivity: Activity? = null
    private lateinit var appOpenManager: AppOpenManager

    // General CoroutineExceptionHandler for key scopes
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "CoroutineExceptionHandler caught exception")
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    override fun onCreate() {
        super<Application>.onCreate()

        // Setup Timber for logging
        setupTimber()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Firebase.database.setPersistenceEnabled(true)

        // Enable Crashlytics collection
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Set up a global exception handler for all threads
        setupGlobalExceptionHandler()

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Register lifecycle observers
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Initialize App Open Ad Manager
        appOpenManager = AppOpenManager(
            context = this,
            adUnitId = getString(R.string.admob_app_open_id)
        )
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (t != null) {
                        // Log exceptions to Crashlytics
                        FirebaseCrashlytics.getInstance().recordException(t)
                    } else {
                        // Log messages as breadcrumbs
                        FirebaseCrashlytics.getInstance().log("$tag: $message")
                    }
                }
            })
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log additional context using Timber
            Timber.e(throwable, "UncaughtException on thread: ${thread.name}")
            // The CrashlyticsTree will automatically record the exception,
            // but we can also record it manually for redundancy if needed.
            // FirebaseCrashlytics.getInstance().recordException(throwable)

            // Let the default handler do its thing (crash the app)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    // --- Lifecycle Callbacks ---

    // Called when the app comes to the foreground
    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { activity ->
            appOpenManager.showAdIfAvailable(activity)
        }
    }

    // --- ActivityLifecycleCallbacks Implementation ---

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        // Set the current screen name as a custom key in Crashlytics
        FirebaseCrashlytics.getInstance().setCustomKey("current_activity", activity.javaClass.simpleName)
    }

    override fun onActivityPaused(activity: Activity) {
        // No-op
    }

    override fun onActivityStopped(activity: Activity) {
        // No-op
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No-op
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
