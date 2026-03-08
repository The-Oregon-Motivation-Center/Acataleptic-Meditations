package com.acataleptic.meditations

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.acataleptic.meditations.data.AppDatabase
import com.acataleptic.meditations.ui.CalendarScreen
import com.acataleptic.meditations.ui.JournalViewModel
import com.acataleptic.meditations.ui.JournalViewModelFactory
import com.acataleptic.meditations.ui.theme.AcatalepticMeditationsTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: JournalViewModel by viewModels {
        JournalViewModelFactory(database.journalEntryDao())
    }

    private lateinit var consentInformation: ConsentInformation
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Consent Flow for AdMob to prevent the consentMap null pointer crash
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w("Ads", "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }

                    // Consent has been gathered or isn't required.
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestConsentError ->
                Log.w("Ads", "${requestConsentError.errorCode}: ${requestConsentError.message}")
            })

        // Check if ads can already be requested (e.g. from a previous session)
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
        
        enableEdgeToEdge()
        setContent {
            AcatalepticMeditationsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalendarScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        MobileAds.initialize(this) {}
    }
}
