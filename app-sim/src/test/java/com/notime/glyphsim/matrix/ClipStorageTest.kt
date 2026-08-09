package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests fuer [ClipStorage] - die Rechnung hinter "passt noch eine Aufnahme?".
 *
 * Reine Arithmetik ohne Android, laeuft deshalb als JVM-Test mit. Die Abfragen beim Dateisystem
 * stehen bewusst getrennt davon in [PlayClipRecorder], damit genau dieser Teil ohne Geraet
 * pruefbar bleibt.
 */
class ClipStorageTest {

    private val einMB = 1024L * 1024
    private val einGB = 1024L * einMB

    // --- Platzbedarf ---------------------------------------------------------------------------

    /**
     * 6 Mbit/s sind 750 KB je Sekunde. Eine Minute liegt damit bei rund 45 MB - die Zahl, die
     * ueberhaupt der Anlass fuer diese ganze Phase war.
     */
    @Test
    fun `eine Minute Aufnahme liegt bei rund 45 MB`() {
        val eineMinute = ClipStorage.estimatedBytes(60)

        assertTrue("erwartet ueber 40 MB, war $eineMinute", eineMinute > 40 * einMB)
        assertTrue("erwartet unter 50 MB, war $eineMinute", eineMinute < 50 * einMB)
    }

    @Test
    fun `null Sekunden brauchen keinen Platz`() {
        assertEquals(0L, ClipStorage.estimatedBytes(0))
    }

    /** Eine negative Laenge ist ein Programmfehler - sie darf keinen negativen Bedarf ergeben. */
    @Test
    fun `negative Laenge ergibt keinen negativen Bedarf`() {
        assertEquals(0L, ClipStorage.estimatedBytes(-30))
    }

    // --- Passt es noch? ------------------------------------------------------------------------

    @Test
    fun `auf einem leeren Geraet passt die laengste Aufnahme`() {
        assertTrue(ClipStorage.hasRoomFor(seconds = 90, freeBytes = 8 * einGB))
    }

    @Test
    fun `bei vollem Speicher passt nichts mehr`() {
        assertFalse(ClipStorage.hasRoomFor(seconds = 90, freeBytes = 0))
    }

    /**
     * Der eigentliche Zweck des Sicherheitsabstands: es ist nicht genug, dass die Aufnahme gerade
     * so hineinpasst. Ein Geraet, dessen Speicher bis aufs letzte Byte gefuellt ist, verhaelt sich
     * in jeder Hinsicht unangenehm - Datenbankschreibvorgaenge scheitern, das System warnt.
     */
    @Test
    fun `gerade so hineinpassen genuegt nicht`() {
        val bedarf = ClipStorage.estimatedBytes(90)

        assertFalse(
            "Ohne Sicherheitsabstand wuerde die Aufnahme den Speicher bis zum Rand fuellen",
            ClipStorage.hasRoomFor(seconds = 90, freeBytes = bedarf)
        )
        assertTrue(ClipStorage.hasRoomFor(seconds = 90, freeBytes = bedarf + ClipStorage.HEADROOM_BYTES))
    }

    // --- Belegter Platz ------------------------------------------------------------------------

    @Test
    fun `ohne Aufnahmen ist nichts belegt`() {
        assertEquals(0L, ClipStorage.usedBytes(emptyList()))
    }

    // --- Anzeige -------------------------------------------------------------------------------

    /**
     * Die Anzeige ist das Einzige, woran der Nutzer erkennt, worueber er entscheidet - diese App
     * loescht nichts von allein. Sie muss deshalb auf einen Blick einzuordnen sein: "1434 MB" ist
     * es nicht, "1,4 GB" schon.
     */
    @Test
    fun `Groessen werden lesbar dargestellt`() {
        assertEquals("512 B", ClipStorage.formatBytes(512))
        assertEquals("2 KB", ClipStorage.formatBytes(2 * 1024))
        assertEquals("45,0 MB", ClipStorage.formatBytes(45 * einMB).replace('.', ','))
        assertEquals("1,4 GB", ClipStorage.formatBytes((1.4 * einGB).toLong()).replace('.', ','))
    }

    @Test
    fun `an der Grenze wird auf die groessere Einheit gewechselt`() {
        // Knapp unter einem Megabyte bleibt es bei Kilobyte - 1048575 / 1024 = 1023.
        assertEquals("1023 KB", ClipStorage.formatBytes(einMB - 1))
        assertTrue(ClipStorage.formatBytes(einMB).endsWith("MB"))
        assertTrue(ClipStorage.formatBytes(einGB - 1).endsWith("MB"))
        assertTrue(ClipStorage.formatBytes(einGB).endsWith("GB"))
    }

    // --- Bruchstuecke --------------------------------------------------------------------------

    /**
     * Kodiert wird unter `.part` und erst bei Erfolg umbenannt. Damit ist eine `.mp4` per
     * Konstruktion vollstaendig - und ein Abbruch hinterlaesst etwas, das sich eindeutig als
     * Bruchstueck erkennen laesst, statt in der Galerie als kaputter Film zu stehen.
     */
    @Test
    fun `Bruchstuecke werden an ihrer Endung erkannt`() {
        val dateien = listOf(
            File("tama-1.mp4"),
            File("tama-2.mp4" + ClipStorage.PARTIAL_SUFFIX),
            File("tama-3.png"),
            File("tama-4.mp4" + ClipStorage.PARTIAL_SUFFIX)
        )

        assertEquals(
            listOf("tama-2.mp4.part", "tama-4.mp4.part"),
            ClipStorage.partialFiles(dateien).map { it.name }
        )
    }

    @Test
    fun `eine fertige Aufnahme gilt nie als Bruchstueck`() {
        assertTrue(ClipStorage.partialFiles(listOf(File("tama-1.mp4"))).isEmpty())
    }
}
