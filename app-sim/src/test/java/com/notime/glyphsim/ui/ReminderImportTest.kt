package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DAILY_GOAL_OPTIONS
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.INTERVAL_OPTIONS
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.NO_GOAL
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer den KI-Import ([ReminderImport]).
 *
 * Hier kommt die Eingabe von aussen und ist grundsaetzlich unvertrauenswuerdig: erzeugt von einem
 * Sprachmodell, transportiert ueber die Zwischenablage, moeglicherweise abgeschnitten oder aus
 * einer ganz anderen Quelle. Ein Fehler faellt dabei nicht als Absturz auf, sondern als
 * Erinnerung, die zur falschen Zeit oder gar nicht ausloest - deshalb ist gerade diese Schicht
 * dicht abzusichern.
 */
class ReminderImportTest {

    // ---- JSON aus einer Antwort herausschneiden ----

    @Test
    fun `findet JSON zwischen Fliesstext`() {
        val answer = """
            Klar, hier ist deine Konfiguration:
            {"reminders":[{"topic":"DRINK"}]}
            Sag Bescheid, wenn du etwas aendern willst!
        """.trimIndent()
        assertEquals("""{"reminders":[{"topic":"DRINK"}]}""", ReminderImport.extractJson(answer))
    }

    @Test
    fun `findet JSON in einem Markdown-Codeblock`() {
        val answer = "Hier:\n```json\n{\"reminders\":[]}\n```\nViel Erfolg!"
        assertEquals("""{"reminders":[]}""", ReminderImport.extractJson(answer))
    }

    @Test
    fun `beruecksichtigt verschachtelte Klammern`() {
        // Die letzte Klammer der Antwort ist nicht die schliessende des Blocks - ein naives
        // "von erster { bis letzter }" wuerde hier den Schlusssatz mit einsammeln.
        val answer = """{"reminders":[{"topic":"MOVE","dailyGoal":4}]} und noch {Text}"""
        assertEquals("""{"reminders":[{"topic":"MOVE","dailyGoal":4}]}""", ReminderImport.extractJson(answer))
    }

    @Test
    fun `schliessende Klammer in String beendet JSON nicht vorzeitig`() {
        val answer = """{"reminders":[{"label":"Noch } offen","topic":"DRINK"}]} danach"""
        assertEquals(
            """{"reminders":[{"label":"Noch } offen","topic":"DRINK"}]}""",
            ReminderImport.extractJson(answer)
        )
    }

    @Test
    fun `oeffnende Klammer in String erhoeht Tiefe nicht`() {
        val answer = """{"reminders":[{"label":"Hier { beginnen"}]} danach"""
        assertEquals(
            """{"reminders":[{"label":"Hier { beginnen"}]}""",
            ReminderImport.extractJson(answer)
        )
    }

    @Test
    fun `escapete Anfuehrungszeichen und Backslashes erhalten Stringzustand`() {
        val answer = """{"label":"Pfad \\ und Zitat \"} bleibt im Text","topic":"BOOK"} danach"""
        assertEquals(
            """{"label":"Pfad \\ und Zitat \"} bleibt im Text","topic":"BOOK"}""",
            ReminderImport.extractJson(answer)
        )
    }

    @Test
    fun `gerade Backslashanzahl laesst String am folgenden Anfuehrungszeichen enden`() {
        val answer = """{"label":"Pfad \\\\"} danach"""
        assertEquals("""{"label":"Pfad \\\\"}""", ReminderImport.extractJson(answer))
    }

    @Test
    fun `ohne JSON kommt null zurueck`() {
        assertNull(ReminderImport.extractJson("Ich habe leider kein Ergebnis fuer dich."))
    }

    @Test
    fun `unvollstaendiges JSON wird nicht als gueltig ausgegeben`() {
        // Abgeschnittene Antwort - lieber nichts als ein halbes Ergebnis.
        assertNull(ReminderImport.extractJson("""{"reminders":[{"topic":"DRINK"“"""))
    }

    // ---- Uhrzeiten ----

    @Test
    fun `liest Uhrzeiten mit und ohne fuehrende Null`() {
        assertEquals(8 * 60, ReminderImport.parseTime("08:00"))
        assertEquals(8 * 60, ReminderImport.parseTime("8:00"))
        assertEquals(20 * 60 + 30, ReminderImport.parseTime("20:30"))
    }

    @Test
    fun `weist unsinnige Uhrzeiten zurueck`() {
        assertNull(ReminderImport.parseTime("25:00"))
        assertNull(ReminderImport.parseTime("12:99"))
        assertNull(ReminderImport.parseTime("morgens"))
        assertNull(ReminderImport.parseTime(null))
    }

    // ---- Pruefen und Zurechtruecken ----

    @Test
    fun `uebernimmt eine saubere Angabe unveraendert`() {
        val result = ReminderImport.sanitize(
            RawImportedReminder(
                label = "Trinken", topic = "DRINK", dailyGoal = 8,
                intervalMinutes = 45, start = "08:00", end = "20:00",
                days = listOf("MO", "TU", "WE", "TH", "FR")
            )
        )
        assertEquals("Trinken", result.label)
        assertEquals(AnimationChoice.BuiltIn(AnimationType.DRINK), result.animationChoice)
        assertEquals(8, result.dailyGoal)
        assertEquals(45, result.intervalMinutes)
        assertEquals(8 * 60, result.startMinuteOfDay)
        assertEquals(20 * 60, result.endMinuteOfDay)
        assertTrue("Saubere Angaben duerfen nichts korrigieren", result.corrections.isEmpty())
    }

    @Test
    fun `erfundenes Thema wird auf GENERAL gezogen und gemeldet`() {
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "MEDITATION_XL"))
        assertEquals(AnimationChoice.BuiltIn(AnimationType.GENERAL), result.animationChoice)
        assertTrue("Die Ersetzung muss sichtbar sein", result.corrections.any { it.contains("GENERAL") })
    }

    @Test
    fun `fehlende Angaben kommen aus dem Rhythmus des Themas`() {
        // Nichts ausser dem Thema - Ziel, Intervall und Fenster muessen aus dem Konzept kommen
        // (siehe ReminderRhythm), nicht aus willkuerlichen Standardwerten.
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "SLEEP"))
        val rhythm = ReminderRhythm.forType(AnimationType.SLEEP)
        assertEquals(rhythm.suggestedGoal, result.dailyGoal)
        assertEquals(rhythm.suggestedInterval, result.intervalMinutes)
        assertEquals(rhythm.suggestedWindow.startMinute, result.startMinuteOfDay)
    }

    @Test
    fun `unsinnige Zahlen werden ersetzt statt uebernommen`() {
        // -3 ist (anders als eine bewusste 0, siehe eigener Test unten) keine sinnvolle Ansage,
        // sondern eine unsinnige Zahl wie das negative Intervall - beide muessen korrigiert und
        // gemeldet werden.
        val result = ReminderImport.sanitize(
            RawImportedReminder(topic = "MOVE", dailyGoal = -3, intervalMinutes = -5)
        )
        assertEquals("Negatives Tagesziel wird zu keinem Ziel", NO_GOAL, result.dailyGoal)
        assertTrue("Intervall muss positiv sein", result.intervalMinutes > 0)
        assertEquals("Beide Korrekturen muessen gemeldet werden", 2, result.corrections.size)
    }

    @Test
    fun `krumme Werte rasten auf die waehlbaren Stufen ein`() {
        // Eine KI kann 37 Minuten vorschlagen - waehlbar sind aber nur die Stufen aus
        // INTERVAL_OPTIONS. Wuerde 37 uebernommen, stuende im Bearbeiten-Dialog spaeter kein
        // Chip auf ausgewaehlt, und ein unbeabsichtigter Griff wuerde die Einstellung
        // ueberschreiben.
        val result = ReminderImport.sanitize(
            RawImportedReminder(topic = "MOVE", dailyGoal = 7, intervalMinutes = 37)
        )
        assertTrue("Intervall muss eine waehlbare Stufe sein", result.intervalMinutes in INTERVAL_OPTIONS)
        assertTrue("Tagesziel muss eine waehlbare Stufe sein", result.dailyGoal in DAILY_GOAL_OPTIONS)
        assertEquals(30, result.intervalMinutes)
        assertEquals(6, result.dailyGoal)
        assertEquals("Beide Anpassungen muessen gemeldet werden", 2, result.corrections.size)
    }

    @Test
    fun `jedes Ergebnis ist im Bearbeiten-Dialog darstellbar`() {
        // Ueber eine breite Spanne roher Werte: was hier herauskommt, muss immer einer der Chips
        // sein - sonst waere der Import mit der Oberflaeche nicht deckungsgleich.
        for (raw in listOf(1, 3, 7, 13, 22, 37, 55, 100, 200, 700)) {
            val result = ReminderImport.sanitize(
                RawImportedReminder(topic = "GENERAL", dailyGoal = raw, intervalMinutes = raw)
            )
            assertTrue("intervalMinutes=$raw ergab ${result.intervalMinutes}", result.intervalMinutes in INTERVAL_OPTIONS)
            assertTrue("dailyGoal=$raw ergab ${result.dailyGoal}", result.dailyGoal in DAILY_GOAL_OPTIONS)
        }
    }

    @Test
    fun `absurd grosse Werte werden ebenfalls ersetzt`() {
        val result = ReminderImport.sanitize(
            RawImportedReminder(topic = "DRINK", dailyGoal = 500, intervalMinutes = 100000)
        )
        assertTrue(result.dailyGoal in 1..24)
        assertTrue(result.intervalMinutes in 1..720)
    }

    @Test
    fun `Zeitfenster ohne Dauer wird zum ganzen Tag`() {
        // Gleicher Anfang und gleiches Ende ergaebe eine Erinnerung, die nie ausloest.
        val result = ReminderImport.sanitize(
            RawImportedReminder(topic = "REST", start = "09:00", end = "09:00")
        )
        assertTrue(result.endMinuteOfDay > result.startMinuteOfDay)
        assertTrue(result.corrections.any { it.contains("Zeitfenster") })
    }

    @Test
    fun `versteht deutsche und englische Wochentagskuerzel`() {
        val english = ReminderImport.sanitize(RawImportedReminder(topic = "MOVE", days = listOf("MO", "TU")))
        val german = ReminderImport.sanitize(RawImportedReminder(topic = "MOVE", days = listOf("Mo", "Di")))
        assertEquals(
            DaysOfWeekMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)),
            english.daysOfWeekMask
        )
        assertEquals(english.daysOfWeekMask, german.daysOfWeekMask)
    }

    @Test
    fun `ohne brauchbare Wochentage gilt taeglich`() {
        // Sonst entstuende eine Erinnerung ohne aktiven Tag, die nie feuert.
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "MOVE", days = listOf("Blursday")))
        assertEquals(DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()), result.daysOfWeekMask)
    }

    @Test
    fun `ohne Bezeichnung wird das Thema als Name genommen`() {
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "BOOK", label = "   "))
        assertTrue(result.label.isNotBlank())
    }

    @Test
    fun `eine voellig leere Angabe ergibt trotzdem etwas Anlegbares`() {
        // Der Import darf an einer leeren Zeile nicht scheitern - er soll etwas Sinnvolles
        // vorschlagen, das der Nutzer in der Vorschau noch verwerfen kann.
        val result = ReminderImport.sanitize(RawImportedReminder())
        assertEquals(AnimationChoice.BuiltIn(AnimationType.GENERAL), result.animationChoice)
        assertTrue(result.dailyGoal >= 1)
        assertTrue(result.intervalMinutes > 0)
        assertTrue(result.endMinuteOfDay > result.startMinuteOfDay)
    }

    // ---- Explizit zielfrei (dailyGoal = 0) ----

    @Test
    fun `explizite 0 wird zu kein Ziel statt zum Rhythmus-Vorschlag`() {
        // 0 ist eine bewusste Ansage, kein fehlender Wert - im Unterschied zu einem ganz
        // fehlenden dailyGoal-Feld (siehe naechster Test) darf hier kein Vorschlag einspringen.
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "DRINK", dailyGoal = 0))
        assertEquals(NO_GOAL, result.dailyGoal)
        assertTrue("Eine bewusste 0 ist keine Korrektur", result.corrections.isEmpty())
    }

    @Test
    fun `fehlendes dailyGoal-Feld greift weiterhin auf den Rhythmus-Vorschlag zurueck`() {
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "DRINK", dailyGoal = null))
        assertEquals(ReminderRhythm.forType(AnimationType.DRINK).suggestedGoal, result.dailyGoal)
    }

    // ---- Bibliotheks-Animationen ----

    @Test
    fun `libraryAnimation wird per Namen unabhaengig von Gross-Kleinschreibung gefunden`() {
        val library = listOf(LibraryAnimation(id = 42, label = "Gitarre ueben", emoji = "🎸", framesData = ""))
        val result = ReminderImport.sanitize(
            RawImportedReminder(topic = "CREATIVITY", libraryAnimation = "gitarre UEBEN"),
            libraryAnimations = library
        )
        assertEquals(AnimationChoice.Library(42, "Gitarre ueben", "🎸"), result.animationChoice)
        assertTrue("Ein echter Treffer ist keine Korrektur", result.corrections.isEmpty())
    }

    @Test
    fun `unbekannte libraryAnimation faellt auf das Thema zurueck und wird gemeldet`() {
        val result = ReminderImport.sanitize(
            RawImportedReminder(topic = "MOVE", libraryAnimation = "Nicht vorhanden"),
            libraryAnimations = listOf(LibraryAnimation(id = 1, label = "Anderes", emoji = "✨", framesData = ""))
        )
        assertEquals(AnimationChoice.BuiltIn(AnimationType.MOVE), result.animationChoice)
        assertTrue(result.corrections.any { it.contains("libraryAnimation") })
    }

    @Test
    fun `libraryAnimation ohne uebergebene Bibliothek faellt ebenfalls auf das Thema zurueck`() {
        // Kein zweiter Parameter mitgegeben - Standardwert ist eine leere Liste.
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "MOVE", libraryAnimation = "Irgendwas"))
        assertEquals(AnimationChoice.BuiltIn(AnimationType.MOVE), result.animationChoice)
    }

    // ---- Label-Laenge ----

    @Test
    fun `zu lange Labels werden gekuerzt und gemeldet`() {
        val longLabel = "X".repeat(80)
        val result = ReminderImport.sanitize(RawImportedReminder(topic = "GENERAL", label = longLabel))
        assertTrue("Label darf nicht ungekuerzt uebernommen werden", result.label.length < longLabel.length)
        assertTrue(result.corrections.any { it.contains("label") })
    }
}
