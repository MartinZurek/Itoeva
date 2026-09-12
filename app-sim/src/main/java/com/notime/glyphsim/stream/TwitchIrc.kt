package com.notime.glyphsim.stream

/**
 * Das Twitch-Chat-Protokoll, so weit wir es brauchen - als reine Zeichenkettenarbeit.
 *
 * ## Warum anonym
 *
 * Twitch laesst **Mitlesen ohne jedes Konto** zu: Wer sich mit einem Namen der Form
 * `justinfan<Zahl>` anmeldet, bekommt den oeffentlichen Chat eines Kanals, ohne Token, ohne
 * OAuth, ohne Affiliate-Status. Genau das brauchen wir hier und nicht mehr - wir wollen lesen,
 * nicht schreiben.
 *
 * Das ist kein Trick, sondern der vorgesehene Weg fuer Lesezugriffe, und es hat eine angenehme
 * Folge: In diesem Client existiert **kein Geheimnis**. Es gibt nichts, was versehentlich in ein
 * Protokoll, einen Commit oder ein Bildschirmfoto im Stream geraten koennte. Sobald spaeter
 * Bits oder Subs dazukommen, braucht es OAuth - dann aber in einem eigenen Provider, nicht hier.
 *
 * ## Warum die Aufteilung
 *
 * Dieses Objekt kennt keine Verbindung. Es baut Zeilen und liest Zeilen; das Socket liegt in
 * [TwitchChatInteractionProvider]. Dadurch ist der ganze anfaellige Teil - Handshake-Reihenfolge,
 * PING/PONG, das Zerlegen einer PRIVMSG-Zeile - ohne Netz, ohne Twitch-Konto und ohne Emulator
 * pruefbar, und die Testliste in `tools/reaction-preview/tests.sh` deckt ihn mit ab.
 */
internal object TwitchIrc {

    const val HOST = "irc.chat.twitch.tv"

    /** TLS-Port. Der unverschluesselte 6667 waere ebenso moeglich und kommt nicht in Frage. */
    const val TLS_PORT = 6697

    /** Twitch erwartet bei anonymer Anmeldung irgendein Passwort; dieses ist das uebliche. */
    private const val ANONYMOUS_PASS = "PASS SCHMOOPIIE"

    /** Was eine eingehende Zeile fuer uns bedeutet. Alles Uebrige ist [Line.Ignored]. */
    sealed interface Line {
        /** Eine oeffentliche Chat-Nachricht. [viewerId] ist bereits kleingeschrieben. */
        data class Chat(val viewerId: String, val text: String) : Line

        /** Der Server prueft, ob wir noch da sind. Wer nicht antwortet, fliegt raus. */
        data class Ping(val token: String) : Line

        data object Ignored : Line
    }

    /**
     * Ein anonymer Anmeldename aus einem Startwert.
     *
     * Die Zahl muss nicht eindeutig sein - Twitch vergibt keine Rechte daran. Sie streut nur,
     * damit zwei gleichzeitig laufende Instanzen nicht zufaellig denselben Namen fuehren.
     */
    fun anonymousNick(seed: Int): String {
        val number = 10_000 + (seed.toLong().let { if (it < 0) -it else it } % 80_000L)
        return "justinfan$number"
    }

    /** `itoeva`, `#Itoeva`, ` ITOEVA ` - fuer Twitch ist all das derselbe Kanal. */
    fun normalizeChannel(raw: String): String = raw.trim().removePrefix("#").lowercase()

    /**
     * Die drei Zeilen der Anmeldung, in genau dieser Reihenfolge.
     *
     * Ohne `PASS` vor `NICK` weist Twitch die Verbindung ab, und zwar ohne brauchbare Meldung -
     * deshalb steht die Reihenfolge in einem Test statt in einem Kommentar.
     */
    fun handshake(nick: String, channel: String): List<String> = listOf(
        ANONYMOUS_PASS,
        "NICK $nick",
        "JOIN #${normalizeChannel(channel)}"
    )

    /** Die Antwort auf ein [Line.Ping]. Sie muss den Token unveraendert zuruecktragen. */
    fun pong(token: String): String = "PONG :$token"

    /**
     * Zerlegt eine Serverzeile.
     *
     * Erwartetes Format einer Nachricht:
     * `:name!name@name.tmi.twitch.tv PRIVMSG #kanal :der Text`
     *
     * Bewusst nachsichtig: Alles, was nicht sauber passt, wird [Line.Ignored] statt einer
     * Ausnahme. Ein Chat liefert Beitritte, Austritte, Serverhinweise und gelegentlich Unsinn;
     * nichts davon darf den Lesefaden abreissen lassen.
     */
    fun parseLine(raw: String): Line {
        val line = raw.trimEnd('\r', '\n')
        if (line.startsWith("PING")) {
            return Line.Ping(line.substringAfter(':', "tmi.twitch.tv").ifBlank { "tmi.twitch.tv" })
        }
        if (!line.startsWith(":")) return Line.Ignored
        val prefixEnd = line.indexOf(' ')
        if (prefixEnd <= 1) return Line.Ignored
        val viewerId = line.substring(1, prefixEnd).substringBefore('!').lowercase()
        if (viewerId.isBlank()) return Line.Ignored
        val rest = line.substring(prefixEnd + 1)
        if (!rest.startsWith("PRIVMSG ")) return Line.Ignored
        val textStart = rest.indexOf(" :")
        if (textStart < 0) return Line.Ignored
        val text = rest.substring(textStart + 2)
        if (text.isBlank()) return Line.Ignored
        return Line.Chat(viewerId, text)
    }
}
