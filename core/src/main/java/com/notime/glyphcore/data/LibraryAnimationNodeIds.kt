package com.notime.glyphcore.data

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Traegt [LibraryAnimation.nodeId] in bereits bestehende Zeilen nach.
 *
 * **Warum das im Kern liegt und nicht in den Migrationen.** Beide Apps haben eine eigene
 * Datenbank (`:app` und `:app-sim`), benutzen aber dieselbe Entity aus diesem Modul. Die Spalte
 * kommt deshalb in beiden Migrationen vor - und mit ihr das Nachtragen der Werte. Zweimal
 * dieselben 56 UPDATE-Anweisungen hinzuschreiben hiesse, dass eine der beiden Kopien beim
 * naechsten neuen Motiv vergessen wird und die eine App still ohne Zuordnung dasteht.
 *
 * **Warum aus [AnimationTree] statt aus einer Liste im Migrationscode.** Eine Migration soll
 * eigentlich unveraenderlich sein: Sie beschreibt einen historischen Schritt, und wenn sich der
 * Baum spaeter aendert, aendert sich rueckwirkend, was diese Migration tut. Das ist hier
 * vertretbar und sogar gewollt - der Nachtrag ist keine Datenumformung, sondern eine
 * Zuordnung, die immer den aktuellen Stand des Baums abbilden soll. Ein Geraet, das die
 * Migration spaeter durchlaeuft, bekommt die dann gueltige Zuordnung, und das ist die richtige.
 *
 * Zeilen, die schon einen Wert haben, bleiben unberuehrt (`AND nodeId IS NULL`) - der Aufruf ist
 * damit gefahrlos wiederholbar.
 */
object LibraryAnimationNodeIds {

    fun backfill(db: SupportSQLiteDatabase) {
        for (node in AnimationTree.nodes) {
            val motif = node.motif
            if (motif is AnimationMotif.Library) {
                db.execSQL(
                    "UPDATE library_animations SET nodeId = ? WHERE label = ? AND nodeId IS NULL",
                    arrayOf(node.id, motif.label)
                )
            }
        }
    }
}
