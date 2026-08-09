package com.notime.glyphcore.data

import kotlinx.coroutines.flow.Flow

class BuiltInAnimationRepository(private val dao: BuiltInAnimationSelectionDao) {

    fun observeAll(): Flow<List<BuiltInAnimationSelection>> = dao.observeAll()

    /** Alle 12 Typen starten ausgewaehlt (bisheriges Verhalten: immer im Picker sichtbar). */
    suspend fun seedIfEmpty() {
        dao.seedIfEmpty(AnimationType.entries.map { BuiltInAnimationSelection(it.name, isSelected = true) })
    }

    suspend fun countSelected(): Int = dao.countSelected()

    /**
     * Waehlt aus bzw. ab. Beim AUSWAEHLEN entscheidet die Datenbank in derselben Anweisung, ob
     * noch Platz im Picker ist - `false` heisst abgelehnt, weil [limit] erreicht war. Abwaehlen
     * gelingt immer.
     */
    suspend fun setSelected(type: AnimationType, selected: Boolean, limit: Int): Boolean {
        if (!selected) {
            dao.setSelected(type.name, false)
            return true
        }
        if (dao.selectIfWithinLimit(type.name, limit) > 0) return true
        // War schon ausgewaehlt - siehe LibraryAnimationRepository.setSelected fuer die Begruendung.
        return dao.isSelected(type.name) == true
    }
}
