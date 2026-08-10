package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.data.AppDatabase

/**
 * Welche echten Erinnerungs-Themen (mit Tagesziel, siehe [com.notime.glyphcore.data.GlyphReminder.dailyGoal])
 * heute noch NICHT erreicht sind - das Signal, mit dem [com.notime.glyphsim.matrix.PlayAmbientActivity]
 * ihre Tagesablauf-Gewichtung zusaetzlich zur Tageszeit an das anpasst, was der Nutzer tatsaechlich
 * eingerichtet hat: der Avatar greift dadurch von sich aus oefter zu einem Thema, das heute noch
 * offen ist, und seltener zu einem, das schon erledigt ist - "gefuettert werden" veraendert also
 * sichtbar, was der Avatar von sich aus tut, nicht nur seine Stimmung.
 *
 * Dieselbe Datengrundlage wie [AvatarMoodSnapshot] (dort zu EINER Stimmungszahl verdichtet), hier
 * zu einer Themen-Menge - bewusst als eigene, kleine Abfrage statt AvatarMoodSnapshot zu erweitern,
 * damit beide unabhaengig voneinander erweiterbar bleiben (z.B. spaeter: Gewichtung nach WIE weit
 * ein Ziel offen ist, nicht nur ob).
 */
object PlayHabitSignal {

    /**
     * [companionProfileId] ist das anwesende Wesen - sein Pflegebuch entscheidet, was heute schon
     * erledigt ist. Die Ziele selbst kommen aus den Routinen des Nutzers ([RoutineOwner]); die
     * beiden Besitzer werden bewusst getrennt gefragt, auch wenn sie heute denselben Wert haben.
     */
    suspend fun underfulfilledTopics(context: Context, companionProfileId: String): Set<AnimationType> {
        val db = AppDatabase.getInstance(context)
        val since = FeedStatsPeriod.TODAY.startMillis()

        // isPlayMode ausgeschlossen: die eine Spiel-Erinnerung hat ohnehin nie ein Tagesziel
        // (siehe PlayModeRoll), aber explizit statt implizit ist hier sicherer, falls sich das
        // je aendert - eine Spiel-Erinnerung darf sich nicht selbst verstaerken.
        val goals = db.glyphReminderDao().getEnabledForProfile(RoutineOwner.current(context))
            .filter { it.dailyGoal > 0 && !it.isPlayMode }
        if (goals.isEmpty()) return emptySet()

        val fedByReminder = db.avatarFeedEventDao()
            .countFedPerReminderSince(companionProfileId, since)
            .associate { it.reminderId to it.count }

        return goals.asSequence()
            .filter { (fedByReminder[it.id] ?: 0) < it.dailyGoal }
            .map { it.animationType }
            .toSet()
    }
}
