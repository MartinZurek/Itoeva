package com.notime.glyphcore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eine Animation aus der erweiterbaren Bibliothek, zusaetzlich zu den 12 fest
 * eingebauten [AnimationType]s. [framesData] ist eine mit [FrameCodec] kodierte
 * Punktliste je Frame - Animationen leben damit als Daten in der Datenbank statt als
 * Kotlin-Code, neue lassen sich also rein per DB-Eintrag ergaenzen (Grundlage fuer ein
 * spaeteres Kauf-/Erweiterungspaket). [isSelected] steuert, ob die Animation aktuell
 * im Animations-Picker beim Anlegen eines Reminders auftaucht (siehe LibraryScreen.kt).
 */
@Entity(tableName = "library_animations")
data class LibraryAnimation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val emoji: String,
    val framesData: String,
    val isSelected: Boolean = false,
    val sortOrder: Int = 0
)
