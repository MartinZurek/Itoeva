package com.notime.glyphsim.stream

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.ui.SavedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die ganze Strecke an einem Stueck: rohe Chat-Zeile bis Zielvorschlag an den Living Agent.
 *
 * ## Warum dieser Test der wichtigste der Reihe ist
 *
 * Die Einzelteile - Protokoll, Parser, Tor, Auswahl - haben jeweils eigene Tests, und alle vier
 * koennen gruen sein, waehrend die Naht dazwischen nicht haelt. Genau an dieser Naht liegt die
 * Behauptung, um die es beim Stream-Client ueberhaupt geht:
 *
 * > Die Spielschicht muss nicht wissen, ob eine Aktion aus einem kostenlosen Chat-Befehl, einem
 * > Testereignis oder spaeter aus Bits entstanden ist.
 *
 * Der letzte Test hier haelt genau das fest: Zwei Angebote unterschiedlicher Herkunft erzeugen
 * denselben Vorschlag. Wer das spaeter bricht - etwa indem er die Bezahlfrage in die Auswahl
 * oder in den Agenten traegt - faellt hier auf.
 */
class StreamViewerChainTest {

    private fun saved(occurrenceId: Long, type: AnimationType = AnimationType.DRINK) = SavedAction(
        reminderId = 7L,
        occurrenceId = occurrenceId,
        animationType = type,
        libraryAnimationLabel = null,
        frames = listOf(IntArray(169))
    )

    private val welt = StreamInteractionState(slots = (1L..4L).map { saved(it) })

    /** Eine Zeile, wie Twitch sie tatsaechlich schickt. */
    private fun chatZeile(nick: String, text: String) =
        ":$nick!$nick@$nick.tmi.twitch.tv PRIVMSG #itoeva :$text"

    @Test
    fun `eine Chat-Zeile wird zu einem Zielvorschlag fuer den Living Agent`() {
        // 1. Serverzeile lesen
        val gelesen = TwitchIrc.parseLine(chatZeile("lea", "!drop B")) as TwitchIrc.Line.Chat

        // 2. In ein plattformfreies Angebot uebersetzen
        val angebot = viewerCommandOf(
            gelesen.viewerId,
            gelesen.text,
            ExternalImpulseSource.TWITCH_CHAT_FREE,
            atMillis = 0L
        )
        assertNotNull(angebot)
        assertEquals(StreamInteraction.DropSafeSlot(2), angebot!!.interaction)

        // 3. Durch die Schutzschicht
        val entschieden = StreamCommandGate.admit(StreamGateState(), angebot, welt)
        assertTrue(entschieden is StreamGateDecision.Accepted)
        val slotId = (entschieden as StreamGateDecision.Accepted).slotId

        // 4. In die BESTEHENDE Auswahl - dieselbe, die ein Tippen im Client benutzt
        val gewaehlt = StreamInteractions.select(
            welt,
            slotId = slotId,
            atMinute = 720,
            source = angebot.origin
        )
        assertTrue(gewaehlt is StreamSelection.Accepted)
        val impuls = (gewaehlt as StreamSelection.Accepted).impulse
        assertEquals(2, impuls.savedSlotId)
        assertEquals(ExternalImpulseSource.TWITCH_CHAT_FREE, impuls.source)

        // 5. Und dort ist es ein VORSCHLAG, kein Befehl.
        val einfluss = StreamInteractions.influenceFor(impuls)
        assertEquals(GoalKind.GET_FOOD, einfluss?.goal)
    }

    @Test
    fun `gewoehnlicher Chat laeuft durch die ganze Kette hindurch ins Leere`() {
        for (text in listOf("hallo", "wer streamt hier", "!drop Z", "drop A")) {
            val gelesen = TwitchIrc.parseLine(chatZeile("kim", text)) as TwitchIrc.Line.Chat
            assertNull(
                "'$text' haette nichts erzeugen duerfen",
                viewerCommandOf(gelesen.viewerId, gelesen.text, ExternalImpulseSource.TWITCH_CHAT_FREE, 0L)
            )
        }
    }

    @Test
    fun `derselbe Mensch in zwei Schreibweisen teilt sich einen Abstand`() {
        // Sonst umgeht jeder den Einzelabstand, indem er seinen Namen anders schreibt - und der
        // Schutz waere eine Fassade.
        val erst = viewerCommandOf("Lea", "!drop A", ExternalImpulseSource.LOCAL_TEST_PROVIDER, 0L)!!
        val zweit = viewerCommandOf("  lea ", "!drop B", ExternalImpulseSource.LOCAL_TEST_PROVIDER, 1L)!!
        assertEquals(erst.viewerId, zweit.viewerId)

        val nachDemErsten = StreamCommandGate.admit(StreamGateState(), erst, welt)
        val zweiter = StreamCommandGate.admit(nachDemErsten.state, zweit, welt)
        assertEquals(
            StreamRejection.VIEWER_COOLDOWN,
            (zweiter as StreamGateDecision.Rejected).reason
        )
    }

    @Test
    fun `die Herkunft aendert am Ergebnis im Spiel nichts`() {
        // Die eigentliche Architekturzusage. Heute stehen hier Chat und Testeingang; wenn
        // spaeter TWITCH_BITS dazukommt, gehoert es in dieselbe Liste - und dieser Test muss
        // ohne jede weitere Aenderung gruen bleiben.
        val quellen = listOf(
            ExternalImpulseSource.TWITCH_CHAT_FREE,
            ExternalImpulseSource.LOCAL_TEST_PROVIDER,
            ExternalImpulseSource.LOCAL_VIEWER_SIMULATOR
        )
        val ergebnisse = quellen.map { quelle ->
            val angebot = viewerCommandOf("lea", "!drop A", quelle, 0L)!!
            val entschieden = StreamCommandGate.admit(StreamGateState(), angebot, welt)
            assertTrue("$quelle wurde abgewiesen", entschieden is StreamGateDecision.Accepted)
            val gewaehlt = StreamInteractions.select(
                welt,
                slotId = (entschieden as StreamGateDecision.Accepted).slotId,
                atMinute = 720,
                source = quelle
            ) as StreamSelection.Accepted
            Triple(
                gewaehlt.impulse.savedSlotId,
                gewaehlt.impulse.animationType,
                StreamInteractions.influenceFor(gewaehlt.impulse)?.goal
            )
        }
        assertEquals(
            "Unterschiedliche Herkunft darf im Spiel keinen Unterschied machen",
            1,
            ergebnisse.toSet().size
        )
    }

    @Test
    fun `ein zweites Angebot waehrend eines laufenden Anstosses wird abgewiesen`() {
        val erst = viewerCommandOf("lea", "!drop A", ExternalImpulseSource.TWITCH_CHAT_FREE, 0L)!!
        val angenommen = StreamCommandGate.admit(StreamGateState(), erst, welt) as StreamGateDecision.Accepted
        val laufend = (StreamInteractions.select(welt, angenommen.slotId, 720) as StreamSelection.Accepted).state

        val zweit = viewerCommandOf(
            "kim",
            "!drop C",
            ExternalImpulseSource.TWITCH_CHAT_FREE,
            atMillis = StreamCommandConfig().globalCooldownMillis
        )!!
        val entschieden = StreamCommandGate.admit(angenommen.state, zweit, laufend)
        assertEquals(
            StreamRejection.IMPULSE_PENDING,
            (entschieden as StreamGateDecision.Rejected).reason
        )
    }
}
