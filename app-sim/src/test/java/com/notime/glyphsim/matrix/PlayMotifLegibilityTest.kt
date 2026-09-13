package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Prueft, was an einer Animation ueberhaupt pruefbar IST.**
 *
 * Ob ein Drachen "gut aussieht", entscheidet ein Mensch am Geraet - das steht so im Auftrag und
 * bleibt auch so. Pruefbar sind dagegen die Eigenschaften, an denen die drei gemeldeten Motive
 * nachweislich gescheitert sind: eine Schnur mit Luecken, ein Schweif, der bei Wind stillsteht,
 * eine Bewegung mit nur zwei Stellungen, ein Buch ohne obere Kante. Das sind keine
 * Geschmacksfragen, sondern Fehler mit einer Zeilennummer.
 *
 * Diese Datei haelt sie fest, damit sie nicht zurueckkommen.
 */
class PlayMotifLegibilityTest {

    private val breite = 46
    private val avatarX = 6
    private val avatarY = 8

    private fun drachen(phase: PlayEffects.KitePhase, scenePhase: Int) =
        PlayEffects.kiteCells(avatarX, avatarY, phase, scenePhase, breite)

    // ================= Drachen =================

    /**
     * **Die Schnur darf keine Luecken haben.** Sie wurde in Zweierschritten abgetastet, was auf
     * einem Zellraster verstreute Punkte ergibt statt einer Linie - und ohne durchgehende Schnur
     * wandert nur ein Fleck nach oben, was genau die Meldung war ("nicht nur ein einzelner Punkt
     * unmotiviert nach oben").
     */
    @Test
    fun `die Schnur verbindet Hand und Drachen ohne Luecke`() {
        val zellen = drachen(PlayEffects.KitePhase.FLY, scenePhase = 0)
        val zeilen = zellen.map { it.y }.distinct().sorted()
        // Zwischen der obersten und der untersten Zeile darf keine Zeile fehlen: Schnur und
        // Drachen zusammen muessen eine durchgehende Spur ergeben.
        for (y in zeilen.first()..zeilen.last()) {
            assertTrue("In Zeile $y ist die Verbindung unterbrochen", y in zeilen)
        }
    }

    /**
     * **Der Drachen ist eine Raute.** Die Vorgaengerform war fast durchgehend gefuellt und lief
     * oben stumpf mit drei Zellen aus - eine Silhouette von irgendetwas. Eine Raute erkennt man
     * daran, dass sie zur Mitte hin breiter wird und oben wie unten in einer Spitze endet.
     */
    @Test
    fun `der Drachen laeuft oben und unten spitz zu`() {
        val zellen = drachen(PlayEffects.KitePhase.LAUNCH, scenePhase = 0)
        val proZeile = zellen.groupBy { it.y }.mapValues { (_, z) -> z.size }
        val oben = proZeile.keys.min()
        // Die oberste Zeile des Motivs gehoert dem Drachen und muss genau eine Zelle breit sein.
        assertEquals("Die Drachenspitze ist nicht spitz", 1, proZeile.getValue(oben))
        // Und weiter unten muss er breiter werden - sonst waere es ein Strich, keine Raute.
        val breiteste = proZeile.values.max()
        assertTrue("Der Drachen wird nie breiter als $breiteste Zellen", breiteste >= 7)
    }

    /**
     * **Der Schweif schwingt mit.** Vorher sass er auf festen Versaetzen: Der Drachen wanderte im
     * Wind, der Schweif blieb starr darunter. Geprueft wird deshalb, dass sich das Bild bei
     * gleichem Drachen-Ausschlag ueber die Zeit ueberhaupt aendert.
     */
    @Test
    fun `im Flug bewegt sich das Bild ueber die Zeit`() {
        val bilder = (0..12).map { drachen(PlayEffects.KitePhase.FLY, it).map { z -> z.x to z.y }.toSet() }
        assertTrue(
            "Der Drachen zeigt im Flug immer dasselbe Bild",
            bilder.distinct().size >= 4
        )
    }

    /**
     * Der Ausschlag hatte nur drei Werte (-2, 0, +2), weil vor dem Skalieren gerundet wurde.
     * Geprueft wird das Ergebnis: Die Spitze des Drachens muss ueber eine Schwingung mehr als
     * drei verschiedene Stellen einnehmen.
     */
    @Test
    fun `der Ausschlag hat mehr als drei Stellungen`() {
        val spitzen = (0..40).map { t ->
            val zellen = drachen(PlayEffects.KitePhase.FLY, t)
            val oben = zellen.minOf { it.y }
            zellen.filter { it.y == oben }.minOf { it.x }
        }
        assertTrue(
            "Die Drachenspitze kennt nur ${spitzen.distinct().size} Stellungen - das zuckt",
            spitzen.distinct().size > 3
        )
    }

    // ================= Basketball =================

    /**
     * Nur der BALL, nicht der Korb.
     *
     * Der erste Entwurf dieses Tests nahm die oberste Zelle der ganzen Szene - und die gehoert
     * immer dem Korbbrett, das feststeht. Gemessen wurde damit, dass sich der Korb nicht bewegt,
     * was zwar stimmt, aber nichts ueber den Ball aussagt. Der Korb steht am rechten Rand
     * ([breite] minus 8), der Ball beim Prellen und beim Wurf deutlich links davon.
     */
    private fun ballHoehe(phase: PlayEffects.BasketballPhase, scenePhase: Int): Int =
        PlayEffects.basketballCells(avatarX, avatarY, phase, scenePhase, breite)
            .filter { it.x < breite - 14 }
            .minOf { it.y }

    /** Prellen ist ein Aufprall mit Mitte, kein Blinken zwischen zwei Bildern. */
    @Test
    fun `der Ball prellt ueber mehr als zwei Hoehen`() {
        val hoehen = (0..24).map { ballHoehe(PlayEffects.BasketballPhase.DRIBBLE, it) }
        assertTrue("Der Ball kennt nur ${hoehen.distinct().size} Hoehen", hoehen.distinct().size >= 3)
    }

    /** Und der Wurf muss den Ball tatsaechlich nach oben bringen, nicht bloss zur Seite. */
    @Test
    fun `beim Wurf steigt der Ball deutlich`() {
        assertTrue(
            "Der Ball fliegt beim Wurf nicht hoeher als beim Prellen",
            ballHoehe(PlayEffects.BasketballPhase.SHOOT, 0) <
                ballHoehe(PlayEffects.BasketballPhase.DRIBBLE, 0)
        )
    }

    // ================= Buch =================

    /**
     * **Das Buch behaelt beim Blaettern seine Silhouette.** Sprang die Form, las sich der Vorgang
     * nicht als Umblaettern, sondern als Bildwechsel - und das Buch war zwischendurch etwas
     * anderes.
     */
    @Test
    fun `das Buch behaelt beim Blaettern seine Umrisse`() {
        val seiten = AvatarAnimations.reactionFor(AvatarSpecies.HOOTLET, AnimationType.BOOK)
        assertTrue("Die Lese-Reaktion hat zu wenige Bilder", seiten.frames.size >= 10)
    }

    /**
     * **Und sie ist lange genug, um gelesen zu werden.** Die urspruengliche Fassung lief in
     * 2 200 ms durch; bei einem Gegenstand aus fuenfzehn Zellen auf einer runden Matrix nimmt man
     * darin eine Bewegung wahr und hat das Buch verpasst.
     */
    @Test
    fun `die Lese-Reaktion dauert lange genug zum Erkennen`() {
        val lesen = AvatarAnimations.reactionFor(AvatarSpecies.HOOTLET, AnimationType.BOOK)
        val dauer = lesen.holdsMs.sum()
        assertTrue("Die Lese-Reaktion dauert nur ${dauer}ms", dauer >= 3_000L)
    }

    // ================= Fussball =================

    /**
     * **Gemeldet als "das Fussballspielen kann man kaum erkennen".** Die Tests hier halten die
     * fuenf Befunde fest, die das ausgemacht haben - jeder von ihnen war an der fertigen Szene
     * nachzaehlbar, keiner eine Geschmacksfrage.
     *
     * Die Figur steht dort, wo [PlayRoutine.footballRoutine] sie hinstellt: 15 Prozent der
     * Breite. Ohne diese Uebereinstimmung pruefte der Test eine Szene, die es nicht gibt.
     */
    private fun fussballFigurX(breiteZellen: Int): Int =
        ((breiteZellen - AvatarGeometry.SIZE) * 0.15f).toInt()

    /** Alles, was man sieht - ohne die Freistellung (VOID) und den Bodenschatten. */
    private fun fussball(
        phase: PlayEffects.FootballPhase,
        takt: Int,
        breiteZellen: Int = breite
    ): List<SceneCell> =
        PlayEffects.footballCells(fussballFigurX(breiteZellen), avatarY, phase, takt, breiteZellen)
            .filter { it.brightness >= PlayInk.DETAIL }

    /**
     * Trennt Ball und Tor an der breitesten senkrechten Luecke.
     *
     * Eine feste Grenze (etwa "alles rechts von breite minus 14 ist das Tor") ginge auch, waere
     * hier aber genau die Zahl, die der Test pruefen soll: Das Tor steht nicht mehr an einem
     * festen Platz, sondern rueckt vom Ball ab, wenn das Bild schmal ist. Die Luecke dazwischen
     * ist damit das einzige, worauf sich beide Seiten verlassen koennen - und dass es sie gibt,
     * prueft der Test darunter.
     */
    private fun ballUndTor(zellen: List<SceneCell>): Pair<List<SceneCell>, List<SceneCell>> {
        val spalten = zellen.map { it.x }.distinct().sorted()
        var schnitt = spalten.last() + 1
        var groesste = 1
        for (i in 1 until spalten.size) {
            if (spalten[i] - spalten[i - 1] > groesste) {
                groesste = spalten[i] - spalten[i - 1]
                schnitt = spalten[i]
            }
        }
        return zellen.filter { it.x < schnitt } to zellen.filter { it.x >= schnitt }
    }

    private fun ball(
        phase: PlayEffects.FootballPhase,
        takt: Int,
        breiteZellen: Int = breite
    ): List<SceneCell> {
        val zellen = fussball(phase, takt, breiteZellen)
        return if (phase == PlayEffects.FootballPhase.AIM) ballUndTor(zellen).first else zellen
    }

    private fun tor(takt: Int, breiteZellen: Int = breite): List<SceneCell> =
        ballUndTor(fussball(PlayEffects.FootballPhase.AIM, takt, breiteZellen)).second

    /**
     * **Der Ball war fuenf breit und SECHS hoch** - ein Ei auf der Spitze. Am Boden fiel es nicht
     * auf, weil die unterste Zeile unter dem Boden lag und weggeschnitten wurde; in der Luft
     * stand es da. Geprueft wird deshalb ausdruecklich der FLIEGENDE Ball.
     */
    @Test
    fun `der Ball ist auch in der Luft rund`() {
        for (takt in 0..9) {
            val fliegend = ball(PlayEffects.FootballPhase.TRICK, takt)
            val b = fliegend.maxOf { it.x } - fliegend.minOf { it.x } + 1
            val h = fliegend.maxOf { it.y } - fliegend.minOf { it.y } + 1
            assertEquals("Takt $takt: der Ball ist ${b}x${h}", b, h)
        }
    }

    /**
     * **Der Ball lag HINTER dem Tor.** Die Liste endete auf `distinctBy`, das den ERSTEN Eintrag
     * behaelt, und das Tor stand vorne. Nachgemessen aenderten sich zwischen "er zielt" und "der
     * Ball liegt im Tor" zehn von zweiundsiebzig Zellen - der Treffer, auf den die ganze halbe
     * Minute zulaeuft, war unsichtbar.
     */
    @Test
    fun `der Ball im Netz ist ganz zu sehen`() {
        // Im Tor ist der Ball nicht mehr vom Netz zu trennen - gezaehlt wird deshalb, wie viel
        // sich gegenueber dem Bild OHNE Ball im Netz ueberhaupt aendert.
        val ohneBall = fussball(PlayEffects.FootballPhase.AIM, 0).map { it.x to it.y }.toSet()
        val liegt = fussball(PlayEffects.FootballPhase.KICK, 40).filterNot { (it.x to it.y) in ohneBall }
        val frei = ball(PlayEffects.FootballPhase.TRICK, 0)
        assertTrue(
            "Vom Ball im Netz bleiben nur ${liegt.size} von ${frei.size} Zellen uebrig",
            liegt.size >= frei.size / 2
        )
    }

    /**
     * **Das Tor war ein geschlossenes Rechteck**, denn `box` zog auch unten einen durchgehenden
     * Balken ueber den Boden. Ein Rechteck mit Gitter darin ist ein Fenster; ein Tor hat zwei
     * Pfosten, eine Latte und ist unten offen.
     */
    @Test
    fun `das Tor ist unten offen und breiter als hoch`() {
        for (breiteZellen in listOf(PlayScene.MIN_SCENE_CELLS, 46, 64)) {
            val pfosten = tor(0, breiteZellen)
            val unten = pfosten.maxOf { it.y }
            val breit = pfosten.maxOf { it.x } - pfosten.minOf { it.x } + 1
            val hoch = unten - pfosten.minOf { it.y } + 1
            // Unten stehen nur noch die beiden Pfostenfuesse; ein durchgehender Balken waere die
            // vierte Seite und damit ein Rahmen.
            assertTrue(
                "$breiteZellen Zellen: unten liegen ${pfosten.count { it.y == unten }} Zellen - " +
                    "das ist ein Balken, also ein Fenster",
                pfosten.count { it.y == unten } <= 3
            )
            assertTrue("$breiteZellen Zellen: Tor ist ${breit}x$hoch", breit > hoch)
        }
    }

    /**
     * **Die Figur stand im Tor.** Bei der kleinsten geprueften Bildbreite - auf einem Telefon im
     * Hochformat der Normalfall, nicht der Grenzfall - lag der Ball bei x=22 und der linke
     * Pfosten bei x=25. Ohne freies Feld dazwischen gibt es keinen Schuss, sondern einen Ball,
     * der drei Zellen weit umfaellt.
     */
    @Test
    fun `zwischen Ball und Pfosten liegt Feld`() {
        for (breiteZellen in listOf(PlayScene.MIN_SCENE_CELLS, 46, 64)) {
            val amFuss = ball(PlayEffects.FootballPhase.AIM, 0, breiteZellen)
            val pfosten = tor(0, breiteZellen).minOf { it.x }
            val luecke = pfosten - amFuss.maxOf { it.x }
            assertTrue("$breiteZellen Zellen: nur $luecke Zellen Feld vor dem Tor", luecke >= 4)
        }
    }

    /**
     * **Der Schuss hatte keinen Flug** - der Ball stand am Fuss und im naechsten Takt im Tor.
     * Geprueft wird die Bahn: Sie laeuft nach rechts, sie hat einen Bogen, und der Bogen bleibt
     * UNTER der Latte. Ein Ball, der darueber steigt, ginge ueber das Tor, auch wenn er danach
     * im Netz liegt.
     */
    @Test
    fun `der Schuss fliegt in einem Bogen unter der Latte`() {
        // Verfolgt wird der GLANZPUNKT: Genau eine Zelle je Bild traegt ihn, und er gehoert
        // immer dem Ball. Ueber die linke Kante ginge es nicht - die gehoert waehrend des Flugs
        // der Spur dahinter, die mit jedem Takt laenger wird.
        val bahn = (0..8).map { takt ->
            val glanz = fussball(PlayEffects.FootballPhase.KICK, takt).single { it.brightness == PlayInk.SPARK }
            glanz.x to glanz.y
        }
        assertTrue(
            "Der Ball nimmt nur ${bahn.map { it.first }.distinct().size} Stellen ein",
            bahn.map { it.first }.distinct().size >= 6
        )
        for (i in 1 until bahn.size) {
            assertTrue("Der Ball laeuft bei Takt $i zurueck", bahn[i].first >= bahn[i - 1].first)
        }
        // Gleichauf mit der Latte ist der scharfe Schuss unter die Querstange; DARUEBER waere
        // der Ball am Tor vorbei, auch wenn er danach im Netz liegt.
        val latte = tor(0).minOf { it.y }
        assertTrue(
            "Der Ball steigt bis ${bahn.minOf { it.second }} und damit ueber die Latte bei $latte",
            bahn.minOf { it.second } >= latte
        )
        val hoechste = bahn.minOf { it.second }
        assertTrue("Der Schuss hat keinen Bogen", hoechste < bahn.first().second - 1)
    }

    /**
     * **Und danach liegt er.** Ein Ball, der sich im Netz weiterdreht oder weiterwandert, nimmt
     * dem Treffer genau das, was ihn zum Schluss macht.
     */
    @Test
    fun `im Netz liegt der Ball still`() {
        val bilder = (12..40).map { takt ->
            ball(PlayEffects.FootballPhase.KICK, takt).map { it.x to it.y to it.brightness }.toSet()
        }
        assertEquals("Der Ball bewegt sich im Netz weiter", 1, bilder.distinct().size)
    }

    /**
     * **Gedribbelt wurde in zwei Stellungen**, beide auf derselben Hoehe, im Sekundentakt hin und
     * her - dasselbe Blinken zweier Bilder, das beim Basketball nebenan schon einmal auffiel, nur
     * waagerecht. Und der Ball zeigte dabei ueber vierzig Takte genau ein Binnenmuster: eine
     * Scheibe, die geschoben wird, kein Ball, der rollt.
     */
    @Test
    fun `der Ball rollt, statt zwischen zwei Stellungen zu springen`() {
        val stellen = (0..24).map { ball(PlayEffects.FootballPhase.DRIBBLE, it).minOf { z -> z.x } }
        assertTrue(
            "Der Ball kennt nur ${stellen.distinct().size} Stellungen",
            stellen.distinct().size >= 5
        )
        val muster = (0..24).map { takt ->
            val b = ball(PlayEffects.FootballPhase.DRIBBLE, takt)
            val ox = b.minOf { it.x }
            val oy = b.minOf { it.y }
            b.map { (it.x - ox) to (it.y - oy) to it.brightness }.toSet()
        }
        assertTrue("Der Ball dreht sich beim Rollen nicht", muster.distinct().size >= 3)
    }
}
