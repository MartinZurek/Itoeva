package com.notime.glyphsim.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphcore.data.BuiltInAnimationRepository
import com.notime.glyphcore.data.BuiltInAnimationSelection
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphcore.data.GlyphReminderRepository
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.LibraryAnimationRepository
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.ReminderAnimations
import com.notime.glyphcore.reminder.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GlyphReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GlyphReminderRepository(AppDatabase.getInstance(application).glyphReminderDao())
    private val libraryRepository =
        LibraryAnimationRepository(AppDatabase.getInstance(application).libraryAnimationDao())
    private val builtInRepository =
        BuiltInAnimationRepository(AppDatabase.getInstance(application).builtInAnimationSelectionDao())

    /**
     * Nur die Erinnerungen des gerade gewaehlten Avatars - jeder Avatar hat seinen eigenen
     * Satz (im gemeinsamen Kern neutral als Profil modelliert, siehe [GlyphReminder.profileId]).
     * flatMapLatest, damit ein Avatar-Wechsel im Einstellungs-Dialog die Liste sofort umschaltet,
     * statt sie bis zum Neuaufbau des Screens auf dem alten Stand stehen zu lassen.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val reminders: StateFlow<List<GlyphReminder>> =
        AvatarSpeciesPrefs.selected(application)
            .flatMapLatest { species ->
                repository.observeForProfile(
                    AvatarSpeciesPrefs.profileId(species ?: AvatarSpecies.PUFFLING)
                )
            }
            // Die Spiel-Erinnerung gehoert nicht in diese Liste: Sie ist keine Einstellung des
            // Nutzers, sondern wird vom Spielmodus selbst verwaltet und bei jeder Neuplanung neu
            // gewuerfelt (siehe PlayModeViewModel/PlayModeRoll). Sichtbar waere sie hier nicht nur
            // verwirrend - jede Bearbeitung daran waere beim naechsten Wuerfeln wieder weg.
            .map { list -> list.filter { !it.isPlayMode } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Der aktuell gewaehlte Avatar - der Erinnerungs-Screen zeigt ihn als Kontext an. */
    val activeSpecies: StateFlow<AvatarSpecies> =
        AvatarSpeciesPrefs.selected(application)
            .map { it ?: AvatarSpecies.PUFFLING }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AvatarSpecies.PUFFLING)

    val libraryAnimations: StateFlow<List<LibraryAnimation>> = libraryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** On/Off-Status je fest eingebautem [AnimationType] (siehe BuiltInAnimationSelection.kt). */
    val builtInSelections: StateFlow<List<BuiltInAnimationSelection>> = builtInRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val libraryFullEvents = Channel<Unit>(Channel.CONFLATED)

    /** Feuert, wenn eine Auswahl am Picker-Limit ([MAX_ANIMATIONS_IN_PICKER]) abgelehnt wurde (fuer eine Snackbar-Meldung). */
    val libraryFullEvent: Flow<Unit> = libraryFullEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            libraryRepository.seedOrRefresh()
            builtInRepository.seedIfEmpty()
            // Voreingestellte Erinnerungen pro Avatar anlegen (siehe seedIfEmpty) und danach
            // sicherstellen, dass wirklich nur die Alarme des aktiven Avatars stehen.
            repository.seedIfEmpty(
                AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(getApplication()))
            )
            ReminderScheduler.rescheduleAll(getApplication())
        }
    }

    fun setLibraryAnimationSelected(id: Long, selected: Boolean) {
        viewModelScope.launch {
            val accepted = libraryRepository.setSelected(id, selected, MAX_ANIMATIONS_IN_PICKER)
            if (!accepted) libraryFullEvents.trySend(Unit)
        }
    }

    fun setBuiltInAnimationSelected(type: AnimationType, selected: Boolean) {
        viewModelScope.launch {
            val accepted = builtInRepository.setSelected(type, selected, MAX_ANIMATIONS_IN_PICKER)
            if (!accepted) libraryFullEvents.trySend(Unit)
        }
    }

    /*
     * Die Obergrenze pruefte hier frueher ein eigenes `canSelectMore()`: erst beide Quellen
     * zaehlen, dann schreiben. Zwischen Zaehlen und Schreiben kann sich der Stand aendern - zwei
     * schnell hintereinander angetippte Kacheln sahen beide "noch Platz" und belegten denselben
     * letzten. Jetzt entscheidet die Datenbank in derselben Anweisung, in der sie schreibt, und
     * meldet zurueck, ob sie angenommen hat (siehe LibraryAnimationDao.selectIfWithinLimit).
     */

    /** Loest eine [AnimationChoice] in abspielbare Frames auf, z.B. fuer die Live-Vorschau. */
    suspend fun framesFor(choice: AnimationChoice): List<IntArray> = when (choice) {
        is AnimationChoice.BuiltIn -> ReminderAnimations.framesFor(choice.type)
        is AnimationChoice.Library -> libraryRepository.framesFor(choice.id) ?: emptyList()
    }

    fun addReminder(
        label: String,
        animationChoice: AnimationChoice,
        daysOfWeekMask: Int,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        intervalMinutes: Int,
        openDurationSeconds: Int = ReminderOpenDuration.DEFAULT_SECONDS,
        dailyGoal: Int = NO_GOAL
    ) {
        viewModelScope.launch {
            addReminderNow(
                label, animationChoice, daysOfWeekMask, startMinuteOfDay, endMinuteOfDay,
                intervalMinutes, openDurationSeconds, dailyGoal
            )
        }
    }

    /**
     * Mehrere Erinnerungen auf einen Schlag anlegen (z.B. KI-Import) - bewusst NACHEINANDER in
     * EINER Coroutine statt einmal [addReminder] pro Eintrag: dessen eigenes `viewModelScope.launch`
     * wuerde fuer jeden Aufruf eine eigene, parallel laufende Coroutine starten. Jeder Aufruf legt
     * eine Zeile an und plant ihren Alarm; parallel laufend schreiben mehrere davon gleichzeitig
     * in dieselbe Tabelle, und die Reihenfolge der entstehenden IDs waere nicht mehr die der
     * importierten Liste. Nacheinander ist hier also nicht Vorsicht, sondern die Zusage, dass am
     * Ende genau das dasteht, was der Nutzer herueberreicht hat.
     */
    fun addReminders(items: List<SanitizedReminder>) {
        viewModelScope.launch {
            items.forEach { item ->
                addReminderNow(
                    label = item.label,
                    animationChoice = item.animationChoice,
                    daysOfWeekMask = item.daysOfWeekMask,
                    startMinuteOfDay = item.startMinuteOfDay,
                    endMinuteOfDay = item.endMinuteOfDay,
                    intervalMinutes = item.intervalMinutes,
                    dailyGoal = item.dailyGoal
                )
            }
        }
    }

    private suspend fun addReminderNow(
        label: String,
        animationChoice: AnimationChoice,
        daysOfWeekMask: Int,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        intervalMinutes: Int,
        openDurationSeconds: Int = ReminderOpenDuration.DEFAULT_SECONDS,
        dailyGoal: Int = NO_GOAL
    ) {
        val (resolvedType, libraryAnimationId) = animationChoice.toStorage()
        val saved = repository.add(
            GlyphReminder(
                label = label,
                animationType = resolvedType,
                libraryAnimationId = libraryAnimationId,
                daysOfWeekMask = daysOfWeekMask,
                startMinuteOfDay = startMinuteOfDay,
                endMinuteOfDay = endMinuteOfDay,
                intervalMinutes = intervalMinutes,
                enabled = true,
                // Neue Erinnerungen gehoeren immer dem gerade gewaehlten Avatar.
                profileId = AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(getApplication())),
                // Nochmal geklemmt statt dem Dialog blind zu vertrauen (siehe
                // ReminderOpenDuration.coerceToInterval) - der KI-Import geht z.B. gar nicht
                // durch den Dialog, sondern direkt hierher.
                openDurationSeconds = ReminderOpenDuration.coerceToInterval(openDurationSeconds, intervalMinutes),
                dailyGoal = dailyGoal
            )
        )
        ReminderScheduler.schedule(getApplication(), saved)
    }

    /**
     * Uebernimmt den Erinnerungs-Satz eines anderen Avatars in den aktuellen. Jeder Avatar hat
     * seinen eigenen Satz - ohne diese Moeglichkeit muesste ein sorgfaeltig eingerichteter Satz
     * fuer jeden weiteren Avatar von Hand nachgebaut werden.
     */
    fun copyRemindersFrom(source: AvatarSpecies, replace: Boolean) {
        viewModelScope.launch {
            val target = AvatarSpeciesPrefs.get(getApplication())
            repository.copyProfile(
                fromProfileId = AvatarSpeciesPrefs.profileId(source),
                toProfileId = AvatarSpeciesPrefs.profileId(target),
                replace = replace
            )
            // Ueber rescheduleAll statt einzeln: beim Ersetzen muessen auch die Alarme der
            // geloeschten Erinnerungen weg, nicht nur die neuen dazukommen.
            ReminderScheduler.rescheduleAll(getApplication())
        }
    }

    /** Wie viele Erinnerungen die einzelnen Avatare haben - fuer die Auswahl beim Kopieren.
     *  Ohne die Spiel-Erinnerung, die hier nichts zu suchen hat (siehe [reminders]). */
    suspend fun reminderCountsBySpecies(): Map<AvatarSpecies, Int> {
        val all = repository.observeAll().first().filter { !it.isPlayMode }
        return AvatarSpecies.entries.associateWith { species ->
            all.count { it.profileId == AvatarSpeciesPrefs.profileId(species) }
        }
    }

    fun updateReminder(reminder: GlyphReminder) {
        viewModelScope.launch {
            // Klemmt bei jedem Speichern erneut (siehe addReminderNow) - heilt nebenbei auch
            // Erinnerungen, die noch von vor dieser Regel stammen, sobald sie naechstens
            // angefasst werden (auch ein blosses An-/Abschalten via setEnabled zaehlt).
            val coerced = reminder.copy(
                openDurationSeconds = ReminderOpenDuration.coerceToInterval(
                    reminder.openDurationSeconds,
                    reminder.intervalMinutes
                )
            )
            repository.update(coerced)
            ReminderScheduler.schedule(getApplication(), coerced)
        }
    }

    fun setEnabled(reminder: GlyphReminder, enabled: Boolean) {
        updateReminder(reminder.copy(enabled = enabled))
    }

    fun deleteReminder(reminder: GlyphReminder) {
        viewModelScope.launch {
            repository.delete(reminder)
            ReminderScheduler.cancel(getApplication(), reminder)
        }
    }

    companion object {
        const val MAX_ANIMATIONS_IN_PICKER = 16
    }
}
