package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationTree
import kotlin.math.pow

/**
 * **Welchen Zweig des Animations-Baums bedient jemand tatsaechlich?**
 *
 * Grundlage jeder Freischaltung (siehe [UnlockOffer]): Was beim Levelaufstieg angeboten wird, soll
 * aus dem eigenen Verhalten folgen und nicht aus einer Liste, die jemand einmal festgelegt hat.
 *
 * ## Woraus gerechnet wird
 *
 * Aus der vorhandenen Fuetter-Historie ([com.notime.glyphsim.data.AvatarFeedEvent]) - es musste
 * dafuer nichts Neues mitgeschrieben werden, jede Ausloesung liegt dort ohnehin seit Langem mit
 * ihrem Knoten und ihrem Antwortzeitpunkt.
 *
 * **Nur beantwortete Ausloesungen zaehlen.** Eine Erinnerung, die jemand hat verstreichen lassen,
 * sagt nichts ueber sein Interesse - eher das Gegenteil. Wuerde sie mitzaehlen, bekaeme
 * ausgerechnet der Zweig den staerksten Ausschlag, den man am haeufigsten uebergeht.
 *
 * **Aeltere Antworten zaehlen weniger.** Gemeint ist "was tue ich ZURZEIT", nicht "was habe ich
 * jemals getan". Ohne Abschwaechung koennte ein halbes Jahr alter Schwerpunkt jede aktuelle
 * Gewohnheit ueberstimmen, und der Baum bliebe fuer immer dort stehen, wo er einmal angefangen
 * hat. Das Gewicht halbiert sich alle [HALF_LIFE_DAYS] Tage - nach zwei Wochen zaehlt eine Antwort
 * halb, nach sechs Wochen noch ein Achtel.
 *
 * **Gezaehlt wird auf Ebene der Hauptgruppe.** Ob jemand Basketball oder Fussball beantwortet hat,
 * ist fuer die Frage "welcher Zweig" gleichgueltig; beides ist Sport. Der Knoten wird deshalb ueber
 * [AnimationTree.fallbackChain] auf seine Hauptgruppe zurueckgefuehrt.
 */
object BranchAffinity {

    /** Nach dieser Zeit zaehlt eine Antwort nur noch halb. */
    const val HALF_LIFE_DAYS = 14.0

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Eine beantwortete Ausloesung, so weit sie hier interessiert. */
    data class Answer(val nodeId: String, val fedAtMillis: Long)

    /**
     * Punktzahl je Hauptgruppe. Enthaelt **alle neun**, auch mit 0.0 - ein Aufrufer soll nicht
     * zwischen "kein Eintrag" und "noch nie bedient" unterscheiden muessen, das ist dasselbe.
     */
    fun scores(answers: List<Answer>, nowMillis: Long): Map<String, Double> {
        val result = AnimationTree.roots().associate { it.id to 0.0 }.toMutableMap()
        for (answer in answers) {
            val root = rootOf(answer.nodeId) ?: continue
            val ageDays = (nowMillis - answer.fedAtMillis).coerceAtLeast(0L).toDouble() / DAY_MILLIS
            result[root] = (result[root] ?: 0.0) + 0.5.pow(ageDays / HALF_LIFE_DAYS)
        }
        return result
    }

    /**
     * Die Hauptgruppen, die staerkste zuerst.
     *
     * **Bei Gleichstand entscheidet die Reihenfolge im Baum**, nicht der Zufall und nicht die
     * Reihenfolge der Zeilen aus der Datenbank. Sonst saehe ein frischer Spielstand - in dem alle
     * neun auf 0.0 stehen - bei jedem Aufruf anders aus, und dieselbe Historie ergaebe je nach
     * Abfrage ein anderes Angebot.
     */
    fun ranked(answers: List<Answer>, nowMillis: Long): List<String> {
        val scores = scores(answers, nowMillis)
        val treeOrder = AnimationTree.roots().map { it.id }
        return treeOrder.sortedWith(
            compareByDescending<String> { scores[it] ?: 0.0 }.thenBy { treeOrder.indexOf(it) }
        )
    }

    /** Die Hauptgruppe ueber einem Knoten - das letzte Glied der Rueckfallkette. */
    private fun rootOf(nodeId: String): String? =
        AnimationTree.fallbackChain(nodeId).lastOrNull()
}
