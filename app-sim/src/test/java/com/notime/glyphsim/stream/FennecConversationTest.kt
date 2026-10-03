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
        assertFalse(gate.admit(ask("kim", 9_999), 9_999))
        assertTrue(gate.admit(ask("kim", 10_000), 10_000))
        assertFalse(gate.admit(ask("lea", 20_000), 20_000))
        assertTrue(gate.admit(ask("lea", 30_000), 30_000))
        assertFalse(gate.admit(ask("ben", 35_000), 41_000))
    }
}
