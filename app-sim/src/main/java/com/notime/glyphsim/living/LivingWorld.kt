package com.notime.glyphsim.living

/**
 * **Die Welt, so weit der Agent sie zum Entscheiden braucht** - und keinen Schritt weiter.
 *
 * ## Warum hier ein eigenes Ortsmodell steht
 *
 * Die sichtbare Welt kennt sechzehn Orte ([com.notime.glyphsim.matrix.PlayScene.Place]) mit
 * Stationen, Moebeln und Wegen. Fuer eine Entscheidung zaehlt davon fast nichts: Ob der Vorrat
 * in der Kueche oder in der Speisekammer liegt, aendert an "ich habe Hunger und nichts da" gar
 * nichts. Diese Domaene fuehrt deshalb vier Orte, und der Runtime-Adapter (NT-065) bildet die
 * sechzehn darauf ab - nicht umgekehrt.
 *
 * Das ist kein Ordnungsfimmel. `PlayScene.kt` hat 3.672 Zeilen; haenge die Entscheidungslogik
 * daran, ist sie ohne Emulator nicht mehr pruefbar, und genau das soll sie bleiben.
 *
 * ## Was hier ausdruecklich NICHT steht
 *
 * **Keine Uhr.** [minuteOfDay] und [day] werden hereingereicht, nie gelesen. Ein Agent, der
 * `System.currentTimeMillis` kennt, laesst sich nicht ueber sieben Tage pruefen, ohne sieben
 * Tage zu warten - und im Zeitraffer ([com.notime.glyphsim.matrix.PlayTimeLapse]) rechnete er
 * gegen die falsche Uhr.
 *
 * **Kein Zufall.** Gleiche Welt und gleicher Agent ergeben dieselbe Entscheidung. Wo zwei
 * Moeglichkeiten gleich gut sind, entscheidet eine feste Reihenfolge und kein Wuerfel (siehe
 * [UtilitySelector]).
 */
data class WorldState(
    /** Simulationstag, ab 0. Nur fuer Ereignisse und Vergleiche gedacht, nie fuer eine Rechnung. */
    val day: Int,
    /** Minute seit Mitternacht, 0 bis 1439. */
    val minuteOfDay: Int,
    /** Wo der Agent gerade ist. */
    val site: LivingSite,
    /**
     * Muenzen. Absichtlich dieselbe Groessenordnung wie
     * [com.notime.glyphsim.matrix.PlayWallet] (Lohn 2, Einkauf 2), damit der spaetere Adapter
     * nicht umrechnen muss.
     */
    val coins: Int,
    /**
     * Portionen im Vorrat. Entspricht [com.notime.glyphsim.matrix.PlayPantry] (voll = 3).
     */
    val portions: Int,
    /**
     * Welche Orte gerade betretbar sind - der Laden hat nachts zu, die Arbeit auch.
     *
     * Als Menge und nicht als Oeffnungszeiten-Tabelle: Wer die Zeiten hier hineinschreibt,
     * verdoppelt eine Regel, die die Welt spaeter besser kennt als diese Domaene.
     */
    val openSites: Set<LivingSite>
) {

    fun has(requirement: Requirement): Boolean = when (requirement) {
        is Requirement.Coins -> coins >= requirement.amount
        is Requirement.Portions -> portions >= requirement.amount
        is Requirement.At -> site == requirement.site
        is Requirement.SiteOpen -> requirement.site in openSites
    }

    /**
     * Die Welt [minutes] Minuten spaeter. Ueber Mitternacht hinweg zaehlt [day] weiter - ein
     * Mehrtagestest darf nicht daran scheitern, dass jemand die Nacht vergessen hat.
     */
    fun advanced(minutes: Int): WorldState {
        val gesamt = minuteOfDay + minutes
        return copy(day = day + gesamt / MINUTES_PER_DAY, minuteOfDay = gesamt % MINUTES_PER_DAY)
    }

    /** Ein Zeitstempel, der ueber Tage hinweg vergleichbar ist. */
    val absoluteMinute: Int get() = day * MINUTES_PER_DAY + minuteOfDay

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

/**
 * Die vier Orte, die eine Entscheidung unterscheiden kann.
 *
 * [OUTSIDE] ist bewusst dabei, obwohl heute keine Handlung ihn verlangt: Ein Agent, der
 * unterwegs ist, ist weder zu Hause noch am Ziel, und ohne diesen Zustand muesste die erste
 * Handlung, die Wege wirklich abbildet, das Enum sprengen.
 */
enum class LivingSite { HOME, WORKPLACE, MARKET, OUTSIDE }

/**
 * Eine einzelne Voraussetzung - **und der Grund, warum diese Domaene erklaerbar ist.**
 *
 * Eine Handlung scheitert hier nie an einem `false`, sondern immer an einem benannten Ding. Nur
 * deshalb kann [AgentExplanation.blockedBy] die Frage "was verhindert gerade den Fortschritt?"
 * beantworten, ohne dass irgendwo ein Satz dafuer geschrieben wurde. Ein Text daraus zu machen
 * ist Sache der Anzeige, nicht dieser Schicht - Symbole und Zahlen sind sprachunabhaengig, ein
 * Satz waere es nicht.
 */
sealed interface Requirement {
    /** Mindestens so viele Muenzen. */
    data class Coins(val amount: Int) : Requirement

    /** Mindestens so viele Portionen im Vorrat. */
    data class Portions(val amount: Int) : Requirement

    /** Der Agent muss hier stehen. */
    data class At(val site: LivingSite) : Requirement

    /** Der Ort muss offen sein. Getrennt von [At], weil "zu" und "woanders" verschiedene
     * Hindernisse sind: das eine wartet man ab, das andere laeuft man weg. */
    data class SiteOpen(val site: LivingSite) : Requirement
}
