package com.notime.glyphsim.matrix

/**
 * **Der Waechter ueber die Figur im Spielmodus** - prueft in regelmaessigen Abstaenden, ob der
 * Avatar wirklich zu sehen ist, und benennt, was nicht stimmt.
 *
 * ## Warum es ihn gibt
 *
 * Gemeldet am 26.09.: Mitten im Spiel wurde der Avatar dunkel, blieb stehen oder lief nach einer
 * auf ihn gezogenen Erinnerung aus dem Bild - und danach war niemand mehr zu sehen. Geholfen hat
 * nur, den Spielmodus zu verlassen und wieder zu betreten. In einem durchlaufenden Stream gibt es
 * diesen Ausweg nicht.
 *
 * Die drei gefundenen Ursachen sind einzeln behoben (siehe `moveToPlace`, die Play-Schleife und
 * `feedAvatarNow` in `DockScreen`). Alle drei hatten aber dieselbe Form: Ein Ablauf veraendert
 * die Figur voruebergehend (abblenden, ausblenden, Ruhe-Schleife anhalten, hinausfliegen) und
 * wird abgebrochen, bevor er sie zuruecksetzt. Solche Stellen gibt es in `DockScreen` viele, und
 * es werden mehr. Der Waechter sichert deshalb nicht einen Ablauf ab, sondern das ERGEBNIS: Egal
 * welcher kuenftige Abbruch eine Figur liegen laesst - nach wenigen Sekunden ist sie wieder da.
 *
 * ## Warum das eine eigene Datei ist
 *
 * Dieselbe Trennung wie bei [PlayVisitWindow]: Die Entscheidung besteht nur aus Zahlen und
 * Wahrheitswerten und ist damit ohne Geraet pruefbar. Die Reparatur selbst (Figur neu hinstellen,
 * Ruhe-Schleife starten) bleibt in `DockScreen`, wo die Zustaende leben.
 *
 * ## Warum erst beim zweiten Mal
 *
 * Ein Problem gilt erst als echt, wenn es bei zwei Pruefungen hintereinander besteht
 * ([confirmed]). Jeder der Zustaende kommt fuer Sekundenbruchteile auch im gewollten Ablauf vor -
 * zwischen dem Abbruch einer Ruhe-Schleife und dem Start der naechsten Regung etwa. Ein Waechter,
 * der dort eingreift, waere selbst die Stoerung, die er verhindern soll.
 */
object AvatarWatchdog {

    /** Wie oft nachgesehen wird. Zwei Pruefungen bis zur Reparatur ergeben rund drei Sekunden. */
    const val CHECK_INTERVAL_MS = 1_500L

    /**
     * Ab wann ein unveraendertes Bild ohne laufende Ruhe-Schleife als eingefroren gilt.
     *
     * Die laengste Ruhelage einer Ruhe-Schleife liegt bei 1,3 s ([AvatarAnimations.idleSequence]);
     * vier Sekunden ohne jeden Bildwechsel kommen im gewollten Ablauf ausserhalb eines
     * Tagesablaufs nicht vor.
     */
    const val FROZEN_AFTER_MS = 4_000L

    /** Wie viel der Figur mindestens im Bild liegen muss, damit sie als sichtbar gilt. */
    const val MIN_VISIBLE_FRACTION = 0.5f

    /** Unterhalb dieser Helligkeit gilt die Figur als abgedunkelt. */
    const val MIN_BRIGHTNESS = 0.99f

    enum class Problem {
        /** Im Spielmodus existiert gar keine Figur. */
        MISSING,

        /** Die Figur ist als "im Tuerrahmen" ausgeblendet, obwohl niemand durch eine Tuer geht. */
        HIDDEN,

        /** Die Figur ist abgedunkelt, obwohl niemand durch eine Tuer geht. */
        DIMMED,

        /** Die Figur steht (ueberwiegend) ausserhalb des Bildes. */
        OFF_SCREEN,

        /** Die Figur bewegt sich nicht mehr - keine Ruhe-Schleife und niemand sonst bewegt sie. */
        FROZEN
    }

    /**
     * Alles, was der Waechter ueber die Figur wissen muss - ein Schnappschuss je Pruefung.
     *
     * @param present ob es im Spielmodus ueberhaupt eine Figur gibt.
     * @param left linke Kante der Figur in Pixeln.
     * @param top obere Kante der Figur in Pixeln.
     * @param sizePx Kantenlaenge der (quadratischen) Figur in Pixeln.
     * @param screenWidthPx Breite der Zeichenflaeche.
     * @param screenHeightPx Hoehe der Zeichenflaeche.
     * @param hidden ob die Figur als "im Tuerrahmen" ausgeblendet ist.
     * @param brightness aktuelle Helligkeit der Figur (1 = voll).
     * @param doorTransit ob gerade gewollt eine Tuer durchschritten wird - dann sind Ausblenden
     *   und Abdunkeln richtig.
     * @param reacting ob gerade eine Fuetter-Reaktion laeuft. Die Rakete fliegt dabei gewollt
     *   ueber den oberen Rand hinaus.
     * @param moving ob gerade ein Gang oder ein Hinsetzen die Position Bild fuer Bild schreibt.
     * @param idleLoopActive ob die Ruhe-Schleife laeuft.
     * @param animatedElsewhere ob jemand anderes die Bilder der Figur fuehrt (Tagesablauf, Besuch,
     *   Gruppenspiel, Traumrueckblick).
     * @param frameUnchangedMs wie lange sich das angezeigte Bild schon nicht geaendert hat.
     */
    data class Observation(
        val present: Boolean,
        val left: Float = 0f,
        val top: Float = 0f,
        val sizePx: Float = 0f,
        val screenWidthPx: Float = 0f,
        val screenHeightPx: Float = 0f,
        val hidden: Boolean = false,
        val brightness: Float = 1f,
        val doorTransit: Boolean = false,
        val reacting: Boolean = false,
        val moving: Boolean = false,
        val idleLoopActive: Boolean = true,
        val animatedElsewhere: Boolean = false,
        val frameUnchangedMs: Long = 0L
    )

    /** Was bei DIESER Pruefung nicht stimmt - noch ohne Entprellung, siehe [confirmed]. */
    fun problems(observation: Observation): Set<Problem> {
        if (!observation.present) return setOf(Problem.MISSING)
        val found = mutableSetOf<Problem>()
        with(observation) {
            if (hidden && !doorTransit) found += Problem.HIDDEN
            if (brightness < MIN_BRIGHTNESS && !doorTransit) found += Problem.DIMMED
            if (!doorTransit && !reacting && !moving &&
                !isOnScreen(left, top, sizePx, screenWidthPx, screenHeightPx)
            ) {
                found += Problem.OFF_SCREEN
            }
            if (!idleLoopActive && !animatedElsewhere && !reacting && !moving &&
                frameUnchangedMs >= FROZEN_AFTER_MS
            ) {
                found += Problem.FROZEN
            }
        }
        return found
    }

    /** Nur was bei zwei Pruefungen hintereinander besteht, wird repariert. */
    fun confirmed(previous: Set<Problem>, current: Set<Problem>): Set<Problem> =
        previous intersect current

    /**
     * Ob mindestens [MIN_VISIBLE_FRACTION] der Figur innerhalb der Zeichenflaeche liegt.
     *
     * Ohne gemessene Flaeche (Groesse 0, erste Zeichnung noch nicht erfolgt) gibt es nichts zu
     * beurteilen - dann gilt die Figur als sichtbar, statt sie auf Verdacht umzusetzen.
     */
    fun isOnScreen(left: Float, top: Float, sizePx: Float, screenWidthPx: Float, screenHeightPx: Float): Boolean {
        if (sizePx <= 0f || screenWidthPx <= 0f || screenHeightPx <= 0f) return true
        if (left.isNaN() || top.isNaN()) return false
        val visibleW = (minOf(left + sizePx, screenWidthPx) - maxOf(left, 0f)).coerceAtLeast(0f)
        val visibleH = (minOf(top + sizePx, screenHeightPx) - maxOf(top, 0f)).coerceAtLeast(0f)
        return visibleW * visibleH >= MIN_VISIBLE_FRACTION * sizePx * sizePx
    }

    /**
     * Wo die Figur waagerecht wieder hingestellt wird - als Bruchteil der freien Breite, so nah
     * wie moeglich an der Stelle, an der sie verloren ging.
     */
    fun recoveryFraction(left: Float, sizePx: Float, screenWidthPx: Float): Float {
        val boundX = screenWidthPx - sizePx
        if (boundX <= 0f || left.isNaN()) return 0.5f
        return (left / boundX).coerceIn(0f, 1f)
    }
}
