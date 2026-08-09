package com.notime.glyphsim.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Ob sich die App mitdrehen darf, wenn das Geraet gedreht wird.
 *
 * **Standardmaessig aus.** Die Uhr ist rund und gewinnt im Querformat nichts, waehrend eine
 * Drehung im Dock-Betrieb auf dem Nachttisch schon durch ein Anstossen des Geraets ausgeloest
 * werden kann. Wer es trotzdem will, schaltet es hier frei - dann gilt zusaetzlich weiterhin die
 * Systemeinstellung: bei gesperrter automatischer Drehung bleibt es auch dann beim Hochformat.
 */
object OrientationPrefs {
    fun isRotationAllowed(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.AllowRotation)

    fun setRotationAllowed(context: Context, allowed: Boolean) {
        SettingsStore.write(context, SettingsCatalog.AllowRotation, allowed)
    }

    /**
     * Wendet die Einstellung auf eine laufende Activity an - wirkt sofort, ohne Neustart.
     *
     * [ActivityInfo.SCREEN_ORIENTATION_FULL_USER] statt UNSPECIFIED: es folgt der
     * Drehsperre des Nutzers. Hat er die automatische Drehung im System gesperrt, bleibt es auch
     * mit diesem Schalter beim Hochformat - genau das ist gemeint mit "nur wenn das System es
     * ohnehin erlaubt".
     */
    fun apply(activity: Activity, allowed: Boolean = isRotationAllowed(activity)) {
        activity.requestedOrientation = if (allowed) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

}
