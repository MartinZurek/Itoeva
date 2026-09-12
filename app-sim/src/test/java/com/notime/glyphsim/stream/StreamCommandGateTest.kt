package com.notime.glyphsim.stream

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.ui.SavedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Schutzschicht zwischen Publikum und Wesen.
 *
 * Geprueft wird hier ausschliesslich, **wer wann durchkommt** - nicht, was das Spiel daraus
 * macht. Das ist die Arbeitsteilung, die den ganzen Stream-Client traegt: Ab dem Moment, in dem
 * ein Angebot dieses Tor passiert hat, unterscheidet nichts mehr einen Chat-Befehl von einem
 * spaeteren Bits-Ereignis.
 */
class StreamCommandGateTest {

    private val config = StreamCommandConfig()

    private fun saved(occurrenceId: Long) = SavedAction(
        reminderId = 1L,
        occurrenceId = occurrenceId,
        animationType = AnimationType.DRINK,
        libraryAnimationLabel = null,
        frames = listOf(IntArray(169))
    )

    /** Vier belegte Plaetze, nichts in Arbeit. */
    private fun vollbesetzt() = StreamInteractionState(
        slots = (1L..4L).map { saved(it) }
    )

    private fun befehl(viewer: String, slotId: Int, atMillis: Long) = ViewerCommand(
        viewerId = viewer,
        interaction = StreamInteraction.DropSafeSlot(slotId),
        origin = ExternalImpulseSource.TWITCH_CHAT_FREE,
        receivedAtMillis = atMillis
    )

    private fun admit(state: StreamGateState, viewer: String, slotId: Int, atMillis: Long, world: StreamInteractionState = vollbesetzt()) =
        StreamCommandGate.admit(state, befehl(viewer, slotId, atMillis), world, config)

    @Test
    fun `ein gueltiger Befehl auf einen belegten Platz kommt durch`() {
        val entschieden = admit(StreamGateState(), "lea", slotId = 1, atMillis = 0L)
        assertTrue(entschieden is StreamGateDecision.Accepted)
        assertEquals(1, (entschieden as StreamGateDecision.Accepted).slotId)
        assertEquals(1, entschieden.state.log.size)
        assertTrue(entschieden.state.log.single().accepted)
    }

    @Test
    fun `ein leerer Platz wird abgewiesen`() {
        val leer = StreamInteractionState(slots = listOf(saved(1L), null, null, null))
        val entschieden = admit(StreamGateState(), "lea", slotId = 2, atMillis = 0L, world = leer)
        assertEquals(
            StreamRejection.SLOT_EMPTY,
            (entschieden as StreamGateDecision.Rejected).reason
        )
    }

    @Test
    fun `ein leerer Platz kostet den Zuschauer nicht seinen Abstand`() {
        // Ein Tippfehler soll keine Minute Schweigen nach sich ziehen - sonst lernt ein neues
        // Publikum aus seinem ersten Versuch genau die falsche Lehre.
        val leer = StreamInteractionState(slots = listOf(saved(1L), null, null, null))
        val danach = admit(StreamGateState(), "lea", slotId = 3, atMillis = 0L, world = leer).state
        val zweiter = admit(danach, "lea", slotId = 1, atMillis = 10L, world = leer)
        assertTrue(zweiter is StreamGateDecision.Accepted)
    }

    @Test
    fun `derselbe Zuschauer muss seinen Abstand abwarten`() {
        val erst = admit(StreamGateState(), "lea", slotId = 1, atMillis = 0L)
        val zuFrueh = admit(erst.state, "lea", slotId = 2, atMillis = config.perViewerCooldownMillis - 1)
        assertEquals(
            StreamRejection.VIEWER_COOLDOWN,
            (zuFrueh as StreamGateDecision.Rejected).reason
        )

        val spaet = admit(zuFrueh.state, "lea", slotId = 2, atMillis = config.perViewerCooldownMillis)
        assertTrue(spaet is StreamGateDecision.Accepted)
    }

    @Test
    fun `zwei verschiedene Zuschauer teilen sich den gemeinsamen Abstand`() {
        val erst = admit(StreamGateState(), "lea", slotId = 1, atMillis = 0L)
        val sofort = admit(erst.state, "kim", slotId = 2, atMillis = config.globalCooldownMillis - 1)
        assertEquals(
            StreamRejection.GLOBAL_COOLDOWN,
            (sofort as StreamGateDecision.Rejected).reason
        )

        val danach = admit(sofort.state, "kim", slotId = 2, atMillis = config.globalCooldownMillis)
        assertTrue(danach is StreamGateDecision.Accepted)
    }

    @Test
    fun `ein Sturm von Befehlen erzeugt genau einen Anstoss`() {
        // Der Fall, den der erste oeffentliche Lauf garantiert produziert: Alle tippen zugleich.
        val zuschauer = (1..50).map { "viewer$it" }
        var state = StreamGateState()
        var angenommen = 0
        zuschauer.forEachIndexed { index, name ->
            val entschieden = admit(state, name, slotId = 1, atMillis = index.toLong())
            state = entschieden.state
            if (entschieden is StreamGateDecision.Accepted) angenommen++
        }
        assertEquals(1, angenommen)
    }

    @Test
    fun `waehrend ein Anstoss laeuft kommt kein zweiter durch`() {
        val laufend = vollbesetzt().let {
            it.copy(
                pending = ExternalImpulse(
                    impulseId = "1:1:720",
                    savedSlotId = 1,
                    reminderId = 1L,
                    occurrenceId = 1L,
                    animationType = AnimationType.DRINK,
                    source = ExternalImpulseSource.TWITCH_CHAT_FREE,
                    atMinute = 720
                )
            )
        }
        val entschieden = admit(StreamGateState(), "kim", slotId = 2, atMillis = 0L, world = laufend)
        assertEquals(
            StreamRejection.IMPULSE_PENDING,
            (entschieden as StreamGateDecision.Rejected).reason
        )
    }

    @Test
    fun `angenommene und abgelehnte Angebote stehen beide im Protokoll`() {
        val erst = admit(StreamGateState(), "lea", slotId = 1, atMillis = 0L)
        val abgelehnt = admit(erst.state, "lea", slotId = 2, atMillis = 1L)
        val protokoll = abgelehnt.state.log
        assertEquals(2, protokoll.size)
        assertTrue(protokoll[0].accepted)
        assertEquals(StreamRejection.VIEWER_COOLDOWN, protokoll[1].rejection)
        assertEquals(ExternalImpulseSource.TWITCH_CHAT_FREE, protokoll[1].origin)
    }

    @Test
    fun `das Protokoll waechst nicht ueber seine Grenze hinaus`() {
        val klein = StreamCommandConfig(logCapacity = 5)
        var state = StreamGateState()
        repeat(40) { index ->
            state = StreamCommandGate.admit(
                state,
                befehl("lea", slotId = 1, atMillis = index.toLong()),
                vollbesetzt(),
                klein
            ).state
        }
        assertEquals(5, state.log.size)
    }

    @Test
    fun `das Gedaechtnis vergisst Zuschauer deren Abstand abgelaufen ist`() {
        // Sonst traegt ein Lauf ueber viele Stunden am Ende ein Publikum mit sich herum,
        // das laengst weg ist.
        var state = StreamGateState()
        repeat(20) { index ->
            state = admit(
                state,
                "viewer$index",
                slotId = 1,
                atMillis = index * config.globalCooldownMillis
            ).state
        }
        val lebendig = state.lastAcceptByViewer.size
        assertTrue(
            "Es sollten nur Zuschauer der letzten Minute uebrig sein, es sind $lebendig",
            lebendig < 20
        )
    }
}
