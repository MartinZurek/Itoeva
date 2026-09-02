package com.notime.glyphsim.skilltree

import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.AvatarUnlockedNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/**
 * Der Freischalt-Stand eines Avatars: was ist offen, was steht als Naechstes zur Wahl.
 *
 * Bindeglied zwischen der reinen Rechnung ([UnlockOffers]) und der Datenbank. Die Rechnung selbst
 * kennt weder Room noch Profile - sie laesst sich damit ohne Geraet pruefen, und genau dort liegen
 * die Regeln, an denen sich etwas falsch machen laesst.
 */
class AvatarUnlockRepository(private val database: AppDatabase) {

    /**
     * Legt die neun Hauptgruppen an, falls dieses Profil noch gar nichts offen hat.
     *
     * **Nicht in der Migration**, weil dort nicht bekannt ist, welche Profile es gibt - der
     * gewaehlte Avatar steht in den Einstellungen, nicht in der Datenbank. Beim ersten Zugriff je
     * Profil ist der richtige Zeitpunkt, und `IGNORE` macht den Aufruf beliebig wiederholbar.
     *
     * Bewusst an "hat noch gar nichts" geknuepft und nicht an "hat alle neun": Wer eine
     * Hauptgruppe haette und die uebrigen acht nicht, waere ein Zustand, den es nicht geben kann -
     * ihn stillschweigend aufzufuellen wuerde einen Fehler kaschieren statt ihn zu zeigen.
     */
    suspend fun ensureSeeded(profileId: String, nowMillis: Long) {
        val dao = database.avatarUnlockedNodeDao()
        if (dao.countFor(profileId) > 0) return
        dao.insertIfAbsent(
            UnlockOffers.startingNodes().map { nodeId ->
                AvatarUnlockedNode(profileId = profileId, nodeId = nodeId, unlockedAtMillis = nowMillis)
            }
        )
    }

    suspend fun unlockedNodes(profileId: String): Set<String> =
        database.avatarUnlockedNodeDao().nodeIdsFor(profileId).toSet()

    /**
     * Fuer die Zieh-Leiste (Paket P5) - sie soll mitwachsen, sobald etwas dazukommt.
     *
     * Saet beim ersten Sammeln selbst nach: Ein Aufrufer, der vor dem ersten Levelaufstieg
     * beobachtet (und noch nie freigeschaltet hat), saehe sonst dauerhaft ein leeres Profil statt
     * der neun Startknoten. `ensureSeeded` ist idempotent, ein zusaetzlicher Aufruf von aussen
     * bleibt also folgenlos.
     */
    fun observeUnlockedNodes(profileId: String): Flow<List<String>> =
        database.avatarUnlockedNodeDao().observeNodeIdsFor(profileId)
            .onStart { ensureSeeded(profileId, System.currentTimeMillis()) }

    /**
     * Schaltet die Wahl des Nutzers frei.
     *
     * Ohne Pruefung, ob der Knoten wirklich antippbar (Grenze + Punkt uebrig) war: Das Brett
     * prueft das bereits, bevor es diesen Aufruf ausloest, und eine zweite Pruefung hier waere eine
     * Kopie derselben Regel an einer Stelle, an der sie auseinanderlaufen kann. Was hier ankommt,
     * wird offen.
     */
    suspend fun unlock(profileId: String, nodeId: String, nowMillis: Long) {
        database.avatarUnlockedNodeDao().insertIfAbsent(
            listOf(AvatarUnlockedNode(profileId, nodeId, nowMillis))
        )
    }
}
