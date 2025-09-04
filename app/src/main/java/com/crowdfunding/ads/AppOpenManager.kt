package com.crowdfunding.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.*

class AppOpenManager(
    private val context: Context,
    private val adUnitId: String
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var isShowing = false
    private var loadTime: Long = 0

    private fun isAdAvailable(): Boolean {
        val adAvailable = appOpenAd != null
        val notExpired = (Date().time - loadTime) < 4 * 60 * 60 * 1000L // 4 hours
        return adAvailable && notExpired
    }

    fun loadAd() {
        if (isLoading || isAdAvailable()) return
        isLoading = true

        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoading = false
                    loadTime = Date().time
                    Log.d("AppOpen", "Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("AppOpen", "Load failed: ${error.message}")
                    isLoading = false
                    appOpenAd = null
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity, onComplete: (() -> Unit)? = null) {
        if (isShowing) {
            onComplete?.invoke()
            return
        }

        if (!isAdAvailable()) {
            onComplete?.invoke()
            loadAd() // Prepare for the next opportunity
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                appOpenAd = null
                loadAd() // Preload the next ad
                onComplete?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowing = false
                appOpenAd = null
                loadAd()
                onComplete?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                isShowing = true
            }
        }

        appOpenAd?.show(activity)
    }

    init { loadAd() }
}
