package com.notime.glyphsim.ui

/**
 * XP/Level-Rechnung des Play Mode (siehe [PlayModeViewModel]). Das Level wird bewusst NICHT
 * gespeichert, sondern immer aus `xp` abgeleitet ([levelFor]) - dadurch kann es nie mit der
 * gespeicherten XP-Zahl auseinanderlaufen.
 */
object PlayModeXp {
    /** XP je beantworteter Play-Mode-Ausloesung. */
    const val XP_PER_FEED = 10

    /** XP-Bedarf je Level - bewusst ein einziger fester Wert statt einer Kurve: das ist der
     *  Grundstein, eine spuerbarere Progression kann spaeter draufgesetzt werden. */
    const val XP_PER_LEVEL = 50

    fun levelFor(xp: Int): Int = 1 + xp / XP_PER_LEVEL

    /**
     * Wie viel ein ganzer Punkt Wohlbefinden waere, wenn man ihn auf einmal gewaenne.
     *
     * Erreichbar ist das nie - eine einzelne Handlung bewegt das Wohlbefinden um wenige
     * Hundertstel. Die Zahl ist der Massstab, nicht ein Versprechen.
     */
    const val XP_PER_WELLBEING_POINT = 50

    /** Mehr als das kann eine einzelne Handlung nicht einbringen, wie gut sie auch tut. */
    const val MAX_WELLBEING_BONUS = 8

    /**
     * **Zusatz-XP fuer das, was eine Handlung dem Wesen tatsaechlich gebracht hat** (NT-072).
     *
     * Bis hierher gab jede beantwortete Erinnerung genau [XP_PER_FEED] - eine Tablette so viel
     * wie ein Nachmittag draussen. Das ist verstaendlich als Grundstein, sagt aber ueber das
     * Wesen nichts aus: Erfahrung entstand aus dem ZAEHLEN von Erinnerungen, nicht aus dem
     * Erleben.
     *
     * Dieser Zuschlag kommt oben drauf und misst die eine Groesse, die das Living Agent System
     * ohnehin fuehrt: das Wohlbefinden vor und nach der Handlung
     * ([com.notime.glyphsim.living.Needs.wellbeing]).
     *
     * **Nie negativ.** Arbeit und Konzentration kosten Kraft und senken das Wohlbefinden kurz -
     * dafuer XP abzuziehen hiesse, das Wesen fuer Anstrengung zu bestrafen. Der Grundbetrag
     * bleibt in jedem Fall.
     */
    fun wellbeingBonus(before: Double, after: Double): Int {
        val gain = (after - before).coerceAtLeast(0.0)
        return (gain * XP_PER_WELLBEING_POINT)
            .toInt()
            .coerceIn(0, MAX_WELLBEING_BONUS)
    }
}
