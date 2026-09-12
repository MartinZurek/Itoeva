package com.notime.glyphsim.living

/**
 * **Was von der Entscheidung eines Wesens nach aussen dringt** - als Symbol, nie als Satz.
 *
 * ## Der Befund, der dazu gefuehrt hat
 *
 * Der Living Agent entscheidet seit NT-065 tatsaechlich, was in der Pixelwelt geschieht: Er
 * waehlt ein Ziel, leitet einen Plan ab, prueft Voraussetzungen und plant um. Jeder Schritt
 * erzeugt eine vollstaendige [AgentExplanation] - Ziel, Begruendung, Plan, Hindernis,
 * wirksame Erinnerungen.
 *
 * Und nichts davon war zu sehen. Die Erklaerung ging in
 * [com.notime.glyphsim.stream.LivingObservationFeed], und den las niemand; er ist fuer ein
 * kuenftiges Overlay gedacht. Das Einzige, was ueber dem Kopf erschien, war ein zufaellig
 * gewuerfelter Satz aus `PlaySpeech`, der mit dem Ziel des Wesens nichts zu tun hatte. Wer
 * zusah, konnte deshalb nicht unterscheiden, ob die Figur etwas WOLLTE oder ob der Wuerfel es
 * ergab - obwohl sie es seit Wochen wirklich will.
 *
 * Diese Datei schliesst genau diese Luecke, und zwar an der Stelle, an der die Entscheidung
 * entsteht, statt an der, an der sie gezeichnet wird.
 *
 * ## Warum Symbole und keine Saetze
 *
 * Die Vorgabe steht in `LIVING_AGENT.md` und ist aelter als dieser Code: Ereignisse und
 * Erklaerungen tragen **keinen freien Text**. Ein Satz ueber dem Kopf ist in einem Stream fuer
 * die Haelfte der Zuschauer wertlos, er braucht eine Uebersetzung je Sprache, und er verleitet
 * dazu, dem Wesen Dinge in den Mund zu legen, die die Simulation gar nicht weiss. Ein Symbol
 * kann nur zeigen, was tatsaechlich im Zustand steht.
 *
 * **Der Text beim Antippen bleibt** (siehe `PlayTalkPanel`). Wer ausdruecklich fragt, bekommt
 * eine Antwort in Sprache; wer nur zusieht, bekommt ein Bild. Das ist keine Inkonsequenz,
 * sondern der Unterschied zwischen einer Auskunft und einem Kommentar.
 *
 * ## Zwei Symbole, nicht eines
 *
 * [wish] ist, was das Wesen **will**. [obstacle] ist, woran es gerade **haengt**. Zusammen
 * ergeben sie die kleinste Geschichte, die diese Welt erzaehlen kann - "ich will essen" plus
 * "ich habe kein Geld" ist bereits ein Konflikt, und Konflikte sind das, worauf man zusieht.
 * Einzeln waere jedes davon nur eine Zustandsanzeige.
 *
 * Alles hier ist reine Rechnung ohne Android - dieselbe Bedingung wie im uebrigen Kern.
 */
object LivingSymbols {

    /**
     * Was das Wesen gerade will - `null`, wenn es nichts vorhat.
     *
     * **Ohne Ziel kein Symbol.** Ein zufriedenes Wesen, das gerade nichts braucht, soll auch
     * nichts anzeigen; eine dauerhaft belegte Blase ueber dem Kopf waere wieder nur Dekoration.
     */
    fun wish(goal: GoalKind?): SymbolicIntent? = when (goal) {
        GoalKind.GET_FOOD -> SymbolicIntent.FOOD
        GoalKind.REST -> SymbolicIntent.TIRED
        GoalKind.HAVE_FUN -> SymbolicIntent.PLAY
        // Kein eigenes Lern-Symbol erfinden: Der feste Satz aus LIVING_AGENT.md kennt QUESTION,
        // und "etwas wissen wollen" ist genau das, was DEVELOP antreibt. Ein zwoelftes Symbol
        // waere eine Aenderung am Vertrag, die ein einzelnes Ziel nicht rechtfertigt.
        GoalKind.DEVELOP -> SymbolicIntent.QUESTION
        GoalKind.CONNECT_WITH -> SymbolicIntent.AFFECTION
        GoalKind.EARN_MONEY -> SymbolicIntent.WORK
        // Erkunden ist Ueberraschung, bevor sie eintritt: Wer losgeht, weiss noch nicht, was
        // er findet. SURPRISE war bis hierher das einzige Symbol ohne Verwendung.
        GoalKind.EXPLORE -> SymbolicIntent.SURPRISE
        // Behaglichkeit ist nach Hause wollen - im woertlichen wie im uebertragenen Sinn.
        GoalKind.SEEK_COMFORT -> SymbolicIntent.HOME
        null -> null
    }

    /**
     * Woran es gerade haengt - `null`, wenn nichts im Weg steht.
     *
     * **Der Ort ist ausdruecklich kein Hindernis.** [Requirement.At] bedeutet nur, dass das
     * Wesen noch unterwegs ist, und das sieht man ohnehin: Es laeuft gerade. Ein Symbol dafuer
     * hinge bei jedem zweiten Schritt ueber dem Kopf und wuerde die Faelle verdecken, in denen
     * wirklich etwas fehlt.
     */
    fun obstacle(blockedBy: Requirement?): SymbolicIntent? = when (blockedBy) {
        // Ihm fehlt Geld - und was er dagegen braucht, ist Arbeit. Das Symbol zeigt den Ausweg
        // und nicht den Mangel: Eine leere Hand liesse sich nicht von "kein Hunger" unterscheiden.
        is Requirement.Coins -> SymbolicIntent.WORK
        is Requirement.Portions -> SymbolicIntent.FOOD
        // Zu. Das einzige Hindernis, das reines Warten verlangt.
        is Requirement.SiteOpen -> SymbolicIntent.NO
        // Er sucht jemanden, der nicht da ist. Zusammen mit dem Wunsch AFFECTION liest sich das
        // als Frage nach einem Wesen - genau so ist es gemeint.
        is Requirement.Near -> SymbolicIntent.QUESTION
        is Requirement.At -> null
        null -> null
    }

    /**
     * Beides aus einer Erklaerung - die Form, in der die Anzeige es braucht.
     *
     * `null` heisst: diesmal ist nichts zu zeigen.
     */
    fun of(explanation: AgentExplanation): LivingSymbolPair? {
        val wunsch = wish(explanation.goal) ?: return null
        return LivingSymbolPair(wunsch, obstacle(explanation.blockedBy))
    }
}

/**
 * Ein Wunsch und, wenn es eines gibt, sein Hindernis.
 *
 * Getrennte Felder und keine Liste: Die beiden sind nicht gleichrangig. Der Wunsch steht immer,
 * das Hindernis kommt dazu - eine Anzeige, die beide gleich behandelt, verliert genau diese
 * Aussage.
 */
data class LivingSymbolPair(
    val wish: SymbolicIntent,
    val obstacle: SymbolicIntent? = null
) {
    val isBlocked: Boolean get() = obstacle != null
}
