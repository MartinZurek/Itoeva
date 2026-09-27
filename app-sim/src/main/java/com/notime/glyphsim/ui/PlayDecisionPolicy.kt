package com.notime.glyphsim.ui

import android.content.Context
import android.util.Log
import com.notime.glyphsim.decision.DecisionHistory
import com.notime.glyphsim.decision.DecisionPolicies

/**
 * Die Android-Seite der Decision Policy: das Modell aus `assets/` laden und den Verlauf je Wesen
 * ablegen.
 *
 * **Dieselbe Datei fuer App und Stream.** `decision_policy_v1.onnx` liegt unter `src/main/assets`
 * und damit in jeder Build-Variante von `:app-sim` - auch in der Stream-APK, die dieselbe Runtime
 * mit anderer `applicationId` baut. Es gibt keinen zweiten Pfad, auf dem eine Variante ein
 * anderes Modell bekaeme.
 *
 * Geladen wird einmal je Prozess. Fehlt die Datei oder passt sie nicht, gilt die bisherige Logik
 * ([DecisionPolicies.load] nennt den Grund) - kein Absturz, kein Netz, kein Nachladen.
 */
object PlayDecisionPolicy {

    private const val TAG = "PlayDecisionPolicy"
    private const val PREFS = "play_decisions"

    @Volatile
    private var cached: DecisionPolicies.Loaded? = null

    fun loaded(context: Context): DecisionPolicies.Loaded {
        cached?.let { return it }
        val bytes = try {
            context.assets.open(DecisionPolicies.ASSET_NAME).use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
        val result = DecisionPolicies.load(bytes)
        result.fallbackReason?.let { Log.w(TAG, "decision policy falls back to existing logic: $it") }
        cached = result
        return result
    }

    /** Der Verlauf dieses Wesens. Je Wesen, wie der Bewegungsdrang (siehe PlayMovementLog). */
    fun history(context: Context, profileId: String): DecisionHistory =
        DecisionHistory.decode(prefs(context).getString(key(profileId), null))

    fun save(context: Context, profileId: String, history: DecisionHistory) {
        prefs(context).edit().putString(key(profileId), history.encode()).apply()
    }

    private fun key(profileId: String) = "history_$profileId"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
