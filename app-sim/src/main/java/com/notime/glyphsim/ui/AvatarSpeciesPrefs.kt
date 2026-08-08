package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.reminder.ActiveProfilePrefs
import com.notime.glyphsim.matrix.AvatarSpecies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistiert den vom Nutzer gewaehlten Avatar (siehe [AvatarSpecies]), Standard PUFFLING.
 *
 * Zusaetzlich als [selected]-Flow beobachtbar: der Avatar bestimmt inzwischen nicht mehr nur
 * das Aussehen, sondern auch WELCHE Erinnerungen gelten - jeder Avatar hat seinen eigenen Satz.
 * Die Erinnerungs-Liste und die geplanten Alarme muessen daher sofort mitwechseln, wenn hier
 * umgeschaltet wird; ein reines SharedPreferences-Lesen beim Screen-Aufbau haette das nicht
 * mitbekommen.
 *
 * **Bruecke zum gemeinsamen Kern:** dort heisst dasselbe Konzept neutral "Profil"
 * ([ActiveProfilePrefs]), weil das :app-Modul die echte Glyph-Matrix ansteuert und gar keine
 * Avatare kennt. Diese Klasse haelt beide Seiten synchron: der Avatar-Name IST die Profil-ID
 * ([profileId]). [set] schreibt deshalb immer beides - schriebe es nur die Avatar-Auswahl,
 * wuerde der Scheduler weiterhin die Erinnerungen des alten Profils einplanen.
 */
object AvatarSpeciesPrefs {
    private const val PREFS_NAME = "avatar_species_prefs"
    private const val KEY_SPECIES = "species"

    private val _selected = MutableStateFlow<AvatarSpecies?>(null)

    /** Profil-ID des Avatars im gemeinsamen Kern (siehe [ActiveProfilePrefs]). */
    fun profileId(species: AvatarSpecies): String = species.name

    fun get(context: Context): AvatarSpecies {
        _selected.value?.let { return it }
        val name = prefs(context).getString(KEY_SPECIES, null)
        val species = name
            ?.let { runCatching { AvatarSpecies.valueOf(it) }.getOrNull() }
            ?: AvatarSpecies.PUFFLING
        _selected.value = species
        // Der Kern liest die aktive Profil-ID auch aus kalt gestarteten Prozessen (Alarm, Boot),
        // in denen set() nie lief - hier nachziehen, damit beide Seiten uebereinstimmen.
        ActiveProfilePrefs.set(context, profileId(species))
        return species
    }

    /**
     * Beobachtbarer Auswahl-Zustand. Beim ersten Zugriff aus [get] befuellt, damit der
     * gespeicherte Wert nicht erst nach dem ersten [set] sichtbar wird.
     */
    fun selected(context: Context): StateFlow<AvatarSpecies?> {
        get(context)
        return _selected.asStateFlow()
    }

    fun set(context: Context, species: AvatarSpecies) {
        prefs(context).edit().putString(KEY_SPECIES, species.name).apply()
        ActiveProfilePrefs.set(context, profileId(species))
        _selected.value = species
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
