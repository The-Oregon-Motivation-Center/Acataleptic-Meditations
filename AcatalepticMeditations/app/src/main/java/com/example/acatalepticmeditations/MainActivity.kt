package com.acataleptic.meditations

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: JournalViewModel by viewModels {
        JournalViewModelFactory(database.journalEntryDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
