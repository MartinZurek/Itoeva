package com.notime.glyphsim.matrix

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt die EIGENSCHAFTEN der Kreaturenfarben fest, nicht die Zahlen.
 *
 * Gepruefte Zahlen im Test waeren hier wertlos: Sie wuerden nur bestaetigen, dass jemand die
 * Farbe abgeschrieben hat, die in [AvatarPalette] steht. Interessant ist, ob eine Farbe TAUGT -
 * ob man die Kreatur unten noch sieht, ob zwei Kreaturen nebeneinander unterscheidbar sind, und
 * ob keine heller ist als die andere. Wer eine Farbe austauscht oder eine siebte Kreatur
 * anhaengt, bekommt hier die Antwort, ohne den Weg noch einmal gehen zu muessen.
 */
class AvatarPaletteTest {

    private val hintergrund = 0xFF0D0D0D.toInt()

    /** Dunkelster Punkt einer Figur: ihre beschattete Flanke (siehe [AvatarShading]). */
    private val tiefsterPunkt = AvatarShading.SHADE

    private fun kanal(argb: Int, schieben: Int) = (argb shr schieben) and 0xFF

    private fun linear(wert: Int): Double {
        val c = wert / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    /** Wahrgenommene Helligkeit nach sRGB. */
    private fun helligkeit(argb: Int): Double =
        0.2126 * linear(kanal(argb, 16)) +
            0.7152 * linear(kanal(argb, 8)) +
            0.0722 * linear(kanal(argb, 0))

    /** Kontrastverhaeltnis nach WCAG - 1 heisst "nicht zu unterscheiden". */
    private fun kontrast(a: Int, b: Int): Double {
        val hell = maxOf(helligkeit(a), helligkeit(b))
        val dunkel = minOf(helligkeit(a), helligkeit(b))
        return (hell + 0.05) / (dunkel + 0.05)
    }

    private fun gedimmt(argb: Int, faktor: Float): Int {
        fun f(schieben: Int) = (kanal(argb, schieben) * faktor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (f(16) shl 16) or (f(8) shl 8) or f(0)
    }

    /** CIE-Lab - der Abstand darin entspricht dem, was das Auge als Farbunterschied sieht. */
    private fun lab(argb: Int): Triple<Double, Double, Double> {
        val r = linear(kanal(argb, 16))
        val g = linear(kanal(argb, 8))
        val b = linear(kanal(argb, 0))
        val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
        val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
        fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116.0
        return Triple(116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
    }

    private fun farbabstand(a: Int, b: Int): Double {
        val (l1, a1, b1) = lab(a)
        val (l2, a2, b2) = lab(b)
        return sqrt((l1 - l2).pow(2) + (a1 - a2).pow(2) + (b1 - b2).pow(2))
    }

    @Test
    fun `jede Spezies hat eine eigene Farbe`() {
        assertEquals(AvatarSpecies.entries.size, AvatarPalette.all.toSet().size)
    }

    @Test
    fun `keine Kreatur ist heller oder dunkler als eine andere`() {
        // Sonst waere eine Spezies allein durch ihre Farbe benachteiligt - sie wirkte
        // kraenklich neben einer anderen, ohne dass es irgendetwas bedeuten wuerde.
        val werte = AvatarPalette.all.map(::helligkeit)
        val spanne = werte.max() - werte.min()
        assertTrue(
            "Helligkeit der Grundtoene laeuft um $spanne auseinander: $werte",
            spanne < 0.02
        )
    }

    @Test
    fun `auch die beschattete Flanke hebt sich noch vom Hintergrund ab`() {
        // Die dunkelste Stelle einer Kreatur ist ihre beschattete Flanke. Sie darf ruhig dunkler
        // sein als der Rest - aber nicht so dunkel, dass die Silhouette dort ausfranst.
        for (species in AvatarSpecies.entries) {
            val flanke = gedimmt(AvatarPalette.tintFor(species), tiefsterPunkt)
            val k = kontrast(flanke, hintergrund)
            assertTrue("$species im Schatten: Kontrast nur $k", k >= 4.0)
        }
    }

    @Test
    fun `die beschattete Flanke ist vom Rest zu unterscheiden`() {
        // Sonst waere die Schattierung eine Behauptung im Kommentar. Gefordert wird ein
        // Unterschied, den ein Auge als Kante liest - deutlich weniger als der Abstand zum
        // Hintergrund, aber mehr als nichts.
        for (species in AvatarSpecies.entries) {
            val grund = AvatarPalette.tintFor(species)
            val flanke = gedimmt(grund, tiefsterPunkt)
            val abstand = farbabstand(grund, flanke)
            assertTrue("$species: Grundton und Flanke liegen nur $abstand auseinander", abstand >= 3.0)
        }
    }

    @Test
    fun `zwei Kreaturen nebeneinander sind auseinanderzuhalten`() {
        // Im Spielmodus kommt Besuch: Dann stehen zwei Figuren gleichzeitig im Bild. Ein
        // Farbabstand ab etwa 20 gilt als muehelos unterscheidbar; darunter waere die Farbe
        // reine Behauptung.
        var kleinster = Double.MAX_VALUE
        var paar = ""
        for (a in AvatarSpecies.entries) {
            for (b in AvatarSpecies.entries) {
                if (a.ordinal >= b.ordinal) continue
                val d = farbabstand(AvatarPalette.tintFor(a), AvatarPalette.tintFor(b))
                if (d < kleinster) {
                    kleinster = d
                    paar = "$a / $b"
                }
            }
        }
        assertTrue("$paar liegen nur $kleinster auseinander", kleinster >= 20.0)
    }

    @Test
    fun `der Farbton kommt vom Schwerpunkt der Kreatur`() {
        // Die Helligkeit wurde angehoben (siehe AvatarPalette), der FARBTON nicht - sonst waere
        // die Herkunft aus ui-AnimationVisuals nur noch eine Behauptung im Kommentar.
        val quellen = mapOf(
            AvatarSpecies.PUFFLING to 0xFF546E7A.toInt(),
            AvatarSpecies.STARLET to 0xFF43A047.toInt(),
            AvatarSpecies.WYRMLING to 0xFFF4511E.toInt(),
            AvatarSpecies.FENNEC to 0xFF1E88E5.toInt(),
            AvatarSpecies.GLOOP to 0xFF00897B.toInt(),
            AvatarSpecies.HOOTLET to 0xFF6750A4.toInt()
        )
        for ((species, quelle) in quellen) {
            val abweichung = abs(farbton(AvatarPalette.tintFor(species)) - farbton(quelle))
            val ueberDenNullpunkt = minOf(abweichung, 360f - abweichung)
            assertTrue(
                "$species weicht um $ueberDenNullpunkt Grad vom Schwerpunkt-Farbton ab",
                ueberDenNullpunkt <= 2f
            )
        }
    }

    private fun farbton(argb: Int): Float {
        val r = kanal(argb, 16) / 255f
        val g = kanal(argb, 8) / 255f
        val b = kanal(argb, 0) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        if (max == min) return 0f
        val h = when (max) {
            r -> 60f * (((g - b) / (max - min)) % 6f)
            g -> 60f * ((b - r) / (max - min) + 2f)
            else -> 60f * ((r - g) / (max - min) + 4f)
        }
        return (h + 360f) % 360f
    }

    @Test
    fun `die Kulissenfarbe gehoert keiner Kreatur`() {
        // Kulisse, Uhr und die Zeichen in den Blasen bleiben weiss - sie sind Aussagen ueber die
        // Welt, keine Koerper. Traege eine Kreatur denselben Ton, liesse sich beides nicht mehr
        // trennen.
        assertTrue(
            "LED_ON darf nicht in der Kreaturenpalette auftauchen",
            MatrixColors.LED_ON !in AvatarPalette.all
        )
    }
}
