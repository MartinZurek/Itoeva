package com.notime.glyphcore.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.Deflater
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Das Vorschau-Werkzeug: schreibt jede Animation als Kontaktbogen nach `core/build/preview/`.**
 *
 * Eine Animation ist hier eine Liste von Koordinaten. Ob sie etwas Erkennbares zeigt, laesst sich
 * daran nicht ablesen - und das ist keine Kleinigkeit, sondern der Grund, warum in
 * [DefaultLibraryAnimations] ein `render.py` erwaehnt wird, das nie im Repository lag. Ohne
 * Bildkontrolle ist jede Pixelarbeit Blindflug: Ein Motiv, das um eine Zelle verrutscht ist oder
 * dessen zweite Bewegung fehlt, faellt in den Zahlen nicht auf.
 *
 * Bewusst als Test und nicht als eigenes Skript: Er laeuft mit `gradlew :core:test` ohnehin mit,
 * braucht keine Zusatzwerkzeuge und kann dabei gleich die Invarianten pruefen, die sich MASCHINELL
 * pruefen lassen. Was das Auge entscheiden muss, liegt danach als PNG daneben.
 *
 *     gradlew.bat :core:testDebugUnitTest --tests "*AnimationPreviewTest*"
 *     core/build/preview/<Label>.png
 *
 * **Warum das PNG von Hand geschrieben wird:** `java.awt` und `javax.imageio` gibt es in einem
 * Android-Unittest nicht - `android.jar` verdeckt sie. Ein PNG besteht aber nur aus vier Bloecken,
 * und `Deflater`/`CRC32` sind vorhanden; das ist weniger Aufwand als eine Bildbibliothek als
 * Testabhaengigkeit einzuschleppen.
 */
class AnimationPreviewTest {

    private val size = ReminderFrameGrid.SIZE
    private val cell = 11
    private val gap = 8
    private val outputDir = File("build/preview")

    private val lit = intArrayOf(0xF1, 0xEE, 0xE6)
    private val dark = intArrayOf(0x0A, 0x0A, 0x0B)
    private val gridLine = intArrayOf(0x24, 0x24, 0x2A)

    /**
     * Zeichnet alle Frames einer Animation nebeneinander, in Abspielreihenfolge.
     *
     * Das schwache Raster im Hintergrund ist kein Schmuck: Ohne es laesst sich nicht sagen, ob ein
     * Motiv mittig sitzt oder um eine Zelle verschoben ist - und genau das sieht man auf dem
     * Geraet erst, wenn man weiss, wonach man sucht.
     */
    private fun renderSheet(frames: List<List<Pair<Int, Int>>>): ByteArray {
        val frameSize = size * cell
        val width = frames.size * frameSize + (frames.size + 1) * gap
        val height = frameSize + 2 * gap
        val pixels = IntArray(width * height * 3)

        fun set(x: Int, y: Int, rgb: IntArray) {
            if (x !in 0 until width || y !in 0 until height) return
            val i = (y * width + x) * 3
            pixels[i] = rgb[0]; pixels[i + 1] = rgb[1]; pixels[i + 2] = rgb[2]
        }

        for (y in 0 until height) for (x in 0 until width) set(x, y, dark)

        frames.forEachIndexed { index, frame ->
            val ox = gap + index * (frameSize + gap)
            val oy = gap
            for (i in 0..size) {
                for (t in 0..frameSize) {
                    set(ox + i * cell, oy + t, gridLine)
                    set(ox + t, oy + i * cell, gridLine)
                }
            }
            for ((px, py) in frame) {
                if (px !in 0 until size || py !in 0 until size) continue
                for (dy in 1 until cell) for (dx in 1 until cell) {
                    set(ox + px * cell + dx, oy + py * cell + dy, lit)
                }
            }
        }
        return encodePng(width, height, pixels)
    }

    // ================= PNG von Hand =================

    private fun encodePng(width: Int, height: Int, rgb: IntArray): ByteArray {
        val raw = ByteArrayOutputStream()
        for (y in 0 until height) {
            raw.write(0) // Filtertyp "none" - fuer Flaechen wie diese voellig ausreichend
            for (x in 0 until width) {
                val i = (y * width + x) * 3
                raw.write(rgb[i]); raw.write(rgb[i + 1]); raw.write(rgb[i + 2])
            }
        }

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        chunk(out, "IHDR", ByteArrayOutputStream().apply {
            write(intBytes(width)); write(intBytes(height))
            write(8)  // Bittiefe
            write(2)  // Farbtyp 2 = RGB ohne Palette
            write(0); write(0); write(0)
        }.toByteArray())
        chunk(out, "IDAT", deflate(raw.toByteArray()))
        chunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun chunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        out.write(intBytes(data.size))
        out.write(typeBytes)
        out.write(data)
        val crc = CRC32().apply { update(typeBytes); update(data) }
        out.write(intBytes(crc.value.toInt()))
    }

    private fun intBytes(value: Int) = byteArrayOf(
        (value ushr 24).toByte(), (value ushr 16).toByte(),
        (value ushr 8).toByte(), value.toByte()
    )

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return out.toByteArray()
    }

    // ================= Der Lauf =================

    /**
     * Motive, bei denen eine der Regeln unten bewusst gebrochen wird.
     *
     * **Beide sind beim ersten Lauf dieses Werkzeugs aufgefallen** und haben sich als Absicht
     * herausgestellt - die Ausnahmen stehen hier namentlich, damit sie eine Entscheidung bleiben
     * und nicht zu einer stillschweigend gelockerten Regel werden.
     */
    private val ausnahmen = mapOf(
        "Rocket" to "fliegt oben aus dem Raster hinaus - das Abschneiden IST der Start",
        "TAMA" to "leere Frames trennen die Buchstaben, sonst verschwaemmen sie ineinander"
    )

    /**
     * Schreibt die Boegen und prueft dabei, was sich ohne Auge pruefen laesst.
     *
     * Die drei Zusicherungen sind nicht theoretisch: Ein Punkt ausserhalb des Rasters verschwindet
     * beim Abspielen stillschweigend, ein leerer Frame ist ein Aussetzer mitten in der Bewegung,
     * und eine Animation aus zwei Bildern ist keine Bewegung, sondern ein Blinken.
     */
    @Test
    fun `alle Animationen werden als Kontaktbogen geschrieben`() {
        outputDir.mkdirs()

        val fehler = mutableListOf<String>()
        for (animation in DefaultLibraryAnimations.seed()) {
            val frames = FrameCodec.decode(animation.framesData)
            val darfAbweichen = animation.label in ausnahmen

            if (frames.size < 4) fehler += "${animation.label}: nur ${frames.size} Frames"
            if (!darfAbweichen && frames.any { it.isEmpty() }) {
                fehler += "${animation.label}: leerer Frame"
            }
            val ausserhalb = frames.flatten().filterNot { (x, y) ->
                x in 0 until size && y in 0 until size
            }
            if (!darfAbweichen && ausserhalb.isNotEmpty()) {
                fehler += "${animation.label}: ${ausserhalb.size} Punkte ausserhalb des Rasters"
            }

            File(outputDir, "${animation.label}.png").writeBytes(renderSheet(frames))
        }

        assertTrue(fehler.joinToString("\n"), fehler.isEmpty())
        assertTrue(
            "Es wurde kein einziger Bogen geschrieben - Pfad falsch?",
            (outputDir.listFiles()?.size ?: 0) > 0
        )
    }

    /**
     * Sammelbogen fuer eine Handvoll Motive: alle untereinander in EINEM Bild.
     *
     * Beim Zeichnen sieht man mehrere Motive nebeneinander sonst nur, indem man Datei fuer Datei
     * oeffnet - und genau der Vergleich untereinander ist das, was zeigt, ob ein neues Motiv zum
     * Rest passt.
     */
    @Test
    fun `die neuen Motive kommen zusaetzlich als Sammelbogen`() {
        outputDir.mkdirs()
        val neu = SkillTreeAnimations.seed()
        neu.chunked(4).forEachIndexed { index, gruppe ->
            val boegen = gruppe.map { FrameCodec.decode(it.framesData) }
            File(outputDir, "_neu-${index + 1}.png").writeBytes(renderStack(boegen))
        }
        assertTrue(neu.isNotEmpty())
    }

    /** Mehrere Animationen untereinander, jede Zeile eine. */
    private fun renderStack(sheets: List<List<List<Pair<Int, Int>>>>): ByteArray {
        val frameSize = size * cell
        val spalten = sheets.maxOf { it.size }
        val width = spalten * frameSize + (spalten + 1) * gap
        val zeilenHoehe = frameSize + gap
        val height = sheets.size * zeilenHoehe + gap
        val pixels = IntArray(width * height * 3)

        fun set(x: Int, y: Int, rgb: IntArray) {
            if (x !in 0 until width || y !in 0 until height) return
            val i = (y * width + x) * 3
            pixels[i] = rgb[0]; pixels[i + 1] = rgb[1]; pixels[i + 2] = rgb[2]
        }
        for (y in 0 until height) for (x in 0 until width) set(x, y, dark)

        sheets.forEachIndexed { row, frames ->
            val oyBase = gap + row * zeilenHoehe
            frames.forEachIndexed { col, frame ->
                val ox = gap + col * (frameSize + gap)
                for (i in 0..size) {
                    for (t in 0..frameSize) {
                        set(ox + i * cell, oyBase + t, gridLine)
                        set(ox + t, oyBase + i * cell, gridLine)
                    }
                }
                for ((px, py) in frame) {
                    if (px !in 0 until size || py !in 0 until size) continue
                    for (dy in 1 until cell) for (dx in 1 until cell) {
                        set(ox + px * cell + dx, oyBase + py * cell + dy, lit)
                    }
                }
            }
        }
        return encodePng(width, height, pixels)
    }

    /**
     * Eine Ausnahme, die keine mehr noetig hat, gehoert weg - sonst deckt die Liste irgendwann
     * einen echten Fehler.
     */
    @Test
    fun `jede Ausnahme wird noch gebraucht`() {
        val ueberfluessig = ausnahmen.keys.filter { label ->
            val animation = DefaultLibraryAnimations.seed().firstOrNull { it.label == label }
                ?: return@filter true
            val frames = FrameCodec.decode(animation.framesData)
            frames.none { it.isEmpty() } && frames.flatten().all { (x, y) ->
                x in 0 until size && y in 0 until size
            }
        }
        assertTrue("Diese Ausnahmen sind ueberfluessig geworden: $ueberfluessig", ueberfluessig.isEmpty())
    }
}
