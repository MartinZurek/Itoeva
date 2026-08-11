package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Geometrie-Pruefungen fuer die Kulisse ([PlayScene]) - reine Rechnung, laeuft ohne Geraet.
 *
 * Der Grund fuer diese Tests: Die Requisiten werden zur Laufzeit an einem Bruchteil der
 * Bildschirmbreite verankert und ueber ihre eigene Hoehe auf den Boden gerechnet. Ein
 * Vorzeichen- oder Off-by-one-Fehler faellt dabei nicht durch einen Absturz auf, sondern durch
 * ein Bett, das einen Pixel ueber dem Boden schwebt oder halb im Boden steckt - und das saehe
 * man erst am Geraet, bei einer bestimmten Bildschirmgroesse, mit etwas Glueck.
 */
class PlaySceneTest {

    /** Immer bei klarem Wetter - sonst haengt das Ergebnis am Kalender, siehe SceneCompositionTest. */
    @Before
    fun clearSky() {
        PlayWeather.forceForPreview(PlayWeather.CLEAR)
    }

    @After
    fun restoreSky() {
        PlayWeather.forceForPreview(null)
    }

    // Typische Werte: 16 Zellen Avatarhoehe bei ~7,4dp Zelle, Bildschirm 1080x2400.
    private val width = 54
    private val floorY = 86

    private fun build(
        place: PlayScene.Place,
        phase: Int = 0,
        dayPhase: PlayAmbientActivity.DayPhase = PlayAmbientActivity.DayPhase.MIDDAY,
        widthCells: Int = width,
        floor: Int = floorY
    ) = PlayScene.build(place, phase, widthCells, floor, dayPhase)

    // ---- Ort folgt der Taetigkeit ----

    @Test
    fun `jede Taetigkeit hat einen Ort`() {
        // Auch MEDICINE, das autonom nie vorkommt, aber ueber eine echte Erinnerung schon.
        for (topic in AnimationType.entries) {
            PlayScene.forTopic(topic)
        }
        assertEquals(PlayScene.Place.BEDROOM, PlayScene.forTopic(AnimationType.SLEEP))
        // Arbeiten faengt auf der STRASSE an - der Arbeitsweg gehoert dazu, und der Ablauf geht
        // von dort in den Arbeitsraum hinein (siehe PlayRoutines.allFor(WORK)).
        assertEquals(PlayScene.Place.STREET, PlayScene.forTopic(AnimationType.WORK))
        assertEquals(PlayScene.Place.DESK, PlayScene.forTopic(AnimationType.FOCUS))
        assertEquals(PlayScene.Place.BATH, PlayScene.forTopic(AnimationType.MEDICINE))
        assertEquals(PlayScene.Place.KITCHEN, PlayScene.forTopic(AnimationType.DRINK))
        assertEquals(PlayScene.Place.PARK, PlayScene.forTopic(AnimationType.MOVE))
        assertEquals(PlayScene.Place.LIVING, PlayScene.forTopic(AnimationType.REST))
        assertEquals(PlayScene.Place.NOOK, PlayScene.forTopic(AnimationType.BOOK))
        // Etwas machen hat einen eigenen Ort - die Leseecke war der falsche: Dort gab es nichts,
        // womit sich haette umgehen lassen.
        assertEquals(PlayScene.Place.CRAFT, PlayScene.forTopic(AnimationType.CREATIVITY))
        // Ohne Thema (z.B. eine Bibliotheks-Animation) darf es keinen Absturz geben.
        assertEquals(PlayScene.Place.LIVING, PlayScene.forTopic(null))
    }

    // ---- Aufbau der Szene ----

    @Test
    fun `jeder Ort zeichnet etwas`() {
        for (place in PlayScene.Place.entries) {
            assertTrue("$place liefert eine leere Kulisse", build(place).isNotEmpty())
        }
    }

    @Test
    fun `der Boden spannt sich ueber die volle Breite`() {
        for (place in PlayScene.Place.entries) {
            val floorCells = build(place).filter { it.y == floorY }.map { it.x }.toSet()
            assertEquals("$place hat einen lueckenhaften Boden", width, floorCells.size)
        }
    }

    @Test
    fun `nichts steht im Boden oder schwebt darueber`() {
        // Alles, was auf dem Boden steht, muss mit seiner untersten Zeile GENAU eine Zelle
        // ueber der Bodenlinie enden. Ein Moebelstueck, das die Bodenlinie ueberschreitet,
        // saehe aus, als versaenke es.
        for (place in PlayScene.Place.entries) {
            val aboveFloor = build(place).filter { it.y < floorY }
            assertTrue("$place zeichnet nichts oberhalb des Bodens", aboveFloor.isNotEmpty())
            val lowest = aboveFloor.maxOf { it.y }
            assertEquals(
                "$place setzt nicht sauber auf dem Boden auf",
                floorY - 1,
                lowest
            )
        }
    }

    @Test
    fun `nichts laeuft aus dem Bild`() {
        // Ueber viele Breiten und Bodenhoehen, weil die Verankerung ein Bruchteil der Breite
        // ist - genau dort entstehen die Randfaelle.
        for (w in listOf(20, 32, 54, 80)) {
            for (f in listOf(30, 60, 86, 120)) {
                for (place in PlayScene.Place.entries) {
                    for (phase in 0..6) {
                        for (cell in build(place, phase = phase, widthCells = w, floor = f)) {
                            assertTrue(
                                "$place ragt bei Breite $w links heraus: ${cell.x}",
                                cell.x >= 0
                            )
                            assertTrue(
                                "$place ragt bei Breite $w rechts heraus: ${cell.x}",
                                cell.x < w
                            )
                            assertTrue(
                                "$place zeichnet bei Boden $f oberhalb des Bildes: ${cell.y}",
                                cell.y >= 0
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `unsinnige Masse liefern eine leere statt einer kaputten Kulisse`() {
        assertTrue(build(PlayScene.Place.NOOK, widthCells = 0).isEmpty())
        assertTrue(build(PlayScene.Place.NOOK, floor = 0).isEmpty())
    }

    // ---- Aufsetzen auf dem Boden ----

    @Test
    fun `jede Spezies hat eine plausible Aufsetz-Zeile`() {
        // Der Boden der Kulisse rechnet damit, dass die Figur mit dieser Zeile aufsetzt. Waere
        // sie falsch, verschwaenden die Fuesse im Boden oder die Figur schwebte darueber - je
        // nach gewaehltem Avatar, weshalb es beim Ausprobieren mit nur einer Spezies gar nicht
        // auffiele.
        for (species in AvatarSpecies.entries) {
            val row = AvatarBodies.forSpecies(species).groundRow()
            // Im hohen Raster (Kopffreiheit oben, siehe AvatarGeometry.HEIGHT).
            assertTrue("$species setzt ausserhalb des Rasters auf: $row", row in 0 until AvatarGeometry.HEIGHT)
            // Keine Spezies endet weiter oben als in der unteren Haelfte ihres Koerpers - sonst
            // stimmt etwas an der Silhouette nicht.
            assertTrue(
                "$species endet unplausibel weit oben: $row",
                row >= AvatarGeometry.HEADROOM + AvatarGeometry.SIZE / 2
            )
        }
    }

    @Test
    fun `die Aufsetz-Zeile beruecksichtigt Fuesse UND Silhouette`() {
        // GLOOP hat keine Fuesse (feet = noFeet) - dort muss die Silhouette den Ausschlag geben,
        // sonst saesse der Wert bei 0 und die Figur haenge weit ueber dem Boden.
        val gloop = AvatarBodies.forSpecies(AvatarSpecies.GLOOP)
        assertTrue("GLOOP hat wider Erwarten Fuesse", gloop.feet(0).isEmpty())
        assertEquals(
            gloop.silhouette.maxOf { it.second } + AvatarGeometry.HEADROOM,
            gloop.groundRow()
        )
    }

    // ---- Geraete und benutzte Requisiten ----

    @Test
    fun `ausgeschaltete Geraete nehmen ihr Licht mit`() {
        // Ein Schalter, dessen Betaetigung nichts wegnimmt, ist keiner. Geprueft wird die
        // Lichtmenge, nicht einzelne Zellen - die Silhouette des Geraets bleibt ja stehen.
        fun litOf(place: PlayScene.Place, lamp: Boolean, tv: Boolean) =
            PlayScene.build(place, 0, width, floorY, PlayAmbientActivity.DayPhase.NIGHT, 1f, lamp, tv)
                .filter { it.isLight }.sumOf { it.brightness }

        assertTrue(
            "Die Lampe auszuschalten aendert nichts am Bild",
            litOf(PlayScene.Place.NOOK, lamp = false, tv = true) <
                litOf(PlayScene.Place.NOOK, lamp = true, tv = true)
        )
        assertTrue(
            "Den Fernseher auszuschalten aendert nichts am Bild",
            litOf(PlayScene.Place.LIVING, lamp = true, tv = false) <
                litOf(PlayScene.Place.LIVING, lamp = true, tv = true)
        )
    }

    @Test
    fun `benutzte Requisiten sehen anders aus als unbenutzte`() {
        // Der getragene Gegenstand erzaehlt den Zweck, aber die QUELLE muss auch antworten:
        // Kuehlschranktuer auf, Buch aus dem Regal verschwunden. Ohne diesen Unterschied bliebe
        // unklar, woher das Ding in der Hand kommt.
        for ((place, station) in listOf(
            PlayScene.Place.KITCHEN to PlayScene.Station.FRIDGE,
            PlayScene.Place.NOOK to PlayScene.Station.BOOKSHELF,
            PlayScene.Place.SHOP to PlayScene.Station.RACK
        )) {
            val idleLook = PlayScene.build(place, 0, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY).toSet()
            val usedLook = PlayScene.build(
                place, 0, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY,
                fade = 1f, lampOn = true, tvOn = true, activeStation = station
            ).toSet()
            assertTrue("$station sieht bei Benutzung genauso aus wie sonst", idleLook != usedLook)
        }
    }

    // ---- Tiefenstaffelung ----

    @Test
    fun `Licht ist heller als Moebel und Moebel heller als der Boden`() {
        // Die ganze Tiefenwirkung haengt an dieser Reihenfolge (siehe Klassendoku) - kehrt sie
        // sich um, wird die Lampe zur Silhouette und der Boden zur Hauptsache.
        assertTrue(PlayScene.HIGHLIGHT > PlayScene.GLOW)
        assertTrue(PlayScene.GLOW > PlayScene.FURNITURE)
        assertTrue(PlayScene.FURNITURE > PlayScene.BACKDROP)
        assertTrue(PlayScene.BACKDROP > PlayScene.STRUCTURE)
        // ... und alles muss unter der vollen Helligkeit des Avatars bleiben, damit er die
        // hellste Erscheinung im Bild ist.
        // Auch das Glanzlicht bleibt UNTER der Figur - es setzt einen Akzent, es tritt nicht
        // mit ihr in Wettbewerb.
        assertTrue(PlayScene.HIGHLIGHT < AvatarGeometry.MAX_BRIGHTNESS)
    }

    @Test
    fun `die Kulisse laesst sich vollstaendig abblenden`() {
        // Wird fuer den Ortswechsel gebraucht: erst weg, dann der neue Ort herein.
        assertTrue(PlayScene.build(PlayScene.Place.NOOK, 0, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY, fade = 0f).isEmpty())
        val half = PlayScene.build(PlayScene.Place.NOOK, 0, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY, fade = 0.5f)
        val full = build(PlayScene.Place.NOOK)
        assertTrue("Abgeblendet muss dunkler sein", half.sumOf { it.brightness } < full.sumOf { it.brightness })
    }

    // ---- Umgebungsanimation ----

    @Test
    fun `jeder Ort bewegt sich zu jeder Tageszeit`() {
        // Eine statische Kulisse hinter einer animierten Figur wirkt wie ein Darstellungsfehler.
        //
        // Das Fenster von 48 Zaehlschritten ist bewusst so gross: Die Details laufen absichtlich
        // auf UNTERSCHIEDLICHEN Vielfachen des Grundtakts (siehe PlayScene.beat), das langsamste
        // davon braucht ueber zwei Dutzend Schritte fuer einen Durchlauf. Ein zu kurzes Fenster
        // wuerde genau die langsamen, ruhigen Regungen als "steht still" melden - und die sind
        // gewollt.
        for (place in PlayScene.Place.entries) {
            for (dayPhase in PlayAmbientActivity.DayPhase.entries) {
                val phases = (0..48).map { phase ->
                    build(place, phase = phase, dayPhase = dayPhase)
                        .map { it.x to (it.y to it.brightness) }
                        .toSet()
                }
                assertTrue("$place steht bei $dayPhase ueber alle Phasen still", phases.distinct().size > 1)
            }
        }
    }

    @Test
    fun `der Himmel unterscheidet Tag und Nacht`() {
        val day = build(PlayScene.Place.BEDROOM, dayPhase = PlayAmbientActivity.DayPhase.MIDDAY).toSet()
        val night = build(PlayScene.Place.BEDROOM, dayPhase = PlayAmbientActivity.DayPhase.NIGHT).toSet()
        assertTrue("Tag- und Nachthimmel sehen identisch aus", day != night)
    }
}
