package com.notime.glyphsim.matrix

/**
 * **WANN ueberhaupt jemand vorbeikommen darf** - die Bedingung des Besuchstakts, herausgeloest
 * aus `DockScreen`.
 *
 * ## Warum das eine eigene Datei ist
 *
 * Die Regel stand als `visitPossible()` mitten in einer Compose-Funktion und war damit nur am
 * Geraet zu beobachten: Man haette warten muessen, bis der Zufall die richtige Lage erzeugt, und
 * haette bei ausbleibendem Besuch nicht sagen koennen, ob die Regel falsch ist oder der Wuerfel.
 * Genau so ist der Fehler entstanden, den [isOpen] jetzt behebt - er stand seit dem ersten Besuch
 * im Code und ist niemandem aufgefallen, weil man ihn nur an einer *ausbleibenden* Sache haette
 * bemerken koennen.
 *
 * Hier sind es reine Wahrheitswerte. Die Wiedergabe (`runVisit`) bleibt in `DockScreen`; nur die
 * ENTSCHEIDUNG wandert heraus - dieselbe Trennung wie zwischen [MusicResolver] und `PlayMusic`.
 */
object PlayVisitWindow {

    /**
     * Ob an dieser Stelle ein Besuch stattfinden darf.
     *
     * @param place wo die Figur gerade ist - siehe [PlayScene.allowsVisitors].
     * @param routineRunning ob gerade ein Tagesablauf laeuft.
     * @param lingeringOutdoors ob die Figur dabei **unter freiem Himmel steht und wartet**
     *   (ein `RoutineStep.Linger` draussen). Die eine Ausnahme von [routineRunning].
     * @param occupied ob sie in etwas sitzt oder liegt (Bett, Sessel, Bank).
     * @param walking ob sie gerade geht.
     * @param settling ob sie sich gerade hinsetzt oder aufsteht.
     * @param hidden ob sie derzeit gar nicht im Bild ist.
     * @param userBusy ob eine echte Erinnerung offen ist oder gefuettert wird - dann gehoert die
     *   Aufmerksamkeit dem Nutzer und nicht der Kulisse.
     */
    fun isOpen(
        place: PlayScene.Place,
        routineRunning: Boolean,
        lingeringOutdoors: Boolean,
        occupied: Boolean,
        walking: Boolean,
        settling: Boolean,
        hidden: Boolean,
        userBusy: Boolean
    ): Boolean {
        if (userBusy || hidden || occupied || walking || settling) return false
        if (!PlayScene.allowsVisitors(place)) return false
        // **Die eigentliche Aenderung.** Ein laufender Ablauf schliesst einen Besuch weiterhin
        // aus - ein Gast wuerde sich sonst mit dem Ablauf um dieselbe Figur streiten, und sie
        // ginge zum Bett, waehrend sie sich unterhaelt.
        //
        // Die Ausnahme ist das Stehenbleiben DRAUSSEN. Vorher war "draussen jemanden treffen"
        // strukturell unmoeglich: Unter freien Himmel kommt die Figur ausschliesslich innerhalb
        // eines Ablaufs, und waehrend eines Ablaufs war jeder Besuch gesperrt. Uebrig blieb der
        // schmale Rest zwischen zwei Ablaeufen - und auch der nur, wenn der letzte zufaellig an
        // einem Ort endete, an dem man Leute trifft.
        //
        // Ein Linger ist dabei die einzige Stelle, an der der urspruengliche Einwand nicht
        // greift: Dort hat die Figur nichts vor ausser dazustehen. Damit sie dem Gast nicht nach
        // zwei Sekunden davonlaeuft, wartet der Ablauf anschliessend auf das Ende des Besuchs
        // (siehe `visitRunning` in DockScreen) - das ist die andere Haelfte dieser Regel und
        // steht dort, weil nur dort die Wiedergabe laeuft.
        return !routineRunning || lingeringOutdoors
    }
}
