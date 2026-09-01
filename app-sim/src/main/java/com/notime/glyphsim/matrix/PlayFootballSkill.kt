package com.notime.glyphsim.matrix

import android.content.Context
import com.notime.glyphcore.data.AnimationType

/** Dauerhaft gelernte Fussballbewegung eines einzelnen Avatar-Profils. */
object PlayFootballSkill {
    private const val PREFS = "play_football_skills"
    private const val SUFFIX = "rainbow_flick"

    fun isLearned(context: Context, profileId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("${profileId}_$SUFFIX", false)

    fun learn(context: Context, profileId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("${profileId}_$SUFFIX", true).apply()
    }

    /** Das Thema wird auf dem Platz als Fussball interpretiert; freie Animationen brauchen Namen. */
    fun isFootballAnimation(type: AnimationType?, label: String?): Boolean {
        val named = label.orEmpty().lowercase()
        val footballName = listOf("football", "fußball", "fussball", "soccer").any { it in named }
        // MOVE allein reicht nicht: Auch ein normaler Spaziergang hat diesen groben Typ.
        return footballName && (type == null || type == AnimationType.MOVE)
    }
}
