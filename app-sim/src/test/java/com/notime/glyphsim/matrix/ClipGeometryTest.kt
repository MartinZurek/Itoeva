package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was sich am Filmen OHNE Geraet pruefen laesst.
 *
 * Ehrlich zur Reichweite dieser Pruefungen: Ob der Videokodierer des jeweiligen Geraets die Datei
 * am Ende wirklich schreibt, laesst sich hier nicht feststellen - dafuer braucht es echte
 * Hardware. Pruefbar sind die Dinge, die davor schiefgehen koennen und dann eine Datei erzeugen,
 * die zwar entsteht, aber falsch aussieht: unpassende Bildmasse und eine Helligkeitsumrechnung,
 * die nicht stimmt.
 */
class ClipGeometryTest {

    @Test
    fun `die Bildmasse sind fuer Videokodierer geeignet`() {
        // Kodierer arbeiten blockweise und lehnen ungerade Kantenlaengen haeufig ab. Ein Film,
        // der deshalb gar nicht erst entsteht, waere ein aergerlicher Fehler fuer eine Zahl, die
        // sich hier festhalten laesst.
        assertEquals(0, PlayClipRenderer.DEFAULT_WIDTH % 2)
        assertEquals(0, PlayClipRenderer.DEFAULT_HEIGHT % 2)
        // Hochformat, wie es soziale Netzwerke erwarten.
        assertTrue(
            "Das Bild ist nicht im Hochformat",
            PlayClipRenderer.DEFAULT_HEIGHT > PlayClipRenderer.DEFAULT_WIDTH
        )
        val ratio = PlayClipRenderer.DEFAULT_HEIGHT.toDouble() / PlayClipRenderer.DEFAULT_WIDTH
        assertTrue("Seitenverhaeltnis $ratio weicht zu weit von 16:9 ab", ratio in 1.7..1.8)
    }

    @Test
    fun `eine Aufnahme sammelt Beschreibungen und stoppt an der Obergrenze`() {
        // Der Kern der Aufnahme: Sie darf nicht unbegrenzt weiterlaufen. Eine vergessene
        // Aufnahme wuerde sonst stundenlang mitschreiben und waere danach nicht mehr zu
        // verarbeiten - jedes Bild muss einzeln gezeichnet und umgerechnet werden.
        val session = PlayClipRecorder.Session()
        val frame = PlayClipRenderer.Frame(
            place = PlayScene.Place.LIVING,
            species = AvatarSpecies.PUFFLING,
            dayPhase = PlayAmbientActivity.DayPhase.MIDDAY,
            avatarFrame = AvatarAnimations.idlePose(AvatarSpecies.PUFFLING),
            avatarAnchorX = 0.5f,
            scenePhase = 0
        )
        // Deutlich mehr anbieten, als hineinpasst.
        repeat(PlayClipRecorder.MAX_SECONDS * 20) { session.add(frame) }
        assertTrue("Die Aufnahme laeuft ueber die Obergrenze hinaus", session.isFull)
        assertTrue(
            "Die Aufnahme ist mit ${session.seconds} s laenger als erlaubt",
            session.seconds <= PlayClipRecorder.MAX_SECONDS
        )
    }

    @Test
    fun `die Obergrenze laesst wirklich lange Szenen zu`() {
        // Der Nutzer wollte ausdruecklich mehr als die urspruenglichen rund fuenfzehn Sekunden -
        // "auch fuer die grossen Szenen".
        assertTrue(
            "Mit ${PlayClipRecorder.MAX_SECONDS} s bleibt zu wenig Spielraum fuer eine Szene",
            PlayClipRecorder.MAX_SECONDS >= 60
        )
    }

    @Test
    fun `eine leere Aufnahme ergibt keinen Film`() {
        assertTrue("Eine leere Aufnahme meldet Bilder", PlayClipRecorder.Session().frameCount == 0)
    }

    /**
     * Die Helligkeitsumrechnung ist die Stelle, an der ein Film "entsteht, aber falsch aussieht".
     * Nachgerechnet wird dieselbe Formel wie im Kodierer - stimmt sie nicht, waere das Bild
     * flau oder ueberstrahlt, und man saehe es erst am fertigen Video.
     */
    private fun luma(r: Int, g: Int, b: Int): Int =
        (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)

    @Test
    fun `Schwarz und das warme Weiss landen an den richtigen Enden`() {
        val black = luma(0, 0, 0)
        val ledOn = luma(0xF3, 0xF1, 0xEA)
        assertEquals("Schwarz muss auf dem Fusspunkt der Fernsehnorm liegen", 16, black)
        // Die Norm reicht von 16 (Schwarz) bis 235 (reines Weiss). Unser Leuchtton ist bewusst
        // ein gedaempftes Warmweiss und liegt deshalb knapp darunter - er DARF die 235 nicht
        // erreichen, sonst waere die Anmutung im Film eine andere als auf dem Bildschirm.
        assertTrue("Das helle Weiss ist zu dunkel geraten: $ledOn", ledOn >= 215)
        assertTrue("Das helle Weiss uebersteuert: $ledOn", ledOn < 235)
        assertTrue("Schwarz und Weiss liegen zu dicht beieinander", ledOn - black > 180)
    }

    @Test
    fun `die Helligkeit steigt gleichmaessig mit der Zellhelligkeit`() {
        // Die Kulisse lebt von ihrer Staffelung (Boden schwach, Moebel mittel, Licht hell). Wenn
        // die Umrechnung diese Reihenfolge nicht erhaelt, ist im Film die Tiefe weg.
        val stufen = listOf(
            PlayScene.STRUCTURE, PlayScene.BACKDROP, PlayScene.FURNITURE, PlayScene.GLOW,
            AvatarGeometry.MAX_BRIGHTNESS
        )
        val werte = stufen.map { brightness ->
            val f = brightness.toFloat() / AvatarGeometry.MAX_BRIGHTNESS
            luma((0xF3 * f).toInt(), (0xF1 * f).toInt(), (0xEA * f).toInt())
        }
        for (i in 1 until werte.size) {
            assertTrue(
                "Stufe ${stufen[i]} ist im Film nicht heller als ${stufen[i - 1]} " +
                    "(${werte[i]} gegen ${werte[i - 1]})",
                werte[i] > werte[i - 1]
            )
        }
    }
}
