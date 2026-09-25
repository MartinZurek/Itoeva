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

    // Dieselbe Aufhellung wie AvatarSpriteView.EYE_GLINT_FRACTION - der Clip soll zeigen, was auf
    // dem Bildschirm zu sehen war, nicht eine flachere Zweitfassung davon.
    private const val EYE_GLINT_FRACTION = 0.4f

    /** Blendet [color] Richtung Weiss - siehe [AvatarAccent.eyesIn]. */
    private fun lightened(color: Int, fraction: Float): Int = Color.rgb(
        (Color.red(color) + (255 - Color.red(color)) * fraction).roundToInt(),
        (Color.green(color) + (255 - Color.green(color)) * fraction).roundToInt(),
        (Color.blue(color) + (255 - Color.blue(color)) * fraction).roundToInt()
    )

    /** Eine kleinere Hintergrundfigur; Lage und Breite gelten relativ zum ganzen Bild. */
    class ResidentFigure(
        val frame: IntArray,
        val species: AvatarSpecies,
        val leftFraction: Float,
        val widthFraction: Float
    )

    /** Ein eigenstaendiger, volle-Groesse Besucher - mehrere gleichzeitig moeglich. */
    class VisitorFrame(
        val frame: IntArray,
        val species: AvatarSpecies,
        /** Beim Laufen die Seite, von der die Kreatur kommt - siehe [Frame.shadeSide]. */
        val shadeSide: AvatarShading.Side = AvatarShading.Side.NONE,
        /** Waagerechte Lage als Bruchteil der Breite. */
        val anchorX: Float = 0f
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
        /**
         * Beschattete Flanke (siehe [AvatarShading.Side]) - beim Laufen die Seite, von der die
         * Kreatur kommt. Muss mit in den Film, sonst laeuft sie in der Aufnahme anders herum
         * als auf dem Bildschirm.
         */
        val shadeSide: AvatarShading.Side = AvatarShading.Side.NONE,
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
        /** Eigenstaendige Gaeste, falls gerade welche durchs Bild gehen - mehrere moeglich. */
        val visitors: List<VisitorFrame> = emptyList(),
        /** Tatsaechlich anwesende Einwohner hinter Hauptfigur und aktiven Gaesten. */
        val residents: List<ResidentFigure> = emptyList()
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

        // Dieselbe Schattierung wie auf dem Bildschirm (siehe AvatarShading und
        // AvatarSpriteView). Ohne diese Zeile saehe eine exportierte Aufnahme flacher aus als
        // das, was man beim Aufnehmen gesehen hat - und das faellt erst auf, wenn man sie
        // jemandem zeigt.
        //
        // Dasselbe gilt fuer die FARBE (siehe AvatarPalette): Eine Aufnahme, in der die Kreatur
        // weiss ist, zeigt nicht die Kreatur, die man gerade begleitet hat. Die Kulisse bleibt
        // dagegen weiss - genau wie auf dem Bildschirm.
        // Koerper weiss, Farbe im Gesicht - wie auf dem Bildschirm (siehe AvatarAccent).
        val avatarAccent = AvatarPalette.tintFor(frame.species)
        val avatarEyeAccent = lightened(avatarAccent, EYE_GLINT_FRACTION)
        // In Laufrichtung gedreht wie auf dem Bildschirm (siehe AvatarFacing).
        val avatarOriented = AvatarFacing.orient(frame.avatarFrame, frame.shadeSide)
        val avatarFace = AvatarAccent.facesIn(avatarOriented)
        val avatarEyes = AvatarAccent.eyesIn(avatarOriented)
        val avatarFrame = AvatarShading.shade(avatarOriented, side = frame.shadeSide)
        for (y in 0 until AvatarGeometry.HEIGHT) {
            for (x in 0 until AvatarGeometry.SIZE) {
                val index = y * AvatarGeometry.SIZE + x
                val imGesicht = avatarFace.getOrElse(index) { false }
                val brightness = avatarFrame.getOrElse(index) { 0 }
                if (brightness <= 0 && !imGesicht) continue
                val ton = if (avatarEyes.getOrElse(index) { false }) {
                    avatarEyeAccent
                } else if (imGesicht) {
                    avatarAccent
                } else {
                    ledOn
                }
                val f = (if (imGesicht) AvatarGeometry.MAX_BRIGHTNESS else brightness)
                    .coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                    AvatarGeometry.MAX_BRIGHTNESS
                paint.color = Color.rgb(
                    (Color.red(ton) * f).roundToInt(),
                    (Color.green(ton) * f).roundToInt(),
                    (Color.blue(ton) * f).roundToInt()
                )
                val left = (originCellX + x) * cell
                val top = (originCellY + y) * cell
                canvas.drawRect(left, top, left + cell + overlap, top + cell + overlap, paint)
            }
        }

        // Ruhige Hintergrundwesen. Ihre kleinere Zellgroesse ist die Lesbarkeitsentscheidung
        // aus NT-089: Auf der normalen Breite von vierzig Szenenzellen passen mehrere
        // vollgrosse 16-Zellen-Figuren nicht neben den Hauptavatar. Sie stehen auf derselben
        // Bodenlinie und bleiben gedaempft, damit die begleitete Figur Mittelpunkt bleibt.
        for (resident in frame.residents) {
            val residentCell = widthPx * resident.widthFraction / AvatarGeometry.SIZE
            if (residentCell <= 0f) continue
            val residentX = widthPx * resident.leftFraction
            val residentGroundY = floorY * cell
            val residentY = residentGroundY -
                (AvatarBodies.forSpecies(resident.species).groundRow() + 1) * residentCell
            val accent = AvatarPalette.tintFor(resident.species)
            val eyeAccent = lightened(accent, EYE_GLINT_FRACTION)
            val face = AvatarAccent.facesIn(resident.frame)
            val eyes = AvatarAccent.eyesIn(resident.frame)
            for (y in 0 until AvatarGeometry.HEIGHT) {
                for (x in 0 until AvatarGeometry.SIZE) {
                    val index = y * AvatarGeometry.SIZE + x
                    val imGesicht = face.getOrElse(index) { false }
                    val brightness = resident.frame.getOrElse(index) { 0 }
                    if (brightness <= 0 && !imGesicht) continue
                    val ton = if (eyes.getOrElse(index) { false }) {
                        eyeAccent
                    } else if (imGesicht) {
                        accent
                    } else {
                        ledOn
                    }
                    val f = ((if (imGesicht) AvatarGeometry.MAX_BRIGHTNESS else brightness)
                        .coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                        AvatarGeometry.MAX_BRIGHTNESS) * RESIDENT_DIM
                    paint.color = Color.rgb(
                        (Color.red(ton) * f).roundToInt(),
                        (Color.green(ton) * f).roundToInt(),
                        (Color.blue(ton) * f).roundToInt()
                    )
                    val left = residentX + x * residentCell
                    val top = residentY + y * residentCell
                    canvas.drawRect(
                        left,
                        top,
                        left + residentCell + residentCell * 0.04f,
                        top + residentCell + residentCell * 0.04f,
                        paint
                    )
                }
            }
        }

        // Eigenstaendige Gaeste, falls gerade welche vorbeikommen - gedaempft wie auf dem
        // Bildschirm, mehrere gleichzeitig moeglich (siehe LivingPopulationLayout.visitorCapFor).
        for (guest in frame.visitors) {
            val gx = ((widthCells - AvatarGeometry.SIZE) * guest.anchorX).roundToInt()
            val gy = (floorY - 1) - AvatarBodies.forSpecies(guest.species).groundRow()
            // Der Gast bekommt dieselbe Woelbung; seine Daempfung kommt zusaetzlich obendrauf,
            // weil AvatarShading skaliert statt zu ersetzen.
            val gastAccent = AvatarPalette.tintFor(guest.species)
            val gastEyeAccent = lightened(gastAccent, EYE_GLINT_FRACTION)
            val gastOriented = AvatarFacing.orient(guest.frame, guest.shadeSide)
            val gastFace = AvatarAccent.facesIn(gastOriented)
            val gastEyes = AvatarAccent.eyesIn(gastOriented)
            val gastFrame = AvatarShading.shade(gastOriented, side = guest.shadeSide)
            for (y in 0 until AvatarGeometry.HEIGHT) {
                for (x in 0 until AvatarGeometry.SIZE) {
                    val index = y * AvatarGeometry.SIZE + x
                    val imGesicht = gastFace.getOrElse(index) { false }
                    val b = gastFrame.getOrElse(index) { 0 }
                    if (b <= 0 && !imGesicht) continue
                    val ton = if (gastEyes.getOrElse(index) { false }) {
                        gastEyeAccent
                    } else if (imGesicht) {
                        gastAccent
                    } else {
                        ledOn
                    }
                    val f = ((if (imGesicht) AvatarGeometry.MAX_BRIGHTNESS else b)
                        .coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                        AvatarGeometry.MAX_BRIGHTNESS) * VISITOR_DIM
                    paint.color = Color.rgb(
                        (Color.red(ton) * f).roundToInt(),
                        (Color.green(ton) * f).roundToInt(),
                        (Color.blue(ton) * f).roundToInt()
                    )
                    val left = (gx + x) * cell
                    val top = (gy + y) * cell
                    canvas.drawRect(left, top, left + cell + overlap, top + cell + overlap, paint)
                }
            }
        }

        // Getragener Gegenstand, an derselben Stelle wie auf dem Bildschirm.
        frame.carried?.let { item ->
            drawCells(
                PlayEffects.carriedCells(
                    item,
                    originCellX,
                    originCellY,
                    gaitPhase = frame.scenePhase,
                    moving = frame.shadeSide != AvatarShading.Side.NONE
                )
            )
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

    /** Hintergrundwesen stehen noch eine Ebene hinter dem aktiven Gast. */
    private const val RESIDENT_DIM = 0.66f
}
