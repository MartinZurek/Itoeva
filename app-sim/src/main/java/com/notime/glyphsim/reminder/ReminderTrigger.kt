package com.notime.glyphsim.reminder

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimationRepository
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphcore.reminder.PlayModeState
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.AvatarFeedEvent
import com.notime.glyphsim.matrix.ReminderAnimationBus
import com.notime.glyphsim.matrix.ReminderAnimationEvent
import com.notime.glyphsim.matrix.ReminderAnimations
import com.notime.glyphsim.ui.AvatarSpeciesPrefs
import com.notime.glyphsim.widget.GlyphClockWidgetProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Das Auslösen einer Erinnerung - Animation zeigen, Auftreten protokollieren, naechsten Slot
 * planen. Frueher stand das komplett in [ReminderAlarmReceiver]; seit es zwei Wege dorthin gibt,
 * liegt es hier gemeinsam.
 *
 * **Warum zwei Wege.** `AlarmManager` ist dafuer gebaut, ein *schlafendes* Geraet zu wecken, und
 * bezahlt das mit Doze, Drosselung und App-Standby-Buckets: ohne die Exact-Alarm-Berechtigung
 * (die diese App bewusst nicht mehr fuehrt, siehe Manifest) laesst das System pro App nur grob
 * einen `setAndAllowWhileIdle`-Alarm alle 9-15 Minuten durch. Fuer eine App, deren Erinnerungen
 * ohnehin nur bei eingeschaltetem Bildschirm sichtbar sind, ist das das falsche Werkzeug.
 *
 * Solange die App im Vordergrund ist, treibt deshalb [firePending] die Erinnerungen - eine
 * schlichte Schleife im laufenden Prozess, die weder Doze noch Drosselung kennt und die Minute
 * trifft. Der Alarmweg ([fireFromAlarm]) bleibt fuer das Home-Screen-Widget zustaendig, das auch
 * ohne offene App sichtbar ist.
 *
 * **Warum sich beide nicht ins Gehege kommen.** `glyph_reminders.nextTriggerEpochMillis` ist der
 * gemeinsame Zeiger auf den naechsten faelligen Slot. Wer zuerst ausloest, ruft am Ende
 * [ReminderScheduler.schedule] - das storniert den stehenden Alarm und setzt den Zeiger auf den
 * naechsten Slot. Der jeweils andere Weg sieht dann einen Zeiger in der Zukunft und zeigt nichts.
 * Die [mutex] serialisiert das zusaetzlich, damit beide nicht gleichzeitig hineinlaufen.
 */
object ReminderTrigger {

    private const val TAG = "ReminderTrigger"

    /**
     * Wie lange ein verpasster Slot noch nachgeholt werden darf - knapp ueber eine Minute, also
     * gerade so viel, dass ein um den Minutenwechsel herum verpasster Slot noch durchkommt.
     *
     * Frueher standen hier fuenf Minuten. Das war zu grosszuegig: wer das Widget waehrend einer
     * Erinnerung antippte und dadurch die App oeffnete, bekam sofort eine *zweite*, laengst
     * abgelaufene Erinnerung obendrauf - die dann auch noch die erste von der Anzeige verdraengte
     * und den Fuetter-Vorgang ins Leere laufen liess. Ein Slot, der Minuten her ist, ist vorbei;
     * ihn nachzureichen war nie der Sinn des Nachholens.
     */
    private const val CATCH_UP_WINDOW_MS = 90_000L

    /**
     * Toleranz beim Faelligkeits-Vergleich im Alarmweg. Ein Alarm kann ein paar Millisekunden vor
     * seinem Slot eintreffen; ohne Spielraum wuerde er sich dann selbst fuer "noch nicht faellig"
     * halten und stumm bleiben.
     */
    private const val DUE_TOLERANCE_MS = 2_000L

    /**
     * Obergrenze, wie lange eine offene Erinnerung den Alarmweg blockieren darf. Entspricht der
     * Deckelung der Widget-Wiedergabe (`MAX_WIDGET_DURATION_SECONDS`): laenger als das ist im
     * Widget ohnehin nichts zu sehen, also gibt es auch nichts mehr zu schuetzen.
     */
    private const val ALARM_BLOCK_CAP_SECONDS = 60

    /**
     * Spielraum der Mindestabstands-Regel (siehe [firedTooRecently]). Deckt den kleinen
     * Weck-Aufschlag des Takts und Rundungen ab, ohne dass sich zwei Ausloesungen spuerbar
     * naeher kommen koennten als das eingestellte Intervall.
     */
    private const val MIN_GAP_TOLERANCE_MS = 20_000L

    private val mutex = Mutex()

    /**
     * Der Alarmweg: [ReminderAlarmReceiver] hat einen Alarm fuer [reminderId] bekommen. Plant den
     * naechsten Slot in jedem Fall neu - wuerde hier abgebrochen, risse die Alarmkette ab und die
     * Erinnerung waere dauerhaft tot.
     */
    suspend fun fireFromAlarm(context: Context, reminderId: Long) {
        mutex.withLock {
            val dao = AppDatabase.getInstance(context).glyphReminderDao()
            val reminder = dao.getById(reminderId)
            if (reminder == null) {
                Log.d(TAG, "Erinnerung id=$reminderId nicht mehr vorhanden, ignoriere Alarm")
                return
            }
            // Verspaeteter Alarm eines inzwischen abgewaehlten Avatars: still verwerfen und auch
            // NICHT neu planen - sonst haelt sich der Alarm eines nicht mehr aktiven Avatars
            // selbst am Leben (siehe GlyphReminder.avatarSpecies).
            val activeProfile = AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(context))
            if (reminder.profileId != activeProfile) {
                Log.d(TAG, "Erinnerung gehoert zu Profil ${reminder.profileId}, aktiv ist $activeProfile - ignoriere")
                ReminderScheduler.cancel(context, reminder)
                return
            }
            // Verspaeteter Alarm aus dem jeweils anderen Modus (siehe PlayModeState): Wer den
            // Spielmodus einschaltet, waehrend noch ein Alarm einer echten Erinnerung steht, soll
            // diese Erinnerung nicht trotzdem noch bekommen - und umgekehrt. Wie beim
            // Profilwechsel abbestellen statt neu planen, sonst hielte sich der Alarm des gerade
            // ruhenden Satzes selbst am Leben.
            if (!PlayModeState.matchesCurrentMode(context, reminder)) {
                Log.d(TAG, "\"${reminder.label}\" gehoert nicht zum aktiven Modus - ignoriere")
                ReminderScheduler.cancel(context, reminder)
                return
            }

            // Der Zeiger steht schon auf einem spaeteren Slot als dem, fuer den dieser Alarm
            // gedacht war - der In-Prozess-Takt war also schneller. Verglichen wird bewusst
            // gegen den Slot und NICHT gegen die aktuelle Zeit: ein ungefaehrer Alarm kann
            // beliebig verspaetet eintreffen, dann laege der bereits weitergerueckte Zeiger in
            // der Vergangenheit und die Pruefung liesse dieselbe Ausloesung ein zweites Mal
            // durch. Da der Alarm seinen Slot nicht mitbringt, dient die letzte Ausloesung als
            // Bezug - die Mindestabstands-Regel in [show] faengt den Rest ohnehin ab.
            val pointer = reminder.nextTriggerEpochMillis
            val lastShown = AppDatabase.getInstance(context).avatarFeedEventDao()
                .latestOccurrenceMillis(reminder.id)
            val alreadyShown = pointer != null && lastShown != null &&
                pointer > lastShown + DUE_TOLERANCE_MS
            when {
                !reminder.enabled -> Unit
                alreadyShown ->
                    // Zweimal dasselbe Auftreten zu zeigen (und zu zaehlen) waere ein
                    // Doppeleintrag im Pflegebuch.
                    Log.d(TAG, "\"${reminder.label}\" wurde bereits in der App gezeigt, Alarm verworfen")
                !isScreenOn(context) ->
                    Log.d(TAG, "Bildschirm aus - \"${reminder.label}\" wird nicht gezeigt und nicht gezaehlt")
                // Dieselbe Regel wie im Takt: laeuft noch eine unbeantwortete Erinnerung, darf
                // keine zweite darueber. Ohne das blieb ein Loch offen - der Takt ueberspringt
                // eine faellige Erinnerung, waehrend eine andere offen ist, ruehrt ihren Zeiger
                // aber nicht an; ihr Alarm steht also weiter und kam hier an. Genau der Fall
                // "Widget antippen, fuettern wollen, und die Anzeige springt weg".
                //
                // Anders als im Takt aber GEDECKELT: dort ist der Nutzer anwesend und kann eine
                // "Nonstop"-Erinnerung durch Fuettern aufloesen. Hier kann die App geschlossen
                // sein - ein unbegrenztes Blockieren wuerde alle weiteren Erinnerungen dauerhaft
                // verschlucken. Nach der Deckelung laeuft es weiter, egal was offen ist.
                hasOpenUnfedReminder(context, activeProfile, ALARM_BLOCK_CAP_SECONDS) ->
                    Log.d(TAG, "Andere Erinnerung noch offen - \"${reminder.label}\" uebersprungen")
                else -> show(context, reminder)
            }
            ReminderScheduler.schedule(context, reminder)
        }
    }

    /**
     * Der In-Prozess-Weg: prueft, ob eine Erinnerung des aktiven Profils faellig ist, und zeigt
     * sie. Wird aus MainActivity aufgerufen, solange die App sichtbar ist.
     *
     * Nebeneffekt mit Absicht: Erinnerungen ohne stehenden Alarm (Zeiger `null`) werden neu
     * geplant. Damit heilt allein das Oeffnen der App eine abgerissene Alarmkette - dieselbe
     * Aufgabe, die sonst nur der Watchdog erledigt, nur sofort.
     */
    suspend fun firePending(context: Context) {
        mutex.withLock {
            val profileId = AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(context))
            val dao = AppDatabase.getInstance(context).glyphReminderDao()
            val now = System.currentTimeMillis()

            // Nur der Satz des gerade laufenden Modus (siehe PlayModeState) - im Spielmodus
            // ausschliesslich die gewuerfelte Erinnerung, sonst ausschliesslich die echten.
            val all = dao.getEnabledForProfile(profileId)
                .filter { PlayModeState.matchesCurrentMode(context, it) }

            // Erinnerungen ohne stehenden Alarm zuerst versorgen - siehe Doku oben.
            //
            // [canEverSchedule] haelt die aussichtslosen Faelle heraus: eine Erinnerung ohne
            // ausgewaehlten Wochentag bekommt nie einen Slot, ihr Zeiger bleibt also dauerhaft
            // null. Ohne diese Pruefung versuchte der Takt sie jede Minute erneut - und jeder
            // Versuch kostete zwei Schreibvorgaenge in der Datenbank, weil ReminderScheduler
            // zuerst storniert und dabei den Zeiger setzt. Folgenlos, aber unnoetig.
            all.filter { it.nextTriggerEpochMillis == null && canEverSchedule(it) }
                .forEach { ReminderScheduler.schedule(context, it) }

            // Laeuft noch eine unbeantwortete Erinnerung, wird KEINE weitere gezeigt - und ihr
            // Zeiger bleibt bewusst stehen, sodass sie es beim naechsten Minutenwechsel erneut
            // versucht. Damit reihen sich mehrere faellige Erinnerungen von selbst auf, statt
            // einander zu verdraengen.
            //
            // Ohne das war der Weg "Widget zeigt Erinnerung -> antippen -> in der App fuettern"
            // kaputt: Das Oeffnen der App liess den Takt sofort loslaufen, der schob eine eigene
            // Erinnerung auf den Bus, und die ueberschrieb im HomeScreen genau die Erinnerung,
            // die man gerade fuettern wollte (siehe OpenReminderLookup, das sie aus der Datenbank
            // wiederherstellt). Das Fuettern ging dann auf ein anderes Auftreten - oder ins Leere.
            if (hasOpenUnfedReminder(context, profileId)) return


            // Fallen mehrere auf denselben Moment, entscheidet das Intervall die Reihenfolge:
            // kurze zuerst, laengere direkt danach. Frueher schob CollisionPrefs.SPREAD
            // kollidierende Erinnerungen zeitlich auseinander - das verschob sie aber weg von
            // dem Zeitpunkt, fuer den sie gestellt waren, und machte aus einer 5-Minuten-
            // Erinnerung faktisch eine 8-Minuten-Erinnerung. Die Reihenfolge loest dasselbe
            // Problem, ohne einen einzigen Slot zu verschieben: eine Erinnerung, die alle fuenf
            // Minuten kommt, hat weniger Zeit uebrig als eine stuendliche und geht deshalb
            // zuerst. Bei gleichem Intervall entscheidet die ID, damit die Reihenfolge ueber
            // Neustarts hinweg stabil bleibt und nicht von der Sortierung der Abfrage abhaengt.
            all.filter { reminder ->
                val pointer = reminder.nextTriggerEpochMillis
                pointer != null && pointer <= now
            }.sortedWith(compareBy({ it.intervalMinutes }, { it.id }))
                .forEach { reminder ->
                    val pointer = reminder.nextTriggerEpochMillis ?: return@forEach
                    if (now - pointer <= CATCH_UP_WINDOW_MS) {
                        show(context, reminder)
                    } else {
                        Log.d(TAG, "Slot von \"${reminder.label}\" ist zu lange her, nur neu geplant")
                    }
                    ReminderScheduler.schedule(context, reminder)
                }
        }
    }

    /**
     * Ob diese Erinnerung ueberhaupt je einen Slot bekommen kann. Spiegelt die Vorbedingung von
     * [ReminderScheduler.nextTrigger], das ohne Intervall oder ohne ausgewaehlten Wochentag
     * grundsaetzlich null liefert - bewusst als billige Pruefung statt eines Probelaufs, der
     * dafuer ueber zehn Tage und alle Slots iterieren muesste.
     */
    private fun canEverSchedule(reminder: GlyphReminder): Boolean =
        reminder.intervalMinutes > 0 && reminder.daysOfWeekMask != 0

    /**
     * Ob gerade noch eine Erinnerung offen ist, auf die niemand reagiert hat - dieselbe Frage,
     * die `OpenReminderLookup` beim Oeffnen der App stellt, nur ohne die Frames dazu aufzuloesen.
     *
     * "Nonstop" ([ReminderOpenDuration.UNTIL_FED]) gilt so lange als offen, bis gefuettert wird.
     * Dass dadurch nachfolgende Erinnerungen warten, ist keine Panne, sondern die Zusage dieser
     * Einstellung: sie bleibt stehen, bis man sich um sie gekuemmert hat.
     */
    private suspend fun hasOpenUnfedReminder(
        context: Context,
        profileId: String,
        capSeconds: Int? = null
    ): Boolean {
        val db = AppDatabase.getInstance(context)
        val occurrence = db.avatarFeedEventDao().latestUnfed(profileId) ?: return false
        val reminder = db.glyphReminderDao().getById(occurrence.reminderId) ?: return false
        if (!reminder.enabled) return false

        val openSeconds = if (reminder.openDurationSeconds == ReminderOpenDuration.UNTIL_FED) {
            // Ohne Deckel gilt "Nonstop" als dauerhaft offen; mit Deckel endet die Sperre nach
            // dessen Ablauf.
            capSeconds ?: return true
        } else {
            capSeconds?.let { minOf(reminder.openDurationSeconds, it) } ?: reminder.openDurationSeconds
        }
        val elapsed = System.currentTimeMillis() - occurrence.epochMillis
        return elapsed >= 0 && elapsed < openSeconds * 1000L
    }

    /**
     * Die harte Zusage der App: **eine Erinnerung erscheint nie haeufiger als ihr Intervall.**
     *
     * Bewusst als eigene Sperre unmittelbar vor dem Anzeigen und nicht als Korrektur an einem
     * einzelnen Ausloese-Weg. Es gibt zwei davon (Alarm und In-Prozess-Takt), dazu Neuplanungen
     * aus Aktivitaets-Start, Watchdog und Profilwechsel - jeder einzelne Pfad korrekt zu halten
     * ist eine Zusicherung ueber viele Stellen hinweg, die beim naechsten Umbau wieder kippen
     * kann. Diese Sperre steht an der einen Stelle, durch die alle muessen.
     *
     * Konkreter Anlass: Die vorherige Doppel-Erkennung verglich den Zeiger der Erinnerung gegen
     * die AKTUELLE Zeit. Traf ein Alarm verspaetet ein - und ungefaehre Alarme tun genau das -,
     * lag der bereits weitergerueckte Zeiger in der Vergangenheit, die Pruefung hielt die
     * Ausloesung faelschlich fuer neu und zeigte denselben Slot ein zweites Mal. Aus fuenf
     * Minuten wurden so zwei.
     *
     * [MIN_GAP_TOLERANCE_MS] laesst eine Ausloesung knapp vor dem rechnerischen Slot durch (der
     * Takt weckt mit einem kleinen Sicherheitsaufschlag, siehe ClockTick), ohne das Intervall
     * nennenswert aufzuweichen.
     */
    private suspend fun firedTooRecently(context: Context, reminder: GlyphReminder): Boolean {
        val last = AppDatabase.getInstance(context).avatarFeedEventDao()
            .latestOccurrenceMillis(reminder.id) ?: return false
        val minGapMillis = reminder.intervalMinutes * 60_000L - MIN_GAP_TOLERANCE_MS
        if (minGapMillis <= 0) return false
        val since = System.currentTimeMillis() - last
        if (since >= minGapMillis) return false
        Log.d(TAG, "\"${reminder.label}\" lief erst vor ${since / 1000}s - Intervall noch nicht um, nicht gezeigt")
        return true
    }

    /**
     * Zeigt die Erinnerung auf Widget und - ueber [ReminderAnimationBus] - in allen gerade
     * sichtbaren In-App-Ansichten, und haelt das Auftreten fest.
     */
    private suspend fun show(context: Context, reminder: GlyphReminder) {
        if (firedTooRecently(context, reminder)) return
        val resolved = resolveFrames(context, reminder)
        if (resolved.frames.isEmpty()) return

        val animationType = if (reminder.libraryAnimationId == null) reminder.animationType else null
        // Die Ausloesung wird festgehalten, unabhaengig davon, ob jemand reagiert - erst dadurch
        // laesst sich Uebergangenes ueberhaupt zaehlen (siehe AvatarFeedEvent.fedAtMillis). Die
        // Zeilen-ID wandert mit dem Ereignis mit, damit eine spaetere Reaktion genau dieses
        // Auftreten markiert und nicht irgendeines derselben Erinnerung.
        val occurrenceId = AppDatabase.getInstance(context)
            .avatarFeedEventDao()
            .insert(
                AvatarFeedEvent(
                    reminderId = reminder.id,
                    animationType = animationType,
                    epochMillis = System.currentTimeMillis(),
                    profileId = reminder.profileId,
                    libraryAnimationLabel = resolved.libraryAnimationLabel,
                    isPlayMode = reminder.isPlayMode
                )
            )
        GlyphClockWidgetProvider.playReminderAnimation(
            context,
            resolved.frames,
            reminder.openDurationSeconds
        )
        ReminderAnimationBus.emit(
            ReminderAnimationEvent(
                reminder.id,
                occurrenceId,
                animationType,
                resolved.libraryAnimationLabel,
                resolved.frames,
                reminder.openDurationSeconds
            )
        )
        Log.d(TAG, "\"${reminder.label}\" (id=${reminder.id}) gezeigt")
    }

    /**
     * Ob der Bildschirm gerade an ist. `isInteractive` und nicht das veraltete `isScreenOn`:
     * Letzteres meldete auch bei reinem Ambient-/Always-on-Zustand true, in dem die App gar
     * nicht gezeichnet wird.
     */
    private fun isScreenOn(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isInteractive ?: true

    /** [libraryAnimationLabel] ist nur bei Bibliotheks-Animationen gesetzt - AvatarAnimations
     *  nutzt ihn, um vereinzelten Bibliotheks-Animationen (z.B. "Rocket") eine eigene, individuelle
     *  Avatar-Reaktion zu geben statt der generischen Fallback-Reaktion. */
    private data class ResolvedAnimation(val frames: List<IntArray>, val libraryAnimationLabel: String?)

    private suspend fun resolveFrames(context: Context, reminder: GlyphReminder): ResolvedAnimation {
        val libraryId = reminder.libraryAnimationId
        if (libraryId != null) {
            val dao = AppDatabase.getInstance(context).libraryAnimationDao()
            val frames = LibraryAnimationRepository(dao).framesFor(libraryId)
            if (frames != null) {
                return ResolvedAnimation(frames, dao.getById(libraryId)?.label)
            }
            Log.w(TAG, "Bibliotheks-Animation id=$libraryId nicht mehr vorhanden, falle auf ${reminder.animationType} zurueck")
        }
        return ResolvedAnimation(ReminderAnimations.framesFor(reminder.animationType), null)
    }
}
