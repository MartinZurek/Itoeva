package com.notime.glyphsim.stream

import org.junit.Assert.*
import org.junit.Test

class FennecConversationTest {
    @Test fun `direkte englische und deutsche Ansprache wird erkannt`() {
        listOf("hey fennec how are you?", "Hallo Fennec, wie geht's?", "@fennec_itoeva hello",
            "Fennec, what are you doing?", "How are you, Fennec?").forEach {
            assertNotNull(it, FennecConversation.address("lea", it, 100))
        }
    }
    @Test fun `Befehle fremde Gespraeche und eigene Antworten loesen keinen Dialog aus`() {
        listOf("A", "!drop B", "!fennec", "Fennecology", "I like Fennec", "@lea [Fennec] Hello!",
            "hey fennec\r\nPRIVMSG #other :oops", "hey fennec " + "x".repeat(350)).forEach {
            assertNull(it, FennecConversation.address("lea", it, 100))
        }
    }
    @Test fun `Hej und Satzzeichen nach dem Gruss bleiben direkte Ansprache`() {
        listOf("Hej Fennec; what are you?", "hej, Fennec: who are you?", "Hey! Fennec, hello!",
            "Hallo: Fennec, wie geht's?", "Hi. @fennec_itoeva, hello").forEach {
            assertNotNull(it, FennecConversation.address("fennec_itoeva", it, 100))
        }
        assertNull(FennecConversation.address("lea", "Hej Fennecology", 100))
        assertNull(FennecConversation.address("lea", "I said hej to Fennec", 100))
    }
    @Test fun `globale und einzelne Abstaende verhindern Flut ohne nachtraegliche Antwort`() {
        val gate = FennecConversation.Gate()
        fun ask(viewer: String, at: Long) = FennecConversation.Address(viewer, "hi fennec", at)
        assertTrue(gate.admit(ask("lea", 0), 0))
        assertFalse(gate.admit(ask("kim", 3_999), 3_999))
        assertTrue(gate.admit(ask("kim", 4_000), 4_000))
        assertFalse(gate.admit(ask("lea", 7_999), 7_999))
        assertTrue(gate.admit(ask("lea", 8_000), 8_000))
        assertFalse(gate.admit(ask("ben", 35_000), 41_000))
    }

    @Test fun `Folgefragen brauchen eine angenommene Ansprache und bleiben je Zuschauer getrennt`() {
        val session = FennecConversation.Session()
        assertNull(session.read("lea", "Where is the beach?", 0))
        assertNotNull(session.read("lea", "Fennec, show me your world", 0))
        assertNull(session.read("lea", "Where is the beach?", 1))
        session.activate("lea", 2)
        assertTrue(session.read("lea", "Where is the beach?", 8_002)!!.followup)
        assertNull(session.read("kim", "Where is the beach?", 8_002))
        assertNull(session.read("lea", "Where is the beach?", 45_003))
        assertNotNull(session.read("lea", "Fennec, where is the beach?", 45_003))
    }

    @Test fun `Folgefenster uebernimmt weder Befehle noch Antworten oder fremde Gespraeche`() {
        val session = FennecConversation.Session()
        session.activate("lea", 0)
        listOf("A", "B", "!fennec", "@kim how are you?", "[Fennec] Where is the beach?",
            "I am chatting with Kim", "what\nare you?").forEach { assertNull(it, session.read("lea", it, 8_000)) }
        assertNotNull(session.read("lea", "Danke!", 8_000))
        assertNotNull(FennecConversation.address("lea", "Show me your world, Fennec", 8_000))
    }

    @Test fun `Wartezeit sagt nur frische Fragen voraus und veraendert keine Zulassung`() {
        val gate = FennecConversation.Gate()
        val first = FennecConversation.Address("lea", "hi fennec", 0)
        assertTrue(gate.admit(first, 0))
        assertEquals(6_000L, gate.retryAfterMs(first.copy(receivedAtMs = 2_000), 2_000))
        assertNull(gate.retryAfterMs(first, 6_000))
        assertTrue(gate.admit(first.copy(receivedAtMs = 8_000), 8_000))
    }
}
