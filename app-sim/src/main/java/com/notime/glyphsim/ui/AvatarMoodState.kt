package com.notime.glyphsim.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.GoalProgress
import com.notime.glyphsim.matrix.AvatarSpecies

/**
 * Stimmung ausserhalb der Komposition ermitteln - der Dock-Modus braucht sie genau einmal beim
 * Auftauchen des Avatars, mitten in einer Coroutine, wo kein Composable-Kontext zur Verfuegung
 * steht.
 */
object AvatarMoodSnapshot {
    suspend fun forSpecies(context: Context, species: AvatarSpecies): AvatarMood {
        if (!MoodPrefs.isEnabled(context)) return AvatarMood.NEUTRAL
        val db = AppDatabase.getInstance(context)
        // Zwei Quellen, zwei Besitzer: die Ziele stammen aus den Routinen des NUTZERS, das
        // Erreichte aus dem Pflegebuch des anwesenden WESENS (siehe RoutineOwner /
        // PresentCompanion). Heute derselbe Wert; getrennt gefragt, damit die Stimmung nach dem
        // Entkoppeln das Richtige bedeutet - naemlich "wie hat DIESES Wesen den Tag erlebt".
        val routineOwner = RoutineOwner.current(context)
        val companionId = AvatarSpeciesPrefs.profileId(species)
        // Tagesgrenze statt gleitender 24 Stunden: ein Tagesziel endet um Mitternacht.
        val since = FeedStatsPeriod.TODAY.startMillis()

        // Nur Erinnerungen MIT Ziel zaehlen - reine Stupser ohne Vorgabe bleiben aussen vor und
        // koennen den Avatar daher nie truebe machen (siehe GlyphReminder.dailyGoal).
        val goals = db.glyphReminderDao().getEnabledForProfile(routineOwner).filter { it.dailyGoal > 0 }
        if (goals.isEmpty()) return AvatarMood.NEUTRAL

        val fedByReminder = db.avatarFeedEventDao()
            .countFedPerReminderSince(companionId, since)
            .associate { it.reminderId to it.count }

        return AvatarMood.fromGoals(
            goals.map { GoalProgress(goal = it.dailyGoal, achieved = fedByReminder[it.id] ?: 0) }
        )
    }
}

/**
 * Aktuelle Stimmung eines Avatars, berechnet aus den Erinnerungen der letzten 24 Stunden
 * (siehe [AvatarMood]).
 *
 * [refreshKey] erlaubt es dem Aufrufer, eine Neuberechnung anzustossen - etwa direkt nach dem
 * Fuettern, damit die Aufmunterung sofort sichtbar wird statt erst beim naechsten Screen-Aufbau.
 * Ohne diesen Anstoss bliebe die Stimmung stehen, weil sie aus einer einmaligen Abfrage stammt
 * und nicht aus einem Flow: die Zeitgrenze der letzten 24 Stunden wandert staendig weiter, ein
 * Flow muesste dafuer ohnehin regelmaessig neu ausgewertet werden.
 */
@Composable
fun rememberAvatarMood(species: AvatarSpecies, refreshKey: Int = 0): State<AvatarMood> {
    val context = LocalContext.current
    return produceState(initialValue = AvatarMood.NEUTRAL, species, refreshKey) {
        value = AvatarMoodSnapshot.forSpecies(context, species)
    }
}

/**
 * Menschenlesbare Stimmungsbeschreibung fuer TalkBack - dieselbe "traeger"-Sprache wie schon in
 * [R.string.settings_mood_hint], nicht "hungrig"/"traurig": der Avatar soll nie mit
 * Schuldgefuehlen arbeiten (siehe Klassendoku von [AvatarMood]), das gilt fuer einen
 * Screenreader-Text genauso wie fuer die Animation selbst.
 */
@Composable
fun AvatarMood.describeForAccessibility(): String = stringResource(
    when (this) {
        AvatarMood.NEUTRAL -> R.string.a11y_mood_neutral
        AvatarMood.HAPPY -> R.string.a11y_mood_happy
        AvatarMood.CONTENT -> R.string.a11y_mood_content
        AvatarMood.HUNGRY -> R.string.a11y_mood_hungry
        AvatarMood.SAD -> R.string.a11y_mood_sad
    }
)
