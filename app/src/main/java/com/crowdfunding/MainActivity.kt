package com.crowdfunding


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.crowdfunding.ui.theme.CrowdFundingTheme
import android.util.Log
import com.android.installreferrer.BuildConfig
import com.crowdfunding.ads.InterstitialManager
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.*

class MainActivity : ComponentActivity() {

    private lateinit var consentInformation: ConsentInformation
    private var consentForm: ConsentForm? = null
    private var dependentSDKsInitialized = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Configure Consent Request Parameters
        val paramsBuilder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG) {
            val debugSettings = ConsentDebugSettings.Builder(this)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("YOUR_COPIED_TEST_DEVICE_HASHED_ID") // <-- USER NEEDS TO REPLACE THIS
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }
        val requestParameters = paramsBuilder.build()

        // 2. Request Consent Information
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            requestParameters,
            { // OnConsentInfoUpdateSuccessListener
                Log.d("UMP_SDK_TRACE", "ConsentInfoUpdate success. Form available: ${consentInformation.isConsentFormAvailable}, Status: ${consentInformation.consentStatus}")
                if (consentInformation.isConsentFormAvailable) {
                    loadForm()
                } else {
                    Log.d("UMP_SDK_TRACE", "No consent form available/required. Status: ${consentInformation.consentStatus}")
                    initializeDependentSDKs()
                }
            },
            { formError -> // OnConsentInfoUpdateFailureListener
                Log.e("UMP_SDK_TRACE", "ConsentInfoUpdate failed: ${formError.errorCode} - ${formError.message}")
                initializeDependentSDKs() // Proceed with caution
            }
        )

        // Preload an interstitial ad
        InterstitialManager.preload(this, getString(R.string.admob_interstitial_id))

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CrowdFundingTheme {
                    AppNavigation()
                }
            }
        }
    }

    private fun loadForm() {
        Log.d("UMP_SDK_TRACE", "Attempting to load consent form.")
        UserMessagingPlatform.loadConsentForm(
            this,
            { loadedConsentForm -> // OnConsentFormLoadSuccessListener
                this.consentForm = loadedConsentForm
                Log.d("UMP_SDK_TRACE", "Consent form loaded successfully.")
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    Log.d("UMP_SDK_TRACE", "Consent status REQUIRED. Showing form.")
                    consentForm?.show(this) { formError -> // OnConsentFormDismissedListener
                        if (formError != null) {
                            Log.e("UMP_SDK_TRACE", "Consent form dismissal error: ${formError.message}")
                        } else {
                            Log.d("UMP_SDK_TRACE", "Consent form dismissed. Status: ${consentInformation.consentStatus}")
                        }
                        initializeDependentSDKs()
                    }
                } else {
                    Log.d("UMP_SDK_TRACE", "Consent status NOT REQUIRED or OBTAINED. Not showing form. Status: ${consentInformation.consentStatus}")
                    initializeDependentSDKs()
                }
            },
            { formError -> // OnConsentFormLoadFailureListener
                Log.e("UMP_SDK_TRACE", "Consent form load failed: ${formError.message}")
                initializeDependentSDKs() // Proceed with caution
            }
        )
    }

    private fun initializeDependentSDKs() {
        if (dependentSDKsInitialized) {
            Log.d("UMP_SDK_TRACE", "Dependent SDKs (e.g., Ads) already initialized.")
            return
        }

        if (::consentInformation.isInitialized && consentInformation.canRequestAds()) {
            Log.d("UMP_SDK_TRACE", "Consent obtained or not required. Initializing Google Mobile Ads SDK.")
            MobileAds.initialize(this) { initializationStatus ->
                Log.d("UMP_SDK_TRACE", "Google Mobile Ads SDK Initialized.")
                // Now you can load ads.
            }
        } else {
            Log.w("UMP_SDK_TRACE", "Consent not obtained for ads. Ads SDK not initialized for personalized ads.")
        }
        dependentSDKsInitialized = true
    }
}