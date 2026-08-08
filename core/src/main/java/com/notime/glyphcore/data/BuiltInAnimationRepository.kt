package com.notime.glyphcore.data

import kotlinx.coroutines.flow.Flow

class BuiltInAnimationRepository(private val dao: BuiltInAnimationSelectionDao) {

    fun observeAll(): Flow<List<BuiltInAnimationSelection>> = dao.observeAll()

    /** Alle 12 Typen starten ausgewaehlt (bisheriges Verhalten: immer im Picker sichtbar). */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(AnimationType.entries.map { BuiltInAnimationSelection(it.name, isSelected = true) })
    }

    suspend fun countSelected(): Int = dao.countSelected()

    suspend fun setSelected(type: AnimationType, selected: Boolean) = dao.setSelected(type.name, selected)
}
