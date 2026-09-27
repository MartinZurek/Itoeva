package com.notime.glyphsim.matrix

import kotlin.math.abs

/**
 * **Nachbarn kommen vorbei, statt herumzustehen.**
 *
 * Die Hintergrundfiguren (siehe [LivingPopulationLayout]) standen still auf ihrem Platz und
 * atmeten - im Stream als "kleine graue Figuren, bei denen nichts passiert" gemeldet und deshalb
 * zunaechst ausgeblendet. Dabei sind gerade sie das, was aus sechs Einzelwesen eine Nachbarschaft
 * macht. Es fehlte nicht die Figur, sondern eine Handlung, die man ohne Erklaerung versteht.
 *
 * Die einfachste ist die aelteste: **Jemand kommt vorbei, bleibt kurz stehen, gruesst und geht
 * weiter.** Ein Einwohner, der laut Simulation an diesem Ort ist, geht dafuer in eigenem Takt
 * quer durchs Bild - mal von links, mal von rechts -, haelt neben dem Wesen an, huepft zum Gruss
 * und zieht dann hinter ihm vorbei aus dem Bild. Danach ist eine Weile niemand zu sehen, bis er
 * wiederkommt. Wer anwesend ist, entscheidet weiterhin allein die Simulation; hier steht nur,
 * WANN und WO man ihn sieht.
 *
 * Reine Rechnung aus der Uhr, dem Profil und der Lage des Wesens - dadurch ist jeder Moment
 * eines Vorbeigangs offline pruefbar, und Bildschirm und Clip zeigen dasselbe.
 */
object ResidentPassage {

    /** So lange dauert der Weg quer durchs Bild (ohne den Gruss). */
    const val CROSSING_MS = 14_000L

    /** So lange bleibt er stehen und gruesst. */
    const val GREETING_MS = 2_400L

    /** Die Pause zwischen zwei Vorbeigaengen desselben Einwohners: 20 bis 45 Sekunden. */
    private const val MIN_GAP_MS = 20_000L
    private const val GAP_SPREAD_MS = 25_000L

    /** Abstand zum Wesen beim Gruss, als Bruchteil der Bildbreite. */
    private const val GREETING_GAP = 0.01f

    /**
     * Wie viel der Sprite-Breite links und rechts leer ist. Das Raster ist 16 Zellen breit, der
     * Koerper aber schmaler - ohne diesen Abzug hielt der Nachbar eine halbe Koerperlaenge zu
     * weit entfernt an und wirkte, als gruesse er ins Leere.
     */
    const val BODY_MARGIN = 0.2f

    /**
     * Wo der Einwohner gerade ist - `null`, solange er ausserhalb des Bildes ist.
     *
     * @property leftFraction linke Kante als Bruchteil der Bildbreite (darf beim Hinein- und
     *   Hinausgehen ueber den Rand ragen)
     * @property facingLeft ob er nach links geht (sein Bild wird dann gespiegelt)
     * @property walking `true` beim Gehen, `false` waehrend des Grusses
     */
    data class Moment(val leftFraction: Float, val facingLeft: Boolean, val walking: Boolean)

    /** Laenge eines ganzen Zyklus (Vorbeigang plus Pause) fuer dieses Profil. */
    fun cycleMs(profileId: String): Long =
        CROSSING_MS + GREETING_MS + MIN_GAP_MS + Math.floorMod(seed(profileId, 1), GAP_SPREAD_MS.toInt())

    /**
     * Der Moment zur Zeit [nowMs] fuer das Profil [profileId].
     *
     * [widthFraction] ist die Breite des Einwohners, [hostLeftFraction]/[hostWidthFraction] die
     * Lage des Wesens, neben dem er anhaelt.
     */
    fun momentAt(
        profileId: String,
        nowMs: Long,
        widthFraction: Float,
        hostLeftFraction: Float,
        hostWidthFraction: Float
    ): Moment? {
        val cycle = cycleMs(profileId)
        // Jeder Einwohner hat seinen eigenen Versatz - zwei Nachbarn laufen nie im Gleichschritt.
        val shifted = nowMs + Math.floorMod(seed(profileId, 2), cycle.toInt())
        val round = Math.floorDiv(shifted, cycle)
        val t = Math.floorMod(shifted, cycle)
        if (t >= CROSSING_MS + GREETING_MS) return null

        // Die Richtung wechselt von Runde zu Runde - mal kommt er von links, mal von rechts.
        val fromLeft = Math.floorMod(round + seed(profileId, 3), 2L) == 0L
        val start = if (fromLeft) -widthFraction else 1f
        val end = if (fromLeft) 1f else -widthFraction
        val hostBoxLeft = hostLeftFraction.coerceIn(0f, 1f)
        val hostLeft = hostBoxLeft + hostWidthFraction * BODY_MARGIN
        val hostRight = (hostBoxLeft + hostWidthFraction * (1f - BODY_MARGIN)).coerceIn(0f, 1f)
        // Er haelt VOR dem Wesen an, auf der Seite, von der er kommt.
        val meet = if (fromLeft) hostLeft - widthFraction - GREETING_GAP else hostRight + GREETING_GAP

        val distance = abs(end - start)
        val speed = distance / CROSSING_MS
        val toMeet = (abs(meet - start) / speed).toLong()
        // Liegt der Treffpunkt ausserhalb des Bildes (das Wesen steht am Rand), gibt es keinen
        // Gruss - er geht einfach vorbei.
        val greets = meet in 0f..(1f - widthFraction)
        val direction = if (fromLeft) 1f else -1f
        return when {
            !greets -> if (t >= CROSSING_MS) {
                null
            } else {
                Moment(start + direction * speed * t, !fromLeft, walking = true)
            }
            t < toMeet -> Moment(start + direction * speed * t, !fromLeft, walking = true)
            t < toMeet + GREETING_MS -> Moment(meet, !fromLeft, walking = false)
            else -> {
                val walked = t - GREETING_MS
                if (walked >= CROSSING_MS) null
                else Moment(start + direction * speed * walked, !fromLeft, walking = true)
            }
        }
    }

    private fun seed(profileId: String, salt: Int): Int {
        var x = profileId.hashCode() * 31 + salt * -0x61c88647
        x = (x xor (x ushr 16)) * 0x45d9f3b
        return (x xor (x ushr 16)) and 0x7fffffff
    }
}
