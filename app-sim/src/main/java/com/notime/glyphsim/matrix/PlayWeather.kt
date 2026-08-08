package com.notime.glyphsim.matrix

import java.time.LocalDate

/**
 * **Was fuer ein Tag es ist.**
 *
 * Der Horizont ist das Einzige an dieser Welt, das keine Kanten hat - alles andere ist ein Zimmer
 * mit Waenden. Genau deshalb traegt er die Stimmung: Der abgesenkte Boden im naechtlichen Park,
 * der Mond darueber, die ziehenden Wolken. Wetter ist die naechste Sprosse auf derselben Leiter,
 * und es kostet fast nichts: ein paar Zellen, die von oben nach unten wandern.
 *
 * **Warum das mehr ist als Zierrat.** Ein Regentag aendert nichts an dem, was der Avatar tut - und
 * doch sieht derselbe Tagesablauf anders aus. Das ist der billigste Weg zu dem Gefuehl, dass diese
 * Welt weiterlaeuft, ohne dass man ihr zusieht: Man kommt zurueck, und es hat sich etwas
 * geaendert, das niemand ausgeloest hat.
 *
 * **Es gilt DRINNEN wie DRAUSSEN.** Der schoenste Teil ist nicht der Regen im Park, sondern der
 * Regen, den man vom Sessel aus durchs Fenster sieht (siehe [PlayScene]). Genau daran erkennt man,
 * dass drinnen und draussen derselbe Ort sind und nicht zwei Kulissen.
 *
 * **Aus dem Datum gerechnet, nicht gewuerfelt.** Wetter, das sich bei jedem Hinsehen neu
 * entscheidet, ist kein Wetter, sondern Flimmern. Ein Tag hat EIN Wetter, und wer morgens
 * hineinschaut, sieht abends dasselbe - es sei denn, es zieht ueber den Tag weiter (siehe
 * [forDate]). Damit braucht es keinen gespeicherten Zustand und keine Hintergrundarbeit.
 */
enum class PlayWeather {
    CLEAR,
    RAIN,
    SNOW;

    val isFalling: Boolean get() = this != CLEAR

    companion object {

        private const val PREFS = "play_weather"
        private const val KEY_FORCED = "forced"

        /**
         * Ein festgehaltenes Wetter statt des heutigen - zum Ansehen.
         *
         * **Aus demselben Grund wie der Zeitraffer** (siehe [PlayTimeLapse]): Regen faellt an
         * knapp jedem fuenften Tag und Schnee nur im Winter. Ohne diesen Schalter liesse sich
         * nicht beurteilen, ob es gut aussieht - man muesste warten, bis das Wetter von selbst
         * kommt, und haette dann keinen Vergleich.
         *
         * Im Speicher gehalten, weil [current] im Zeichenpfad liegt und dort kein Dateizugriff
         * hingehoert.
         */
        @Volatile
        private var forced: PlayWeather? = null

        fun current(): PlayWeather = forced ?: forDate()

        fun forcedOrNull(): PlayWeather? = forced

        /**
         * Nur fuer die Zeichenraster-Vorschau (siehe ScenePreviewTool) - ohne Einstellungen und
         * ohne Geraet. Getrennt von [force], damit ein Testlauf nie in die gespeicherten
         * Einstellungen eines Nutzers schreibt.
         */
        fun forceForPreview(value: PlayWeather?) {
            forced = value
        }

        fun restore(context: android.content.Context) {
            val stored = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .getString(KEY_FORCED, null)
            forced = stored?.let { name -> runCatching { valueOf(name) }.getOrNull() }
        }

        fun force(context: android.content.Context, value: PlayWeather?) {
            context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putString(KEY_FORCED, value?.name).apply()
            forced = value
        }

        /**
         * Das Wetter eines Tages.
         *
         * **Schnee nur im Winter, Regen das ganze Jahr** - eine Welt, in der es im Juli schneit,
         * ist keine Welt mehr, sondern eine Zufallsanzeige. Die Verteilung ist bewusst
         * zurueckhaltend: Die allermeisten Tage sind klar. Wetter, das staendig da ist, hoert auf
         * aufzufallen, und dann waere der Aufwand umsonst.
         *
         * Gerechnet statt gespeichert: Aus der Tageszahl wird eine feste Zahl gezogen. Derselbe
         * Tag ergibt immer dasselbe Wetter, auch nach einem Neustart - und ueber die Monatszahl
         * gemischt, damit nicht jeder Erste im Monat gleich aussieht.
         */
        fun forDate(date: LocalDate = LocalDate.now()): PlayWeather {
            val roll = ((date.dayOfYear * 37 + date.monthValue * 11 + date.year) % 100 + 100) % 100
            val wintry = date.monthValue == 12 || date.monthValue <= 2
            return when {
                roll < 18 -> RAIN
                wintry && roll < 34 -> SNOW
                else -> CLEAR
            }
        }
    }
}
