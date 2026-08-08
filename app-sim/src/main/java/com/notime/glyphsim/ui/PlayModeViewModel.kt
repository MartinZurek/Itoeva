package com.notime.glyphsim.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.AvatarPlayState
import com.notime.glyphsim.matrix.AvatarSpecies
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/** Was [AvatarAssistant]s Play-Mode-Ansicht ueber den gerade gewaehlten Avatar braucht. */
data class PlayModeUiState(
    val species: AvatarSpecies = AvatarSpecies.PUFFLING,
    /**
     * Ob dieser Avatar schon einmal gespielt hat - entscheidet ueber „starten" vs. „fortsetzen".
     * Bewusst getrennt von [active]: der Fortschritt bleibt, wenn man den Modus verlaesst.
     */
    val started: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    /** Level ist hoeher als das zuletzt bestaetigte - der Avatar gratuliert einmalig, siehe
     *  [acknowledgeLevelUp]. */
    val showLevelUp: Boolean = false
)

/**
 * Traegt den Play Mode des gerade gewaehlten Avatars (siehe [AvatarSpeciesPrefs]) - das Spiel,
 * bei dem der Nutzer NICHTS einrichtet, sondern eine einzelne, vom Avatar-Charakter gesteuerte
 * Erinnerung (siehe [com.notime.glyphsim.matrix.PlayModeRoll]) selbststaendig zufaellig feuert.
 *
 * Jeder Avatar hat seinen eigenen Fortschritt ([AvatarPlayState], eine Zeile je Profil) und seine
 * eigene Play-Mode-Erinnerung ([GlyphReminder.isPlayMode]) - beide entstehen gemeinsam beim
 * ersten Einschalten ueber [setActive].
 */
class PlayModeViewModel(application: Application) : AndroidViewModel(application) {

    private val reminderDao = AppDatabase.getInstance(application).glyphReminderDao()
    private val playStateDao = AppDatabase.getInstance(application).avatarPlayStateDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<PlayModeUiState> = AvatarSpeciesPrefs.selected(application)
        .map { it ?: AvatarSpecies.PUFFLING }
        .flatMapLatest { species ->
            playStateDao.observeForProfile(AvatarSpeciesPrefs.profileId(species)).map { playState ->
                val xp = playState?.xp ?: 0
                val level = PlayModeXp.levelFor(xp)
                PlayModeUiState(
                    species = species,
                    started = playState != null,
                    xp = xp,
                    level = level,
                    showLevelUp = playState != null && level > playState.lastSeenLevel
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayModeUiState())

    /**
     * Schaltet zwischen Spielmodus und Normalbetrieb um - **der eine Schalter, um den sich das
     * ganze Feature dreht.**
     *
     * Beim Einschalten entstehen Erinnerung und Fortschrittszeile, falls es sie fuer diesen Avatar
     * noch nicht gibt. Danach wird in beiden Richtungen [ReminderScheduler.rescheduleAll]
     * aufgerufen: Es bestellt zuerst alle stehenden Alarme ab und plant anschliessend nur noch
     * den Satz, der zum neuen Modus gehoert (siehe
     * [com.notime.glyphcore.reminder.PlayModeState]). Genau das ist der Moduswechsel - der eine
     * Satz schlaeft ein, der andere wacht auf.
     *
     * Der Fortschritt (XP/Level) bleibt beim Ausschalten selbstverstaendlich bestehen; nur die
     * Zufalls-Erinnerung ruht.
     */
    fun setActive(active: Boolean) {
        viewModelScope.launch {
            val application = getApplication<Application>()
            val species = AvatarSpeciesPrefs.get(application)
            val profileId = AvatarSpeciesPrefs.profileId(species)

            if (active) ensurePlayReminder(application, species, profileId)

            // Erst den Modus setzen, dann neu planen - rescheduleAll fragt PlayModeState ab und
            // muss dabei schon den NEUEN Stand sehen.
            PlayModePrefs.setActive(application, active)
            ReminderScheduler.rescheduleAll(application)

            if (active) {
                playReminderId(profileId)?.let { pullFirstTriggerForward(it) }
            }
        }
    }

    /**
     * Legt die Spiel-Erinnerung dieses Avatars an, falls noch nicht vorhanden, und stellt sicher,
     * dass sie aktiviert ist. Thema und Intervall sind hier nur Startwerte - beim Einplanen wird
     * ohnehin sofort neu gewuerfelt (siehe [com.notime.glyphsim.matrix.PlayModeRoll]).
     */
    private suspend fun ensurePlayReminder(
        application: Application,
        species: AvatarSpecies,
        profileId: String
    ) {
        val existing = reminderDao.getAllForProfile(profileId).firstOrNull { it.isPlayMode }
        if (existing != null) {
            if (!existing.enabled) reminderDao.update(existing.copy(enabled = true))
        } else {
            reminderDao.insert(
                GlyphReminder(
                    label = application.getString(species.signatureTopic.labelRes),
                    animationType = species.signatureTopic,
                    daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
                    startMinuteOfDay = 0,
                    endMinuteOfDay = 23 * 60 + 59,
                    intervalMinutes = ReminderRhythm.forType(species.signatureTopic).suggestedInterval,
                    enabled = true,
                    profileId = profileId,
                    dailyGoal = NO_GOAL,
                    isPlayMode = true
                )
            )
        }
        if (playStateDao.getForProfile(profileId) == null) {
            playStateDao.insert(
                AvatarPlayState(profileId = profileId, startedAtMillis = System.currentTimeMillis())
            )
        }
    }

    private suspend fun playReminderId(profileId: String): Long? =
        reminderDao.getAllForProfile(profileId).firstOrNull { it.isPlayMode }?.id

    /**
     * Zieht die erste Auftauchen auf die naechste volle Minute vor.
     *
     * Ohne das laege der erste Slot auf dem regulaeren Raster (Fensterbeginn plus einem
     * Vielfachen des Intervalls) und damit bis zu ein volles Intervall entfernt - beim Start
     * eines Spiels also gut und gerne zehn Minuten Stillstand. Genau das liess den Spielmodus
     * beim ersten Ausprobieren wie eine tote Funktion wirken.
     *
     * Bewusst die naechste MINUTE und nicht „sofort": Erinnerungs-Slots liegen grundsaetzlich auf
     * vollen Minuten, und der Takt, der sie ausloest, haengt am Minutenwechsel (siehe
     * `MainActivity` und `ClockTick`). Ein Zeitpunkt dazwischen wuerde ohnehin bis zum naechsten
     * Minutenwechsel warten - er waere nur scheinbar frueher.
     *
     * Der stehende Alarm bleibt unberuehrt; er zeigt weiter auf den regulaeren Slot. Feuert der
     * vorgezogene Zeiger zuerst, plant `ReminderTrigger` beides ohnehin gemeinsam neu.
     */
    private suspend fun pullFirstTriggerForward(reminderId: Long) {
        val nextMinute = (System.currentTimeMillis() / 60_000L + 1) * 60_000L
        reminderDao.updateNextTrigger(reminderId, nextMinute)
    }

    /** Der Avatar hat gerade gratuliert - merkt sich, bis zu welchem Level schon bestaetigt wurde. */
    fun acknowledgeLevelUp() {
        viewModelScope.launch {
            val profileId = AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(getApplication()))
            val current = playStateDao.getForProfile(profileId) ?: return@launch
            playStateDao.setLastSeenLevel(profileId, PlayModeXp.levelFor(current.xp))
        }
    }
}
