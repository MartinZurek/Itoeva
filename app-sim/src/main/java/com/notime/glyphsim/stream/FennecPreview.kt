package com.notime.glyphsim.stream

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Lokale Vorschau ohne Twitch-Schreiben. DUMP erlaubt den Eingang nur Android-System/Shell. */
internal class FennecPreview {
    private val stream = MutableSharedFlow<FennecConversation.Address>(extraBufferCapacity = 1)
    val addresses = stream.asSharedFlow()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION) return
            FennecConversation.address("local_preview", intent.getStringExtra("message") ?: "",
                System.currentTimeMillis())?.let { stream.tryEmit(it.copy(preview = true)) }
        }
    }

    fun register(context: Context) {
        ContextCompat.registerReceiver(context, receiver, IntentFilter(ACTION), "android.permission.DUMP",
            null, ContextCompat.RECEIVER_EXPORTED)
    }

    fun unregister(context: Context) = context.unregisterReceiver(receiver)

    companion object {
        const val ACTION = "com.notime.glyphminderwatch.stream.FENNEC_PREVIEW"
    }
}
