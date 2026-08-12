package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.AvatarSignatureAnimations
import com.notime.glyphcore.data.LibraryAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * **Drei Beobachtungen vom Geraet, die alle dasselbe Thema haben: zu wenig Abwechslung.**
 *
 * Gemeldet wurde: "Im Play-Modus kommt fast immer dieselbe Glocke, und die Reaktion ist auch immer
 * dieselbe" und "er rennt nur von einem Raum in den naechsten". Beides waren keine falschen Werte,
 * sondern fehlende Verbindungen - eine vorhandene Bibliothek, die das Spiel nie benutzt hat, und
 * eine Themenwahl, die den jetzigen Ort nicht kannte. Genau solche Luecken sieht man im Code nicht;
 * man sieht sie erst, wenn man eine Weile zuschaut.
 */
class PlayVarietyTest {

    // ---- Die Motive der Spiel-Erinnerung ----

    private fun library(labels: List<String>): List<LibraryAnimation> =
        labels.mapIndexed { index, label ->
            LibraryAnimation(id = index + 1L, label = label, emoji = "", framesData = "")
        }

    @Test
    fun `die Spiel-Erinnerung greift auf die Bibliothek zurueck`() {
        // Der eigentliche Befund: Vorher stand im Wurf `libraryAnimationId = null`, es kam also
        // ausschliesslich die Typ-Animation. Geprueft wird deshalb das Verhaeltnis, nicht ein
        // einzelner Wurf.
        val all = library(AvatarSignatureReactions.labels + listOf("Rocket", "Star", "Wave"))
        val own = AvatarSignatureReactions.labelsFor(AvatarSpecies.GLOOP).toSet()
        val random = Random(1)

        var fromLibrary = 0
        val draws = 2_000
        repeat(draws) {
            if (PlayModeRoll.chooseMotif(all, own, random) != null) fromLibrary++
        }
        val share = fromLibrary.toFloat() / draws
        assertTrue(
            "Nur ${(share * 100).toInt()} % der Spiel-Erinnerungen zeigen ein Motiv aus der " +
                "Bibliothek - dann bleibt es beim immer gleichen Bild.",
            share > 0.5f
        )
        assertTrue(
            "Die Animation des Themas kommt gar nicht mehr vor - sie ist die einzige, die zeigt, " +
                "worum es ueberhaupt geht.",
            share < 0.9f
        )
    }

    @Test
    fun `die eigenen Motive kommen haeufiger als fremde`() {
        // Die Handschrift der Figur soll erkennbar bleiben: Gloop bekommt eher seine Wolke als
        // Wyrmlings Blitz - beides darf vorkommen.
        val all = library(AvatarSignatureReactions.labels)
        val own = AvatarSignatureReactions.labelsFor(AvatarSpecies.GLOOP).toSet()
        val random = Random(2)

        var mine = 0
        var others = 0
        repeat(2_000) {
            val motif = PlayModeRoll.chooseMotif(all, own, random) ?: return@repeat
            if (motif.label in own) mine++ else others++
        }
        assertTrue("Die eigenen Motive kommen nie", mine > 0)
        assertTrue("Fremde Motive kommen nie - dann waeren es nur noch fuenf Bilder", others > 0)
        assertTrue(
            "Die eigenen Motive ($mine) kommen nicht haeufiger als die fremden ($others), obwohl " +
                "es davon fuenf und hiervon fuenfundzwanzig gibt",
            mine > others
        )
    }

    @Test
    fun `eine leere Bibliothek ist kein Fehlerfall`() {
        // Beim allerersten Start ist noch nichts eingespielt - dann gilt wieder, was vorher immer
        // galt: die Animation des Themas.
        repeat(50) {
            assertEquals(null, PlayModeRoll.chooseMotif(emptyList(), emptySet(), Random(it)))
        }
    }

    @Test
    fun `jede Kreatur hat genau ihre fuenf Motive`() {
        // Die Zuordnung wird aus der REIHENFOLGE der Liste abgeleitet (siehe
        // AvatarSignatureReactions.labelsFor). Laeuft sie gegen die Reihenfolge im Katalog
        // auseinander, bekaeme die falsche Kreatur das falsche Motiv - lautlos, denn beide Listen
        // waeren fuer sich gueltig.
        val seeded = AvatarSignatureAnimations.seed().map { it.label }
        val gathered = AvatarSpecies.entries.flatMap { AvatarSignatureReactions.labelsFor(it) }
        assertEquals(seeded, gathered)
        for (species in AvatarSpecies.entries) {
            assertEquals(5, AvatarSignatureReactions.labelsFor(species).size)
        }
    }

    // ---- Verweilen statt Rennen ----

    @Test
    fun `wer irgendwo ist, bleibt eher dort`() {
        // Ohne diesen Zuschlag wechselte die Figur bei jeder Regung das Zimmer - im Schnitt alle
        // zehn Sekunden, weil so lang die Pause zwischen zwei Regungen ist.
        val phase = PlayAmbientActivity.DayPhase.MIDDAY
        fun shareOf(topic: AnimationType, stayAt: PlayScene.Place?): Float {
            val random = Random(7)
            var hits = 0
            val draws = 3_000
            repeat(draws) {
                if (PlayAmbientActivity.nextTopic(phase, stayAt = stayAt, random = random) == topic) hits++
            }
            return hits.toFloat() / draws
        }

        val loose = shareOf(AnimationType.DRINK, stayAt = null)
        val staying = shareOf(AnimationType.DRINK, stayAt = PlayScene.Place.KITCHEN)
        assertTrue(
            "In der Kueche wird nicht haeufiger getrunken als anderswo ($loose vs $staying)",
            staying > loose * 1.5f
        )
    }

    @Test
    fun `das Verweilen ueberstimmt den Tagesablauf nicht`() {
        // Der Zuschlag ist ein Zuschlag, keine Sperre: Nachts gehoert die Figur ins Bett, auch
        // wenn sie gerade in der Kueche steht. Sonst waere aus dem Verweilen ein Feststecken
        // geworden - der umgekehrte Fehler.
        val random = Random(11)
        var sleep = 0
        val draws = 2_000
        repeat(draws) {
            val topic = PlayAmbientActivity.nextTopic(
                PlayAmbientActivity.DayPhase.NIGHT,
                stayAt = PlayScene.Place.KITCHEN,
                random = random
            )
            if (topic == AnimationType.SLEEP) sleep++
        }
        assertTrue(
            "Nachts wird nur noch in ${(sleep * 100f / draws).toInt()} % der Faelle geschlafen",
            sleep.toFloat() / draws > 0.4f
        )
    }

    @Test
    fun `trotz Verweilen kommt der Avatar weiterhin nach draussen`() {
        // **Die Gegenprobe zum Verweilen, und der Grund fuer die Obergrenze.** Ein Zuschlag fuers
        // Bleiben wirkt in genau die Richtung, gegen die eine frühere Beobachtung angegangen ist
        // ("diese Welt besteht fast nur aus Zimmern"). Geprueft wird deshalb nicht die einzelne
        // Ziehung, sondern der ganze Kreislauf, wie ihn der Dock-Bildschirm fuehrt: mitzaehlen,
        // wie oft die Figur schon geblieben ist, und den Zuschlag ab MAX_STAY_ROUNDS aussetzen.
        val random = Random(23)
        for (phase in listOf(
            PlayAmbientActivity.DayPhase.MORNING,
            PlayAmbientActivity.DayPhase.MIDDAY,
            PlayAmbientActivity.DayPhase.EVENING
        )) {
            var place = PlayScene.Place.LIVING
            var stayed = 0
            var outside = 0
            val draws = 3_000
            repeat(draws) {
                val topic = PlayAmbientActivity.nextTopic(
                    phase,
                    stayAt = place.takeIf { stayed < PlayAmbientActivity.MAX_STAY_ROUNDS },
                    random = random
                )
                val next = PlayScene.forTopic(topic)
                stayed = if (next == place) stayed + 1 else 0
                place = next
                // Wie im Ablauf selbst: Der Startort zaehlt, und die Spaziergaenge fuehren
                // ueberdies noch weiter hinaus (siehe PlayRoutines zu MOVE).
                if (PlayScene.isOutdoors(next)) outside++
            }
            val share = outside.toFloat() / draws
            assertTrue(
                "In der Phase $phase fuehren nur noch ${(share * 100).toInt()} % der Regungen " +
                    "nach draussen - das Verweilen ist zum Feststecken geworden.",
                share >= 0.20f
            )
        }
    }

    @Test
    fun `die Neigung faerbt, ohne den Tag zu uebernehmen`() {
        // **Die Entwicklung muss man SEHEN**, sonst ist sie eine Behauptung im Gespraech. Sie darf
        // aber auch nicht alles verdraengen: Ein Wesen, das ausschliesslich tut, wozu es neigt,
        // waere keine Figur mehr, sondern eine Einstellung.
        val phase = PlayAmbientActivity.DayPhase.MIDDAY
        val leaning = setOf(AnimationType.MOVE, AnimationType.WORK)

        fun shareOfLeaning(withLeaning: Boolean): Float {
            val random = Random(31)
            var hits = 0
            val draws = 4_000
            repeat(draws) {
                val topic = PlayAmbientActivity.nextTopic(
                    phase,
                    leaning = if (withLeaning) leaning else emptySet(),
                    random = random
                )
                if (topic in leaning) hits++
            }
            return hits.toFloat() / draws
        }

        val plain = shareOfLeaning(withLeaning = false)
        val leaned = shareOfLeaning(withLeaning = true)
        assertTrue(
            "Die Neigung aendert nichts am Verhalten ($plain vs $leaned)",
            leaned > plain * 1.2f
        )
        assertTrue(
            "Die Neigung verdraengt alles andere ($leaned)",
            leaned < 0.75f
        )
    }

    @Test
    fun `das Verweilen erfindet keine Themen`() {
        // Ein Thema, das zur Tageszeit gar nicht vorkommt, darf nicht allein deshalb auftauchen,
        // weil die Figur zufaellig im passenden Zimmer steht - sonst schliefe sie mittags weiter,
        // bloss weil sie im Schlafzimmer aufgewacht ist.
        val random = Random(13)
        repeat(3_000) {
            val topic = PlayAmbientActivity.nextTopic(
                PlayAmbientActivity.DayPhase.MORNING,
                stayAt = PlayScene.Place.BEDROOM,
                random = random
            )
            assertTrue("Morgens wurde SLEEP gewuerfelt", topic != AnimationType.SLEEP)
        }
    }
}
