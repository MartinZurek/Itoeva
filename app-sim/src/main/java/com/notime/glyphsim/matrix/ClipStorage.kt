package com.notime.glyphsim.matrix

import java.io.File

/**
 * Wie viel Platz die Aufnahmen belegen, und ob eine weitere noch hineinpasst.
 *
 * ## Warum es das braucht
 *
 * Die Filme werden mit 6 Mbit/s bei 15 Bildern je Sekunde kodiert (siehe [ClipEncoder]) - das
 * sind rund **45 MB je Minute**. Bis hierher gab es dafuer keinerlei Grenze: kein Anzeigen des
 * belegten Platzes, keine Pruefung des freien, und in der Galerie nicht einmal einen
 * Loeschen-Knopf. Wer seine Aufnahmen loswerden wollte, musste ueber die Systemeinstellungen die
 * App-Daten leeren - und verlor damit Erinnerungen und Pflegebuch gleich mit.
 *
 * Lief der Speicher waehrend einer Aufnahme voll, brach das Kodieren mit einer IO-Ausnahme ab und
 * hinterliess eine unvollstaendige Datei, die danach in der Galerie stand.
 *
 * ## Die Linie
 *
 * **Diese App loescht nichts von allein.** Sie zeigt an, sie warnt, und sie lehnt ab, wenn kein
 * Platz mehr ist - aber die Aufnahmen des Nutzers wirft sie nicht ungefragt weg. Eine automatische
 * Rotation waere bequemer und stand zur Wahl; sie ist bewusst nicht genommen worden, weil es die
 * einzige Stelle in dieser App waere, an der Nutzerdaten ohne Zutun verschwinden.
 *
 * Reine Rechnung ohne Android-Aufrufe, damit sie ohne Geraet pruefbar ist (siehe
 * `ClipStorageTest`) - die Dateisystem-Abfragen stehen in [PlayClipRecorder].
 */
object ClipStorage {

    /** Bitrate und Bildrate aus [ClipEncoder], hier als Groessenabschaetzung. */
    private const val BYTES_PER_SECOND = 6_000_000L / 8

    /**
     * Sicherheitsabstand, der nach einer Aufnahme frei bleiben soll.
     *
     * Ein Geraet, dessen Speicher bis auf das letzte Byte gefuellt ist, verhaelt sich in jeder
     * Hinsicht unangenehm - Datenbankschreibvorgaenge scheitern, das System meldet Warnungen. Die
     * Aufnahme soll den Speicher also nicht gerade so fuellen, sondern mit Abstand hineinpassen.
     */
    const val HEADROOM_BYTES = 100L * 1024 * 1024

    /** Grobe Groesse einer Aufnahme dieser Laenge - bewusst grosszuegig geschaetzt. */
    fun estimatedBytes(seconds: Int): Long = seconds.coerceAtLeast(0) * BYTES_PER_SECOND

    /**
     * Passt eine Aufnahme von [seconds] Sekunden noch in [freeBytes]?
     *
     * Geprueft wird gegen Schaetzung PLUS [HEADROOM_BYTES]. Lieber einmal zu frueh ablehnen als
     * mitten im Kodieren abbrechen: ein abgelehnter Versuch kostet den Nutzer nichts, ein
     * abgebrochener die ganze Aufnahme.
     */
    fun hasRoomFor(seconds: Int, freeBytes: Long): Boolean =
        freeBytes >= estimatedBytes(seconds) + HEADROOM_BYTES

    /** Summe der Dateigroessen - was die Galerie als belegten Platz anzeigt. */
    fun usedBytes(files: List<File>): Long = files.sumOf { it.length() }

    /**
     * Menschenlesbare Groesse.
     *
     * Bewusst mit einer Nachkommastelle ab einem Megabyte: "1,4 GB" sagt mehr als "1 GB", und
     * "1434 MB" laesst sich nicht auf einen Blick einordnen. Basis 1024, weil Android den
     * Speicher ebenso ausweist - eine andere Basis waere hier zwar formal korrekter, wuerde aber
     * von den Zahlen in den Systemeinstellungen abweichen und damit verwirren.
     */
    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    }

    /**
     * Endung fuer noch unfertige Aufnahmen.
     *
     * Kodiert wird unter diesem Namen und erst bei Erfolg umbenannt. Damit ist eine Datei mit
     * `.mp4` per Konstruktion vollstaendig - und ein Abbruch (Speicher voll, Prozess beendet,
     * Codec-Fehler) hinterlaesst etwas, das sich eindeutig als Bruchstueck erkennen und
     * aufraeumen laesst, statt in der Galerie als kaputter Film zu stehen.
     */
    const val PARTIAL_SUFFIX = ".part"

    /** Bruchstuecke aus abgebrochenen Aufnahmen - beim naechsten Blick in die Galerie zu entfernen. */
    fun partialFiles(files: List<File>): List<File> =
        files.filter { it.name.endsWith(PARTIAL_SUFFIX) }
}
