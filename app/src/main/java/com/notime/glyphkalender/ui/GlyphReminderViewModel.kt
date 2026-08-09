package com.notime.glyphkalender.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphkalender.data.AppDatabase
import com.notime.glyphcore.data.BuiltInAnimationRepository
import com.notime.glyphcore.data.BuiltInAnimationSelection
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.GlyphReminderRepository
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.LibraryAnimationRepository
import com.notime.glyphkalender.glyph.ReminderAnimations
import com.notime.glyphcore.reminder.ActiveProfilePrefs
import com.notime.glyphcore.reminder.ReminderScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GlyphReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GlyphReminderRepository(AppDatabase.getInstance(application).glyphReminderDao())
    private val libraryRepository =
        LibraryAnimationRepository(AppDatabase.getInstance(application).libraryAnimationDao())
    private val builtInRepository =
        BuiltInAnimationRepository(AppDatabase.getInstance(application).builtInAnimationSelectionDao())

    val reminders: StateFlow<List<GlyphReminder>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            // Diese App kennt keine Avatare/Profile - alle Erinnerungen liegen im Standardprofil
            // (siehe GlyphKalenderApp und core/reminder/ActiveProfilePrefs.kt).
            repository.seedIfEmpty(ActiveProfilePrefs.DEFAULT_PROFILE_ID)
                .forEach { ReminderScheduler.schedule(getApplication(), it) }
        }
    }

    fun setLibraryAnimationSelected(id: Long, selected: Boolean) {
        viewModelScope.launch {
            if (selected && !canSelectMore()) {
                libraryFullEvents.trySend(Unit)
                return@launch
            }
            libraryRepository.setSelected(id, selected)
        }
    }

    fun setBuiltInAnimationSelected(type: AnimationType, selected: Boolean) {
        viewModelScope.launch {
            if (selected && !canSelectMore()) {
                libraryFullEvents.trySend(Unit)
                return@launch
            }
            builtInRepository.setSelected(type, selected)
        }
    }

    /**
     * Gesamt-Obergrenze fuer den Animations-Picker: fest eingebaute Typen und Bibliotheks-
     * Animationen teilen sich denselben Topf aus [MAX_ANIMATIONS_IN_PICKER] Plaetzen - beide
     * Quellen sind gleichermassen an/abwaehlbar (siehe LibraryScreen.kt).
     */
    private suspend fun canSelectMore(): Boolean =
        builtInRepository.countSelected() + libraryRepository.countSelected() < MAX_ANIMATIONS_IN_PICKER

    /** Loest eine [AnimationChoice] in abspielbare Frames auf, fuer die On-Screen-Vorschau. */
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
        intervalMinutes: Int
    ) {
        viewModelScope.launch {
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
                    enabled = true
                )
            )
            ReminderScheduler.schedule(getApplication(), saved)
        }
    }

    fun updateReminder(reminder: GlyphReminder) {
        viewModelScope.launch {
            repository.update(reminder)
            ReminderScheduler.schedule(getApplication(), reminder)
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

    fun canScheduleExactAlarms(): Boolean = ReminderScheduler.canScheduleExact(getApplication())

    companion object {
        const val MAX_ANIMATIONS_IN_PICKER = 16
    }
}
