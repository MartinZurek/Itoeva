package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Die Pruefung zu der Meldung "man sieht immer dasselbe".**
 *
 * Gemeldet wurden drei Beispiele: Basketball war beim normalen Zuschauen nie zu sehen, Buchlesen
 * und der Drachen kaum. Nachgerechnet ist keine der drei gesperrt - sie sind zu selten, um
 * bemerkt zu werden. Mittags hat MOVE Gewicht 3 von 16, davon fuehren 70 % zu einer
 * Sonderaktivitaet, und die wird unter fuenf gleichberechtigten gezogen: rund 2,6 % je Ablauf.
 * In fuenf Minuten laufen etwa drei Ablaeufe, macht knapp 8 % Chance auf Basketball.
 *
 * Am Geraet ist das nicht zu pruefen - man muesste stundenlang zusehen und koennte danach
 * trotzdem nicht sagen, ob eine Aktivitaet fehlt oder der Wuerfel ungnaedig war. Genau deshalb
 * steht die Pruefung hier: als Simulation mit festem Startwert.
 */
class PlayVarietyGuardTest {

    private val tagphasen = listOf(
        PlayAmbientActivity.DayPhase.MORNING,
        PlayAmbientActivity.DayPhase.MIDDAY,
        PlayAmbientActivity.DayPhase.EVENING
    )

    /**
     * Spielt eine Folge autonomer Regungen durch und fuehrt dabei denselben Verlauf mit, den
     * `DockScreen` im Arbeitsspeicher haelt.
     *
     * Bildet damit ab, was beim Zuschauen tatsaechlich passiert - nicht, was eine einzelne
     * Ziehung fuer sich genommen koennte.
     */
    private fun beobachte(
        phase: PlayAmbientActivity.DayPhase,
        runden: Int,
        random: Random,
        mitVielfalt: Boolean = true
    ): Pair<List<AnimationType>, List<PlayRoutines.SpecialActivity>> {
        val themen = mutableListOf<AnimationType>()
        val sonder = mutableListOf<PlayRoutines.SpecialActivity>()
        val letzteThemen = ArrayDeque<AnimationType>()
        val letzteSonder = ArrayDeque<PlayRoutines.SpecialActivity>()
        repeat(runden) {
            val topic = PlayAmbientActivity.nextTopic(
                phase = phase,
                justPlayed = letzteThemen.firstOrNull(),
                recentTopics = if (mitVielfalt) letzteThemen.toList() else emptyList(),
                random = random
            )
            val routine = PlayRoutines.forTopic(
                topic = topic,
                recentSpecials = if (mitVielfalt) letzteSonder.toList() else emptyList(),
                random = random
            )
            themen += topic
            letzteThemen.addFirst(topic)
            while (letzteThemen.size > 4) letzteThemen.removeLast()
            PlayRoutines.specialOf(routine)?.let {
                sonder += it
                letzteSonder.addFirst(it)
                while (letzteSonder.size > 4) letzteSonder.removeLast()
            }
        }
        return themen to sonder
    }

    // ================= Erreichbarkeit =================

    /**
     * **Die wichtigste Zusicherung dieser Datei.** Kein Grundgewicht von null, keine widerspruech-
     * liche Bedingung: Buchlesen, Drachen und Basketball muessen in ihrem vorgesehenen Kontext
     * tatsaechlich vorkommen. Eine Aktivitaet, die es gibt, die aber niemand je zu Gesicht
     * bekommt, ist Aufwand ohne Wirkung.
     */
    @Test
    fun `Buchlesen, Drachen und Basketball sind erreichbar`() {
        val random = Random(11)
        val (themen, sonder) = beobachte(PlayAmbientActivity.DayPhase.EVENING, 4_000, random)

        assertTrue("Buchlesen kommt abends nie vor", AnimationType.BOOK in themen)
        assertTrue("Der Drachen kommt nie vor", PlayRoutines.SpecialActivity.KITE in sonder)
        assertTrue("Basketball kommt nie vor", PlayRoutines.SpecialActivity.BASKETBALL in sonder)
    }

    /** Und keine der fuenf Sonderbeschaeftigungen faellt unter den Tisch, nicht nur diese drei. */
    @Test
    fun `jede Sonderbeschaeftigung wird tatsaechlich gezogen`() {
        val (_, sonder) = beobachte(PlayAmbientActivity.DayPhase.MIDDAY, 4_000, Random(23))
        for (art in PlayRoutines.SpecialActivity.entries) {
            assertTrue("$art wird nie gezogen", art in sonder)
        }
    }

    // ================= Wirkung der Vielfaltssicherung =================

    /**
     * Der eigentliche Nachweis: Die Sonderaktivitaeten verteilen sich mit Verlaufserinnerung
     * gleichmaessiger als ohne. Gemessen wird der Abstand zwischen der haeufigsten und der
     * seltensten der fuenf - je kleiner, desto eher sieht man beim Zuschauen alle.
     */
    @Test
    fun `die Verlaufserinnerung glaettet die Sonderaktivitaeten`() {
        fun spanne(mitVielfalt: Boolean): Int {
            val (_, sonder) = beobachte(
                PlayAmbientActivity.DayPhase.MIDDAY, 2_000, Random(7), mitVielfalt
            )
            val zaehlung = PlayRoutines.SpecialActivity.entries.map { art -> sonder.count { it == art } }
            return zaehlung.max() - zaehlung.min()
        }
        assertTrue(
            "Die Verlaufserinnerung macht die Verteilung nicht gleichmaessiger",
            spanne(mitVielfalt = true) < spanne(mitVielfalt = false)
        )
    }

    /**
     * **Kein starrer Wechsel.** Der Auftrag verlangt ausdruecklich beides: weniger Wiederholung,
     * aber keine Hektik. Ein Rundlauf, der jede Aktivitaet der Reihe nach abarbeitet, waere
     * genauso falsch wie die Dauerwiederholung - deshalb muss dasselbe Thema durchaus noch
     * zweimal hintereinander vorkommen duerfen.
     */
    @Test
    fun `Abwechslung erzeugt keinen Rundlauf`() {
        val (themen, _) = beobachte(PlayAmbientActivity.DayPhase.MIDDAY, 2_000, Random(5))
        val wiederholungen = themen.zipWithNext().count { (a, b) -> a == b }
        assertTrue("Keine einzige Wiederholung - das ist ein Rundlauf, kein Tagesablauf", wiederholungen > 0)
        assertTrue(
            "Jede fuenfte Regung wiederholt sich - der Daempfer wirkt nicht",
            wiederholungen < themen.size / 5
        )
    }

    /** Und kein Thema darf eine Folge dauerhaft beherrschen. */
    @Test
    fun `kein Thema beherrscht eine repraesentative Folge`() {
        for (phase in tagphasen) {
            val (themen, _) = beobachte(phase, 1_500, Random(31))
            val haeufigstes = themen.groupingBy { it }.eachCount().maxOf { it.value }
            assertTrue(
                "In $phase stellt ein einziges Thema ${haeufigstes * 100 / themen.size} % der Regungen",
                haeufigstes < themen.size / 2
            )
        }
    }

    // ================= Die Grenzen, die bleiben muessen =================

    /**
     * **Abwechslung darf nichts erfinden.** Nachts ist SLEEP das einzige Thema mit Grundgewicht;
     * ein Bonus fuer "lange nicht dran gewesene" Themen wuerde ausgerechnet die Nachtruhe
     * aufbrechen, die [PlayAmbientActivity] ausdruecklich garantiert.
     */
    @Test
    fun `nachts bleibt es beim Schlafen, egal wie lange nichts anderes dran war`() {
        val lange = listOf(AnimationType.SLEEP, AnimationType.SLEEP, AnimationType.SLEEP, AnimationType.SLEEP)
        val random = Random(3)
        repeat(500) {
            assertEquals(
                AnimationType.SLEEP,
                PlayAmbientActivity.nextTopic(
                    phase = PlayAmbientActivity.DayPhase.NIGHT,
                    justPlayed = AnimationType.SLEEP,
                    recentTopics = lange,
                    random = random
                )
            )
        }
    }

    /** MEDICINE bleibt aus der autonomen Auswahl heraus - auch mit Verlaufserinnerung. */
    @Test
    fun `MEDICINE kommt aus einer autonomen Regung weiterhin nie`() {
        for (phase in PlayAmbientActivity.DayPhase.entries) {
            val (themen, _) = beobachte(phase, 800, Random(13))
            assertTrue("$phase zieht MEDICINE", AnimationType.MEDICINE !in themen)
        }
    }

    /**
     * Die Vielfalt waehlt nur, was zur Tageszeit ohnehin erlaubt ist - sie gewichtet um, sie
     * oeffnet nichts. Ein Thema ohne Grundgewicht in dieser Phase darf auch dann nicht
     * erscheinen, wenn es "lange nicht dran" war.
     */
    @Test
    fun `die Vielfalt oeffnet kein Thema ausserhalb seiner Tageszeit`() {
        val nieDagewesen = listOf(AnimationType.GENERAL, AnimationType.GENERAL, AnimationType.GENERAL)
        val random = Random(17)
        repeat(1_000) {
            val topic = PlayAmbientActivity.nextTopic(
                phase = PlayAmbientActivity.DayPhase.MORNING,
                recentTopics = nieDagewesen,
                random = random
            )
            // Morgens gibt es kein WORK, kein REST, kein BOOK - siehe weightsFor.
            assertTrue(
                "$topic hat morgens gar kein Grundgewicht",
                topic in setOf(
                    AnimationType.MOVE, AnimationType.GENERAL, AnimationType.DRINK,
                    AnimationType.FOCUS, AnimationType.MINDFULNESS
                )
            )
        }
    }
}
