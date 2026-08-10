package com.notime.glyphsim.state

import com.notime.glyphsim.ui.AppMode

/**
 * Uebersetzt zwischen dem neuen [TamaState] und der bisherigen Drei-Modi-Sicht [AppMode].
 *
 * ## Warum es diese Bruecke gibt
 *
 * `AppMode` steht heute an vielen Stellen: in der Auswahlleiste, in der Hilfe, in den Texten. Sie
 * alle in einem Zug umzustellen waere ein grosser Eingriff ohne Gegenwert - Phase 2 soll das
 * Modell einfuehren, nicht die Oberflaeche umbauen.
 *
 * Solange beide Sichten nebeneinander bestehen, muss ihre Zuordnung **an genau einer Stelle**
 * stehen und in beide Richtungen zusammenpassen. Genau das ist diese Datei, und
 * `TamaStateMappingTest` prueft die Rundlaeufe.
 *
 * ## Die Zuordnung heute
 *
 * ```
 * AppMode.WATCH     ->  clockOnly = true   +  WorldMode.ROUTINES
 * AppMode.REMINDER  ->  clockOnly = false  +  WorldMode.ROUTINES
 * AppMode.PLAY      ->  clockOnly = false  +  WorldMode.PLAY
 * ```
 *
 * Zwei Dinge fallen daran auf, und beide sind Absicht dieses Modells:
 *
 * 1. **WATCH und REMINDER unterscheiden sich nur in der Darstellung.** Der Inhalt ist derselbe -
 *    die eigenen Routinen laufen in beiden Faellen. Genau das misst
 *    `nurUhrLaesstDieEchtenErinnerungenWeiterlaufen`.
 * 2. **Der Dock-Modus taucht in AppMode gar nicht auf.** Er laeuft ueber einen eigenen Schalter
 *    und steht quer zu den drei "Modi" - man kann im Dock-Modus sein UND auf die Uhr reduziert,
 *    beides zugleich. Der beste Beleg dafuer, dass Darstellung und Inhalt bisher vermischt waren.
 */
object TamaStateMapping {

    /**
     * Die bisherige Drei-Modi-Sicht auf einen Zustand.
     *
     * Der Dock-Modus geht dabei verloren - `AppMode` kennt ihn nicht. Deshalb ist diese Richtung
     * nur fuer die Auswahlleiste gedacht, nicht zum Speichern.
     */
    fun toAppMode(state: TamaState): AppMode = when {
        state.clockOnly -> AppMode.WATCH
        state.world == WorldMode.PLAY -> AppMode.PLAY
        else -> AppMode.REMINDER
    }

    /**
     * Was der Nutzer meint, wenn er in der Auswahlleiste einen Modus antippt.
     *
     * [dock] wird durchgereicht, weil die Auswahl daran nichts aendert: wer im Dock-Modus den
     * Inhalt umstellt, bleibt im Dock-Modus.
     */
    fun fromAppMode(mode: AppMode, dock: Boolean = false): TamaState {
        val behaelter = if (dock) Presentation.DOCK else Presentation.APP
        return when (mode) {
            AppMode.WATCH -> TamaState(behaelter, clockOnly = true, world = WorldMode.ROUTINES)
            AppMode.REMINDER -> TamaState(behaelter, clockOnly = false, world = WorldMode.ROUTINES)
            AppMode.PLAY -> TamaState(behaelter, clockOnly = false, world = WorldMode.PLAY)
        }
    }

    /**
     * Setzt den Zustand aus den drei gespeicherten Schaltern zusammen - so, wie sie heute liegen.
     *
     * Die Vorrangregel aus `AppMode.current` ("nur Uhr" gewinnt) betrifft ausschliesslich die
     * Modus-SICHT und steht deshalb in [toAppMode], nicht hier. Im Zustand selbst gibt es nichts
     * zu priorisieren: Behaelter, Reduktion und Inhalt sind drei unabhaengige Angaben.
     */
    fun fromSwitches(watchOnly: Boolean, playActive: Boolean, dockEnabled: Boolean): TamaState =
        TamaState(
            presentation = if (dockEnabled) Presentation.DOCK else Presentation.APP,
            clockOnly = watchOnly,
            world = if (playActive) WorldMode.PLAY else WorldMode.ROUTINES
        )
}
