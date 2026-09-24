package com.notime.glyphsim.matrix

/**
 * **Die Naht einer Schleife** - wann und wie ein Stueck in seinen naechsten Durchlauf uebergeht.
 *
 * ## Das Problem
 *
 * Der Player liess jedes Stueck bisher hart in sich selbst zurueckspringen. Das Modell erzeugt
 * aber viele Stuecke mit einem Ausklang, obwohl die Prompts es ausdruecklich verbieten: Gemessen
 * am 2026-09-24 lagen bei elf von siebzehn Dateien die letzten zwei bis sieben Sekunden mehr als
 * 6 dB unter dem Rest. Jede Schleife hatte also ein kleines Loch, bevor der Anfang mit voller
 * Kraft wieder einsetzte - hoerbar als "das Lied hoert auf und faengt neu an".
 *
 * ## Die Loesung
 *
 * Die Engine verlaesst sich nicht mehr darauf, dass die Datei gut endet. [TAIL_TRIM_MS] vor dem
 * Ende ist der Durchlauf vorbei: Ein zweiter Player beginnt das naechste Stueck von vorn und
 * ueberblendet, bevor der Ausklang ueberhaupt beginnt. Das Loch wird nie gespielt.
 *
 * Und weil dort ohnehin eine Naht ist, faellt an ihr auch die Entscheidung der Rotation
 * ([PlayMusicRotation]): Ein anderes Stueck derselben Stimmung beginnt am Ende eines
 * Durchlaufs, nicht mitten im Takt. Bleibt es beim selben Stueck, ist die Ueberblendung kurz
 * ([SEAM_FADE_MS]) - zwei Durchlaeufe desselben Stuecks sollen sich nicht lange ueberlagern,
 * weil ihre Takte nicht uebereinander liegen.
 */
object PlayMusicLoop {

    /**
     * So viel vom Ende eines Stuecks wird nie gespielt. Etwas mehr als der laengste gemessene
     * Ausklang (sieben Sekunden bei `morning-03`).
     */
    const val TAIL_TRIM_MS = 7_500L

    /** Die Naht in dasselbe Stueck: kurz, damit sich die Takte kaum ueberlagern. */
    const val SEAM_FADE_MS = 2_500L

    /**
     * Unter dieser Laenge gibt es keine geplante Naht - dann laeuft die gewoehnliche Schleife
     * des Players. Schuetzt vor kaputten oder unbekannten Laengen (der Player meldet -1).
     */
    const val MIN_PIECE_MS = 30_000L

    /** Was an der naechsten Naht passiert. */
    data class Seam(
        /** Wie lange nach dem Start des Stuecks die Ueberblendung beginnt. */
        val atMs: Long,
        /** Welche Variante danach klingt - dieselbe oder eine andere. */
        val nextVariant: Int,
        val fadeMs: Long
    )

    /**
     * Plant die Naht eines Stuecks der Laenge [durationMs].
     *
     * [fixedVariant] ist gesetzt, wenn die Variante nicht rotieren darf (ein Charakterthema);
     * [rotate] ist die Entscheidung von [PlayMusicRotation.rotationDue] fuer den Zeitpunkt der
     * Naht; [variantFadeMs] die Blende fuer einen echten Stueckwechsel.
     *
     * `null` heisst: keine geplante Naht, der Player schleift wie bisher selbst.
     */
    fun plan(
        durationMs: Long,
        current: Int,
        variants: List<Int>,
        fixedVariant: Int?,
        rotate: Boolean,
        variantFadeMs: Long,
        pickOther: (List<Int>, Int) -> Int?
    ): Seam? {
        if (durationMs < MIN_PIECE_MS) return null
        val next = when {
            fixedVariant != null -> fixedVariant
            rotate && variants.size >= 2 -> pickOther(variants, current) ?: current
            else -> current
        }
        val fade = if (next == current) SEAM_FADE_MS else variantFadeMs
        val at = (durationMs - TAIL_TRIM_MS - fade).coerceAtLeast(0L)
        return Seam(atMs = at, nextVariant = next, fadeMs = fade)
    }

    /**
     * Wie viel eines Stuecks tatsaechlich zu hoeren ist, bevor die Naht beginnt - fuer Anlaesse,
     * die genau einen Durchlauf lang dauern sollen (siehe [PlayCharacterTheme.GREETING_MS]).
     */
    fun playableMs(durationMs: Long, fadeMs: Long): Long =
        (durationMs - TAIL_TRIM_MS - fadeMs).coerceAtLeast(0L)
}
