package com.crowdfunding

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.crowdfunding.ads.AppOpenManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class App : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var currentActivity: Activity? = null
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super<Application>.onCreate()
        Firebase.database.setPersistenceEnabled(true)

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
