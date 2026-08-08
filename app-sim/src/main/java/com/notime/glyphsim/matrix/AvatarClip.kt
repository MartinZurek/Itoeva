package com.notime.glyphsim.matrix

import androidx.annotation.StringRes

/**
 * Ein kurzer, stummer "Vorstellungs-Clip" fuer einen Avatar (siehe
 * [com.notime.glyphsim.ui.AvatarClips] fuer die Zuordnung je Spezies, [com.notime.glyphsim.ui.AvatarClipPlayer]
 * fuer die Wiedergabe). Ein Clip ist nichts als eine geordnete Liste von [ClipBeat]s, die
 * nacheinander abgespielt werden - dieselbe Idee wie ein Storyboard: jeder Beat traegt seine
 * eigene Animation, Dauer, optionale Kamera-Bewegung und optionale Texteinblendung.
 *
 * Bewusst reine Daten ohne jede Wiedergabe-Logik (siehe [ClipBeat]) - die Architektur soll sich
 * beliebig erweitern lassen, ohne dass fuer einen neuen Avatar-Clip neuer PLAYER-Code noetig
 * waere, nur neue Beat-Listen.
 */
data class AvatarClip(val beats: List<ClipBeat>) {
    /** Gesamtlaenge - fuer Vorschauen/Debugging, der Player selbst braucht sie nicht. */
    val totalDurationMs: Long get() = beats.sumOf { it.effectiveDurationMs }
}

/**
 * Ein einzelner Abschnitt eines [AvatarClip].
 *
 * [frames]/[frameHoldsMs] sind bewusst KEINE neue Animationsform, sondern dieselbe
 * Frame/Hold-Paarung wie [AvatarAnimations.AvatarSequence] ueberall sonst in der App (Idle,
 * Reaktionen, Antipp-Freude) - ein Beat spielt also immer eine BEREITS VORHANDENE Animation,
 * ueber [durationMs] in der Laenge angepasst (kuerzere Animationen loopen, laengere werden
 * abgeschnitten - siehe Player). Dadurch braucht die allererste Umsetzung eines Clips keine
 * einzige neue Pixel-Animation.
 *
 * [camera] und [text] sind rein praesentational und wirken nur auf Compose-Ebene (Zoom/Pan des
 * gezeichneten Sprites, Texteinblendung) - sie veraendern nie die Pixel-Frames selbst.
 */
data class ClipBeat(
    val frames: List<IntArray>,
    val frameHoldsMs: List<Long>,
    val durationMs: Long,
    val camera: CameraMove = CameraMove.STATIC,
    /**
     * Laesst den Beat notfalls laenger stehen als [durationMs], damit die Animation **mindestens
     * einmal ganz** durchlaeuft (siehe [effectiveDurationMs]).
     *
     * Der Player schneidet sonst hart bei [durationMs] ab. Bei einer Ruhe-Schleife ist das
     * richtig - sie hat keinen Schluss, sie laeuft eben. Bei einer Reaktion ist es ein Fehler:
     * Der Avatar setzte zum Hopser an und war weg, bevor er wieder aufkam; die Pointe der
     * Choreografie - der Nachklang am Ende, siehe `AvatarAnimations.SETTLE_MS` - fiel regelmaessig
     * dem Schnitt zum Opfer. Genau das war im Clip zu sehen.
     */
    val playToEnd: Boolean = false,
    @StringRes val textRes: Int? = null,
    /** Die Uhr als zweites sichtbares Element dieses Beats - null heisst "nicht zu sehen". */
    val clock: ClockCue? = null,
    /**
     * Frame-synchrone Bildschirm-Verschiebung des AVATARS (Bruchteil der Bildschirmgroesse je
     * Frame, 1:1 dieselbe Zaehlung wie [frames]) - fuer Reaktionen, die ihr eigenes Raster
     * verlassen (aktuell nur "Rocket", siehe [AvatarAnimations.flightOffsetsFor]). `null` heisst
     * "bleibt an Ort und Stelle" (nur [camera] bewegt sich dann). Bewusst getrennt von [camera]:
     * Kamera faehrt gleichmaessig ueber die Beat-Dauer, ein Flug-Offset dagegen folgt exakt dem
     * Bewegungsmuster DIESER EINEN Reaktion, Frame fuer Frame - beides gleichzeitig zu ueberlagern
     * (statt eins von beiden zu waehlen) waere fuer den Fall nicht mehr vorhersagbar.
     */
    val flightOffsets: List<Pair<Float, Float>>? = null
) {
    /**
     * Wie lange dieser Beat tatsaechlich laeuft. Ohne [playToEnd] genau [durationMs]; mit
     * [playToEnd] mindestens so lange, wie die Animation fuer einen vollen Durchlauf braucht.
     */
    val effectiveDurationMs: Long
        get() = if (playToEnd) maxOf(durationMs, frameHoldsMs.sum()) else durationMs
}

/**
 * Die Uhr innerhalb eines Beats: entweder eine feste, bereits vorhandene Frame-Sequenz (z.B. eine
 * echte [com.notime.glyphsim.matrix.ReminderAnimations]-Sequenz, siehe [ClockCue.Animation]) oder
 * die tatsaechliche, aktuelle Uhrzeit (siehe [ClockCue.LiveTime]) - plus optionale Bewegung ueber
 * die Beat-Dauer (siehe [ClockMotion]).
 */
sealed interface ClockCue {
    val motion: ClockMotion

    /** Eine feste, bereits vorhandene Frame-Sequenz (z.B. eine Erinnerungsanimation). */
    data class Animation(
        val frames: List<IntArray>,
        val frameHoldsMs: List<Long>,
        override val motion: ClockMotion = ClockMotion.Still()
    ) : ClockCue

    /**
     * Zeigt die ECHTE, aktuelle Uhrzeit im vom Nutzer gewaehlten
     * [com.notime.glyphsim.matrix.ClockStyle] - fuer den Clip-Moment, der die tatsaechliche
     * Alltagsfunktion vorfuehrt, statt einer eigens gebauten Attrappe. Der Player
     * ([com.notime.glyphsim.ui.AvatarClipPlayer]) loest das erst zur Wiedergabezeit auf (braucht
     * dafuer einen Context), [com.notime.glyphsim.ui.AvatarClips] bleibt dadurch weiterhin reine,
     * Context-freie Daten.
     */
    data class LiveTime(override val motion: ClockMotion = ClockMotion.Still()) : ClockCue
}

/**
 * Ruhewinkel der Uhr: rechts oben ueber dem Avatar. 0 Grad ist rechts, 90 Grad oben (der Player
 * spiegelt das auf die nach unten wachsende Bildschirm-Y-Achse).
 *
 * Deutlich steiler als die frueheren gut 40 Grad, und das aus einem messbaren Grund: der Abstand
 * zum Avatar ist fest (siehe [ClockMotion]), also entscheidet allein der Winkel, wie viel davon
 * auf die Waagerechte faellt - und in der Breite ist der Bildschirm knapp, in der Hoehe reichlich.
 * Bei 62 Grad braucht die Uhr nur rund 90dp zur Seite und passt damit auch auf schmale Geraete.
 */
const val CLOCK_REST_ANGLE_DEG = 62f

/**
 * Wo die Uhr relativ zum Avatar steht - als WINKEL, nicht als Position.
 *
 * Den Abstand bestimmt der Player (siehe AvatarClipPlayer.clockDistance) aus den tatsaechlichen
 * Groessen von Avatar und Uhr, damit beide sich nicht beruehren koennen. Das ist der Kern der
 * Umstellung: frueher standen hier Bruchteile der BILDSCHIRMBREITE, waehrend Avatar und Uhr in dp
 * gemessen werden. Auf einem schmaleren Geraet rueckte die Uhr damit naeher an einen gleich
 * grossen Avatar - ob sie ihn ueberlappte, hing also am Geraet. Ein Winkel hat dieses Problem
 * nicht.
 */
sealed interface ClockMotion {
    /** Steht fest auf [angleDeg]. */
    data class Still(val angleDeg: Float = CLOCK_REST_ANGLE_DEG) : ClockMotion

    /**
     * Wandert auf einem Bogen um den Avatar, von [fromAngleDeg] nach [toAngleDeg] - standardmaessig
     * von der Ruheposition rechts oben ueber den Kopf hinweg auf die spiegelbildliche Stelle links
     * oben.
     *
     * **Bewusst kein voller Kreis mehr.** Die Vorgaengerbewegung umkreiste den Avatar einmal ganz
     * und zog sich dabei "auf Radius 0" zusammen - sie endete also exakt auf ihm. Auch ohne dieses
     * Zusammenziehen geht ein Vollkreis nicht auf: kollisionsfrei braeuchte er den vollen
     * Sicherheitsabstand auch WAAGERECHT, und dort ist der Bildschirm rund ein Drittel schmaler
     * als noetig - er ginge sich nur mit einer so kleinen Uhr aus, dass die Erinnerungsanimation
     * darin nicht mehr zu erkennen waere. Der Bogen ueber den Kopf nutzt stattdessen die Hoehe,
     * in der reichlich Platz ist.
     *
     * Inhaltlich verliert der Clip dadurch nichts: dass die Erinnerung "ankommt", tragen der Bogen
     * und das anschliessende Ausblenden (siehe AvatarClipPlayer, clockAlpha) - so, wie die Uhr
     * auch im echten Dock verschwindet, sobald die Erinnerung angenommen ist.
     */
    data class Arc(
        val fromAngleDeg: Float = CLOCK_REST_ANGLE_DEG,
        val toAngleDeg: Float = 180f - CLOCK_REST_ANGLE_DEG
    ) : ClockMotion

    /**
     * **Die Zustellung:** Die Uhr faehrt von [fromAngleDeg] aus geradewegs auf den Avatar zu, bis
     * sie ihn erreicht - der Abstand geht dabei auf null, die Uhr schrumpft und blendet aus. Genau
     * das, was der Nutzer im echten Betrieb selbst tut, wenn er die Uhr auf den Avatar zieht
     * (siehe `AvatarFeeding`).
     *
     * **Warum zusaetzlich zu [Arc].** Der Bogen zieht ueber den Kopf hinweg und *deutet* die
     * Zustellung nur an; die Uhr trifft den Avatar nie, sie verschwindet daneben. Im Clip las sich
     * das als "Uhr geht vorbei", nicht als "Erinnerung kommt an" - und damit fehlte dem Clip
     * ausgerechnet die eine Geste, um die sich die ganze App dreht. Ein Vollkreis scheiterte
     * seinerzeit an der Bildschirmbreite ([Arc]-Doku); ein direkter Anflug braucht diesen Platz
     * gar nicht, er fuehrt ja nach innen.
     *
     * Der Player beschleunigt die Bewegung bewusst (statt sie gleichmaessig laufen zu lassen):
     * Die Uhr loest sich langsam, wird schneller und geht im Avatar auf.
     */
    data class Deliver(val fromAngleDeg: Float = CLOCK_REST_ANGLE_DEG) : ClockMotion
}

/**
 * Zoom/Pan der Kamera waehrend eines Beats, ueber dessen ganze [ClipBeat.durationMs] hinweg
 * linear von *from* nach *to* animiert (siehe Player). Wirkt als `graphicsLayer`-Transformation
 * auf das gezeichnete Sprite, nicht auf die Frames selbst - ein Zoom auf 1.6f zeigt also
 * weiterhin dieselben Pixel, nur groesser und ggf. verschoben.
 *
 * [offsetFraction] ist ein Bruchteil der Sprite-Groesse (nicht Pixel), damit dieselbe Kamerafahrt
 * unabhaengig von der tatsaechlichen Bildschirmgroesse funktioniert.
 */
data class CameraMove(
    val fromScale: Float = 1f,
    val toScale: Float = 1f,
    val fromOffsetFraction: Pair<Float, Float> = 0f to 0f,
    val toOffsetFraction: Pair<Float, Float> = 0f to 0f
) {
    companion object {
        /** Kein Zoom, keine Verschiebung - fuer Beats, die reine Ruhe brauchen. */
        val STATIC = CameraMove()

        // Die Standardwerte sind bewusst flach: ein Zoom soll spuerbar sein, nicht dominant -
        // bei zu starker Vergroesserung werden aus den Pixeln der Figuren Kacheln (siehe
        // AvatarClips.CAMERA_CLOSE). Alle Clips geben ohnehin beide Werte selbst an.

        /** Sanfter Heranzoom - typischer Clip-Einstieg. */
        fun zoomIn(from: Float = 1f, to: Float = 1.1f) = CameraMove(fromScale = from, toScale = to)

        /** Sanfter Rauszoom - typischer Clip-Ausstieg. */
        fun zoomOut(from: Float = 1.1f, to: Float = 1f) = CameraMove(fromScale = from, toScale = to)
    }
}
