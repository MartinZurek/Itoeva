package com.notime.glyphsim.matrix

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.ui.AvatarSpeciesPrefs
import com.notime.glyphsim.ui.PlayModeXp
import com.notime.glyphsim.ui.PresentCompanion
import kotlin.random.Random

/**
 * Wuerfelt fuer die EINE Spiel-Erinnerung eines Avatars bei jeder Neuplanung neu, was als
 * Naechstes kommt - eingehaengt in [com.notime.glyphcore.reminder.ReminderHost] und von
 * [com.notime.glyphcore.reminder.ReminderScheduler.schedule] fuer jede
 * [GlyphReminder.isPlayMode]-Zeile aufgerufen.
 *
 * **Teil-Zufall, kein Gleichverteilungs-Generator.** Was ueberhaupt in Frage kommt und wie
 * wahrscheinlich es ist, gibt der Spielplan des jeweiligen Avatars vor ([PlayGamePlans]) - und der
 * haengt am Charakter der Figur und an ihrem Fortschritt. Gewuerfelt wird innerhalb dieses Rahmens.
 * Dadurch bleibt jedes einzelne Auftauchen ueberraschend, waehrend sich ueber die Zeit trotzdem
 * eine Handschrift ergibt.
 *
 * Das Level entscheidet ueber die Stufe des Plans, deshalb ist [reroll] suspend: Es liest den
 * Spielstand aus der Datenbank.
 */
object PlayModeRoll {

    suspend fun reroll(context: Context, reminder: GlyphReminder): GlyphReminder {
        // **Das Wesen, nicht die Erinnerung.** Vorher stand hier
        // `AvatarSpecies.valueOf(reminder.profileId)` - die Spezies wurde also aus dem
        // Besitzer-Feld einer Routine zurueckgerechnet. Das ging nur gut, solange die Profil-Id
        // der Avatarname IST; sobald die Routinen dem Nutzer gehoeren, faende das `runCatching`
        // keine Spezies mehr und jedes Wesen wuerfelte still nach dem Plan von PUFFLING.
        //
        // Welchen Plan gewuerfelt wird und wie weit er fortgeschritten ist, sind ohnehin Fragen an
        // das anwesende Wesen (siehe PresentCompanion) - so, wie der Spielstand selbst.
        val species = AvatarSpeciesPrefs.get(context)
        val companionId = PresentCompanion.profileId(context)
        val stage = PlayGamePlans.forSpecies(species).stageFor(currentLevel(context, companionId))
        val topic = pickWeighted(stage.topicWeights)
        return reminder.copy(
            label = context.getString(topic.labelRes),
            animationType = topic,
            libraryAnimationId = null,
            intervalMinutes = stage.intervalMinutes.random(),
            daysOfWeekMask = ALL_DAYS_MASK,
            startMinuteOfDay = 0,
            endMinuteOfDay = END_OF_DAY_MINUTE,
            dailyGoal = NO_GOAL
        )
    }

    private suspend fun currentLevel(context: Context, companionProfileId: String): Int {
        val xp = AppDatabase.getInstance(context).avatarPlayStateDao()
            .getForProfile(companionProfileId)?.xp ?: 0
        return PlayModeXp.levelFor(xp)
    }

    /**
     * Zieht ein Thema entsprechend seinem Gewicht. Leere oder unsinnige Gewichtungen koennen einen
     * Spielplan nicht zum Absturz bringen - dann kommt schlicht das Allgemein-Thema.
     */
    private fun pickWeighted(weights: Map<AnimationType, Int>): AnimationType {
        val total = weights.values.sum()
        if (total <= 0) return AnimationType.GENERAL
        var roll = Random.nextInt(total)
        for ((topic, weight) in weights) {
            roll -= weight
            if (roll < 0) return topic
        }
        return weights.keys.firstOrNull() ?: AnimationType.GENERAL
    }

    /** Alle sieben Wochentage gesetzt (Bit 0..6), siehe [com.notime.glyphcore.data.DaysOfWeekMask]. */
    private const val ALL_DAYS_MASK = 0b1111111
    private const val END_OF_DAY_MINUTE = 23 * 60 + 59
}
