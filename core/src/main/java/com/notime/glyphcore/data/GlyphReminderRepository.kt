package com.notime.glyphcore.data

import kotlinx.coroutines.flow.Flow

class GlyphReminderRepository(private val dao: GlyphReminderDao) {

    fun observeAll(): Flow<List<GlyphReminder>> = dao.observeAll()

    /** Nur die Erinnerungen eines Profils (siehe [GlyphReminder.profileId]). */
    fun observeForProfile(profileId: String): Flow<List<GlyphReminder>> =
        dao.observeForProfile(profileId)

    /**
     * Der Engpass, durch den jede neue Erinnerung muss - und damit die Stelle, an der sich
     * zusichern laesst, dass nichts Ungueltiges in die Datenbank kommt.
     *
     * Siehe [ReminderValidation] fuer die Regeln und [InvalidReminderException] dafuer, warum
     * hier abgebrochen und nicht zurechtgebogen wird.
     */
    suspend fun add(reminder: GlyphReminder): GlyphReminder {
        val id = dao.insert(reminder.validated())
        return reminder.copy(id = id)
    }

    suspend fun update(reminder: GlyphReminder) = dao.update(reminder.validated())

    private fun GlyphReminder.validated(): GlyphReminder {
        val problems = ReminderValidation.validate(this)
        if (problems.isNotEmpty()) throw InvalidReminderException(problems)
        return this
    }

    suspend fun delete(reminder: GlyphReminder) = dao.delete(reminder)

    suspend fun getEnabled(): List<GlyphReminder> = dao.getEnabled()

    suspend fun getById(id: Long): GlyphReminder? = dao.getById(id)

    /**
     * Legt [DefaultReminders.ALL] fuer [profileId] an, aber nur solange dieses Profil noch gar
     * keine Erinnerungen hat. Bewusst pro Profil statt global: ein frisch ausgewaehltes Profil
     * startet damit nicht mit einer leeren Liste, sondern mit denselben Voreinstellungen wie
     * das erste.
     */
    suspend fun seedIfEmpty(profileId: String): List<GlyphReminder> =
        // Die Vorgaben gehen an der Pruefung in add() vorbei, weil sie in einer Transaktion im
        // DAO angelegt werden - deshalb hier ausdruecklich. Ein Tippfehler in DefaultReminders
        // laege sonst in jedem neu angelegten Profil und in keinem Test.
        dao.seedIfEmpty(profileId, DefaultReminders.ALL.map { it.copy(profileId = profileId).validated() })

    /**
     * Kopiert alle Erinnerungen von [fromProfileId] nach [toProfileId] und gibt die neuen
     * Kopien zurueck (der Aufrufer muss sie noch einplanen).
     *
     * [replace] = true loescht vorher die bestehenden Erinnerungen des Zielprofils, sonst werden
     * die Kopien zusaetzlich angelegt. Ohne diese Wahl waere die Funktion in beide Richtungen
     * unangenehm: stilles Ersetzen verliert eingerichtete Erinnerungen, stilles Anhaengen
     * erzeugt bei wiederholtem Kopieren Dubletten.
     *
     * `id` wird bewusst zurueckgesetzt (autoGenerate), sonst wuerde [add] die Quell-Eintraege
     * ueberschreiben statt neue anzulegen; `nextTriggerEpochMillis` ebenso, weil der geplante
     * Zeitpunkt der Quelle fuer die Kopie nichts bedeutet.
     *
     * Erinnerungen des Spielmodus ([GlyphReminder.isPlayMode]) bleiben in BEIDE Richtungen aussen
     * vor: Kopiert wird nur, was der Nutzer selbst eingerichtet hat. Wuerde die Spiel-Zeile
     * mitkopiert, haette das Zielprofil zwei davon - und beide wuerfelten unabhaengig voneinander
     * weiter. Beim Ersetzen darf sie umgekehrt nicht geloescht werden, sonst verloere der
     * Ziel-Avatar seinen laufenden Spielstand-Anker.
     */
    suspend fun copyProfile(
        fromProfileId: String,
        toProfileId: String,
        replace: Boolean
    ): List<GlyphReminder> = dao.copyProfile(fromProfileId, toProfileId, replace)

    /**
     * Stellt die Play-Mode-Erinnerung eines Profils sicher (anlegen oder wieder aktivieren) und
     * gibt sie zurueck. Hoechstens eine je Profil - die Begruendung und warum das eine Transaktion
     * sein muss, steht bei [GlyphReminderDao.insertPlayReminderIfAbsent].
     */
    suspend fun ensurePlayReminder(profileId: String, template: GlyphReminder): GlyphReminder =
        dao.insertPlayReminderIfAbsent(
            profileId,
            template.copy(profileId = profileId, isPlayMode = true).validated()
        )
}
