package com.notime.glyphsim.data

import androidx.room.Entity

/**
 * Ein freigeschalteter Knoten des Animations-Baums, je Avatar (siehe
 * [com.notime.glyphcore.data.AnimationTree] und SKILLBAUM.md).
 *
 * **Zusammengesetzter Primaerschluessel aus Profil und Knoten.** Damit laesst sich derselbe Knoten
 * fuer dasselbe Profil gar nicht zweimal freischalten - die Bedingung steht in der Tabelle statt
 * in einer Pruefung, die jemand vergessen kann. Zwei Avatare koennen denselben Knoten unabhaengig
 * voneinander offen haben; der Fortschritt gehoert der Figur, mit der man spielt.
 *
 * **Was hier NICHT steht: eine Sperre fuer echte Erinnerungen.** Diese Tabelle steuert
 * ausschliesslich, was im Spiel gezogen werden kann. Die Bibliothek fuer echte Erinnerungen bleibt
 * vollstaendig offen - sonst koennte ein Spielfortschritt am Ende eine Erinnerung sperren, und
 * damit waere aus einer Erinnerungs-App ein Spiel geworden, das Gesundheitsfunktionen als
 * Belohnung ausschuettet.
 *
 * [unlockedAtMillis] wird bisher nirgends ausgewertet und ist trotzdem da: Ohne ihn liesse sich
 * spaeter nicht mehr rekonstruieren, in welcher Reihenfolge sich jemand seinen Baum gebaut hat -
 * und genau das ist die Geschichte, die ein Skillbaum erzaehlen soll.
 */
@Entity(tableName = "avatar_unlocked_nodes", primaryKeys = ["profileId", "nodeId"])
data class AvatarUnlockedNode(
    val profileId: String,
    val nodeId: String,
    val unlockedAtMillis: Long
)
