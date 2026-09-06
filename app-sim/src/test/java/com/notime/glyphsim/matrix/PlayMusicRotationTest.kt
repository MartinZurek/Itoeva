package com.notime.glyphsim.matrix

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, WANN innerhalb derselben Rolle ein anderes Stueck kommt.
 *
 * Am Geraet waere das kaum zu beurteilen: Man muesste fuenf Minuten zuhoeren und koennte danach
 * nicht sagen, ob der Wechsel an der Zeit lag, am Zufall oder an einem Ortswechsel, den man nicht
 * bemerkt hat. Genau deshalb ist die Zeitsteuerung als reine Funktion herausgeloest.
 */
class PlayMusicRotationTest {

    private val zweiVarianten = listOf(1, 2)

    // ================= Wann gewechselt wird =================

    /**
     * **Die wichtigste Zusicherung dieser Datei.** Vor drei Minuten wird nie gewechselt - das
     * sind zwei Durchlaeufe eines Neunzig-Sekunden-Stuecks. Frueher hiesse, ein Stueck nie zu
     * Ende zu hoeren, und das waere die andere Art von Unruhe.
     */
    @Test
    fun `vor drei Minuten wird nie gewechselt`() {
        val random = Random(1)
        for (ms in listOf(0L, 30_000L, 120_000L, PlayMusicRotation.MIN_VARIANT_MS - 1)) {
            repeat(200) {
                assertFalse("$ms ms", PlayMusicRotation.rotationDue(ms, 2, random))
            }
        }
    }

    /** Und spaetestens nach fuenf Minuten sicher. */
    @Test
    fun `nach fuenf Minuten wird sicher gewechselt`() {
        val random = Random(2)
        repeat(200) {
            assertTrue(PlayMusicRotation.rotationDue(PlayMusicRotation.MAX_VARIANT_MS, 2, random))
            assertTrue(PlayMusicRotation.rotationDue(600_000L, 2, random))
        }
    }

    /**
     * Dazwischen faellt der Wechsel mal frueher, mal spaeter. Ein fester Zeitpunkt waere als
     * Metronom hoerbar - jeder Besuch haette denselben Takt.
     */
    @Test
    fun `im Fenster dazwischen ist der Zeitpunkt nicht vorhersagbar`() {
        val mitte = (PlayMusicRotation.MIN_VARIANT_MS + PlayMusicRotation.MAX_VARIANT_MS) / 2
        val random = Random(3)
        val ergebnisse = (1..400).map { PlayMusicRotation.rotationDue(mitte, 2, random) }
        assertTrue("Im Fenster wird nie gewechselt", ergebnisse.any { it })
        assertTrue("Im Fenster wird immer gewechselt", ergebnisse.any { !it })
    }

    /**
     * **Mit nur einem Stueck bleibt alles wie vorher.** Das ist heute der Normalfall - vier der
     * fuenf Rollen haben ueberhaupt noch keinen Track, und `main_day` bekommt seinen zweiten
     * erst. Ohne diese Zeile braeuchte der Aufrufer eine Sonderbehandlung.
     */
    @Test
    fun `mit nur einer Variante wird nie gewechselt`() {
        val random = Random(4)
        for (ms in listOf(0L, 200_000L, 600_000L, 3_600_000L)) {
            repeat(100) {
                assertFalse("$ms ms", PlayMusicRotation.rotationDue(ms, 1, random))
                assertFalse("$ms ms, gar keine Variante", PlayMusicRotation.rotationDue(ms, 0, random))
            }
        }
    }

    // ================= Was gewaehlt wird =================

    /**
     * **Nie dieselbe wie die laufende.** Ein Wechsel, der beim selben Stueck landet, waere eine
     * Ueberblendung ohne Wirkung - schlechter als gar kein Wechsel, weil sie nach einem Fehler
     * klingt.
     */
    @Test
    fun `ein Wechsel landet nie beim laufenden Stueck`() {
        val random = Random(5)
        repeat(500) {
            assertNotEquals(1, PlayMusicRotation.pickVariant(zweiVarianten, current = 1, random = random))
            assertNotEquals(2, PlayMusicRotation.pickVariant(zweiVarianten, current = 2, random = random))
        }
    }

    /** Bei drei Varianten kommen ueber viele Ziehungen auch alle drei vor. */
    @Test
    fun `bei mehreren Varianten kommen alle vor`() {
        val random = Random(6)
        val gezogen = (1..600).map { PlayMusicRotation.pickVariant(listOf(1, 2, 3), current = null, random = random) }
        assertEquals(setOf(1, 2, 3), gezogen.toSet())
    }

    /**
     * Gibt es nur eine, wird sie auch dann geliefert, wenn sie schon laeuft - der Aufrufer
     * entscheidet dann selbst, dass nichts zu tun ist. Ein `null` waere hier irrefuehrend: Es
     * gaebe ja ein Stueck.
     */
    @Test
    fun `mit nur einer Variante bleibt es bei ihr`() {
        assertEquals(1, PlayMusicRotation.pickVariant(listOf(1), current = 1, random = Random(7)))
    }

    /** Und ohne jede Variante gibt es nichts zu waehlen. */
    @Test
    fun `ohne Variante gibt es keine Wahl`() {
        assertNull(PlayMusicRotation.pickVariant(emptyList(), current = null, random = Random(8)))
    }

    // ================= Der Vertrag zu den Dateinamen =================

    /**
     * Die Namen sind der Vertrag zum Manifest. Ein Tippfehler bliebe stumm: Der Track waere
     * erzeugt, aber die App faende ihn nie.
     */
    @Test
    fun `Variantennamen sind stabil und eindeutig`() {
        assertEquals("itoeva_main_day_01", MusicRole.MAIN_DAY.variantResource(1))
        assertEquals("itoeva_main_day_02", MusicRole.MAIN_DAY.variantResource(2))
        assertEquals("itoeva_home_evening_01", MusicRole.HOME_EVENING.variantResource(1))
        // Die erste Variante ist genau der Name, unter dem vor der Variantenfaehigkeit
        // ausgeliefert wurde - sonst waeren die bereits gemergten Dateien unauffindbar.
        for (rolle in MusicRole.entries) {
            assertEquals(rolle.androidResource, rolle.variantResource(1))
        }
        val alle = MusicRole.entries.flatMap { r -> (1..MusicRole.MAX_VARIANTS).map { r.variantResource(it) } }
        assertEquals("Zwei Rollen teilen sich einen Dateinamen", alle.size, alle.distinct().size)
    }
}
