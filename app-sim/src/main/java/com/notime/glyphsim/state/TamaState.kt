package com.notime.glyphsim.state

/**
 * Der Zustand der App als ein unveraenderlicher Wert - **Darstellung und Inhalt getrennt.**
 *
 * ## Warum diese Trennung
 *
 * Bisher gab es drei "Modi" (`AppMode`: WATCH, REMINDER, PLAY), die nebeneinander in einer
 * Auswahlleiste standen und dadurch wie drei gleichrangige Dinge aussahen. Sie sind es nicht -
 * die Charakterisierungstests aus Phase 1a haben das nachgemessen:
 *
 * - **"Nur Uhr" aendert nur die Anzeige.** Die echten Erinnerungen bleiben eingeplant und laufen
 *   weiter; man sieht sie bloss nicht (siehe `nurUhrLaesstDieEchtenErinnerungenWeiterlaufen`).
 * - **Der Spielmodus aendert, WAS laeuft.** Er nimmt die echten Erinnerungen aus der Planung und
 *   setzt die gewuerfelte Spiel-Zeile an ihre Stelle (siehe
 *   `imSpielmodusWerdenEchteErinnerungenNichtEingeplant`).
 *
 * Zwei voellig verschiedene Arten von Entscheidung, in einer Leiste nebeneinandergestellt. Genau
 * daher ruehrt Problem 1 der Produktanalyse - drei Modi, die sich wie konkurrierende Produkte
 * verhalten.
 *
 * Dieses Modell trennt sie: [Presentation] beschreibt, **wie** Tama gerade gezeigt wird,
 * [WorldMode] beschreibt, **was** inhaltlich laeuft.
 *
 * ## Was sich damit (noch) NICHT aendert
 *
 * Nichts. Phase 2 beschreibt den heutigen Zustand nur genauer; die Zuordnung zu den beiden
 * gespeicherten Schaltern bleibt exakt dieselbe, und [AppMode] laesst sich daraus weiterhin
 * herleiten. Erst Phase 3 aendert die Zuordnung - und dann an einer Stelle statt in der ganzen App.
 */
data class TamaState(
    /** In welchem Behaelter Tama gezeigt wird. */
    val presentation: Presentation = Presentation.APP,

    /**
     * Ob auf die blosse Uhr reduziert wird.
     *
     * **Quer zur [presentation], nicht als dritter Wert daneben.** Das ist beim Verdrahten
     * aufgefallen und war der erste Entwurf dieses Modells falsch: "nur Uhr" und Dock-Modus
     * schliessen sich nicht aus. Wer das Geraet aufs Nachttischchen legt UND auf die Uhr
     * reduziert, bekommt heute den Dock-Bildschirm in Uhr-Darstellung - beides zugleich. Als
     * Alternativen modelliert waere daraus stillschweigend der Startbildschirm geworden.
     */
    val clockOnly: Boolean = false,

    /** Was inhaltlich laeuft. */
    val world: WorldMode = WorldMode.ROUTINES
) {
    /**
     * Ob der Bildschirm dauerhaft an bleiben soll.
     *
     * Haengt am Behaelter, nicht am Inhalt - im Dock-Modus liegt das Geraet auf dem Nachttisch,
     * unabhaengig davon, ob gerade eigene Routinen oder die Welt laufen und ob auf die Uhr
     * reduziert wurde.
     */
    val keepScreenOn: Boolean
        get() = presentation == Presentation.DOCK

    /**
     * Ob die Fortschrittsanzeige des Spielmodus eingeblendet wird.
     *
     * Braucht beides: den Inhalt (es gibt ueberhaupt einen Fortschritt) und eine Darstellung, die
     * Platz dafuer hat. Auf die Uhr reduziert hat sie nichts zu suchen.
     */
    val showsPlayProgress: Boolean
        get() = world == WorldMode.PLAY && !clockOnly
}

/**
 * In welchem Behaelter Tama gezeigt wird - **eine reine Anzeigefrage.**
 *
 * Keiner dieser Werte darf darueber entscheiden, ob eine Erinnerung ankommt. Das ist die Regel,
 * an der sich Phase 3 messen laesst: die Darstellung haelt sie heute bereits ein,
 * [WorldMode.PLAY] verletzt sie.
 */
enum class Presentation {
    /** Die App im Vordergrund: Wesen, Uhr, Bedienung. */
    APP,

    /** Vollbild fuers Nachttischchen - Bildschirm bleibt an. */
    DOCK
}

/**
 * Was inhaltlich laeuft - **die einzige Unterscheidung, die auf Erinnerungen wirkt.**
 */
enum class WorldMode {
    /**
     * Der Normalbetrieb: die selbst eingerichteten Routinen sind aktiv.
     */
    ROUTINES,

    /**
     * Die Welt des Wesens mit ihren eigenen, gewuerfelten Auftritten.
     *
     * **Heute schliesst das die eigenen Routinen aus** - sie werden waehrenddessen nicht
     * eingeplant. Das ist Problem 2 der Produktanalyse und der Punkt, den Phase 3 aufloest:
     * danach soll die Welt weiterlaufen, WAEHREND die Routinen aktiv bleiben, und beide sollen
     * sich gegenseitig speisen statt einander zu verdraengen.
     */
    PLAY
}
