package com.notime.glyphsim.ui

import androidx.annotation.StringRes
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R

/**
 * **Wohin sich das Wesen entwickelt** - abgeleitet daraus, womit der Nutzer es fuettert.
 *
 * Bis hierher war "Entwicklung" eine Zahl: Erfahrung steigt, Stufe steigt, sonst nichts. Zwei
 * Nutzer mit voellig verschiedenen Gewohnheiten bekamen dasselbe Wesen, nur mit verschiedenen
 * Zaehlerstaenden. Was fehlte, war die Verbindung zwischen dem, was jemand TUT, und dem, was aus
 * seinem Begleiter WIRD.
 *
 * **Vier Pfade, drei Stufen - ein Baum, kein Regler.** Der Pfad ergibt sich aus der Art der
 * Gewohnheiten, die tatsaechlich beantwortet werden; die Stufe aus der Erfahrung. Beides
 * zusammen ist die Entwicklung: WOHIN aus dem Verhalten, WIE WEIT aus der Ausdauer. Eine der
 * beiden Achsen allein waere entweder eine Geschmacksfrage oder wieder nur ein Zaehler.
 *
 * **Was den Pfad ausmacht, sind die eigenen Erinnerungen** - nicht die Spiel-Erinnerungen. Was
 * das Spiel selbst wuerfelt, sagt nichts ueber den Nutzer aus; was er sich VORGENOMMEN und dann
 * auch beantwortet hat, sagt alles. Genau darin liegt der Anreiz, den diese Sache haben soll: Der
 * Weg des Begleiters ist die sichtbare Folge der eigenen Gewohnheiten.
 *
 * **Drei Entscheidungen, die verhindern, dass daraus eine Bevormundung wird:**
 *
 * 1. **Kein Pfad ohne Grundlage.** Unter [MIN_FEEDS] Antworten bleibt er unbestimmt. Ein Wesen,
 *    das nach zwei Fuetterungen verkuendet, es werde jetzt "der Stille", behauptet etwas ueber
 *    jemanden, den es nicht kennt.
 * 2. **Kein Pfad ohne Mehrheit.** Wer alles gleichmaessig macht, bekommt keinen zugewiesen,
 *    sondern bleibt vielseitig ([mixed]). Das ist ein Ergebnis, kein Mangel.
 * 3. **Nichts ist endgueltig.** Gerechnet wird ueber die gesamte Vergangenheit, nicht ueber ein
 *    Fenster: Der Pfad wandert langsam mit, wenn sich die Gewohnheiten aendern, und springt nie.
 *    Wer sich umstellt, sieht seinen Begleiter mitgehen - nur eben nicht am selben Tag.
 */
enum class PlayPath(
    @StringRes val labelRes: Int,
    /** Was der Pfad ueber den Nutzer und sein Wesen sagt. */
    @StringRes val descriptionRes: Int,
    /** Die Themen, die auf diesen Pfad einzahlen. */
    val topics: Set<AnimationType>
) {
    /** Unterwegs sein: Bewegung und Arbeit. */
    WANDERER(
        R.string.path_wanderer,
        R.string.path_wanderer_desc,
        setOf(AnimationType.MOVE, AnimationType.WORK)
    ),

    /** Fuer sich und andere sorgen: Trinken, Koerper, Naehe. */
    KEEPER(
        R.string.path_keeper,
        R.string.path_keeper_desc,
        setOf(AnimationType.DRINK, AnimationType.MEDICINE, AnimationType.LOVE)
    ),

    /** Zur Ruhe kommen: Stille, Lesen, Ausruhen, Schlaf. */
    DREAMER(
        R.string.path_dreamer,
        R.string.path_dreamer_desc,
        setOf(
            AnimationType.MINDFULNESS, AnimationType.BOOK,
            AnimationType.REST, AnimationType.SLEEP
        )
    ),

    /** Etwas zustande bringen: Konzentration und Machen. */
    MAKER(
        R.string.path_maker,
        R.string.path_maker_desc,
        setOf(AnimationType.FOCUS, AnimationType.CREATIVITY, AnimationType.GENERAL)
    );

    companion object {

        /**
         * So viele beantwortete Erinnerungen braucht es, bevor ueberhaupt ein Pfad benannt wird.
         *
         * **Die wichtigste Zahl hier.** Ein Wesen, das nach zwei Fuetterungen verkuendet, es werde
         * jetzt "der Stille", behauptet etwas ueber jemanden, den es nicht kennt - und der Nutzer
         * merkt sofort, dass da nichts dahintersteckt. Zwanzig sind etwa eine Woche mit einer
         * gepflegten Gewohnheit.
         */
        const val MIN_FEEDS = 20

        /** Und so klar muss die Mehrheit sein, damit es eine Neigung ist und kein Zufall. */
        const val MIN_SHARE = 0.4f

        /**
         * Der Pfad zu einer Verteilung - `null` heisst: noch nicht genug, oder zu gleichmaessig.
         *
         * Beide `null`-Faelle sind Ergebnisse und keine Fehler, und sie unterscheiden sich fuer
         * den Nutzer deutlich: einmal "ich kenne dich noch nicht", einmal "du machst von allem
         * etwas". Welcher der beiden vorliegt, sagt [enoughData].
         */
        fun pathFor(counts: Map<AnimationType, Int>): PlayPath? {
            val total = counts.values.sum()
            if (total < MIN_FEEDS) return null
            val byPath = entries.associateWith { path ->
                counts.entries.filter { it.key in path.topics }.sumOf { it.value }
            }
            val best = byPath.maxByOrNull { it.value } ?: return null
            if (best.value.toFloat() / total < MIN_SHARE) return null
            // Gleichstand zaehlt als unentschieden: Zwei Pfade mit derselben Zahl sind keine
            // Neigung, und einen davon auszuwaehlen waere geraten.
            if (byPath.count { it.value == best.value } > 1) return null
            return best.key
        }

        /** Ob ueberhaupt genug beantwortet wurde, um etwas zu sagen. */
        fun enoughData(counts: Map<AnimationType, Int>): Boolean =
            counts.values.sum() >= MIN_FEEDS

        /** Welchen Anteil dieser Pfad an allem hat - fuer die Anzeige "woran liegt das". */
        fun shareOf(counts: Map<AnimationType, Int>, path: PlayPath): Float {
            val total = counts.values.sum()
            if (total <= 0) return 0f
            return counts.entries.filter { it.key in path.topics }.sumOf { it.value }
                .toFloat() / total
        }

        /**
         * Wie weit der Pfad gegangen ist - abgeleitet aus der Stufe des Wesens.
         *
         * Drei Stufen und nicht zehn: Jede muss etwas zu erzaehlen haben (siehe [loreOf]), und
         * drei Abschnitte, die man ueber Wochen erreicht, sind mehr wert als zehn, die im
         * Vorbeigehen durchlaufen.
         */
        fun stageFor(level: Int): Int = when {
            level >= STAGE_3_LEVEL -> 3
            level >= STAGE_2_LEVEL -> 2
            level >= STAGE_1_LEVEL -> 1
            else -> 0
        }

        /** Welche Stufe des Wesens die naechste Stufe des Pfades bringt - `null` beim Ende. */
        fun nextStageLevel(level: Int): Int? = when {
            level < STAGE_1_LEVEL -> STAGE_1_LEVEL
            level < STAGE_2_LEVEL -> STAGE_2_LEVEL
            level < STAGE_3_LEVEL -> STAGE_3_LEVEL
            else -> null
        }

        private const val STAGE_1_LEVEL = 2
        private const val STAGE_2_LEVEL = 5
        private const val STAGE_3_LEVEL = 9

        /**
         * Was der Pfad auf dieser Stufe ueber das Wesen erzaehlt - `null` auf Stufe 0.
         *
         * **Das ist die eigentliche Belohnung** und der Grund, warum sich das Mitmachen lohnt:
         * Nicht eine Zahl wird groesser, sondern das Wesen bekommt etwas zu sagen, das es vorher
         * nicht sagen konnte - und was es sagt, haengt daran, wie der Nutzer gelebt hat.
         */
        @StringRes
        fun loreOf(path: PlayPath, stage: Int): Int? = when (path) {
            WANDERER -> listOf(
                R.string.path_wanderer_1, R.string.path_wanderer_2, R.string.path_wanderer_3
            )
            KEEPER -> listOf(
                R.string.path_keeper_1, R.string.path_keeper_2, R.string.path_keeper_3
            )
            DREAMER -> listOf(
                R.string.path_dreamer_1, R.string.path_dreamer_2, R.string.path_dreamer_3
            )
            MAKER -> listOf(
                R.string.path_maker_1, R.string.path_maker_2, R.string.path_maker_3
            )
        }.getOrNull(stage - 1)
    }
}
