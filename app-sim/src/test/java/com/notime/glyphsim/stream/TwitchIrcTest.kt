package com.notime.glyphsim.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das Chat-Protokoll ohne Netz.
 *
 * Zerlegen und Zusammensetzen von Zeilen ist der Teil der Twitch-Anbindung, der am leisesten
 * kaputtgeht: Ein falsch gelesener Absender trifft den Einzelabstand, eine verpasste PING-Zeile
 * wirft die Verbindung nach Minuten still ab. Beides waere im laufenden Stream nur als
 * "reagiert nicht mehr" sichtbar. Deshalb liegt genau dieser Teil in reinem Kotlin und wird
 * hier gehalten, statt auf einen Emulator mit Twitch-Konto zu warten.
 */
class TwitchIrcTest {

    @Test
    fun `die Anmeldung schickt PASS vor NICK und tritt dem Kanal bei`() {
        val zeilen = TwitchIrc.handshake("justinfan12345", "Itoeva")
        assertEquals(3, zeilen.size)
        assertTrue("PASS muss vor NICK stehen", zeilen[0].startsWith("PASS "))
        assertEquals("NICK justinfan12345", zeilen[1])
        assertEquals("JOIN #itoeva", zeilen[2])
    }

    @Test
    fun `der anonyme Name braucht kein Konto`() {
        for (seed in listOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE + 1)) {
            val nick = TwitchIrc.anonymousNick(seed)
            assertTrue("'$nick' ist kein justinfan-Name", nick.startsWith("justinfan"))
            assertTrue(nick.removePrefix("justinfan").all { it.isDigit() })
        }
    }

    @Test
    fun `Kanalnamen werden vereinheitlicht`() {
        for (eingabe in listOf("Itoeva", "#Itoeva", "  #ITOEVA  ", "itoeva")) {
            assertEquals("itoeva", TwitchIrc.normalizeChannel(eingabe))
        }
        assertEquals("", TwitchIrc.normalizeChannel("   "))
    }

    @Test
    fun `eine Chat-Nachricht liefert Absender und Text`() {
        val zeile = ":lea!lea@lea.tmi.twitch.tv PRIVMSG #itoeva :!drop A"
        val gelesen = TwitchIrc.parseLine(zeile)
        assertEquals(TwitchIrc.Line.Chat("lea", "!drop A"), gelesen)
    }

    @Test
    fun `der Absender wird kleingeschrieben damit der Abstand denselben Menschen trifft`() {
        val gross = TwitchIrc.parseLine(":LeaXY!lea@lea.tmi.twitch.tv PRIVMSG #itoeva :!drop B")
        assertEquals("leaxy", (gross as TwitchIrc.Line.Chat).viewerId)
    }

    @Test
    fun `ein Doppelpunkt im Text bleibt erhalten`() {
        val gelesen = TwitchIrc.parseLine(":kim!kim@kim.tmi.twitch.tv PRIVMSG #itoeva :schau mal: !drop C")
        assertEquals("schau mal: !drop C", (gelesen as TwitchIrc.Line.Chat).text)
    }

    @Test
    fun `PING wird erkannt und die Antwort traegt den Token zurueck`() {
        val gelesen = TwitchIrc.parseLine("PING :tmi.twitch.tv")
        assertEquals(TwitchIrc.Line.Ping("tmi.twitch.tv"), gelesen)
        assertEquals("PONG :tmi.twitch.tv", TwitchIrc.pong("tmi.twitch.tv"))
    }

    @Test
    fun `Beitritte, Serverhinweise und Unsinn reissen den Faden nicht ab`() {
        for (zeile in listOf(
            ":lea!lea@lea.tmi.twitch.tv JOIN #itoeva",
            ":tmi.twitch.tv 001 justinfan1 :Welcome, GLHF!",
            ":tmi.twitch.tv CAP * ACK :twitch.tv/tags",
            "",
            ":",
            ":kaputt",
            ":lea!lea@lea.tmi.twitch.tv PRIVMSG #itoeva :",
            "voelliger unsinn ohne doppelpunkt"
        )) {
            assertEquals(
                "'$zeile' haette ignoriert werden muessen",
                TwitchIrc.Line.Ignored,
                TwitchIrc.parseLine(zeile)
            )
        }
    }

    @Test
    fun `eine vollstaendige Zeile wird bis zum Platz durchgereicht`() {
        // Die Strecke, auf die es ankommt: rohe Serverzeile -> Absender -> Platz.
        val gelesen = TwitchIrc.parseLine(
            ":kim!kim@kim.tmi.twitch.tv PRIVMSG #itoeva :!drop d\r\n"
        ) as TwitchIrc.Line.Chat
        val angebot = StreamCommandParser.parse(gelesen.text)
        assertEquals(StreamInteraction.DropSafeSlot(4), angebot)
        assertEquals("kim", gelesen.viewerId)
    }
}
