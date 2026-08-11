package com.notime.glyphsim.ui

import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.NO_GOAL
import java.time.DayOfWeek

/**
 * **Die eigenen Erinnerungen als Text mitnehmen.**
 *
 * ## Warum es das geben muss
 *
 * Die App speichert alles auf dem Geraet und schickt nichts irgendwohin - das ist eine Zusage und
 * kein Versehen. Die Kehrseite: Es gibt keinen Ort, von dem sich etwas zurueckholen liesse. Wer
 * das Telefon wechselt, die App neu installiert oder von der Debug- auf die Play-Fassung umsteigt
 * (unterschiedliche Signatur, Android verlangt dafuer eine Deinstallation), steht ohne alles da.
 *
 * "Deine Daten gehoeren dir" ist ohne einen Weg nach draussen eine leere Aussage.
 *
 * ## Warum ausgerechnet dieses Format
 *
 * Geschrieben wird genau das, was [ReminderImportDialog] ohnehin liest: ein JSON-Objekt mit einer
 * Liste unter `reminders`. Das Format entstand fuer den Import aus einem KI-Gespraech - es hier
 * wiederzuverwenden hat drei Vorteile, die ein eigenes Exportformat alle nicht haette.
 *
 * **Der Rueckweg existiert schon**, samt Pruefung. Der Import nimmt nichts ungeprueft an: Er
 * begrenzt die Eingabelaenge, deckelt die Anzahl, rastet krumme Werte auf waehlbare Stufen ein
 * und legt offen, was er dabei zurechtgerueckt hat (siehe [ReminderImport.sanitize]). Eine
 * Datei, die zwischendurch von Hand bearbeitet wurde oder aus einer aelteren Fassung stammt,
 * laeuft damit durch dieselbe Schleuse wie alles andere.
 *
 * **Es bleibt lesbar.** Wer nachsehen will, was da eigentlich mitgeht, kann es lesen - bei einer
 * Datei, die nur diese App versteht, muesste er es glauben.
 *
 * **Und es laesst sich weiterverwenden.** Derselbe Text kann in ein KI-Gespraech, in eine Notiz
 * oder in die naechste Installation.
 *
 * ## Was NICHT mitgeht - und warum
 *
 * Nur die Erinnerungen. Nicht das Pflegebuch, nicht Stimmung, Spielstand oder Kapitel.
 *
 * Das ist kein halber Export, sondern die Trennlinie aus Phase 4b: **Die Routinen gehoeren dir,
 * die gemeinsame Geschichte gehoert dem Wesen, mit dem du sie geteilt hast.** Was man mitnimmt,
 * wenn man das Geraet wechselt, ist das, was man sich vorgenommen hat. Ein Pflegebuch, das auf
 * einem neuen Geraet wieder auftaucht, waere die Erinnerung eines Wesens, das gar nicht dabei
 * war - genau die erfundene Beziehung, die `PresentCompanion` ausschliesst.
 *
 * Die Spiel-Zeile faellt ebenfalls weg: Sie ist keine Einstellung des Nutzers, sondern wird vom
 * Spielmodus selbst verwaltet und bei jeder Neuplanung neu gewuerfelt.
 */
object ReminderExport {

    /**
     * Baut den Text - **ohne Android**, damit sich das Ergebnis ohne Geraet pruefen laesst.
     *
     * [libraryAnimations] wird gebraucht, um zu einer Bibliotheks-Animation ihren NAMEN zu finden:
     * Der Import verweist ueber den Namen, nicht ueber die Id (siehe [RawImportedReminder]) - und
     * das ist richtig so, denn auf einem neuen Geraet ist jede Id eine andere.
     *
     * Von Hand zusammengesetzt statt ueber eine Bibliothek: Das Ergebnis ist ein flaches Objekt
     * mit acht Feldern, und der Weg dorthin soll auf der JVM pruefbar sein - `org.json` ist dort
     * nur ein Platzhalter, der bei jedem Aufruf wirft.
     */
    fun toJson(
        reminders: List<GlyphReminder>,
        libraryAnimations: List<LibraryAnimation> = emptyList()
    ): String {
        val nameById = libraryAnimations.associate { it.id to it.label }
        val eintraege = reminders
            // Die vom Spielmodus verwaltete Zeile ist keine Einstellung - siehe Klassendoku.
            .filterNot { it.isPlayMode }
            .joinToString(",\n") { eintrag(it, nameById) }

        return "{\n  \"reminders\": [\n$eintraege\n  ]\n}"
    }

    private fun eintrag(reminder: GlyphReminder, nameById: Map<Long, String>): String {
        val felder = mutableListOf<String>()
        felder += "\"label\": ${quote(reminder.label)}"
        felder += "\"topic\": ${quote(reminder.animationType.name)}"
        // Nur wenn die Animation noch existiert: Ein Verweis auf einen Namen, den es nicht gibt,
        // wuerde der Import ohnehin verwerfen - dann lieber gar nicht erst schreiben.
        reminder.libraryAnimationId?.let { id ->
            nameById[id]?.let { felder += "\"libraryAnimation\": ${quote(it)}" }
        }
        if (reminder.dailyGoal != NO_GOAL) {
            felder += "\"dailyGoal\": ${reminder.dailyGoal}"
        }
        felder += "\"intervalMinutes\": ${reminder.intervalMinutes}"
        felder += "\"start\": ${quote(zeit(reminder.startMinuteOfDay))}"
        felder += "\"end\": ${quote(zeit(reminder.endMinuteOfDay))}"
        felder += "\"days\": [${tage(reminder.daysOfWeekMask)}]"
        return "    { " + felder.joinToString(", ") + " }"
    }

    /** Minuten seit Mitternacht als "08:00" - das Gegenstueck zu [ReminderImport.parseTime]. */
    private fun zeit(minuteOfDay: Int): String =
        "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

    /**
     * Die Wochentage als englische Namen, so wie der Import sie erwartet.
     *
     * Bewusst NICHT uebersetzt: Der Text ist ein Datenformat und keine Anzeige. Eine Datei, die
     * je nach Spracheinstellung des exportierenden Geraets andere Woerter enthaelt, liesse sich
     * auf einem anders eingestellten Geraet nicht mehr einlesen.
     */
    private fun tage(mask: Int): String =
        DayOfWeek.entries
            .filter { DaysOfWeekMask.contains(mask, it) }
            .joinToString(", ") { quote(it.name) }

    /** JSON-Zeichenkette: Anfuehrungszeichen, Rueckstriche und Steuerzeichen muessen escaped werden. */
    private fun quote(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        return sb.append("\"").toString()
    }
}
