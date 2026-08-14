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

}
