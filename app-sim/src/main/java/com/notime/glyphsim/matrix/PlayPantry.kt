package com.notime.glyphsim.matrix

import android.content.Context

/**
 * Wie viel im Kuehlschrank ist - **das erste BEDUERFNIS, das bestimmt, was der Avatar vorhat.**
 *
 * **Warum das mehr ist als ein weiterer Zaehler.** Bisher entschied ausschliesslich der Zufall
 * (gewichtet nach Tageszeit und offenen Gewohnheiten), was als Naechstes geschieht - einkaufen
 * war eine von zwei Varianten des Essen-Ablaufs und passierte deshalb genauso oft, wenn der
 * Kuehlschrank voll war. Sobald ein Vorrat zur Neige geht und DESHALB eingekauft wird, hat eine
 * Handlung zum ersten Mal eine Ursache in der Welt statt in einer Wuerfelzahl. Genau daran
 * unterscheidet sich ein Bewohner von einer Abspielliste.
 *
 * **Bewusst winzig gehalten.** Drei Portionen, kein Geld, keine einzelnen Waren, kein Verderben.
 * Es geht nicht um eine Wirtschaftssimulation, sondern um den einen Zusammenhang "leer → also
 * losgehen", der sich beim Zuschauen von selbst erschliesst.
 *
 * Ueber die Einstellungen hinweg gemerkt, damit ein Vorrat nicht bei jedem Andocken wieder voll
 * ist - eine Welt, die sich beim Hinsehen zuruecksetzt, fuehlt sich nicht wie eine an.
 */
object PlayPantry {

    /** Wie viele Portionen ein voller Kuehlschrank fasst. */
    const val FULL = 3

    private const val PREFS = "play_pantry"
    private const val KEY_LEVEL = "level"

    fun level(context: Context): Int =
        prefs(context).getInt(KEY_LEVEL, FULL).coerceIn(0, FULL)

    /** Ob der Vorrat aufgebraucht ist und deshalb eingekauft werden muss. */
    fun isEmpty(context: Context): Boolean = level(context) <= 0

    /** Eine Portion verbrauchen - beim Essen. */
    fun consume(context: Context) {
        setLevel(context, level(context) - 1)
    }

    /** Nach dem Einkauf wieder auffuellen. */
    fun refill(context: Context) {
        setLevel(context, FULL)
    }

    private fun setLevel(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_LEVEL, value.coerceIn(0, FULL)).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
