package com.notime.glyphcore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * On/Off-Status eines der 12 fest in [ReminderAnimations][com.notime.glyphcore.matrix.ReminderAnimations]
 * verdrahteten [AnimationType]s im Animations-Picker. Die eigentlichen Frames bleiben Kotlin-Code
 * (feine Helligkeits-Ueberblendungen, siehe README) - nur diese Sichtbarkeits-Auswahl wandert in die
 * Datenbank, analog zu [LibraryAnimation.isSelected], damit die 12 Presets genauso frei
 * konfigurierbar sind wie Bibliotheks-Animationen.
 */
@Entity(tableName = "builtin_animation_selection")
data class BuiltInAnimationSelection(
    @PrimaryKey val animationType: String,
    val isSelected: Boolean = true
)
