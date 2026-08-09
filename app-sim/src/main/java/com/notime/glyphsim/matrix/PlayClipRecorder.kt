package com.notime.glyphsim.matrix

import android.content.Context
import java.io.File

/**
 * Schneidet mit, was im Play-Modus geschieht, und schreibt es als MP4.
 *
 * **Was aufgenommen wird, bestimmt der Nutzer** - Start und Ende, bis zur Obergrenze
 * [MAX_SECONDS]. Waehrend der Aufnahme wird der Zustand des Bildschirms regelmaessig als
 * BESCHREIBUNG festgehalten (siehe [Session]); gezeichnet und kodiert wird erst danach.
 *
 * **Warum die Bilder gerechnet und nicht abgefilmt werden** (siehe [PlayClipRenderer]): kein
 * Systemdialog, keine Statusleiste im Bild, festes Hochformat unabhaengig vom Geraet.
 */
object PlayClipRecorder {

    /** Bilder je Sekunde - muss zu [ClipEncoder] passen. */
    private const val FPS = 15

    /**
     * Eine LAUFENDE Aufnahme: sammelt Momentaufnahmen, bis der Nutzer beendet.
     *
     * **Warum mitgeschnitten statt vorgeplant wird.** Der erste Entwurf stellte einen Film aus
     * einer festen Folge zusammen - immer dieselbe Handlung, immer gleich lang. Das ist kein
     * Mitschnitt, sondern eine Vorfuehrung: Was der Avatar in dem Moment tatsaechlich tat, kam
     * darin gar nicht vor, und wer eine gerade entstehende Szene festhalten wollte, konnte es
     * nicht.
     *
     * **Was gesammelt wird, sind BESCHREIBUNGEN, keine Bilder.** Ein Bild in voller Aufloesung
     * belegt Megabytes, eine Beschreibung ein paar Dutzend Byte - sie verweist nur auf ohnehin
     * vorhandene Animationsbilder. Deshalb kann eine Aufnahme minutenlang laufen, ohne dass der
     * Speicher belastet wird; gezeichnet wird erst hinterher, eines nach dem anderen.
     */
    class Session {
        private val frames = mutableListOf<PlayClipRenderer.Frame>()

        val frameCount: Int get() = frames.size
        val seconds: Int get() = frames.size / FPS
        /** Ob die Obergrenze erreicht ist - dann beendet sich die Aufnahme von selbst. */
        val isFull: Boolean get() = frames.size >= MAX_FRAMES

        fun add(frame: PlayClipRenderer.Frame) {
            if (!isFull) frames += frame
        }

        fun snapshot(): List<PlayClipRenderer.Frame> = frames.toList()
    }

    /**
     * Hoechstlaenge einer Aufnahme.
     *
     * Neunzig Sekunden sind lang genug fuer eine ausgespielte Szene und kurz genug, dass das
     * Zusammenrechnen danach nicht ausufert - jedes Bild muss einzeln gezeichnet und umgerechnet
     * werden, und das dauert ungefaehr so lange wie der Film selbst. Ohne Grenze koennte eine
     * vergessene Aufnahme stundenlang mitlaufen und waere danach praktisch nicht mehr zu
     * verarbeiten.
     */
    const val MAX_SECONDS = 90
    private const val MAX_FRAMES = MAX_SECONDS * FPS

    /** Abstand zwischen zwei Momentaufnahmen. */
    const val SAMPLE_INTERVAL_MS = 1000L / FPS

    /**
     * Kodiert eine fertige Aufnahme. [onProgress] meldet den Fortschritt von 0 bis 1 - bei bis zu
     * neunzig Sekunden Material darf der Nutzer nicht im Unklaren gelassen werden.
     */
    fun encode(
        context: Context,
        session: Session,
        onProgress: (Float) -> Unit = {}
    ): File? {
        val frames = session.snapshot()
        if (frames.isEmpty()) return null
        if (!hasRoomForRecording(context, frames.size / FPS)) return null

        /*
         * Kodiert wird unter `.part` und erst bei Erfolg umbenannt.
         *
         * Damit ist eine Datei mit `.mp4` per Konstruktion vollstaendig. Vorher entstand die
         * Zieldatei sofort und wuchs waehrend des Kodierens; brach es ab - Speicher voll, Prozess
         * beendet, Codec-Fehler -, blieb ein kaputter Film in der Galerie stehen, den nichts als
         * solchen erkannte.
         */
        val partial = File(clipDir(context), "tama-${System.currentTimeMillis()}.mp4${ClipStorage.PARTIAL_SUFFIX}")
        val encoded = ClipEncoder.encode(
            frameCount = frames.size,
            width = PlayClipRenderer.DEFAULT_WIDTH,
            height = PlayClipRenderer.DEFAULT_HEIGHT,
            target = partial
        ) { index, into ->
            PlayClipRenderer.renderInto(frames[index], into)
            onProgress((index + 1).toFloat() / frames.size)
        } ?: run {
            partial.delete()
            return null
        }

        val target = File(encoded.path.removeSuffix(ClipStorage.PARTIAL_SUFFIX))
        return if (encoded.renameTo(target)) {
            target
        } else {
            // Umbenennen scheitert im app-eigenen Bereich praktisch nie. Wenn doch, ist ein
            // Bruchstueck stehenzulassen die schlechtere Wahl als gar kein Ergebnis.
            partial.delete()
            null
        }
    }

    /**
     * Ob eine Aufnahme dieser Laenge noch in den freien Speicher passt.
     *
     * Die Rechnung steht in [ClipStorage] und ist dort ohne Geraet geprueft; hier steht nur die
     * Abfrage beim Dateisystem.
     */
    fun hasRoomForRecording(context: Context, seconds: Int): Boolean =
        ClipStorage.hasRoomFor(seconds, clipDir(context).usableSpace)

    /** Belegter Platz aller Aufnahmen - fuer die Anzeige in der Galerie. */
    fun usedBytes(context: Context): Long = ClipStorage.usedBytes(library(context))

    /**
     * Entfernt Bruchstuecke aus abgebrochenen Aufnahmen.
     *
     * Beim Blick in die Galerie aufzuraeumen statt beim App-Start: dort ist es sichtbar folgenlos,
     * und ein Bruchstueck schadet bis dahin nichts ausser Platz zu belegen.
     */
    fun cleanPartials(context: Context): Int {
        val bruchstuecke = clipDir(context).listFiles()?.toList().orEmpty()
            .let { ClipStorage.partialFiles(it) }
        bruchstuecke.forEach { runCatching { it.delete() } }
        return bruchstuecke.size
    }

    /**
     * Ablageort im app-eigenen Bereich. Muss zu der in `res/xml/clip_paths.xml` eingetragenen
     * Freigabe passen - sonst laesst sich die fertige Datei nicht weiterreichen.
     */
    fun clipDir(context: Context): File =
        File(context.filesDir, "clips").apply { mkdirs() }

    /** Die gesammelten Filme, neueste zuerst. */
    fun library(context: Context): List<File> =
        clipDir(context).listFiles { f -> f.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

}
