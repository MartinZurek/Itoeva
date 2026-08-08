package com.notime.glyphsim.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Legt einen aufgenommenen Film zusaetzlich in der GERAETE-GALERIE ab.
 *
 * **Warum die App die Filme trotzdem selbst behaelt.** Die eigene Sammlung
 * ([com.notime.glyphsim.matrix.PlayClipRecorder.library]) liegt im Datenbereich der App: Sie
 * ueberlebt Neustarts, braucht keine Berechtigung, verschwindet aber restlos, wenn jemand die App
 * deinstalliert. Das ist fuer eine Sammlung genau richtig - sie gehoert zur App.
 *
 * Wer einen Film dagegen behalten oder in anderen Apps wiederfinden will, braucht ihn in der
 * Galerie. Deshalb ist das ein eigener, ausdruecklicher Schritt und passiert nicht automatisch:
 * Ungefragt die Galerie des Nutzers mit Dateien zu fuellen, ist eine Zumutung.
 *
 * **Erst ab Android 10.** Seither darf eine App in die Galerie schreiben, ohne Zugriff auf den
 * gesamten Speicher zu verlangen. Davor waere dafuer die Berechtigung fuer ALLE Dateien noetig -
 * eine unverhaeltnismaessige Forderung fuers Ablegen eines Videos. Auf aelteren Geraeten bleibt
 * deshalb das Teilen der Weg nach draussen.
 */
object ClipGallery {

    /** Ob dieses Geraet das Ablegen in der Galerie ohne weitreichende Berechtigung erlaubt. */
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private const val ALBUM = "Tama"

    /**
     * Kopiert [file] in die Galerie. Gibt zurueck, ob es geklappt hat.
     *
     * Film oder Bild wird an der Endung erkannt und landet entsprechend unter `Movies` oder
     * `Pictures` - eine Galerie sortiert danach, und ein Standbild zwischen den Filmen zu
     * hinterlegen waere genau die Art von Unordnung, die man spaeter nicht mehr aufraeumt.
     */
    fun save(context: Context, file: File): Boolean {
        if (!isSupported || !file.exists()) return false
        val isImage = file.extension.equals("png", ignoreCase = true)
        val collection = if (isImage) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        return try {
            val values = ContentValues().apply {
                // Die Spaltennamen sind fuer Bild und Film dieselben Zeichenketten (beide erben
                // von MediaStore.MediaColumns); nur die Sammlung und der Zielordner wechseln.
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, if (isImage) "image/png" else "video/mp4")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (isImage) "Pictures/$ALBUM" else "Movies/$ALBUM"
                )
                // Solange dieses Kennzeichen gesetzt ist, sehen andere Apps die Datei nicht -
                // sonst taucht sie in der Galerie auf, waehrend sie noch halb geschrieben ist.
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(collection, values) ?: return false

            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            } ?: return false

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (t: Throwable) {
            false
        }
    }
}
