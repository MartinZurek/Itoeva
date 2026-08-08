package com.notime.glyphkalender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphkalender.ui.ReminderScreen
import com.notime.glyphkalender.ui.theme.GlyphKalenderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sicherheitsnetz: AlarmManager-Alarme werden bei jeder Aenderung sofort neu
        // gesetzt (siehe GlyphReminderViewModel) und ueberleben dank BootReceiver auch
        // einen Reboot - dieser Aufruf faengt nur Randfaelle ab (z.B. App-Neuinstallation
        // ohne Reboot dazwischen).
        lifecycleScope.launch { ReminderScheduler.rescheduleAll(this@MainActivity) }

        setContent {
            GlyphKalenderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReminderScreen()
                }
            }
        }
    }
}
