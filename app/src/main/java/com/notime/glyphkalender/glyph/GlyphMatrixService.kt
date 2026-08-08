package com.notime.glyphkalender.glyph

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Basisklasse fuer alle Glyph Toys dieser App. Kapselt Verbindungsaufbau,
 * Touch-Event-Weiterleitung und Registrierung beim Glyph-Matrix-Dienst
 * (Aufbau uebernommen aus dem offiziellen Nothing GlyphMatrix-Example-Project).
 */
abstract class GlyphMatrixService(private val tag: String) : Service() {

    private val toyHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                msg.data?.getString(KEY_DATA)?.let { value ->
                    when (value) {
                        GlyphToy.EVENT_ACTION_DOWN -> onTouchPointPressed()
                        GlyphToy.EVENT_ACTION_UP -> onTouchPointReleased()
                        GlyphToy.EVENT_CHANGE -> onTouchPointLongPress()
                    }
                }
            } else {
                super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(toyHandler)

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                GlyphMatrixConnection.markConnected()
                // Nothing Phone (4a) Pro (Glyph Matrix). Aus dem SDK-Bytecode verifiziert:
                // kleines "p" am Ende, nicht zu verwechseln mit Glyph.DEVICE_25111 (das ist
                // die Glyph-Bar-Variante des einfachen (4a) ohne Matrix).
                // Fuer das (Flaggschiff-)Phone (3) waere stattdessen Glyph.DEVICE_23112 zu nutzen.
                gmm.register(Glyph.DEVICE_25111p)
                performOnServiceConnected(applicationContext, gmm)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            GlyphMatrixConnection.markDisconnected()
        }
    }

    final override fun onBind(intent: Intent?): IBinder {
        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(callback)
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        glyphMatrixManager?.let { performOnServiceDisconnected(applicationContext) }
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        // unbindService() loest onServiceDisconnected NICHT aus (das passiert laut
        // Android-Doku nur bei unerwartetem Verbindungsverlust) - Status hier manuell
        // zuruecksetzen, sonst haelt GlyphMatrixConnection den Status faelschlich fuer verbunden.
        GlyphMatrixConnection.markDisconnected()
        return false
    }

    open fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {}
    open fun performOnServiceDisconnected(context: Context) {}

    open fun onTouchPointPressed() {}
    open fun onTouchPointLongPress() {}
    open fun onTouchPointReleased() {}

    private companion object {
        private const val KEY_DATA = "data"
    }
}
