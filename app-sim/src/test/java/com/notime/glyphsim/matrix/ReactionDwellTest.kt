package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Jede Reaktion braucht mindestens einen Moment, auf dem das Auge ruhen kann.**
 *
 * Der Befund, der zu dieser Datei gefuehrt hat, war messbar und nicht Geschmack: MOVE hielt neun
 * von elf Bildern auf [AvatarAnimations.FAST_MS] (90 ms), GENERAL sieben von zehn. Beide hatten
 * vor dem gemeinsamen Ausklang **keine einzige** Standzeit ueber [AvatarAnimations.BEAT_MS]. Bei
 * diesem Tempo loest das Auge die Posen nicht auf; aus drei Spruengen wird ein Flimmern, aus
 * einem Laeuten ein Klicken. Genau das ist gemeint, wenn eine Reaktion "zu kurz" wirkt - nicht
 * die Gesamtdauer, sondern das fehlende Verweilen.
 *
 * Ausdruecklich KEINE Mindestdauer und keine Angleichung der Laengen. BOOK darf mit 3 920 ms
 * dreimal so lang sein wie CREATIVITY: Lesen ist eine laengere Handlung als ein Einfall. Was
 * geprueft wird, ist nur, dass ueberhaupt irgendwo innegehalten wird.
 *
 * Der Ausklang ([AvatarAnimations.SETTLE_MS], das letzte Bild) zaehlt dabei nicht mit - den hat
 * jede Reaktion, er ist gemeinsam und beweist nichts ueber die Choreografie davor. Ohne diesen
 * Ausschluss waere der Test von Anfang an gruen gewesen und haette den Fehler nicht gefunden.
 */
class ReactionDwellTest {

    private fun mitte(species: AvatarSpecies, typ: AnimationType): List<Long> {
        val holds = AvatarAnimations.reactionFor(species, ReactionTrigger.Topic(typ)).holdsMs
        // Das letzte Bild ist der gemeinsame Ausklang - siehe KDoc.
        return holds.dropLast(1)
    }

    @Test
    fun `jede Reaktion haelt irgendwo laenger inne als der Grundtakt`() {
        for (species in AvatarSpecies.entries) {
            for (typ in AnimationType.entries) {
                val mitte = mitte(species, typ)
                val laengste = mitte.maxOrNull() ?: 0L
                assertTrue(
                    "$species/$typ verweilt nirgends laenger als ${AvatarAnimations.BEAT_MS} ms " +
                        "(laengste Standzeit vor dem Ausklang: $laengste ms, Takte: $mitte)",
                    laengste >= AvatarAnimations.SLOW_MS
                )
            }
        }
    }

    /*
     * **Was hier bewusst NICHT geprueft wird, obwohl es naheliegt.**
     *
     * Ein zweiter Test lag nahe: "keine Reaktion besteht ueberwiegend aus dem schnellsten Takt".
     * Er ist geschrieben, ausgefuehrt und wieder entfernt worden, weil er mehr behauptet als
     * gemessen wurde. Er faellt naemlich auch fuer Reaktionen, die den eigentlichen Fehler gar
     * nicht haben - Stand nach dem Umbau:
     *
     *     FOCUS      6 von  8 Bildern auf 90 ms
     *     WORK       8 von 11
     *     MEDICINE   7 von 11
     *     DRINK     10 von 17
     *     LOVE       5 von 11
     *
     * Diese fuenf haben, anders als MOVE und GENERAL vorher, sehr wohl einen Moment zum
     * Innehalten; sie sind nur zusaetzlich flott geschnitten. Ob das ein Mangel ist oder eine
     * Eigenart, laesst sich nicht messen, sondern nur ansehen - und einen Test zu schreiben und
     * danach den Code an die eigene nachtraegliche Erfindung anzupassen ist die falsche
     * Richtung. Die Zahlen stehen hier, damit die naechste Sitzung sie hat, ohne sie neu zu
     * erheben; WORK waere der naechste Kandidat.
     */

    @Test
    fun `der Ausklang ist wirklich das letzte Bild jeder Reaktion`() {
        // Die Annahme, auf der die beiden Tests oben ruhen. Waere sie falsch, wuerden sie
        // stillschweigend das falsche Bild abziehen und damit etwas anderes pruefen als
        // gemeint.
        for (species in AvatarSpecies.entries) {
            for (typ in AnimationType.entries) {
                val holds = AvatarAnimations.reactionFor(species, ReactionTrigger.Topic(typ)).holdsMs
                assertTrue(
                    "$species/$typ endet mit ${holds.last()} ms statt mit einem Ausklang",
                    holds.last() >= AvatarAnimations.SETTLE_MS
                )
            }
        }
    }
}
