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
 * ## Was der Avatar bestimmt - und was seit Phase 4b nicht mehr
 *
 * Er bestimmt, **wer da ist**: Aussehen, Charakter, Tagesablauf, Spielplan - und, ueber
 * [profileId], wessen Pflegebuch, Stimmung und Spielstand gefuehrt werden (siehe
 * [PresentCompanion]).
 *
 * Er bestimmt **nicht mehr, welche Erinnerungen gelten.** Bis Phase 4b hatte jeder Avatar seinen
 * eigenen Satz, und diese Klasse hielt das durch, indem sie bei jedem Lesen und Schreiben den
 * Avatarnamen als aktive Profil-Id in den Kern spiegelte ([ActiveProfilePrefs]). Genau das machte
 * aus einer rein optischen Entscheidung einen kompletten Wechsel der Konfiguration - Problem 3 der
 * Produktanalyse.
 *
 * Die Spiegelung ist deshalb weg. Der Kern bleibt beim Standardprofil, so wie im :app-Modul, und
 * die Routinen gehoeren dem Nutzer (siehe [RoutineOwner]). Wer den Avatar wechselt, wechselt
 * seither wirklich nur die Begleitung.
 *
 * ## Warum trotzdem ein Flow
 *
 * [selected] bleibt beobachtbar: An der Auswahl haengen weiterhin Anzeige, Stimmung und
 * Spielstand, und die sollen ohne Neuaufbau des Bildschirms mitwechseln.
 */
object AvatarSpeciesPrefs {
    private const val PREFS_NAME = "avatar_species_prefs"
    private const val KEY_SPECIES = "species"

    private val _selected = MutableStateFlow<AvatarSpecies?>(null)

    /**
     * Profil-Id **des Wesens** - der Schluessel seines Pflegebuchs, seiner Stimmung und seines
     * Spielstands (siehe [PresentCompanion]).
     *
     * Nicht mehr die Id, unter der Erinnerungen stehen; das ist [RoutineOwner.current].
     */
    fun profileId(species: AvatarSpecies): String = species.name

    fun get(context: Context): AvatarSpecies {
        _selected.value?.let { return it }
        val name = prefs(context).getString(KEY_SPECIES, null)
        val species = name
            ?.let { runCatching { AvatarSpecies.valueOf(it) }.getOrNull() }
            ?: AvatarSpecies.PUFFLING
        _selected.value = species
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
        _selected.value = species
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
