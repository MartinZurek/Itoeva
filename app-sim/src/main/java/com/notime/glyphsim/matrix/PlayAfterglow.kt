package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType

/**
 * **Was eine beantwortete Erinnerung hinterlaesst** - der Nachklang.
 *
 * ## Der Befund, der dazu gefuehrt hat
 *
 * Bis hierher endete eine beantwortete Erinnerung so: kurze Reaktion, einmal an den zum Thema
 * passenden Ort, dort EINE Routine - und danach war sie spurlos. Der naechste Wuerfel wusste
 * nichts mehr davon.
 *
 * Schlimmer noch, die einzige Spur zeigte in die falsche Richtung. [com.notime.glyphsim.ui.PlayHabitSignal]
 * gewichtet, was heute noch NICHT erreicht ist; sobald das Tagesziel erfuellt war, fiel das Thema
 * aus dem Zuschlag ([PlayAmbientActivity.HABIT_BOOST], +4 auf 0). Wer also seine Erinnerung
 * beantwortete, machte damit genau dieses Thema fuer den Rest des Tages UNwahrscheinlicher. Der
 * Nachklang war negativ.
 *
 * ## Zwei Zeitskalen, weil zwei verschiedene Dinge gemeint sind
 *
 * - **Der Nachklang** ([ECHO_MS], [ECHO_BONUS]): die naechsten Minuten. Das Wesen bleibt bei dem,
 *   worum es gebeten wurde, statt nach einer Routine wieder beliebig weiterzuwuerfeln. Der
 *   Zuschlag ist so gross wie [PlayAmbientActivity.STAY_BONUS], der staerkste Einzelzuschlag -
 *   fuer diese wenigen Minuten soll nichts anderes lauter sein.
 * - **Die Tagesfarbe** ([DAY_BONUS]): der Rest des Tages. Nur noch ein Faerben, so gross wie
 *   [PlayAmbientActivity.LEANING_BONUS]. Das ist Absicht und keine Verlegenheit: Eine Neigung ist
 *   ueber Wochen gewachsen, eine heutige Bitte gilt heute - dass beide gleich viel wiegen, ist
 *   die Aussage, nicht ein fehlender Feinschliff.
 *
 * ## Was hier ausdruecklich NICHT passiert
 *
 * **Keine dritte Zeitskala.** "Aendert die ganze Geschichte" gibt es bereits, und zwar besser als
 * ein weiterer Zuschlag es koennte: [com.notime.glyphsim.ui.PlayPath] leitet aus ALLEN je
 * beantworteten Erinnerungen den Entwicklungspfad ab, faerbt darueber dauerhaft die Themenwahl
 * ([PlayAmbientActivity.nextTopic], Parameter `leaning`) und moebliert die Zimmer sichtbar
 * ([com.notime.glyphsim.matrix.PlayScene.Acquisition]). Diese Datei waere die falsche Stelle, das
 * noch einmal zu bauen; sie schliesst die Luecke dazwischen - den Tag.
 *
 * **Keine Spiel-Erinnerungen.** Aus demselben Grund, aus dem [com.notime.glyphsim.ui.PlayHabitSignal]
 * sie ausschliesst: Was das Spiel sich selbst wuerfelt, darf sich nicht selbst verstaerken. Der
 * Aufrufer filtert das, bevor er hier hineingeht.
 *
 * Alles hier ist reine Rechnung - keine Datenbank, keine Uhr im Hintergrund, kein Android.
 */
object PlayAfterglow {

    /**
     * Eine beantwortete Erinnerung, reduziert auf das, was fuer den Nachklang zaehlt.
     *
     * [fedAtMillis] ist ECHTE Zeit (`System.currentTimeMillis`), nicht die Weltzeit aus
     * [PlayTimeLapse]. Das ist wichtig genug, um hier zu stehen: Die Tabelle
     * `avatar_feed_events` haelt echte Zeitpunkte, und ein Nachklang, der eine echte Uhr gegen
     * eine geraffte rechnet, waere im Zeitraffer sofort abgelaufen oder nie. Wer diese Funktion
     * ruft, reicht deshalb auch fuer `nowMillis` und `dayStartMillis` echte Zeit herein.
     */
    data class Answer(val topic: AnimationType, val fedAtMillis: Long)

    /**
     * Wie lange der starke Nachklang haelt.
     *
     * Drei Minuten sind hier kein runder Wert, sondern eine Anzahl Regungen: Zwischen zwei
     * Themenwahlen liegen [PlayAmbientActivity.PAUSE_RANGE_MS] (18-36 s), im Mittel also gut eine
     * halbe Minute. Drei Minuten sind damit rund ein halbes Dutzend Runden - genug, dass aus der
     * einen angeforderten Routine eine zusammenhaengende Weile wird, und kurz genug, dass daraus
     * keine Besessenheit wird.
     */
    const val ECHO_MS = 3L * 60 * 1000

    /** So gross wie [PlayAmbientActivity.STAY_BONUS] - siehe KDoc oben. */
    const val ECHO_BONUS = 5

    /** So gross wie [PlayAmbientActivity.LEANING_BONUS] - siehe KDoc oben. */
    const val DAY_BONUS = 2

    /**
     * Deckel fuer die Summe je Thema.
     *
     * Ohne ihn waere ein Thema mit fuenf Antworten am Tag mit +10 unterwegs und wuerde damit die
     * Tageszeit ueberstimmen - das Wesen taete dann nur noch das eine. Der Deckel liegt
     * ausdruecklich UEBER [ECHO_BONUS]: Zwei Antworten sollen mehr wiegen als eine, drei aber
     * nicht mehr als zwei.
     */
    const val MAX_BONUS = 8

    /**
     * Der Zuschlag je Thema, den [PlayAmbientActivity.nextTopic] dazurechnet.
     *
     * [answers] darf ungefiltert hereingegeben werden: Was vor [dayStartMillis] liegt, in der
     * Zukunft liegt oder MEDICINE ist, faellt hier heraus. MEDICINE ist ueberall in
     * [PlayAmbientActivity] ausgeschlossen - Medikamente sind nichts, was ein Wesen von sich aus
     * tut, und ein Nachklang waere genau das.
     *
     * Der Rueckgabewert enthaelt nur Themen mit einem Zuschlag; ein leeres Ergebnis heisst
     * "heute noch nichts beantwortet" und laesst die Gewichtung unberuehrt.
     */
    fun bonuses(
        answers: List<Answer>,
        nowMillis: Long,
        dayStartMillis: Long
    ): Map<AnimationType, Int> {
        if (answers.isEmpty()) return emptyMap()
        val summe = mutableMapOf<AnimationType, Int>()
        for (answer in answers) {
            if (answer.topic == AnimationType.MEDICINE) continue
            if (answer.fedAtMillis < dayStartMillis) continue
            // Eine Antwort aus der Zukunft kann nur eine verstellte Uhr sein. Sie zu zaehlen
            // hiesse, einen Nachklang fuer etwas zu geben, das noch nicht passiert ist.
            if (answer.fedAtMillis > nowMillis) continue
            val alter = nowMillis - answer.fedAtMillis
            val anteil = if (alter < ECHO_MS) ECHO_BONUS else DAY_BONUS
            summe[answer.topic] = (summe[answer.topic] ?: 0) + anteil
        }
        return summe.mapValues { (_, wert) -> wert.coerceAtMost(MAX_BONUS) }
    }

    /**
     * Ob gerade ein frischer Nachklang laeuft - fuer Aufrufer, die nicht gewichten, sondern
     * etwas anderes daran haengen wollen (etwa: waehrend des Nachklangs keinen Besuch starten).
     */
    fun isEchoing(answers: List<Answer>, nowMillis: Long): Boolean =
        answers.any { it.fedAtMillis in (nowMillis - ECHO_MS + 1)..nowMillis }
}
