package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionCatalog
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.living.Requirement
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Zwei Beobachtungen vom Geraet, beide mit derselben Art Ursache** (NT-073).
 *
 * > "Es kommt mir immer noch so vor, als wuerde er hauptsaechlich in seinem Zimmer hocken."
 * > "Die Traumsequenz hab ich auch noch nicht gesehen."
 *
 * In beiden Faellen lag es nicht an einer zu kleinen Wahrscheinlichkeit, sondern daran, dass es
 * die Sache im Modell gar nicht gab:
 *
 * - **Draussen** war ein Ort, an dem sich das Wesen gelegentlich BEFAND. Kein einziges
 *   `Requirement` im ganzen Kern nannte ihn, also stand er in keinem Plan, und Hinausgehen war
 *   nie eine Absicht, sondern immer nur eine Nebenwirkung der Themenwahl.
 * - **Traeume** gab es ausschliesslich im Nachtschlaf ab 23 Uhr. Wer abends zusieht, konnte
 *   keinen sehen - egal wie lange. Die Sequenz war nicht selten, sie war unerreichbar.
 *
 * Diese Tests halten die beiden Mechanismen fest, nicht ihre Zahlen.
 */
class OutdoorsAndDreamsTest {

    // ---- Draussen ----

    @Test
    fun `genau eine Beschaeftigung verlangt, draussen zu sein`() {
        val draussen = ActionCatalog.FREE_TIME.filter { kind ->
            ActionCatalog[kind].requirements.any {
                it is Requirement.At && it.site == LivingSite.OUTSIDE
            }
        }
        assertEquals(
            "Sich zu bewegen heisst, draussen zu sein - und das ist die Handlung, die den " +
                "Avatar ueberhaupt vor die Tuer bringt",
            listOf(ActionKind.MOVE_BODY),
            draussen
        )
    }

    @Test
    fun `drinnen laesst sich Bewegung gar nicht ausfuehren`() {
        val bewegung = ActionCatalog[ActionKind.MOVE_BODY]
        val zuhause = WorldState(
            day = 1,
            minuteOfDay = 15 * 60,
            site = LivingSite.HOME,
            coins = 3,
            portions = 2,
            openSites = LivingRuntimeAdapter.openSitesAt(15 * 60)
        )
        assertFalse(bewegung.isPossible(zuhause))
        assertEquals(Requirement.At(LivingSite.OUTSIDE), bewegung.blockedBy(zuhause))
        assertTrue(bewegung.isPossible(zuhause.copy(site = LivingSite.OUTSIDE)))
    }

    @Test
    fun `Hinausgehen lohnt sich trotz des Wegs`() {
        // Der Weg kostet jetzt Zeit. Ohne Ausgleich waere Hinausgehen unterm Strich teurer als
        // Herumsitzen - die Aenderung haette dann genau das Gegenteil bewirkt.
        val draussen = ActionCatalog[ActionKind.MOVE_BODY].outcome.needRelief
        val drinnen = ActionCatalog[ActionKind.PURSUE_INTEREST].outcome.needRelief
        assertTrue(
            "Bewegung muss mehr Freude bringen als das Herumsitzen daheim",
            draussen.getValue(NeedKind.FUN) >
                drinnen.getValue(NeedKind.FUN)
        )
    }

    @Test
    fun `draussen ist immer offen`() {
        // Ein Ort, der eine Voraussetzung traegt, darf nicht zufaellig geschlossen sein - sonst
        // waere Bewegung nachts unmoeglich statt nur unwahrscheinlich.
        for (stunde in 0..23) {
            assertTrue(
                "Um $stunde Uhr war draussen zu",
                LivingSite.OUTSIDE in LivingRuntimeAdapter.openSitesAt(stunde * 60)
            )
        }
    }

    // ---- Traeume ----

    @Test
    fun `die ruhigen Abschnitte des Tages tragen eine Traumgelegenheit`() {
        // Nicht ueber Zeitfenster erschlossen, sondern im Ablauf nachgelesen: Genau das ist der
        // Grund, warum der Schritt ausdruecklich existiert.
        for (topic in listOf(AnimationType.REST, AnimationType.MINDFULNESS)) {
            assertTrue(
                "$topic hat keine einzige Traumgelegenheit",
                PlayRoutines.allFor(topic).any { routine ->
                    routine.steps.any { it is RoutineStep.Daydream }
                }
            )
        }
    }

    @Test
    fun `auch draussen auf der Bank kann getraeumt werden`() {
        val aufDerBank = PlayRoutines.allFor(AnimationType.MOVE).filter { routine ->
            routine.steps.any { it is RoutineStep.Occupy && it.station == PlayScene.Station.BENCH }
        }
        assertTrue("Kein Ablauf setzt sich draussen auf eine Bank", aufDerBank.isNotEmpty())
        assertTrue(
            "Auf der Bank zu sitzen ist der ruhigste Moment draussen - dort gehoert ein Traum hin",
            aufDerBank.any { routine -> routine.steps.any { it is RoutineStep.Daydream } }
        )
    }

    @Test
    fun `der Tagtraum kommt, aber seltener als der Nachttraum`() {
        fun anteil(wuerfe: Int, regel: (Random) -> Boolean): Double {
            val zufall = Random(42)
            return (1..wuerfe).count { regel(zufall) } / wuerfe.toDouble()
        }
        val nachts = anteil(4_000) { PlayDreams.shouldDream(it) }
        val tagsueber = anteil(4_000) { PlayDreams.shouldDaydream(it) }

        assertTrue("Ein Tagtraum muss vorkommen, sonst aendert sich fuer den Zuschauer nichts",
            tagsueber > 0.15)
        assertTrue("Er soll ein Aufblitzen bleiben, kein Dauerzustand", tagsueber < nachts)
    }

    @Test
    fun `ein Traum greift nur auf tatsaechlich Erlebtes zurueck`() {
        // Ohne Erinnerungen kein Traum - der Schritt kostet dann nichts und der Ablauf laeuft
        // unveraendert weiter.
        assertEquals(null, PlayDreams.choose(emptyList(), Random(1)))
        // Und geschlafen wird nicht ueber den Schlaf selbst.
        assertEquals(
            null,
            PlayDreams.choose(listOf(AnimationType.SLEEP, AnimationType.MEDICINE), Random(1))
        )
        assertEquals(
            AnimationType.BOOK,
            PlayDreams.choose(listOf(AnimationType.SLEEP, AnimationType.BOOK), Random(1))
        )
    }
}
