package com.notime.glyphsim.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.BuiltInAnimationRepository
import com.notime.glyphcore.data.BuiltInAnimationSelection
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.GlyphReminderRepository
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.LibraryAnimationRepository
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphcore.reminder.ReminderPause
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.ReminderAnimations
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
     * Die Erinnerungen des Nutzers - **ein Satz, unabhaengig vom gewaehlten Wesen** (siehe
     * [RoutineOwner]).
     *
     * Hier stand bis Phase 4b ein `flatMapLatest` auf die Avatar-Auswahl: jeder Avatar hatte
     * seinen eigenen Satz, und ein Wechsel im Einstellungs-Dialog tauschte die Liste sofort aus.
     * Das war die sichtbarste Auswirkung von Problem 3 - eine Entscheidung ueber das Aussehen
     * liess die eigenen Erinnerungen verschwinden. Der Wechsel beruehrt diese Liste jetzt nicht
     * mehr, also braucht sie auch keine Umschaltung.
     */
    val reminders: StateFlow<List<GlyphReminder>> =
        repository.observeForProfile(RoutineOwner.current(application))
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

    private val _pausedUntil = MutableStateFlow(ReminderPause.pausedUntil(application))

    /** Bis wann pausiert ist, oder null. Siehe [pauseReminders]. */
    val pausedUntil: StateFlow<Long?> = _pausedUntil.asStateFlow()

    /**
     * Der Zustand des Bildschirms als EIN unveraenderlicher Wert - siehe [ReminderUiState] fuer
     * die Begruendung.
     *
     * Die vier Fluesse darueber bleiben vorerst bestehen: der Bibliotheks-Bildschirm und der
     * Assistent greifen noch einzeln darauf zu. Sie werden mit deren Umbau eingezogen; bis dahin
     * speisen sich beide Wege aus denselben Quellen, koennen also nicht auseinanderlaufen.
     *
     * `combine` statt vier getrennter Beobachtungen: der Bildschirm bekommt damit immer einen in
     * sich stimmigen Stand und nie eine Mischung aus altem und neuem.
     */
    val uiState: StateFlow<ReminderUiState> =
        combine(
            reminders,
            activeSpecies,
            libraryAnimations,
            builtInSelections,
            pausedUntil
        ) { reminders, species, library, builtIn, paused ->
            ReminderUiState(
                reminders = reminders,
                species = species,
                libraryAnimations = library,
                builtInSelections = builtIn,
                loaded = true,
                pausedUntil = paused
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderUiState())

    /**
     * Pausiert alle Erinnerungen fuer die gewaehlte Dauer.
     *
     * Das Neuplanen danach ist kein Beiwerk, sondern der eigentliche Wirkmechanismus: die Pause
     * verhindert nur, dass NEUE Alarme gestellt werden. Bereits gesetzte blieben sonst stehen und
     * feuerten waehrend der Pause weiter. `rescheduleAll` bestellt zuerst alles ab und stellt dann
     * nur wieder ein, was erlaubt ist - waehrend einer Pause also nichts.
     */
    fun pauseReminders(duration: ReminderPause.Duration) {
        viewModelScope.launch {
            val application = getApplication<Application>()
            val until = ReminderPause.endOf(duration, System.currentTimeMillis())
            ReminderPause.pauseUntil(application, until)
            _pausedUntil.value = until
            ReminderScheduler.rescheduleAll(application)
        }
    }

    /** Hebt eine laufende Pause sofort auf und plant alles wieder ein. */
    fun resumeReminders() {
        viewModelScope.launch {
            val application = getApplication<Application>()
            ReminderPause.resume(application)
            _pausedUntil.value = null
            ReminderScheduler.rescheduleAll(application)
        }
    }

    /**
     * Frischt den Pausen-Zustand auf - beim Zurueckkommen auf den Bildschirm aufzurufen.
     *
     * Eine Pause endet durch Zeitablauf, nicht durch ein Ereignis. Ohne dieses Nachsehen stuende
     * der Hinweis noch da, obwohl die Pause laengst vorbei ist.
     */
    fun refreshPauseState() {
        _pausedUntil.value = ReminderPause.pausedUntil(getApplication())
    }

    private val libraryFullEvents = Channel<Unit>(Channel.CONFLATED)

    /** Feuert, wenn eine Auswahl am Picker-Limit ([MAX_ANIMATIONS_IN_PICKER]) abgelehnt wurde (fuer eine Snackbar-Meldung). */
    val libraryFullEvent: Flow<Unit> = libraryFullEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            libraryRepository.seedOrRefresh()
            builtInRepository.seedIfEmpty()
            // Voreingestellte Erinnerungen beim allerersten Start anlegen (siehe seedIfEmpty) und
            // danach sicherstellen, dass wirklich nur die zulaessigen Alarme stehen.
            repository.seedIfEmpty(RoutineOwner.current(getApplication()))
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
                // Neue Erinnerungen gehoeren dem Nutzer - siehe RoutineOwner.
                profileId = RoutineOwner.current(getApplication()),
                // Nochmal geklemmt statt dem Dialog blind zu vertrauen (siehe
                // ReminderOpenDuration.coerceToInterval) - der KI-Import geht z.B. gar nicht
                // durch den Dialog, sondern direkt hierher.
                openDurationSeconds = ReminderOpenDuration.coerceToInterval(openDurationSeconds, intervalMinutes),
                dailyGoal = dailyGoal
            )
        )
        ReminderScheduler.schedule(getApplication(), saved)
    }

    // "Satz eines anderen Avatars uebernehmen" gab es hier bis Phase 4b (copyRemindersFrom /
    // reminderCountsBySpecies, dazu ein Auswahl-Dialog im ReminderScreen). Die Funktion hat keinen
    // Gegenstand mehr: Es gibt nur noch einen Satz, also nichts, wovon oder wohin man kopieren
    // koennte. Sie war ohnehin nur das Pflaster fuer die Trennung, die es jetzt nicht mehr gibt.
    //
    // GlyphReminderRepository.copyProfile bleibt bestehen - es ist das Werkzeug, mit dem die
    // Zusammenlegung selbst arbeitet (siehe AppDatabaseMigrations).

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
