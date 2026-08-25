package com.notime.glyphsim.skilltree

import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.AvatarUnlockedNode
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow

/**
 * Der Freischalt-Stand eines Avatars: was ist offen, was steht als Naechstes zur Wahl.
 *
 * Bindeglied zwischen der reinen Rechnung ([BranchAffinity], [UnlockOffers]) und der Datenbank.
 * Die Rechnung selbst kennt weder Room noch Profile - sie laesst sich damit gegen eine erfundene
 * Historie pruefen, ohne dass ein Geraet noetig waere, und genau dort liegen die Regeln, an denen
 * sich etwas falsch machen laesst.
 */
class AvatarUnlockRepository(private val database: AppDatabase) {

    /**
     * Wieviele der juengsten Antworten in die Neigung eingehen.
     *
     * 500 ist grosszuegig gewaehlt: Bei einer Halbwertszeit von zwei Wochen ist alles darueber
     * hinaus praktisch gewichtslos, und die Abfrage bleibt unabhaengig davon, ob jemand die App
     * seit einem Monat oder seit drei Jahren benutzt.
     */
    private val historyLimit = 500

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

    /** Fuer die Zieh-Leiste (Paket P5) - sie soll mitwachsen, sobald etwas dazukommt. */
    fun observeUnlockedNodes(profileId: String): Flow<List<String>> =
        database.avatarUnlockedNodeDao().observeNodeIdsFor(profileId)

    /**
     * Das Angebot zum aktuellen Stand - zwei aus dem staerksten Zweig, einer von woanders.
     *
     * Ein leeres Angebot ([UnlockOffer.isEmpty]) heisst: Es gibt nichts mehr freizuschalten, was
     * schon gezeichnet ist. Der Aufrufer soll dann nichts anbieten statt eine leere Auswahl zu
     * zeigen.
     */
    suspend fun offerFor(
        profileId: String,
        nowMillis: Long,
        random: Random = Random
    ): UnlockOffer {
        ensureSeeded(profileId, nowMillis)
        val answers = database.avatarFeedEventDao()
            .answeredNodes(profileId, historyLimit)
            .map { BranchAffinity.Answer(nodeId = it.nodeId, fedAtMillis = it.fedAtMillis) }
        return UnlockOffers.build(
            unlocked = unlockedNodes(profileId),
            answers = answers,
            nowMillis = nowMillis,
            random = random
        )
    }

    /**
     * Schaltet die Wahl des Nutzers frei.
     *
     * Ohne Pruefung, ob der Knoten wirklich im Angebot stand: Der Aufrufer hat ihn von dort, und
     * eine zweite Pruefung waere eine Kopie derselben Regel an einer Stelle, an der sie
     * auseinanderlaufen kann. Was hier ankommt, wird offen.
     */
    suspend fun unlock(profileId: String, nodeId: String, nowMillis: Long) {
        database.avatarUnlockedNodeDao().insertIfAbsent(
            listOf(AvatarUnlockedNode(profileId, nodeId, nowMillis))
        )
    }
}
