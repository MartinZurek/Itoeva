package com.notime.glyphsim.matrix

import android.content.Context

/** Der Quest-Stand eines Avatar-Profils - gerechnet wird in [PlayQuests], hier nur abgelegt. */
object PlayQuestLog {
    private const val PREFS = "play_quests"

    fun load(context: Context, profileId: String): PlayQuests.Progress =
        PlayQuests.decode(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(profileId, null)
        )

    fun save(context: Context, profileId: String, progress: PlayQuests.Progress) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(profileId, PlayQuests.encode(progress)).apply()
    }
}
