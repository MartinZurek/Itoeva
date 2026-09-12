package com.notime.glyphsim.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.LivingAgentStore
import com.notime.glyphsim.data.SharedPreferencesLivingAgentStorage
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayTimeLapse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.GoalProgress
import com.notime.glyphsim.matrix.expectedByNow
import com.notime.glyphsim.matrix.AvatarSpecies
import java.time.LocalTime

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

        val fedByReminder = db.avatarFeedEventDao()
            .countFedPerReminderSince(companionId, since)
            .associate { it.reminderId to it.count }

        // Jede Erinnerung bringt ihr eigenes Zeitfenster mit - "6x zwischen 8 und 20 Uhr" heisst
        // um 12 Uhr etwas anderes als um 19 Uhr. Ohne diese Anteiligkeit stand jeder Morgen bei
        // null Prozent, und selbst ein puenktlich erledigter Tag machte das Wesen bis zum
        // Nachmittag truebe (siehe expectedByNow).
        val minuteOfDay = LocalTime.now().let { it.hour * 60 + it.minute }

        // **Und wie es dem Wesen selbst geht** (NT-074).
        //
        // Der Living Agent fuehrt laengst sieben Beduerfnisse mit. Dass ein hungriges, muedes,
        // einsames Wesen trotzdem gut gelaunt aussah, solange die Haekchen stimmten, war der
        // sichtbarste Bruch zwischen dem, was das Modell weiss, und dem, was das Bild zeigt.
        //
        // Ueber `restore` und nicht ueber einen Rohzugriff: Es laesst die Beduerfnisse um die
        // verstrichene Zeit nachwachsen. Wer die App zwei Tage nicht geoeffnet hat, trifft kein
        // eingefrorenes Wesen. Fehlt ein gespeicherter Zustand - beim allerersten Start -, bleibt
        // nur das Pflegebuch, und die Stimmung ist dieselbe wie bisher.
        val wellbeing = withContext(Dispatchers.IO) {
            runCatching {
                LivingAgentStore(SharedPreferencesLivingAgentStorage(context)).restore(
                    profileId = companionId,
                    currentSimulationMinute = PlayTimeLapse.absoluteMinute(),
                    currentOpenSites = LivingRuntimeAdapter.openSitesAt(
                        PlayTimeLapse.absoluteMinute() % WorldState.MINUTES_PER_DAY
                    )
                )?.agent?.needs?.wellbeing()
            }.getOrNull()
        }

        if (goals.isEmpty()) {
            // Ohne Tagesziele gab es bisher immer NEUTRAL - ein Wesen ohne jede Regung. Jetzt
            // folgt es seinem eigenen Zustand, sofern einer da ist.
            return wellbeing?.let { AvatarMood.of(emptyList(), it) } ?: AvatarMood.NEUTRAL
        }

        val fortschritt = goals.map {
            GoalProgress(
                goal = it.dailyGoal,
                achieved = fedByReminder[it.id] ?: 0,
                expected = expectedByNow(
                    goal = it.dailyGoal,
                    startMinuteOfDay = it.startMinuteOfDay,
                    endMinuteOfDay = it.endMinuteOfDay,
                    minuteOfDay = minuteOfDay
                )
            )
        }
        return wellbeing
            ?.let { AvatarMood.of(fortschritt, it) }
            ?: AvatarMood.fromGoals(fortschritt)
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
