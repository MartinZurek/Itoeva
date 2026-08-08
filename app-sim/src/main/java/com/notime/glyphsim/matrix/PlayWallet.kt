package com.notime.glyphsim.matrix

import android.content.Context

/**
 * Was der Avatar verdient und ausgibt - **der Kreislauf, der aus Arbeit einen Zweck macht.**
 *
 * **Warum das noetig ist.** Arbeiten war bisher eine Taetigkeit wie jede andere: Er ging zur
 * Arbeitsstelle, weil das Thema gerade gewuerfelt wurde, und was dabei herauskam, war eine
 * Animation. Ein Beruf ohne Ergebnis ist aber nur eine Beschaeftigung. Erst wenn Arbeit etwas
 * einbringt, das anderswo gebraucht wird, entsteht ein Zusammenhang, den man beim Zuschauen
 * versteht - und der Grund, warum jemand morgens losgeht.
 *
 * Zusammen mit dem Vorrat ([PlayPantry]) ergibt das die erste vollstaendige Schleife dieser Welt:
 * arbeiten → Geld → einkaufen → Vorrat → essen → Vorrat leer → wieder arbeiten. Jeder Schritt hat
 * seine Ursache im vorherigen, keiner im Zufall.
 *
 * **Bewusst winzig.** Ein Zaehler, feste Betraege, keine Preise je Ware, keine Ersparnisse, keine
 * Schulden. Es geht nicht um Wirtschaft, sondern um den einen sichtbaren Zusammenhang
 * "leer und blank → also arbeiten".
 */
object PlayWallet {

    /** Was ein Arbeitstag einbringt. */
    const val WAGE = 2

    /** Was ein Einkauf kostet - weniger als ein Arbeitstag einbringt, damit die Schleife nicht
     *  klemmt und er nicht dauernd nur arbeiten muss. */
    const val GROCERY_COST = 2

    /** Womit er startet: genug fuer einen Einkauf, damit die Welt nicht mit einem Engpass beginnt. */
    private const val INITIAL = 2

    private const val PREFS = "play_wallet"
    private const val KEY_COINS = "coins"

    fun coins(context: Context): Int = prefs(context).getInt(KEY_COINS, INITIAL).coerceAtLeast(0)

    /** Ob ein Einkauf bezahlbar ist. */
    fun canAfford(context: Context): Boolean = coins(context) >= GROCERY_COST

    fun earn(context: Context) {
        setCoins(context, coins(context) + WAGE)
    }

    /** Bezahlt einen Einkauf; gibt zurueck, ob es gereicht hat. */
    fun pay(context: Context): Boolean {
        if (!canAfford(context)) return false
        setCoins(context, coins(context) - GROCERY_COST)
        return true
    }

    private fun setCoins(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_COINS, value.coerceIn(0, MAX_COINS)).apply()
    }

    /** Deckel, damit die Zahl in der Anzeige nicht ins Absurde waechst, wenn lange niemand
     *  hinsieht - sie ist ein Hinweis, keine Punktetafel. */
    private const val MAX_COINS = 99

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
