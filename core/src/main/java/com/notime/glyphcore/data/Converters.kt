package com.notime.glyphcore.data

import androidx.room.TypeConverter

/**
 * [GlyphReminder.profileId] braucht bewusst keinen Converter: es ist ein reiner String, damit
 * der gemeinsame Kern keinen Avatar-/Profil-Typ der Apps kennen muss (siehe
 * [com.notime.glyphcore.reminder.ActiveProfilePrefs]).
 */
class Converters {
    @TypeConverter
    fun fromAnimationType(value: AnimationType): String = value.name

    @TypeConverter
    fun toAnimationType(value: String): AnimationType = AnimationType.valueOf(value)
}
