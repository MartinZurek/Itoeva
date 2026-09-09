package com.notime.glyphsim.matrix

import java.time.LocalDate

/**
 * **Wann das eigene Stueck eines Wesens erklingt** - der Anlass, nicht das Stueck.
 *
 * ## Der eine Anlass und warum es genau einer ist
 *
 * Jedes der sechs Wesen bekommt ein eigenes Musikstueck (Rolle [MusicRole.CHARACTER_THEME],
 * Variante je Spezies - siehe [MusicRole.characterThemeVariant]). Die naheliegende Umsetzung
 * waere gewesen, es einfach laufen zu lassen, solange dieses Wesen anwesend ist. Genau das ist
 * hier ausgeschlossen: **Ein Stueck, das immer laeuft, ist Hintergrundmusik.** Es waere dann
 * nicht das Thema des Wesens, sondern nur ein sechster Tagesklang, und man haette nach zwei
 * Minuten aufgehoert, es zu hoeren.
 *
 * Der gewaehlte Anlass ist deshalb: **das erste Erscheinen des Wesens im Spielmodus an einem
 * Kalendertag.** Man geht in den Spielmodus, das Wesen ist da - und dazu laeuft sein Stueck,
 * einmal. Danach uebernimmt wieder die Musik, die zur Lage passt ([MusicResolver]).
 *
 * Drei Eigenschaften machen diesen Anlass zum richtigen:
 *
 * - **Er ist selten genug, um etwas zu bedeuten.** Einmal am Tag, nicht bei jedem Ortswechsel.
 * - **Er faellt mit dem Wiedersehen zusammen.** Das Stueck kommentiert nichts, es begruesst -
 *   und ein Wiedersehen ist der einzige Moment am Tag, den jedes Wesen zuverlaessig hat.
 * - **Er unterscheidet die Wesen genau dort, wo man sie vergleicht.** Wer den Avatar wechselt,
 *   hoert am selben Tag beide Themen; das ist Absicht - der Zaehler haengt an der Spezies und
 *   nicht am Tag als Ganzem.
 *
 * ## Was hier ausdruecklich NICHT passiert
 *
 * **Kein Einschalten.** Ob ueberhaupt Musik laufen darf, entscheidet weiterhin allein der
 * Nutzer ueber [com.notime.glyphsim.ui.PlayMusic]; `PlayMusic.decide` ist von dieser Aenderung
 * unberuehrt. Diese Datei aendert nur, WAS die Welt vorschlaegt.
 *
 * **Keine Nachtsperre.** [MusicResolver] laesst nachts bewusst keinen Tagesklang zu; das Thema
 * steht trotzdem vorn. Der Grund ist derselbe, aus dem [com.notime.glyphsim.ui.PlayMusic]
 * ueberhaupt nachts spielen darf: Musik kommt nie unaufgefordert - sie laeuft nur, solange
 * jemand mit eingeschaltetem Bildschirm zusieht und Musik eingeschaltet hat. Wer nachts in den
 * Spielmodus geht, hat sein Wesen besucht, und dann darf es ihn auch begruessen.
 *
 * Alles hier ist reine Rechnung - kein Android, keine Ablage. Die Ablage (welcher Tag zuletzt)
 * liegt in [com.notime.glyphsim.ui.PlayCharacterThemeLog], das Abspielen in `DockScreen`.
 */
object PlayCharacterTheme {

    /**
     * Die Laenge der sechs Stuecke, wie sie in `music/manifest.json` bestellt sind
     * (`duration_seconds` 90).
     *
     * Steht hier und nicht im Manifest-Leser, weil sie eine Entscheidung ueber den ANLASS traegt
     * und nicht ueber die Datei: Die Begruessung dauert ein Stueck lang.
     */
    const val PIECE_MS = 90_000L

    /**
     * Dieselbe Dauer wie `PlayMusic.CROSSFADE_MS`.
     *
     * Absichtlich doppelt und mit einem Test darauf abgesichert
     * (`PlayMusicTest`), statt die Konstante aus der UI-Schicht hierher zu ziehen: Die
     * Ueberblendung ist eine Eigenschaft des Players, der Anlass eine der Welt. Was hier zaehlt,
     * ist nur, dass beide Zahlen zueinander passen - und wenn jemand die eine aendert, soll ein
     * Test das sagen und nicht das Ohr.
     */
    const val FADE_MS = 4_000L

    /**
     * Wie lange das Thema vorn steht.
     *
     * **Kuerzer als das Stueck, und zwar genau um die Ueberblendung.** Der Player laesst jeden
     * Track in einer Schleife laufen; endete das Fenster erst nach [PIECE_MS], begaenne das
     * Stueck hoerbar ein zweites Mal und wuerde vier Sekunden spaeter mitten im Anfang
     * weggeblendet. So klingt es stattdessen genau einmal durch, und die letzten Sekunden davon
     * sind schon der Uebergang zurueck in den Tag.
     */
    const val GREETING_MS = PIECE_MS - FADE_MS

    /**
     * Ob dieses Wesen heute noch nicht begruesst hat.
     *
     * [lastGreetedDay] ist ein Tag nach [LocalDate.toEpochDay], `null` heisst "noch nie".
     *
     * Bewusst ein Vergleich auf Gleichheit und kein "liegt vor heute": Eine zurueckgestellte Uhr
     * ergibt dann einen weiteren Gruss und nicht ein tagelang gesperrtes Thema. Ein Gruss zuviel
     * ist ein kleiner Fehler, ein stumm bleibendes Wesen ein grosser.
     */
    fun isDue(lastGreetedDay: Long?, today: LocalDate): Boolean =
        lastGreetedDay != today.toEpochDay()
}
