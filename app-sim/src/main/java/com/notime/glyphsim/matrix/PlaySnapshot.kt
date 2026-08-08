package com.notime.glyphsim.matrix

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/**
 * Haelt EIN Bild des Play-Modus fest - dieselbe Ansicht wie der Film, nur ohne Dauer.
 *
 * **Warum das neben der Filmaufnahme steht und nicht darin aufgeht.** Technisch ist ein
 * Schnappschuss ein Film mit einem einzigen Bild, und genau so ist er hier auch gebaut: Er benutzt
 * dieselbe Beschreibung ([PlayClipRenderer.Frame]) und denselben Zeichner. Im Gebrauch sind es
 * aber zwei verschiedene Dinge. Ein Film verlangt eine Entscheidung vorher (jetzt anfangen) und
 * eine nachher (jetzt aufhoeren) und laesst sich hinterher nicht mehr zurechtruecken. Ein Bild
 * verlangt nur einen einzigen Griff im richtigen Moment - und der richtige Moment ist bei einem
 * Wesen, das seinen eigenen Tag lebt, meistens schon halb vorbei, bevor man ihn kommen sieht.
 *
 * **PNG und nicht JPEG.** Diese Welt besteht aus harten Hell-Dunkel-Kanten auf schwarzem Grund -
 * also aus genau dem, woran sich die Kompression von JPEG verschluckt: Um jede helle Zelle legte
 * sie einen grauen Hof, und der Eindruck einzelner Leuchtpunkte, von dem das ganze Bild lebt,
 * waere dahin. PNG gibt jede Zelle unveraendert wieder und ist bei so wenigen Farben zusaetzlich
 * KLEINER als JPEG - hier gibt es also nichts abzuwaegen.
 */
object PlaySnapshot {

    /**
     * Zeichnet [frame] und legt ihn in der eigenen Sammlung ab. Gibt die Datei zurueck, oder
     * `null`, wenn das Schreiben fehlschlug (voller Speicher).
     */
    fun capture(context: Context, frame: PlayClipRenderer.Frame): File? {
        val bitmap = PlayClipRenderer.render(frame)
        val target = File(shotDir(context), "tama-${System.currentTimeMillis()}.png")
        return try {
            FileOutputStream(target).use { out ->
                // Die Qualitaetszahl ist bei PNG ohne Wirkung - verlustfrei ist verlustfrei.
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            target
        } catch (t: Throwable) {
            target.delete()
            null
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Eigener Ordner neben den Filmen, nicht derselbe: Die Sammlung listet beides getrennt auf,
     * und eine Trennung nach Ordner ist verlaesslicher als eine nach Dateiendung.
     *
     * Muss wie der Filmordner in `res/xml/clip_paths.xml` freigegeben sein - sonst laesst sich ein
     * Bild zwar aufnehmen, aber nicht weitergeben.
     */
    fun shotDir(context: Context): File =
        File(context.filesDir, "shots").apply { mkdirs() }

    /** Die gesammelten Bilder, neueste zuerst. */
    fun library(context: Context): List<File> =
        shotDir(context).listFiles { f -> f.extension == "png" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
