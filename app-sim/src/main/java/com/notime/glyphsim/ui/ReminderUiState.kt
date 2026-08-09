package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.BuiltInAnimationSelection
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphsim.matrix.AvatarSpecies

/**
 * Alles, was der Erinnerungs-Bildschirm zum Zeichnen braucht - in einem unveraenderlichen Wert.
 *
 * ## Warum einer statt vier
 *
 * Bisher gab das ViewModel vier getrennte Fluesse heraus (Erinnerungen, Avatar, Bibliothek,
 * eingebaute Auswahl), und die Komposition rechnete daraus selbst weiter: welche
 * Bibliotheks-Animationen ausgewaehlt sind, welche eingebauten Typen, und dabei nebenbei ein
 * `AnimationType.valueOf` samt Fehlerbehandlung. Das hatte drei Nachteile.
 *
 * **Der Zustand konnte auseinanderlaufen.** Vier Fluesse werden unabhaengig voneinander
 * aktualisiert; zwischen zwei Aktualisierungen zeichnet der Bildschirm eine Mischung aus altem und
 * neuem Stand. Meist unsichtbar, aber es gibt keinen Zeitpunkt, an dem man sagen koennte: "so sah
 * der Bildschirm aus". Ein Wert kann das.
 *
 * **Die Ableitung lag im falschen Stockwerk.** Ob ein persistierter Zeichenketten-Name noch zu
 * einem bekannten [AnimationType] gehoert, ist eine Frage der Datenschicht und keine der
 * Darstellung - sie stand trotzdem mitten in einer `@Composable`.
 *
 * **Und sie war nicht pruefbar.** Alles, was in der Komposition gerechnet wird, braucht zum
 * Testen ein Geraet. Als Wert laesst sich dasselbe in Millisekunden auf der JVM pruefen (siehe
 * `ReminderUiStateTest`).
 *
 * ## Unveraenderlich, und zwar wirklich
 *
 * Die Listen sind `List`, nicht `MutableList`, und der Zustand wird nur ganz ersetzt, nie
 * verandert. Damit ist er ein tauglicher Compose-Parameter: gleiche Instanz heisst gleicher
 * Inhalt heisst kein Neuzeichnen.
 */
data class ReminderUiState(
    /** Die Erinnerungen des gewaehlten Avatars, ohne die vom Spielmodus verwaltete Zeile. */
    val reminders: List<GlyphReminder> = emptyList(),

    /** Der gewaehlte Avatar - der Bildschirm zeigt ihn als Zusammenhang im Titel. */
    val species: AvatarSpecies = AvatarSpecies.PUFFLING,

    /** Alle Bibliotheks-Animationen, auch die gerade nicht ausgewaehlten (fuer den Bibliotheks-Bildschirm). */
    val libraryAnimations: List<LibraryAnimation> = emptyList(),

    /** Rohzustand der eingebauten Auswahl - fuer den Bibliotheks-Bildschirm, der beides zeigt. */
    val builtInSelections: List<BuiltInAnimationSelection> = emptyList(),

    /**
     * Ob ueberhaupt schon geladen wurde.
     *
     * Ohne diese Angabe laesst sich "es gibt keine Erinnerungen" nicht von "sie sind noch nicht
     * da" unterscheiden - und der Bildschirm zeigte beim Aufbau kurz den Leer-Zustand mit seinem
     * "Noch keine Erinnerungen", bevor die Liste eintraf. Ein Aufblitzen, das wie ein Datenverlust
     * aussieht.
     */
    val loaded: Boolean = false
) {
    /**
     * Die im Picker ausgewaehlten Bibliotheks-Animationen.
     *
     * Als abgeleitete Eigenschaft und nicht als eigenes Feld: sie ergibt sich vollstaendig aus
     * [libraryAnimations]. Ein eigenes Feld waere eine zweite Wahrheit, die man vergessen kann
     * mitzupflegen.
     */
    val selectedLibraryAnimations: List<LibraryAnimation>
        get() = libraryAnimations.filter { it.isSelected }

    /**
     * Die im Picker ausgewaehlten eingebauten Typen.
     *
     * `mapNotNull` mit stillem Verwerfen ist Absicht: der Name steht als Zeichenkette in der
     * Datenbank (siehe Converters). Ein Eintrag, dessen Typ es nicht mehr gibt - etwa nach einem
     * Rueckbau -, soll die Liste nicht sprengen, sondern einfach fehlen.
     */
    val selectedBuiltInTypes: Set<AnimationType>
        get() = builtInSelections
            .filter { it.isSelected }
            .mapNotNull { runCatching { AnimationType.valueOf(it.animationType) }.getOrNull() }
            .toSet()

    /** Zusammen belegen beide Quellen die Plaetze im Picker - siehe `MAX_ANIMATIONS_IN_PICKER`. */
    val selectedAnimationCount: Int
        get() = selectedLibraryAnimations.size + selectedBuiltInTypes.size

    /** Erst wenn geladen wurde, ist "leer" eine Aussage und kein Zwischenstand. */
    val showEmptyState: Boolean
        get() = loaded && reminders.isEmpty()
}
