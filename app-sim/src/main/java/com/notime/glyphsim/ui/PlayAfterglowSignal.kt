package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.PlayAfterglow

/**
 * Holt die heute beantworteten Erinnerungen aus der Datenbank und macht daraus den Nachklang -
 * das Gegenstueck zu [PlayHabitSignal], mit umgekehrtem Vorzeichen.
 *
 * [PlayHabitSignal] gewichtet, was heute noch OFFEN ist; hier zaehlt, was heute schon BEANTWORTET
 * wurde. Bewusst als zweite kleine Abfrage und nicht als Erweiterung der ersten: Die beiden sagen
 * Verschiedenes ueber denselben Tag, und sie sollen sich unabhaengig voneinander veraendern
 * lassen. Die Rechnung selbst steht in [PlayAfterglow] und kommt ohne Datenbank aus - pruefbar
 * mit erfundenen Zeitpunkten statt mit einem Geraet.
 */
object PlayAfterglowSignal {

    /**
     * [companionProfileId] ist das anwesende Wesen: Nachklingen soll, was DIESES Wesen erlebt hat,
     * nicht was ein anderes Profil am selben Tag beantwortet hat.
     *
     * Echte Zeit ueberall, nicht die Weltzeit aus
     * [com.notime.glyphsim.matrix.PlayTimeLapse] - siehe [PlayAfterglow.Answer].
     */
    suspend fun bonuses(context: Context, companionProfileId: String): Map<AnimationType, Int> {
        val tagesbeginn = FeedStatsPeriod.TODAY.startMillis()
        val antworten = AppDatabase.getInstance(context).avatarFeedEventDao()
            .answeredTopicsSince(companionProfileId, tagesbeginn)
            .map { PlayAfterglow.Answer(it.animationType, it.fedAtMillis) }
        return PlayAfterglow.bonuses(
            answers = antworten,
            nowMillis = System.currentTimeMillis(),
            dayStartMillis = tagesbeginn
        )
    }
}
