package com.crowdfunding.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialManager {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    private var lastShownAt = 0L
    private var clicksSinceLastAd = 0

    // Frequency policy values
    private const val MIN_INTERVAL_MS = 60_000L     // 60 seconds
    private const val MIN_CLICKS = 3                // 3 interactions

    fun preload(context: Context, adUnitId: String) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d("Interstitial", "Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w("Interstitial", "Load failed: ${error.message}")
                }
            }
        )
    }

    fun maybeShow(activity: Activity, adUnitId: String, onContinue: () -> Unit) {
        clicksSinceLastAd++

        val now = SystemClock.elapsedRealtime()
        val timeOk = (now - lastShownAt) >= MIN_INTERVAL_MS
        val clicksOk = clicksSinceLastAd >= MIN_CLICKS
        val ready = interstitialAd != null

        if (ready && timeOk && clicksOk) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastShownAt = SystemClock.elapsedRealtime()
                    clicksSinceLastAd = 0
                    preload(activity, adUnitId) // Preload the next one
                    onContinue()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    preload(activity, adUnitId)
                    onContinue()
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null // Ad can't be reused
                }
            }
            interstitialAd?.show(activity)
        } else {
            if (!ready && !isLoading) {
                preload(activity, adUnitId)
            }
            onContinue()
        }
    }
}
