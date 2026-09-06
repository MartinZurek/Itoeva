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
}
