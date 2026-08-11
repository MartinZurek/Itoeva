package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.AvatarAnimations.BEAT_MS
import com.notime.glyphsim.matrix.AvatarAnimations.Beat
import com.notime.glyphsim.matrix.AvatarAnimations.FAST_MS
import com.notime.glyphsim.matrix.AvatarAnimations.SETTLE_MS
import com.notime.glyphsim.matrix.AvatarAnimations.SLOW_MS

/**
 * Eigene Reaktion fuer jede der 30 charakterspezifischen Animationen
 * ([com.notime.glyphcore.data.AvatarSignatureAnimations]).
 *
 * **Warum nicht der gemeinsame Abschluss.** Bibliotheks-Animationen bekamen bisher alle dieselbe
 * arteigene Freuden-Reaktion (`speciesFlourish`): Der Avatar freute sich, aber immer gleich, egal
 * ob gerade ein Blitz eingeschlagen oder eine Feder heruntergesegelt war. Bei einem Satz, der
 * ausdruecklich zum Charakter der Figur gehoert, ist das zu wenig - der Reiz liegt darin, dass die
 * Figur auf **dieses** Motiv antwortet. Gloop schwappt der Wolke hinterher, Hootlet blinzelt mit
 * dem Auge im Takt, Wyrmling zuckt beim Blitz zusammen und geht dann erst recht hoch.
 *
 * **Sie funktionieren fuer jede Spezies, sind aber fuer eine gedacht.** Die Zuordnung Animation →
 * Avatar ist gestalterisch, keine Sperre: Man kann Hootlets Sanduhr auch fuer Wyrmling waehlen.
 * Deshalb kommt hier nichts vor, was eine bestimmte Anatomie voraussetzt - keine Choreografie
 * verlaesst sich auf Schwanz oder Fuesse (die haben nicht alle), gearbeitet wird mit Verschiebung,
 * Akzent, Blick, Mund und Requisiten. `AvatarBody.accent` gibt es bei jeder Spezies, und er
 * bewegt jeweils etwas anderes - dieselbe Choreografie sieht bei Fennec (Ohren) anders aus als
 * bei Gloop (Quetschen).
 *
 * Requisiten liegen bewusst in den oberen Zeilen (y <= 4) bzw. an den Raendern: Dort ist bei jeder
 * Spezies auch nach einer Verschiebung noch Platz, ohne dass sich Motiv und Koerper ueberlagern.
 */
internal object AvatarSignatureReactions {

    /** null = kein eigener Ablauf hinterlegt, der Aufrufer nimmt seinen bisherigen Weg. */
    fun forLabel(label: String, body: AvatarBody): List<Beat>? = with(AvatarAnimations) {
        when (label) {
            // ---- PUFFLING ----
            "Bubble" -> bubble(body)
            "Butterfly" -> butterfly(body)
            "Kite" -> kite(body)
            "Snail" -> snail(body)
            "Compass" -> compass(body)
            // ---- STARLET ----
            "Comet" -> comet(body)
            "Lantern" -> lantern(body)
            "Feather" -> feather(body)
            "Constellation" -> constellation(body)
            "Candle" -> candle(body)
            // ---- WYRMLING ----
            "Bolt" -> bolt(body)
            "Summit" -> summit(body)
            "Ladder" -> ladder(body)
            "Drum" -> drum(body)
            "Flag" -> flag(body)
            // ---- FENNEC ----
            "Shield" -> shield(body)
            "Lighthouse" -> lighthouse(body)
            "Paw" -> paw(body)
            "Nest" -> nest(body)
            "Anchor" -> anchor(body)
            // ---- GLOOP ----
            "Turtle" -> turtle(body)
            "Cloud" -> cloud(body)
            "Drip" -> drip(body)
            "Puddle" -> puddle(body)
            "Balloon" -> balloon(body)
            // ---- HOOTLET ----
            "Eye" -> eye(body)
            "Key" -> key(body)
            "Hourglass" -> hourglass(body)
            "Scroll" -> scroll(body)
            "Puzzle" -> puzzle(body)
            else -> null
        }
    }

    /** Alle Labels mit eigener Reaktion - der Test laeuft sie durch. */
    val labels: List<String> = listOf(
        "Bubble", "Butterfly", "Kite", "Snail", "Compass",
        "Comet", "Lantern", "Feather", "Constellation", "Candle",
        "Bolt", "Summit", "Ladder", "Drum", "Flag",
        "Shield", "Lighthouse", "Paw", "Nest", "Anchor",
        "Turtle", "Cloud", "Drip", "Puddle", "Balloon",
        "Eye", "Key", "Hourglass", "Scroll", "Puzzle"
    )

    /** Wieviele dieser Motive auf jede Kreatur entfallen - die Liste ist danach geordnet. */
    private const val PER_SPECIES = 5

    /**
     * Die fuenf Motive, die DIESER Kreatur gehoeren (siehe
     * [com.notime.glyphcore.data.AvatarSignatureAnimations]).
     *
     * Aus der Reihenfolge von [labels] abgeleitet statt noch einmal aufgezaehlt: Zwei Listen
     * derselben dreissig Namen wuerden beim naechsten neuen Motiv auseinanderlaufen, und zwar
     * lautlos - die falsche Kreatur bekaeme das falsche Motiv, ohne dass irgendwo etwas bricht.
     */
    fun labelsFor(species: AvatarSpecies): List<String> =
        labels.drop(species.ordinal * PER_SPECIES).take(PER_SPECIES)

    // ================= PUFFLING: der neugierige Optimist =================

    /** Blase schwebt heran, er stupst sie an - sie platzt, er freut sich ueber das Nichts. */
    private fun AvatarAnimations.bubble(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(8 to 1)).beat(BEAT_MS),
        creatureFrame(body, prop = listOf(7 to 1, 8 to 1, 7 to 2, 8 to 2)).beat(BEAT_MS),
        // Anstupsen: Kopf hoch, Mund auf.
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(7 to 2, 8 to 2, 7 to 3, 8 to 3)).beat(FAST_MS),
        // Geplatzt - nur noch Spritzer.
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(5 to 1, 10 to 1, 5 to 4, 10 to 4)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(4 to 0, 11 to 0)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Falter zieht von links nach rechts, er dreht den Kopf mit - reine Neugier, kein Fangen. */
    private fun AvatarAnimations.butterfly(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(1 to 3, 2 to 2, 2 to 4)).beat(BEAT_MS),
        creatureFrame(body, dx = -1, prop = listOf(4 to 2, 5 to 1, 5 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(7 to 1, 8 to 0, 8 to 2)).beat(FAST_MS),
        creatureFrame(body, dx = 1, mouthHoles = body.mouthOpen, prop = listOf(10 to 2, 11 to 1, 11 to 3)).beat(BEAT_MS),
        creatureFrame(body, dx = 1, accentPhase = 1, prop = listOf(13 to 3, 14 to 2, 14 to 4)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Der Drachen steigt - er hopst hinterher, kommt aber nicht ran, und findet das gut. */
    private fun AvatarAnimations.kite(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(10 to 4, 9 to 5, 11 to 5, 10 to 6)).beat(BEAT_MS),
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(10 to 2, 9 to 3, 11 to 3, 10 to 4)).beat(FAST_MS),
        creatureFrame(body, dy = -3, feetSpread = 1, mouthHoles = body.mouthOpen, prop = listOf(11 to 1, 10 to 2, 12 to 2, 11 to 3)).beat(FAST_MS),
        creatureFrame(body, dy = 0, accentPhase = 1, prop = listOf(11 to 0, 10 to 1, 12 to 1)).beat(BEAT_MS),
        creatureFrame(body, dy = -2, feetSpread = 1, mouthHoles = body.mouthOpen, prop = listOf(12 to 0, 11 to 1)).beat(FAST_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Etwas ganz Kleines am Boden - er geht runter und schaut es sich in Ruhe an. */
    private fun AvatarAnimations.snail(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(2 to 4)).beat(BEAT_MS),
        creatureFrame(body, dy = 1, eyeHoles = body.eyesHalf, prop = listOf(2 to 4, 3 to 4)).beat(SLOW_MS),
        creatureFrame(body, dy = 2, dx = -1, prop = listOf(3 to 4, 4 to 4)).beat(SLOW_MS),
        creatureFrame(body, dy = 2, dx = -1, mouthHoles = body.mouthOpen, prop = listOf(4 to 4, 5 to 4, 5 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = 1, accentPhase = 1, prop = listOf(5 to 4, 6 to 4)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Er dreht sich einmal komplett um sich selbst - und weiss danach genau, wo es langgeht. */
    private fun AvatarAnimations.compass(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(7 to 1, 8 to 1)).beat(BEAT_MS),
        creatureFrame(body, dx = -2, accentPhase = 1, prop = listOf(5 to 2)).beat(FAST_MS),
        creatureFrame(body, dx = 2, accentPhase = -1, prop = listOf(10 to 2)).beat(FAST_MS),
        creatureFrame(body, dx = -1, accentPhase = 1, prop = listOf(6 to 1)).beat(FAST_MS),
        creatureFrame(body, dx = 1, prop = listOf(9 to 1)).beat(FAST_MS),
        // Eingependelt: Blick geradeaus, Zeiger steht.
        creatureFrame(body, mouthHoles = body.mouthOpen, prop = listOf(7 to 0, 8 to 0, 7 to 1, 8 to 1)).beat(SLOW_MS),
        creatureFrame(body, prop = listOf(7 to 0, 8 to 0)).beat(SETTLE_MS)
    )

    // ================= STARLET: die freundliche Traeumerin =================

    /** Der Komet zieht ueber sie hinweg; sie schaut ihm nach, bis nur noch Funken bleiben. */
    private fun AvatarAnimations.comet(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(1 to 0, 2 to 1)).beat(FAST_MS),
        creatureFrame(body, dy = -1, prop = listOf(4 to 1, 3 to 0, 5 to 2)).beat(FAST_MS),
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(8 to 2, 7 to 1, 6 to 0)).beat(BEAT_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, prop = listOf(12 to 3, 11 to 2, 10 to 1)).beat(BEAT_MS),
        // Nachschauen, waehrend der Schweif zerfaellt.
        creatureFrame(body, accentPhase = 1, prop = listOf(14 to 4, 12 to 2)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Sie lehnt sich ans warme Licht und wird ruhiger, statt sich zu freuen. */
    private fun AvatarAnimations.lantern(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(13 to 3)).beat(SLOW_MS),
        creatureFrame(body, prop = listOf(12 to 3, 13 to 3, 14 to 3, 13 to 2)).beat(SLOW_MS),
        creatureFrame(body, dx = 1, eyeHoles = body.eyesHalf, prop = listOf(12 to 2, 13 to 2, 14 to 2, 12 to 3, 13 to 3, 14 to 3)).beat(SLOW_MS),
        creatureFrame(body, dx = 1, accentPhase = 1, prop = listOf(12 to 3, 13 to 3, 14 to 3, 13 to 4)).beat(BEAT_MS),
        creatureFrame(body, dx = 1, eyeHoles = body.eyesHalf, prop = listOf(13 to 3)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
    )

    /** Die Feder sinkt; sie faengt sie mit einem kleinen Hopser auf. */
    private fun AvatarAnimations.feather(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(8 to 0, 7 to 1)).beat(SLOW_MS),
        creatureFrame(body, prop = listOf(6 to 1, 7 to 2)).beat(SLOW_MS),
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(9 to 2, 8 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = -2, feetSpread = 1, mouthHoles = body.mouthOpen, prop = listOf(7 to 3, 8 to 4)).beat(FAST_MS),
        // Gefangen - die Feder ist weg, sie sinkt zufrieden zurueck.
        creatureFrame(body, dy = 0, accentPhase = 1).beat(BEAT_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Punkte ueber ihr verbinden sich zu einem Bild - sie schaut zu und leuchtet mit. */
    private fun AvatarAnimations.constellation(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(3 to 1, 12 to 2)).beat(BEAT_MS),
        creatureFrame(body, prop = listOf(3 to 1, 12 to 2, 7 to 0, 9 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = -1, prop = listOf(3 to 1, 5 to 1, 7 to 0, 9 to 1, 12 to 2, 9 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(3 to 1, 4 to 1, 5 to 1, 6 to 0, 7 to 0, 8 to 1, 9 to 1, 10 to 2, 11 to 2, 12 to 2)).beat(SLOW_MS),
        // Der eigene Sternzopf antwortet.
        creatureFrame(body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(3 to 1, 7 to 0, 12 to 2, 2 to 3, 13 to 3)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
    )

    /** Sie atmet mit der Flamme - einmal duckt sie sich fast weg, dann steht sie wieder ruhig. */
    private fun AvatarAnimations.candle(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(2 to 3, 2 to 4)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(2 to 2, 2 to 3, 2 to 4)).beat(SLOW_MS),
        // Der Luftzug: die Flamme knickt weg, sie haelt inne.
        creatureFrame(body, dx = 1, prop = listOf(3 to 3, 2 to 4)).beat(FAST_MS),
        creatureFrame(body, dx = 1, mouthHoles = body.mouthOpen, prop = listOf(2 to 4)).beat(BEAT_MS),
        // Sie brennt weiter.
        creatureFrame(body, prop = listOf(2 to 2, 2 to 3, 2 to 4)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(2 to 3, 2 to 4)).beat(SETTLE_MS)
    )

    // ================= WYRMLING: der kleine Motivator =================

    /** Einschlag ueber ihm: kurzes Zusammenzucken - und dann geht er erst recht hoch. */
    private fun AvatarAnimations.bolt(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(8 to 0)).beat(FAST_MS),
        creatureFrame(body, prop = listOf(8 to 0, 7 to 1, 8 to 2)).beat(FAST_MS),
        // Zusammenzucken - geduckt, Augen schmal.
        creatureFrame(body, dy = 1, eyeHoles = body.eyesHalf, prop = listOf(8 to 0, 7 to 1, 8 to 2, 7 to 3)).beat(FAST_MS),
        // Aufladen.
        creatureFrame(body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(3 to 1, 13 to 1)).beat(BEAT_MS),
        creatureFrame(body, dy = -3, accentPhase = -1, mouthHoles = body.mouthOpen, prop = listOf(2 to 2, 14 to 2)).beat(FAST_MS),
        creatureFrame(body, dy = -5, accentPhase = 1, tailWag = 1).beat(BEAT_MS),
        creatureFrame(body, dy = -1, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Er steigt Stufe um Stufe - und oben angekommen reisst er die Arme hoch. */
    private fun AvatarAnimations.summit(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, dy = 2, prop = listOf(1 to 4, 2 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = 1, accentPhase = 1, prop = listOf(2 to 3, 3 to 2)).beat(BEAT_MS),
        creatureFrame(body, dy = 0, accentPhase = -1, prop = listOf(3 to 2, 4 to 1)).beat(BEAT_MS),
        creatureFrame(body, dy = -2, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(4 to 1, 5 to 0)).beat(FAST_MS),
        // Oben: Fahne steht.
        creatureFrame(body, dy = -3, mouthHoles = body.mouthOpen, prop = listOf(5 to 0, 6 to 0, 7 to 0)).beat(SLOW_MS),
        creatureFrame(body, dy = -1, accentPhase = -1, prop = listOf(6 to 0, 7 to 0)).beat(SETTLE_MS)
    )

    /** Sprosse fuer Sprosse nach oben, gleichmaessig - Motivation ist bei ihm nie Hektik. */
    private fun AvatarAnimations.ladder(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, dy = 2, prop = listOf(1 to 2, 3 to 2)).beat(BEAT_MS),
        creatureFrame(body, dy = 1, feetSpread = 1, accentPhase = 1, prop = listOf(1 to 2, 3 to 2, 1 to 4, 3 to 4)).beat(BEAT_MS),
        creatureFrame(body, dy = 0, accentPhase = -1, prop = listOf(1 to 1, 3 to 1, 1 to 3, 3 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = -2, feetSpread = 1, accentPhase = 1, prop = listOf(1 to 0, 3 to 0, 1 to 2, 3 to 2)).beat(BEAT_MS),
        creatureFrame(body, dy = -4, mouthHoles = body.mouthOpen, prop = listOf(1 to 0, 3 to 0)).beat(FAST_MS),
        creatureFrame(body, dy = -1, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Zwei Schlaege, und er federt bei jedem mit - der Takt geht ihm sofort in die Beine. */
    private fun AvatarAnimations.drum(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(2 to 2, 13 to 2)).beat(FAST_MS),
        creatureFrame(body, dy = -2, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(1 to 3, 13 to 2)).beat(FAST_MS),
        creatureFrame(body, dy = 0, prop = listOf(0 to 4, 13 to 2)).beat(FAST_MS),
        creatureFrame(body, dy = -2, accentPhase = -1, mouthHoles = body.mouthOpen, prop = listOf(2 to 2, 14 to 3)).beat(FAST_MS),
        creatureFrame(body, dy = 0, prop = listOf(2 to 2, 15 to 4)).beat(FAST_MS),
        creatureFrame(body, dy = -3, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(0 to 4, 15 to 4)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Die Fahne weht - er antwortet mit dem gleichen Rhythmus, Fluegel gegen Tuch. */
    private fun AvatarAnimations.flag(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(13 to 1, 13 to 2, 13 to 3, 14 to 1)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1, prop = listOf(13 to 1, 13 to 2, 13 to 3, 14 to 2, 15 to 1)).beat(FAST_MS),
        creatureFrame(body, accentPhase = -1, prop = listOf(13 to 1, 13 to 2, 13 to 3, 14 to 0, 15 to 1)).beat(FAST_MS),
        creatureFrame(body, dy = -2, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(13 to 1, 13 to 2, 13 to 3, 14 to 2, 15 to 2)).beat(BEAT_MS),
        creatureFrame(body, dy = -1, accentPhase = -1, prop = listOf(13 to 1, 13 to 2, 13 to 3, 14 to 1)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
    )

    // ================= FENNEC: der ruhige Beschuetzer =================

    /** Das Schild baut sich vor ihm auf; er stellt sich fest hin, statt zu jubeln. */
    private fun AvatarAnimations.shield(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(7 to 1, 8 to 1)).beat(BEAT_MS),
        creatureFrame(body, prop = listOf(6 to 1, 7 to 1, 8 to 1, 9 to 1, 6 to 2, 9 to 2)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1, prop = listOf(6 to 1, 7 to 1, 8 to 1, 9 to 1, 6 to 2, 9 to 2, 7 to 3, 8 to 3)).beat(FAST_MS),
        // Standhalten: Ohren nach vorn, Koerper leicht gesenkt und breit.
        creatureFrame(body, dy = 1, feetSpread = 1, accentPhase = 1, prop = listOf(5 to 1, 6 to 1, 7 to 1, 8 to 1, 9 to 1, 10 to 1, 6 to 2, 9 to 2, 7 to 3, 8 to 3)).beat(SLOW_MS),
        creatureFrame(body, dy = 1, feetSpread = 1, accentPhase = -1, prop = listOf(6 to 1, 7 to 1, 8 to 1, 9 to 1, 7 to 2, 8 to 2)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
    )

    /** Der Strahl wandert; sein Kopf und seine Ohren gehen ruhig mit - Wache halten, nicht jagen. */
    private fun AvatarAnimations.lighthouse(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(1 to 2, 2 to 3)).beat(SLOW_MS),
        creatureFrame(body, dx = -1, accentPhase = -1, prop = listOf(3 to 2, 4 to 3)).beat(BEAT_MS),
        creatureFrame(body, dx = 0, accentPhase = 1, prop = listOf(7 to 1, 8 to 1)).beat(BEAT_MS),
        creatureFrame(body, dx = 1, accentPhase = -1, prop = listOf(11 to 2, 12 to 3)).beat(BEAT_MS),
        creatureFrame(body, dx = 1, accentPhase = 1, eyeHoles = body.eyesHalf, prop = listOf(14 to 2, 13 to 3)).beat(SLOW_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Eine Spur zieht an ihm vorbei; er tritt auf der Stelle mit, ohne ihr zu folgen. */
    private fun AvatarAnimations.paw(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(1 to 4)).beat(BEAT_MS),
        creatureFrame(body, feetSpread = 1, accentPhase = 1, prop = listOf(1 to 4, 3 to 3)).beat(FAST_MS),
        creatureFrame(body, dy = -1, prop = listOf(1 to 4, 3 to 3, 5 to 2)).beat(FAST_MS),
        creatureFrame(body, feetSpread = 1, accentPhase = -1, prop = listOf(3 to 3, 5 to 2, 7 to 1)).beat(FAST_MS),
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(5 to 2, 7 to 1, 9 to 0)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1, prop = listOf(9 to 0)).beat(SETTLE_MS)
    )

    /** Er legt sich schuetzend ueber das Nest und wird ganz ruhig - der einzige Abschluss nach unten. */
    private fun AvatarAnimations.nest(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(6 to 3, 7 to 3, 8 to 3, 9 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = 1, prop = listOf(5 to 3, 6 to 4, 7 to 4, 8 to 4, 9 to 4, 10 to 3)).beat(SLOW_MS),
        creatureFrame(body, dy = 2, eyeHoles = body.eyesHalf, tailWag = 1, prop = listOf(5 to 3, 6 to 4, 7 to 4, 8 to 4, 9 to 4, 10 to 3)).beat(SLOW_MS),
        creatureFrame(body, dy = 2, accentPhase = -1, tailWag = -1, prop = listOf(5 to 3, 7 to 4, 8 to 4, 10 to 3)).beat(BEAT_MS),
        creatureFrame(body, dy = 3, eyeHoles = body.eyesHalf, accentPhase = -1, prop = listOf(6 to 4, 7 to 4, 8 to 4, 9 to 4)).beat(SLOW_MS),
        creatureFrame(body, dy = 3, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
    )

    /** Der Anker faellt und fasst; er pflanzt sich daneben genauso fest hin. */
    private fun AvatarAnimations.anchor(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(12 to 0, 12 to 1)).beat(FAST_MS),
        creatureFrame(body, prop = listOf(12 to 1, 12 to 2, 11 to 3, 13 to 3)).beat(FAST_MS),
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(12 to 2, 12 to 3, 11 to 4, 13 to 4)).beat(BEAT_MS),
        // Gefasst - kurzer Ruck, dann steht alles.
        creatureFrame(body, dy = 1, feetSpread = 1, accentPhase = 1, prop = listOf(12 to 3, 12 to 4, 11 to 4, 13 to 4, 10 to 4, 14 to 4)).beat(FAST_MS),
        creatureFrame(body, dy = 1, feetSpread = 1, accentPhase = -1, prop = listOf(12 to 3, 12 to 4, 11 to 4, 13 to 4)).beat(SLOW_MS),
        creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
    )

    // ================= GLOOP: der Entschleuniger =================

    /** Er passt sich dem Tempo an - langsamer geht es in diesem ganzen Satz nicht. */
    private fun AvatarAnimations.turtle(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(1 to 4, 2 to 4, 3 to 4)).beat(SLOW_MS),
        creatureFrame(body, dx = -1, eyeHoles = body.eyesHalf, prop = listOf(2 to 4, 3 to 4, 4 to 4)).beat(SLOW_MS),
        creatureFrame(body, dx = -1, accentPhase = 1, prop = listOf(3 to 4, 4 to 4, 5 to 4)).beat(SLOW_MS),
        creatureFrame(body, dx = 0, mouthHoles = body.mouthOpen, prop = listOf(4 to 4, 5 to 4, 6 to 4)).beat(SLOW_MS),
        creatureFrame(body, dx = 1, accentPhase = -1, prop = listOf(5 to 4, 6 to 4, 7 to 4)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, accentPhase = 1).beat(SETTLE_MS)
    )

    /** Die Wolke zieht, und er zieht einfach mit - ohne jeden Grund, es eilig zu haben. */
    private fun AvatarAnimations.cloud(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(1 to 2, 2 to 2, 3 to 2, 2 to 1)).beat(SLOW_MS),
        creatureFrame(body, dx = -2, eyeHoles = body.eyesHalf, prop = listOf(3 to 2, 4 to 2, 5 to 2, 4 to 1)).beat(SLOW_MS),
        creatureFrame(body, dx = 0, accentPhase = 1, prop = listOf(6 to 2, 7 to 2, 8 to 2, 7 to 1)).beat(SLOW_MS),
        creatureFrame(body, dx = 2, accentPhase = -1, prop = listOf(9 to 2, 10 to 2, 11 to 2, 10 to 1)).beat(SLOW_MS),
        creatureFrame(body, dx = 3, eyeHoles = body.eyesHalf, prop = listOf(12 to 2, 13 to 2, 14 to 2, 13 to 1)).beat(SLOW_MS),
        creatureFrame(body, dx = 1, accentPhase = 1).beat(SETTLE_MS)
    )

    /** Ein Tropfen sammelt sich auf ihm, wird schwer und faellt ab - er wabbelt kurz nach. */
    private fun AvatarAnimations.drip(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(8 to 2)).beat(SLOW_MS),
        creatureFrame(body, prop = listOf(8 to 2, 8 to 3)).beat(SLOW_MS),
        creatureFrame(body, accentPhase = -1, prop = listOf(8 to 2, 8 to 3, 7 to 3, 9 to 3)).beat(BEAT_MS),
        // Abgerissen.
        creatureFrame(body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(8 to 4)).beat(FAST_MS),
        creatureFrame(body, dy = 1, accentPhase = 1, prop = listOf(7 to 4, 8 to 4, 9 to 4)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
    )

    /** Er laesst sich fallen, zerlaeuft breit - und zieht sich gemuetlich wieder zusammen. */
    private fun AvatarAnimations.puddle(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, dy = -2, mouthHoles = body.mouthOpen).beat(FAST_MS),
        creatureFrame(body, dy = 1, accentPhase = 1, prop = listOf(2 to 4, 13 to 4)).beat(FAST_MS),
        // Zerlaufen: breit, flach, Augen halb.
        creatureFrame(body, dy = 2, accentPhase = 1, eyeHoles = body.eyesHalf, prop = listOf(1 to 4, 2 to 4, 13 to 4, 14 to 4)).beat(SLOW_MS),
        creatureFrame(body, dy = 2, accentPhase = 1, prop = listOf(0 to 4, 15 to 4)).beat(BEAT_MS),
        // Wieder zusammen.
        creatureFrame(body, dy = 1, accentPhase = -1).beat(SLOW_MS),
        creatureFrame(body, accentPhase = 1, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
    )

    /** Der Ballon steigt; er streckt sich lang hinterher und plumpst dann wieder zusammen. */
    private fun AvatarAnimations.balloon(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(7 to 4, 8 to 4, 7 to 3, 8 to 3)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1, prop = listOf(7 to 2, 8 to 2, 7 to 1, 8 to 1)).beat(BEAT_MS),
        creatureFrame(body, dy = -2, accentPhase = -1, mouthHoles = body.mouthOpen, prop = listOf(7 to 1, 8 to 1, 7 to 0, 8 to 0)).beat(SLOW_MS),
        creatureFrame(body, dy = -3, accentPhase = -1, prop = listOf(7 to 0, 8 to 0)).beat(BEAT_MS),
        // Verpasst - und das ist voellig in Ordnung.
        creatureFrame(body, dy = 1, accentPhase = 1, eyeHoles = body.eyesHalf).beat(SLOW_MS),
        creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
    )

    // ================= HOOTLET: der weise Beobachter =================

    /** Ein zweites Auge sieht ihn an; er blinzelt zurueck, sonst bewegt sich fast nichts. */
    private fun AvatarAnimations.eye(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(6 to 1, 7 to 1, 8 to 1, 9 to 1)).beat(SLOW_MS),
        creatureFrame(body, prop = listOf(5 to 1, 10 to 1, 6 to 0, 7 to 0, 8 to 0, 9 to 0, 7 to 2, 8 to 2)).beat(SLOW_MS),
        // Blickwechsel - nur die Pupille wandert.
        creatureFrame(body, prop = listOf(5 to 1, 10 to 1, 6 to 0, 9 to 0, 6 to 2, 7 to 1)).beat(BEAT_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(5 to 1, 10 to 1, 6 to 0, 9 to 0, 9 to 2, 8 to 1)).beat(BEAT_MS),
        // Beide blinzeln gleichzeitig.
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(5 to 1, 6 to 1, 7 to 1, 8 to 1, 9 to 1, 10 to 1)).beat(SLOW_MS),
        creatureFrame(body, accentPhase = 1, prop = listOf(6 to 1, 7 to 1, 8 to 1, 9 to 1)).beat(SETTLE_MS)
    )

    /** Der Schluessel dreht sich - und er nickt einmal, als haette er es laengst gewusst. */
    private fun AvatarAnimations.key(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(1 to 2, 2 to 2, 3 to 2)).beat(BEAT_MS),
        creatureFrame(body, prop = listOf(2 to 2, 3 to 2, 4 to 2, 4 to 3)).beat(BEAT_MS),
        // Umdrehen: der Bart klappt um.
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(2 to 2, 3 to 2, 4 to 2, 4 to 1)).beat(SLOW_MS),
        creatureFrame(body, prop = listOf(2 to 2, 3 to 2, 4 to 2, 4 to 3, 5 to 1)).beat(FAST_MS),
        // Das Nicken.
        creatureFrame(body, dy = 1, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(2 to 2, 3 to 2, 4 to 2)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Er wartet die Sanduhr aus - fast reglos, und genau das ist die Reaktion. */
    private fun AvatarAnimations.hourglass(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(13 to 0, 14 to 0, 13 to 1, 14 to 1, 13 to 4, 14 to 4)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(13 to 0, 14 to 0, 13 to 2, 13 to 4, 14 to 4)).beat(SLOW_MS),
        creatureFrame(body, prop = listOf(13 to 0, 13 to 2, 13 to 3, 13 to 4, 14 to 4)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(13 to 2, 13 to 3, 13 to 4, 14 to 4, 14 to 3)).beat(SLOW_MS),
        // Durchgelaufen - erst jetzt eine Regung.
        creatureFrame(body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(13 to 3, 14 to 3, 13 to 4, 14 to 4)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Er liest - der Blick wandert zeilenweise, der Koerper bleibt stehen. */
    private fun AvatarAnimations.scroll(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(6 to 2, 9 to 2)).beat(BEAT_MS),
        creatureFrame(body, prop = listOf(5 to 2, 10 to 2, 6 to 1, 7 to 1, 8 to 1, 9 to 1)).beat(BEAT_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = listOf(5 to 2, 10 to 2, 6 to 1, 7 to 1, 8 to 1, 9 to 1, 6 to 3, 7 to 3)).beat(SLOW_MS),
        creatureFrame(body, dx = 1, eyeHoles = body.eyesHalf, prop = listOf(5 to 2, 10 to 2, 6 to 1, 9 to 1, 8 to 3, 9 to 3)).beat(SLOW_MS),
        // Verstanden.
        creatureFrame(body, dx = 0, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(5 to 2, 10 to 2, 7 to 1, 8 to 1)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )

    /** Er legt den Kopf schief, bis das Teil einrastet - dann richtet er sich auf. */
    private fun AvatarAnimations.puzzle(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(11 to 0, 12 to 0, 11 to 1, 12 to 1)).beat(BEAT_MS),
        creatureFrame(body, dx = 1, eyeHoles = body.eyesHalf, prop = listOf(10 to 1, 11 to 1, 10 to 2, 11 to 2)).beat(BEAT_MS),
        creatureFrame(body, dx = -1, prop = listOf(9 to 2, 10 to 2, 9 to 3, 10 to 3)).beat(BEAT_MS),
        // Eingerastet.
        creatureFrame(body, dx = 0, mouthHoles = body.mouthOpen, prop = listOf(6 to 3, 7 to 3, 8 to 3, 9 to 3)).beat(FAST_MS),
        creatureFrame(body, dy = -2, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(5 to 3, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 10 to 3)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1).beat(SETTLE_MS)
    )
}
