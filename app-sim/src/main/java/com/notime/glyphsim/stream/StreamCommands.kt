package com.notime.glyphsim.stream

import com.notime.glyphsim.ui.ACTION_SLOT_COUNT

/**
 * Was ein Zuschauer der laufenden Instanz anbieten kann - plattformfrei formuliert.
 *
 * Das ist die schmale Stelle, auf die sich alle Eingangsquellen einigen. Ein kostenloser
 * Chat-Befehl, ein Tippen im Testmodus und spaeter ein Bits-Ereignis erzeugen denselben Wert;
 * ab hier weiss niemand mehr, wo er herkam. Genau das macht die Bezahlfrage spaeter zu einer
 * Frage der Eingangsschicht statt zu einer Frage des Spiels.
 */
internal sealed interface StreamInteraction {

    /**
     * "Wirf, was in Platz [slotId] liegt, dem Wesen hin."
     *
     * Bewusst kein Befehl, sondern ein Angebot: was daraus wird, entscheidet weiterhin die
     * bestehende Auswahl in [StreamInteractions] und der Living Agent.
     */
    data class DropSafeSlot(val slotId: Int) : StreamInteraction {
        init {
            require(slotId in 1..ACTION_SLOT_COUNT) { "slotId out of range: $slotId" }
        }
    }
}

/**
 * Ein Angebot, wie es die Eingangsschicht verlaesst: wer, was, woher, wann.
 *
 * [viewerId] ist die Kennung, gegen die der Einzelabstand zaehlt - im Chat der Anzeigename in
 * Kleinschreibung, im Testmodus ein frei gewaehlter Name. Sie wandert nie in die Spielschicht
 * und nie in die Datenbank; sie lebt nur im Eingangsprotokoll dieser Sitzung.
 */
internal data class ViewerCommand(
    val viewerId: String,
    val interaction: StreamInteraction,
    val origin: ExternalImpulseSource,
    val receivedAtMillis: Long
)

/**
 * Der gemeinsame Schritt jeder Textquelle: aus "wer" und "was stand da" ein Angebot machen.
 *
 * Steht hier statt in den Providern, weil beide Textquellen - der Demo-Eingang und der
 * Twitch-Chat - dieselbe Vereinheitlichung brauchen: Ein Name muss getrimmt und kleingeschrieben
 * werden, sonst zaehlt der Einzelabstand fuer `Lea` und `lea` getrennt und der Schutz ist
 * wirkungslos. Als reine Funktion ist genau das offline pruefbar.
 *
 * `null` heisst "war kein Befehl" - der Regelfall fuer fast jede Chat-Zeile.
 */
internal fun viewerCommandOf(
    viewerId: String,
    message: String,
    origin: ExternalImpulseSource,
    atMillis: Long,
    config: StreamCommandConfig = StreamCommandConfig()
): ViewerCommand? = StreamCommandParser.parse(message, config)?.let { interaction ->
    ViewerCommand(
        viewerId = viewerId.trim().lowercase(),
        interaction = interaction,
        origin = origin,
        receivedAtMillis = atMillis
    )
}

/**
 * Die zentralen Stellschrauben der Zuschauer-Interaktion.
 *
 * Absichtlich ein Datenobjekt und keine Konstanten irgendwo in der Spielschicht: Die Werte sind
 * das, was der erste oeffentliche Lauf herausfinden soll, und sie sollen sich aendern lassen,
 * ohne dass jemand Gameplay-Code anfasst.
 *
 * **Warum diese Voreinstellungen.** Eine angenommene Aktion laeuft im Spiel je nach Ziel ueber
 * mehrere simulierte Minuten. [globalCooldownMillis] begrenzt deshalb den Gesamtdurchsatz auf
 * gut sieben Anstoesse pro Minute - genug, damit sich der Stream lebendig anfuehlt, zu wenig,
 * als dass ein Publikum das Wesen im Sekundentakt herumreissen koennte.
 * [perViewerCooldownMillis] sorgt dafuer, dass dieser knappe Durchsatz nicht einem einzigen
 * schnellen Tipper gehoert.
 */
internal data class StreamCommandConfig(
    val prefix: String = "!",
    val dropKeyword: String = "drop",
    val perViewerCooldownMillis: Long = 60_000L,
    val globalCooldownMillis: Long = 8_000L,
    val logCapacity: Int = 40
) {
    init {
        require(prefix.isNotBlank()) { "prefix must not be blank" }
        require(dropKeyword.isNotBlank()) { "dropKeyword must not be blank" }
        require(perViewerCooldownMillis >= 0L) { "perViewerCooldownMillis must not be negative" }
        require(globalCooldownMillis >= 0L) { "globalCooldownMillis must not be negative" }
        require(logCapacity > 0) { "logCapacity must be positive" }
    }

    /** Die Zeile, die im Bild steht. Sie wird aus derselben Quelle gebaut wie der Parser. */
    fun hint(): String = (1..ACTION_SLOT_COUNT).joinToString("  ") { slotId ->
        "$prefix$dropKeyword ${StreamCommandParser.letterFor(slotId)}"
    }
}

/**
 * Text zu Angebot - und sonst nichts.
 *
 * Reine Funktion ohne Zustand, ohne Uhr, ohne Netz. Dadurch laesst sich die gesamte Syntax
 * offline pruefen, lange bevor eine echte Twitch-Verbindung existiert. Alles, was nicht genau
 * passt, ist `null`; ein Chat enthaelt ueberwiegend Saetze, die uns nichts angehen, und die
 * duerfen keinen Pfad im Spiel anstossen.
 */
internal object StreamCommandParser {

    private const val FIRST_LETTER = 'A'

    /** Der Buchstabe, unter dem Platz [slotId] im Bild steht: 1 -> A, 2 -> B, ... */
    fun letterFor(slotId: Int): Char {
        require(slotId in 1..ACTION_SLOT_COUNT) { "slotId out of range: $slotId" }
        return FIRST_LETTER + (slotId - 1)
    }

    /**
     * Erlaubt sind `!drop A` bis `!drop D` und - als Nachsicht gegenueber dem, was Leute
     * tatsaechlich tippen - `!drop 1` bis `!drop 4`.
     *
     * Gross- und Kleinschreibung sind egal, zusaetzlicher Leerraum ebenso. Was dahinter noch
     * folgt, wird verworfen statt den Befehl ungueltig zu machen: `!drop a bitte!!` ist
     * erkennbar gemeint und soll nicht an einem Ausrufezeichen scheitern.
     */
    fun parse(message: String, config: StreamCommandConfig = StreamCommandConfig()): StreamInteraction? {
        val words = message.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 2) return null
        val head = words[0].lowercase()
        if (head != (config.prefix + config.dropKeyword).lowercase()) return null
        val slotId = slotIdOf(words[1]) ?: return null
        return StreamInteraction.DropSafeSlot(slotId)
    }

    private fun slotIdOf(argument: String): Int? {
        val token = argument.trim().trimEnd('.', ',', '!', '?').uppercase()
        if (token.length != 1) return null
        val character = token[0]
        val slotId = when {
            character in FIRST_LETTER..(FIRST_LETTER + ACTION_SLOT_COUNT - 1) ->
                character - FIRST_LETTER + 1
            character.isDigit() -> character - '0'
            else -> return null
        }
        return slotId.takeIf { it in 1..ACTION_SLOT_COUNT }
    }
}
