package com.notime.glyphsim.matrix

import android.content.Context

/** Der Stand der Vorhaben eines Avatar-Profils - gerechnet wird in [PlayGoals], hier nur abgelegt. */
object PlayGoalLog {
    private const val PREFS = "play_goals"

    fun load(context: Context, profileId: String): PlayGoals.Progress =
        PlayGoals.decode(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(profileId, null)
        )

    fun save(context: Context, profileId: String, progress: PlayGoals.Progress) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(profileId, PlayGoals.encode(progress)).apply()
    }
}
