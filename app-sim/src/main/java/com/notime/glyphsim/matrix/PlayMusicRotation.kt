package com.notime.glyphsim.matrix

import kotlin.random.Random

/**
 * **Welche VARIANTE einer Musikrolle gerade laufen soll** - die zweite Auswahlstufe unter
 * [MusicResolver].
 *
 * ## Warum es diese Stufe gibt
 *
 * Gemeldet: "Innerhalb eines stabilen Zustands bleibt dieselbe Musik zu lange unveraendert. Nach
 * ungefaehr fuenf Minuten wirkt ein einzelner wiederholter Track bereits monoton."
 *
 * Bis hierher galt "eine Rolle entspricht genau einer Datei". Wer nachmittags zu Hause sitzt,
 * hoerte deshalb denselben Neunzig-Sekunden-Loop, solange er zusah. Die Rolle war richtig - es
 * fehlte nur die Moeglichkeit, sie mit mehr als einem Stueck zu erfuellen.
 *
 * ## Die Trennung, die dabei erhalten bleibt
 *
 * Die Rolle beantwortet weiterhin allein [MusicResolver] aus der Weltlage. Erst wenn sie
 * feststeht, waehlt diese Datei ein Stueck AUS DIESER Rolle. Ein Ortswechsel innerhalb derselben
 * Rolle aendert damit nichts, ein Rollenwechsel wirkt sofort - beides unveraendert.
 *
 * Und ausdruecklich getrennt vom Verhalten der Figur: Eine Taetigkeit, die wechselt, ohne dass
 * sich die Rolle aendert, loest hier gar nichts aus. Die Musik folgt der LAGE, nicht jeder
 * Bewegung.
 *
 * Bewusst ohne Android: [rotationDue] und [pickVariant] sind reine Funktionen. Die
 * Zeitsteuerung eines Musikwechsels am Geraet zu pruefen hiesse, fuenf Minuten zuzuhoeren und
 * danach zu raten, ob der Wechsel an der Zeit lag oder am Zufall.
 */
object PlayMusicRotation {

    /**
     * Fruehestens nach dieser Zeit wechselt die Musik innerhalb derselben Rolle.
     *
     * Drei Minuten sind zwei Durchlaeufe eines Neunzig-Sekunden-Stuecks. Frueher zu wechseln
     * hiesse, ein Stueck nie zu Ende zu hoeren; das waere die andere Art von Unruhe.
     */
    const val MIN_VARIANT_MS = 180_000L

    /**
     * Und spaetestens nach dieser. Der Auftrag nennt "drei bis spaetestens fuenf Minuten"; der
     * tatsaechliche Zeitpunkt liegt dazwischen und haengt am Zufall, damit nicht jeder Besuch
     * denselben Takt hat. Ein fester Wert waere als Metronom hoerbar.
     */
    const val MAX_VARIANT_MS = 300_000L

    /**
     * Ob nach [elapsedMs] in derselben Rolle ein Variantenwechsel ansteht.
     *
     * [variantCount] entscheidet mit: Gibt es zu einer Rolle nur ein Stueck, wird nie gewechselt -
     * die Wiedergabe laeuft dann genau wie vor dieser Aenderung weiter. Das ist heute der
     * Normalfall und darf keine Sonderbehandlung beim Aufrufer verlangen.
     */
    fun rotationDue(elapsedMs: Long, variantCount: Int, random: Random = Random): Boolean {
        if (variantCount < 2) return false
        if (elapsedMs < MIN_VARIANT_MS) return false
        if (elapsedMs >= MAX_VARIANT_MS) return true
        // Im Fenster dazwischen mit steigender Wahrscheinlichkeit - so faellt der Wechsel mal
        // frueher, mal spaeter, ohne je die Obergrenze zu reissen.
        val fenster = (MAX_VARIANT_MS - MIN_VARIANT_MS).toFloat()
        val anteil = (elapsedMs - MIN_VARIANT_MS) / fenster
        return random.nextFloat() < anteil
    }

    /**
     * Waehlt eine Variante aus [available] - **nie dieselbe wie [current]**, solange es eine
     * Alternative gibt.
     *
     * Ohne diese Zusicherung koennte ein Wechsel beim selben Stueck landen, und der Nutzer haette
     * eine Ueberblendung gehoert, die nichts veraendert - schlechter als gar kein Wechsel, weil
     * sie nach einem Fehler klingt.
     */
    fun pickVariant(available: List<Int>, current: Int?, random: Random = Random): Int? {
        if (available.isEmpty()) return null
        val andere = available.filter { it != current }
        val feld = andere.ifEmpty { available }
        return feld[random.nextInt(feld.size)]
    }
}
