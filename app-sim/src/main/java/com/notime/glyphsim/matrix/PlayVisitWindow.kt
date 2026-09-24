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

    /**
     * Wie lange es bis zum naechsten Besuch dauert.
     *
     * **Gemeldet als "ich habe noch nie drei oder vier zusammen gesehen".** Ein Besuch dauert
     * vom Hereinkommen bis zum Gehen etwa eine Viertelminute, der naechste kam aber erst
     * anderthalb bis dreieinhalb Minuten spaeter. Selbst wenn drei Bewohner gleichzeitig im Park
     * waren, traten sie deshalb nacheinander auf und nie miteinander - der Deckel von vier
     * gleichzeitigen Gaesten (siehe [LivingPopulationLayout.visitorCapFor]) wurde nie erreicht.
     *
     * Jetzt gilt: Steht schon ein Gast im Bild und ist noch jemand da, der kommen koennte, folgt
     * er nach [FOLLOW_UP_MS] - so entsteht ein Grueppchen statt einer Reihe.
     *
     * @param visitorsOnScreen wie viele Besuche gerade laufen.
     * @param moreCandidates ob noch ein anwesender Bewohner uebrig ist, der nicht schon zu Besuch ist.
     */
    fun intervalMs(place: PlayScene.Place, visitorsOnScreen: Int, moreCandidates: Boolean): LongRange =
        when {
            visitorsOnScreen > 0 && moreCandidates -> FOLLOW_UP_MS
            place == PlayScene.Place.STREET || place == PlayScene.Place.CITY -> BUSY_INTERVAL_MS
            else -> INTERVAL_MS
        }

    /**
     * Wie lange ein Gast nach dem Gespraech noch bleibt, bevor er geht.
     *
     * Vorher ging er sofort - wer vorbeikam, war nach zwei Wortwechseln wieder weg, und zwei
     * Gaeste standen deshalb kaum je gleichzeitig im Bild. Unter freiem Himmel bleibt er jetzt
     * eine Weile stehen (man haengt zusammen ab), drinnen nur kurz.
     */
    fun lingerMs(place: PlayScene.Place): LongRange =
        if (PlayScene.isOutdoors(place)) OUTDOOR_LINGER_MS else INDOOR_LINGER_MS

    /** Abstand zwischen zwei Besuchen. */
    val INTERVAL_MS = 90_000L..210_000L

    /**
     * Deutlich kuerzerer Abstand fuer die belebten Orte (Strasse, Stadt): Auf einem Weg begegnet
     * man einander haeufiger als beim Ausruhen im Park oder beim Einkaufen.
     */
    val BUSY_INTERVAL_MS = 30_000L..70_000L

    /** Der naechste Gast, waehrend schon jemand da ist - er kommt dazu, statt abzuloesen. */
    val FOLLOW_UP_MS = 5_000L..14_000L

    val OUTDOOR_LINGER_MS = 15_000L..30_000L
    val INDOOR_LINGER_MS = 3_000L..6_000L

    /**
     * Wer gerade NICHT zu Besuch kommen kann: wer schon im Bild ist, und wer eben erst gegangen
     * ist.
     *
     * Der zweite Teil kam mit dem schnellen Dazukommen ([FOLLOW_UP_MS]): Sind nur zwei Bewohner
     * da, haette sonst der eben Gegangene sofort wieder kehrtgemacht, sobald der andere noch im
     * Bild steht - zwei, die sich endlos abwechseln, statt eines Grueppchens.
     *
     * @param lastLeftAtMs wann jeder Bewohner zuletzt das Bild verlassen hat.
     * @param pace der Zeitraffer-Faktor (siehe [PlayTimeLapse.paceFactor]).
     */
    fun unavailableGuests(
        visiting: Collection<String>,
        lastLeftAtMs: Map<String, Long>,
        nowMs: Long,
        pace: Float
    ): Set<String> {
        val sperre = (RECENT_GUEST_MS * pace).toLong()
        return visiting.toSet() + lastLeftAtMs.filterValues { nowMs - it in 0 until sperre }.keys
    }

    /** So lange kommt ein Gast, der eben gegangen ist, nicht gleich wieder. */
    const val RECENT_GUEST_MS = 90_000L

    /**
     * Wie lange die wartende Figur draussen noch fuer Neuankoemmlinge offen bleibt, solange schon
     * Gaeste bei ihr stehen - danach geht ihr Ablauf weiter, sobald der letzte gegangen ist.
     * Ohne diese Grenze koennte eine Kette von Besuchen den Ablauf beliebig lange aufhalten.
     */
    const val GROUP_WINDOW_MS = 45_000L
}

