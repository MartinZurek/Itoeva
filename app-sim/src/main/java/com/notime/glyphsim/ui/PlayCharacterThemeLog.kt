package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.PlayCharacterTheme
import java.time.LocalDate

/**
 * Merkt sich, an welchem Tag ein Wesen zuletzt mit seinem eigenen Stueck begruesst hat.
 *
 * Die ganze Ueberlegung dazu steht in [PlayCharacterTheme]; hier liegt nur die Ablage. Getrennt,
 * weil die Regel ohne Android pruefbar bleiben soll und diese vier Zeilen es nicht sein koennen.
 *
 * **Je Wesen und nicht je Tag** - wer den Avatar wechselt, hoert am selben Tag auch das zweite
 * Thema. Aus demselben Grund liegt der Schluessel wie bei [PlayLore] nicht im
 * `SettingsCatalog`: Der fuehrt feste Schluessel, hier haengt einer an einem Enum-Wert.
 */
object PlayCharacterThemeLog {

    private const val PREFS = "play_character_theme"

    private fun key(species: AvatarSpecies) = "last_day_${species.name}"

    /** Ob dieses Wesen heute noch nicht begruesst hat. */
    fun isDue(
        context: Context,
        species: AvatarSpecies,
        today: LocalDate = LocalDate.now()
    ): Boolean = PlayCharacterTheme.isDue(lastGreetedDay(context, species), today)

    /**
     * Haelt fest, dass die Begruessung stattgefunden hat.
     *
     * Der Aufrufer ruft das **am Ende** des Stuecks und nicht an seinem Anfang: Ein Anlass ist
     * verbraucht, wenn er stattgefunden hat. Wer den Spielmodus nach fuenf Sekunden wieder
     * verlaesst, hat keine Begruessung gehoert - und bekommt sie beim naechsten Hineingehen.
     */
    fun greeted(
        context: Context,
        species: AvatarSpecies,
        today: LocalDate = LocalDate.now()
    ) {
        prefs(context).edit().putLong(key(species), today.toEpochDay()).apply()
    }

    /**
     * `null` heisst "noch nie". Der 1. Januar 1970 ist als Tag Null damit nicht darstellbar; das
     * ist die guenstigere Seite des Tauschs gegenueber einem zweiten Schluessel je Wesen.
     */
    private fun lastGreetedDay(context: Context, species: AvatarSpecies): Long? =
        prefs(context).getLong(key(species), 0L).takeIf { it != 0L }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
