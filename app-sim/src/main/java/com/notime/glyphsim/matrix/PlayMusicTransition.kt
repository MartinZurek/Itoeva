package com.notime.glyphsim.matrix

/**
 * **Wie ein Musikwechsel klingt und wann er tatsaechlich stattfindet** - der Teil der
 * Uebergaenge, der sich ohne Android pruefen laesst.
 *
 * Bis hierhin kannte die Musik genau einen Uebergang: vier Sekunden Ueberblendung, sofort, fuer
 * alles. Das klang bei einem Tageszeitwechsel zu eilig, beim Betreten des Sportplatzes zu
 * zaeh - und ein Avatar, der abends nur kurz ueber die Strasse ging, bekam zwei volle Wechsel
 * hintereinander (Wohnzimmer -> Strasse -> Park), jeder davon hoerbar.
 *
 * ## Zwei Fragen, zwei Antworten
 *
 * - **Wann** ([settle]): Ein Rollenwechsel aus einem Ortswechsel muss sich erst
 *   [ROLE_SETTLE_MS] lang halten. Wer nur durchgeht, behaelt die Musik; wer bleibt, bekommt die
 *   neue. Ereignisse mit fester Dauer - das eigene Stueck am Tagesanfang - warten nicht, sonst
 *   verschoebe sich ihr Ende gegenueber [PlayCharacterTheme.GREETING_MS].
 * - **Wie lange** ([fadeMs]): je nach Art des Wechsels (siehe dort).
 */
object PlayMusicTransition {

    /** So lange muss eine neue Rolle gewuenscht bleiben, bevor die Musik ihr folgt. */
    const val ROLE_SETTLE_MS = 10_000L

    /** Einstieg aus der Stille - dieselben vier Sekunden wie bisher. */
    const val START_FADE_MS = 4_000L

    /** Der gewoehnliche Szenen- oder Tageszeitwechsel: ruhig, wie ein Lichtwechsel. */
    const val SCENE_FADE_MS = 6_000L

    /** Hinein in Bewegung oder aus ihr heraus: mit Impuls, sonst kommt der Beat zu spaet. */
    const val SPORT_FADE_MS = 3_000L

    /** Ein anderes Stueck derselben Stimmung - so lang, dass man die Naht nicht bemerkt. */
    const val VARIANT_FADE_MS = 8_000L

    /** Ein Rollenwechsel, der noch auf seine Bestaetigung wartet. */
    data class Pending(val role: MusicRole, val sinceMs: Long)

    /**
     * Das Ergebnis von [settle]: jetzt wechseln oder nicht, was vorgemerkt bleibt, und wann es
     * sich lohnt, wieder nachzusehen (`null` = kein eigener Termin, der gewoehnliche Takt genuegt).
     */
    data class Settle(val switchNow: Boolean, val pending: Pending?, val recheckInMs: Long?)

    /** Wie lange ein Wechsel von [playing] nach [wanted] warten muss. */
    fun settleMs(playing: MusicRole?, wanted: MusicRole): Long = when {
        playing == null || playing == wanted -> 0L
        playing == MusicRole.CHARACTER_THEME || wanted == MusicRole.CHARACTER_THEME -> 0L
        // Eine Aktivitaet beginnt, wenn sie beginnt: Der Wurf, der erste Auswurf der Angel -
        // zehn Sekunden spaeter waere die Musik zu spaet. Das ENDE einer Aktivitaet wartet
        // dagegen wie jeder Ortswechsel, damit eine kurze Pause zwischen zwei Wuerfen die
        // Musik nicht umwirft.
        wanted in ACTIVITY_ROLES -> 0L
        else -> ROLE_SETTLE_MS
    }

    /**
     * Rollen, die an einer laufenden Handlung haengen und nicht an einem Ort.
     *
     * SPORT gehoert seit dem Gruppenspiel dazu (siehe [PlayGroupGame]): Es dauert eine Minute,
     * und zehn Sekunden Wartezeit waeren ein Sechstel davon ohne seine Musik. Auch auf dem
     * Sportplatz gilt SPORT nur, solange wirklich Bewegung laeuft - also ebenfalls eine Handlung.
     */
    val ACTIVITY_ROLES: Set<MusicRole> = setOf(MusicRole.FISHING, MusicRole.BALLGAME, MusicRole.SPORT)

    /**
     * Entscheidet, ob der Player jetzt auf [wanted] wechseln soll, und fuehrt die Vormerkung
     * fort. Aufgerufen bei jedem Abgleich; [pending] ist das, was der letzte Aufruf
     * zurueckgegeben hat.
     *
     * Der Kern ist die Zeile mit `pending?.role == wanted`: Nur wer DENSELBEN Wunsch wiederholt,
     * sammelt Wartezeit. Pendelt die Lage zwischen zwei neuen Rollen, beginnt die Uhr jedes Mal
     * neu - und geht sie zur laufenden Rolle zurueck, wird die Vormerkung verworfen.
     */
    fun settle(playing: MusicRole?, wanted: MusicRole, pending: Pending?, nowMs: Long): Settle {
        if (wanted == playing) return Settle(switchNow = false, pending = null, recheckInMs = null)
        val window = settleMs(playing, wanted)
        if (window == 0L) return Settle(switchNow = true, pending = null, recheckInMs = null)
        val current = pending?.takeIf { it.role == wanted && nowMs >= it.sinceMs }
            ?: Pending(wanted, nowMs)
        val waited = nowMs - current.sinceMs
        if (waited >= window) return Settle(switchNow = true, pending = null, recheckInMs = null)
        return Settle(switchNow = false, pending = current, recheckInMs = window - waited)
    }

    /**
     * Wie lange die Ueberblendung von [from] nach [to] dauert.
     *
     * [variantOnly] heisst: dieselbe Rolle, nur ein anderes Stueck (siehe [PlayMusicRotation]).
     * Alles mit dem eigenen Stueck bleibt bei [PlayCharacterTheme.FADE_MS] - dessen Ende ist
     * darauf gerechnet.
     */
    fun fadeMs(from: MusicRole?, to: MusicRole, variantOnly: Boolean): Long = when {
        from == null -> START_FADE_MS
        from == MusicRole.CHARACTER_THEME || to == MusicRole.CHARACTER_THEME ->
            PlayCharacterTheme.FADE_MS
        variantOnly -> VARIANT_FADE_MS
        from == MusicRole.SPORT || to == MusicRole.SPORT -> SPORT_FADE_MS
        from == MusicRole.BALLGAME || to == MusicRole.BALLGAME -> SPORT_FADE_MS
        else -> SCENE_FADE_MS
    }
}
