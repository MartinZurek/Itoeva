package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.AnimationType
import kotlin.random.Random

/**
 * **Was das Wesen kann** - die freigeschalteten Knoten unterhalb eines Themas.
 *
 * ## Warum es das gibt
 *
 * Der Skillbaum hatte seit dem Entfernen der Zieh-Leiste keinen Abnehmer mehr: Ein eingesetzter
 * Skillpunkt schrieb eine Datenbankzeile, und das war alles, was je damit geschah (siehe
 * SKILLBAUM.md, P14). Hier bekommt er einen - die Welt selbst. Was freigeschaltet ist, taucht im
 * Alltag des Wesens auf: Wer "Basketball" gewaehlt hat, sieht es spaeter beim Sport tatsaechlich
 * Basketball spielen, statt nur "Sport" zu sehen wie vorher.
 *
 * ## Zwei Arten, wie ein Skill sichtbar wird
 *
 * Der erste Stand spielte jeden Skill als kurze Einlage NACH einer ansonsten unveraenderten
 * Handlung. Das bleibt der Rueckfall fuer Bereiche, die noch keinen eigenen vertikalen Schnitt
 * besitzen. Sobald ein Knoten aber von [AvatarActivityPlans] kontextuell aufgeloest wird, gehoert
 * er IN die Handlung selbst und darf hier nicht ein zweites Mal gewuerfelt werden. So entstehen
 * keine zwei konkurrierenden Handlungssysteme: Der Skillbaum entscheidet das Repertoire, die
 * vorhandene PlayRoutine fuehrt es aus.
 *
 * ## Warum MEDICINE nicht vorkommen kann
 *
 * Nicht durch eine Pruefung hier, sondern durch den Baum: MEDICINE steht in
 * [AnimationTree.EXCLUDED_TYPES] und hat deshalb gar keinen Knoten - [AnimationTree.nodeIdFor]
 * liefert `null`, und ohne Wirtsknoten gibt es nichts zu waehlen. Die Garantie liegt damit an
 * derselben Stelle wie die Regel, statt als Kopie daneben.
 */
object SkillRepertoire {

    /**
     * Der Knoten, unter dem die Faehigkeiten zu [topic] haengen.
     *
     * Fuer neun Themen ist das eine Hauptgruppe, fuer REST und FOCUS eine Untergruppe
     * (`ruhe/pause`, `arbeit/erledigen` - die beiden `subBuiltin`-Knoten des Baums). Beides
     * beantwortet [AnimationTree.nodeIdFor] von sich aus richtig; hier wird nichts nachgebildet.
     */
    fun hostNodeFor(topic: AnimationType): String? = AnimationTree.nodeIdFor(topic)

    /**
     * Alle freigeschalteten, gezeichneten Knoten UNTERHALB des Themas, die noch als klassische
     * Einlage gebraucht werden - in Baumreihenfolge.
     *
     * Der Wirtsknoten selbst fehlt bewusst: Seine Reaktion IST die Handlung, die der Ablauf gerade
     * gespielt hat. Kontextuell ausgefuehrte Skills fehlen ebenfalls: Deren Freischaltung hat die
     * Routine bereits veraendert und soll nicht danach nochmals als Einzelanimation auftauchen.
     */
    fun skillsFor(topic: AnimationType, unlocked: Set<String>): List<String> {
        val host = hostNodeFor(topic) ?: return emptyList()
        return AnimationTree.nodes
            .filter { node ->
                node.id != host &&
                    node.motif != null &&
                    node.id in unlocked &&
                    host in AnimationTree.fallbackChain(node.id) &&
                    !AvatarActivityPlans.supportsContextualExecution(node)
            }
            .map { it.id }
    }

    /**
     * Eine Faehigkeit fuer diese Handlung, oder `null`, wenn das Wesen in diesem Bereich noch
     * keine klassische Einlage hat.
     *
     * Gleichverteilt und ohne Gedaechtnis: Eine Einlage kommt ohnehin nur selten (siehe
     * [com.notime.glyphsim.matrix.PlayAmbientActivity.playsSkillFlourish]), und zwischen zwei
     * Einlagen desselben Bereichs liegen dadurch Minuten. Ein Daempfer wie bei den Themen
     * ([com.notime.glyphsim.matrix.PlayAmbientActivity] `justPlayed`) wuerde hier eine
     * Wiederholung bekaempfen, die es kaum gibt.
     */
    fun pick(topic: AnimationType, unlocked: Set<String>, random: Random = Random): String? =
        skillsFor(topic, unlocked).let { if (it.isEmpty()) null else it[random.nextInt(it.size)] }
}
