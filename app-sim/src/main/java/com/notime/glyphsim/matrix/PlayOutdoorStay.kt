package com.notime.glyphsim.matrix

/**
 * **Wie lange ein Aufenthalt unter freiem Himmel mindestens dauert** (NT-057).
 *
 * ## Warum das nicht der Musik gehoert
 *
 * Gemeldet wurde es an der Musik: "der Wechsel war viel zu schnell, dann wieder in diese ruhige
 * Stimmung". Naheliegend waere deshalb eine Regel im Player - ein Track laeuft mindestens X.
 * Das waere aber die Behandlung des Symptoms: Die Musik wechselte richtig, sie folgte nur einer
 * Welt, in der die Figur nach zwanzig Sekunden wieder hineinging.
 *
 * Deshalb steht die Mindestdauer hier, beim ZUSTAND. Wer draussen ist, bleibt eine Weile
 * draussen; die Musik tut daraufhin von selbst das Richtige, ohne eine zweite Zeitregel zu
 * kennen. Eine Regel im Player haette ausserdem die schlechtere Eigenschaft gehabt, den Ton von
 * dem zu entkoppeln, was zu sehen ist - Musik fuer draussen, waehrend die Figur schon am
 * Kuehlschrank steht.
 *
 * ## Was diese Regel ausdruecklich NICHT anfasst
 *
 * Sie greift nur in die autonome Themenwahl ein. Eine echte Erinnerung, eine ausdrueckliche
 * Bitte des Nutzers und der Zwang zu arbeiten, wenn Vorrat und Geld fehlen, laufen alle an
 * [PlayAmbientActivity.nextTopic] vorbei und bleiben unberuehrt. Und nachts gilt sie gar nicht:
 * Dort ist SLEEP das einzige Thema, und ein Wesen, das um drei Uhr auf der Strasse festgehalten
 * wird, weil es dort zufaellig die Tageszeit gewechselt hat, waere ein schlimmerer Fehler als der,
 * den diese Datei behebt.
 */
object PlayOutdoorStay {

    /**
     * Wie lange ein Aufenthalt draussen mindestens laeuft, bevor die Figur von sich aus wieder
     * hineingehen darf.
     *
     * Neunzig Sekunden sind ungefaehr drei Regungen (der Takt liegt bei 18 bis 36 Sekunden zuzueglich
     * der Ablaufdauer) - lang genug, dass ein Ausflug als Ausflug gelesen wird und die Musik
     * ankommt, kurz genug, dass es sich nicht wie eine Sperre anfuehlt. Bewusst NICHT laenger als
     * die laengste Aussenroutine ohnehin dauert: Die Regel soll das Zurueckgehen bremsen, nicht
     * einen zweiten Aufenthalt erzwingen.
     */
    const val MIN_STAY_MS = 90_000L

    /**
     * Ob die Figur gerade draussen GEHALTEN wird.
     *
     * @param outdoorsForMs wie lange sie schon ununterbrochen unter freiem Himmel ist. Wer
     *   drinnen ist, uebergibt hier nichts - die Regel gilt nur nach draussen, nie nach drinnen.
     *   Eine Mindestdauer fuers Drinnenbleiben braucht es nicht: Drinnen war nie das Problem.
     * @param phase nachts immer `false`, siehe Klassendoku.
     */
    fun holdsOutdoors(outdoorsForMs: Long, phase: PlayAmbientActivity.DayPhase): Boolean =
        phase != PlayAmbientActivity.DayPhase.NIGHT && outdoorsForMs in 0 until MIN_STAY_MS

    /**
     * Die Themen, die den Aufenthalt draussen fortsetzen - alle, deren Ort unter freiem Himmel
     * liegt (siehe [PlayScene.forTopic]).
     *
     * Ueber den Ort abgeleitet statt als gepflegte Liste, aus demselben Grund wie bei
     * [PlayRoutines.specialOf]: Die Zuordnung Thema-zu-Ort IST schon die Auskunft, und eine
     * zweite Liste daneben koennte davon abweichen, ohne dass es jemandem auffiele.
     */
    fun outdoorTopics(candidates: Set<com.notime.glyphcore.data.AnimationType>) =
        candidates.filterTo(mutableSetOf()) { PlayScene.isOutdoors(PlayScene.forTopic(it)) }
}
