package com.notime.glyphsim.matrix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.roundToInt

/**
 * Zeichnet die Lebenswelt in eine [Bitmap] - die Grundlage fuers Aufnehmen kleiner Filme
 * (siehe PlayClipRecorder).
 *
 * **Warum neu gezeichnet und nicht der Bildschirm abgefilmt wird.** Der naheliegende Weg waere
 * eine Bildschirmaufnahme. Die braeuchte aber bei jedem Mal eine Systemabfrage, nimmt Statusleiste
 * und Uhrzeit mit auf, liefert das Seitenverhaeltnis des Geraets statt des gewuenschten, und ihre
 * Qualitaet haengt an der Bildschirmaufloesung.
 *
 * Diese Welt besteht aber ohnehin nur aus Zellen und Helligkeiten - sie laesst sich in JEDER
 * Groesse und JEDEM Seitenverhaeltnis neu zeichnen, unabhaengig davon, was das Geraet gerade
 * anzeigt. Ein Hochformat-Film in 1080x1920 entsteht damit auf einem alten Telefon genauso sauber
 * wie auf einem neuen, ohne Berechtigung und ohne dass die Bedienoberflaeche mit im Bild ist. Und
 * weil das Zeichnen nicht an die Bildwiederholrate gebunden ist, kann ein Film in Sekunden
 * entstehen statt in Echtzeit.
 */
object PlayClipRenderer {

    /** Hochformat wie fuer soziale Netzwerke ueblich. */
    const val DEFAULT_WIDTH = 1080
    const val DEFAULT_HEIGHT = 1920

    private val ledOn = Color.rgb(
        android.graphics.Color.red(MatrixColors.LED_ON),
        android.graphics.Color.green(MatrixColors.LED_ON),
        android.graphics.Color.blue(MatrixColors.LED_ON)
    )

    /**
     * Alles, was ein einzelnes Bild ausmacht. Bewusst als Datenklasse: Der Aufnehmer erzeugt
     * daraus eine Folge, ohne dass diese Datei etwas ueber Abläufe wissen muesste.
     */
    data class Frame(
        val place: PlayScene.Place,
        val species: AvatarSpecies,
        val dayPhase: PlayAmbientActivity.DayPhase,
        val avatarFrame: IntArray,
        /** Waagerechte Lage der Figur als Bruchteil der Breite. */
        val avatarAnchorX: Float,
        val scenePhase: Int,
        val station: PlayScene.Station? = null,
        val lampOn: Boolean = true,
        val tvOn: Boolean = true,
        /**
         * Die UHR - Bild, Lage und Groesse, alles als Bruchteil der Bildflaeche.
         *
         * Sie hat im ersten Entwurf gefehlt, und das war kein Schoenheitsfehler: Die Uhr ist der
         * Gegenstand, um den sich diese App dreht, sie ist frei verschiebbar, sie wird zum
         * Fuettern benutzt und nachts im Park zum Mond. Ein Film ohne sie zeigt nicht, was auf
         * dem Bildschirm zu sehen war.
         *
         * Als Bruchteile und nicht in Pixeln, weil der Film in einer anderen Aufloesung entsteht
         * als der Bildschirm, auf dem aufgenommen wurde.
         */
        val clockFrame: IntArray? = null,
        val clockLeftFraction: Float = 0f,
        val clockTopFraction: Float = 0f,
        val clockSizeFraction: Float = 0.45f,
        /** Getragener Gegenstand, damit auch im Film zu sehen ist, was die Figur in der Hand hat. */
        val carried: PlayEffects.Carried? = null,
        /** Ein Gast, falls gerade einer durchs Bild geht. */
        val visitorFrame: IntArray? = null,
        val visitorSpecies: AvatarSpecies? = null,
        val visitorAnchorX: Float = 0f
    ) {
        // IntArray hat keine sinnvolle Gleichheit - fuer eine Datenklasse mit Array-Feld muss das
        // von Hand kommen, sonst warnt der Compiler zu Recht.
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    /**
     * Zeichnet ein Bild. [widthPx]/[heightPx] bestimmen Aufloesung und Seitenverhaeltnis; die
     * Zellgroesse ergibt sich daraus, damit dieselbe Szene in jeder Groesse gleich aufgebaut ist.
     */
    fun render(frame: Frame, widthPx: Int = DEFAULT_WIDTH, heightPx: Int = DEFAULT_HEIGHT): Bitmap =
        renderInto(frame, Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888))

    /**
     * Zeichnet in eine BESTEHENDE Bitmap.
     *
     * **Das ist keine Feinheit, sondern der Unterschied zwischen lauffaehig und nicht.** Ein Bild
     * in 1080x1920 belegt knapp acht Megabyte; ein Film von sechs Sekunden hat neunzig davon. Sie
     * alle gleichzeitig zu halten braeuchte ueber siebenhundert Megabyte und wuerde auf den
     * meisten Geraeten am Speicher scheitern. Wird stattdessen EINE Bitmap immer wieder
     * ueberschrieben und sofort weiterkodiert, bleibt es bei acht Megabyte, ganz gleich wie lang
     * der Film wird.
     */
    fun renderInto(frame: Frame, bitmap: Bitmap): Bitmap {
        val widthPx = bitmap.width
        val heightPx = bitmap.height
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val avatarPx = widthPx * AVATAR_WIDTH_FRACTION
        val cell = avatarPx / AvatarGeometry.SIZE
        if (cell <= 0f) return bitmap
        val widthCells = (widthPx / cell).toInt()
        val floorFraction = PlayScene.floorFraction(frame.place, frame.dayPhase)
        val floorY = (heightPx * floorFraction / cell).toInt()

        val paint = Paint()
        // Winziger Ueberlapp wie in den Bildschirm-Ansichten, sonst zeichnet das Glaetten
        // haarfeine Luecken zwischen benachbarten Zellen.
        val overlap = cell * 0.04f

        fun drawCells(cells: List<SceneCell>) {
            for (c in cells) {
                if (c.brightness <= 0) continue
                val f = c.brightness.coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                    AvatarGeometry.MAX_BRIGHTNESS
                paint.color = Color.rgb(
                    (Color.red(ledOn) * f).roundToInt(),
                    (Color.green(ledOn) * f).roundToInt(),
                    (Color.blue(ledOn) * f).roundToInt()
                )
                val left = c.x * cell
                val top = c.y * cell
                canvas.drawRect(left, top, left + cell + overlap, top + cell + overlap, paint)
            }
        }

        drawCells(
            PlayScene.build(
                place = frame.place,
                phase = frame.scenePhase,
                widthCells = widthCells,
                floorY = floorY,
                dayPhase = frame.dayPhase,
                fade = 1f,
                lampOn = frame.lampOn,
                tvOn = frame.tvOn,
                activeStation = frame.station,
                species = frame.species
            )
        )

        // Figur: entweder an ihrem Platz an einer Requisite oder frei auf dem Boden.
        val spot = frame.station?.let {
            PlayScene.stationSpot(frame.place, it, widthCells, floorY, frame.species)
        }
        val groundRow = AvatarBodies.forSpecies(frame.species).groundRow()
        val originCellX = spot?.let { it.centerX - AvatarGeometry.SIZE / 2 }
            ?: ((widthCells - AvatarGeometry.SIZE) * frame.avatarAnchorX).roundToInt()
        val originCellY = (spot?.groundY ?: (floorY - 1)) - groundRow

        for (y in 0 until AvatarGeometry.HEIGHT) {
            for (x in 0 until AvatarGeometry.SIZE) {
                val brightness = frame.avatarFrame.getOrElse(y * AvatarGeometry.SIZE + x) { 0 }
                if (brightness <= 0) continue
                val f = brightness.coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                    AvatarGeometry.MAX_BRIGHTNESS
                paint.color = Color.rgb(
                    (Color.red(ledOn) * f).roundToInt(),
                    (Color.green(ledOn) * f).roundToInt(),
                    (Color.blue(ledOn) * f).roundToInt()
                )
                val left = (originCellX + x) * cell
                val top = (originCellY + y) * cell
                canvas.drawRect(left, top, left + cell + overlap, top + cell + overlap, paint)
            }
        }

        // Gast, falls einer vorbeikommt - gedaempft wie auf dem Bildschirm.
        val guestFrame = frame.visitorFrame
        val guestSpecies = frame.visitorSpecies
        if (guestFrame != null && guestSpecies != null) {
            val gx = ((widthCells - AvatarGeometry.SIZE) * frame.visitorAnchorX).roundToInt()
            val gy = (floorY - 1) - AvatarBodies.forSpecies(guestSpecies).groundRow()
            for (y in 0 until AvatarGeometry.HEIGHT) {
                for (x in 0 until AvatarGeometry.SIZE) {
                    val b = guestFrame.getOrElse(y * AvatarGeometry.SIZE + x) { 0 }
                    if (b <= 0) continue
                    val f = (b.coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                        AvatarGeometry.MAX_BRIGHTNESS) * VISITOR_DIM
                    paint.color = Color.rgb(
                        (Color.red(ledOn) * f).roundToInt(),
                        (Color.green(ledOn) * f).roundToInt(),
                        (Color.blue(ledOn) * f).roundToInt()
                    )
                    val left = (gx + x) * cell
                    val top = (gy + y) * cell
                    canvas.drawRect(left, top, left + cell + overlap, top + cell + overlap, paint)
                }
            }
        }

        // Getragener Gegenstand, an derselben Stelle wie auf dem Bildschirm.
        frame.carried?.let { item ->
            drawCells(PlayEffects.carriedCells(item, originCellX, originCellY))
        }

        // Vordere Ebene zuletzt - sie liegt auch im Bild vor der Figur.
        frame.station?.let { station ->
            drawCells(
                PlayScene.buildFront(
                    frame.place, station, widthCells, floorY, frame.dayPhase, 1f, frame.species
                )
            )
        }

        // Die UHR ganz zuletzt und ueber allem - so liegt sie auch auf dem Bildschirm.
        // Gezeichnet mit demselben Puck-Aussehen wie das Widget (siehe MatrixBitmapRenderer),
        // damit Film und Bildschirm dieselbe Uhr zeigen.
        frame.clockFrame?.let { clock ->
            val clockPx = (widthPx * frame.clockSizeFraction).toInt().coerceAtLeast(1)
            val puck = MatrixBitmapRenderer.render(clock, clockPx)
            canvas.drawBitmap(
                puck,
                widthPx * frame.clockLeftFraction,
                heightPx * frame.clockTopFraction,
                null
            )
            puck.recycle()
        }

        return bitmap
    }

    /**
     * Wie breit die Figur im Verhaeltnis zum Bild ist.
     *
     * 0,30 ergibt bei 1080 Pixeln Breite rund 20 Pixel je Zelle - grob genug, dass die einzelnen
     * Zellen als Pixelgrafik erkennbar bleiben (darum geht es ja), fein genug, dass die Welt
     * daneben Platz hat.
     */
    private const val AVATAR_WIDTH_FRACTION = 0.30f

    /** Wie im Dock: Der Gast wird zurueckgenommen, damit der eigene Avatar die hellste Figur bleibt. */
    private const val VISITOR_DIM = 0.62f
}
