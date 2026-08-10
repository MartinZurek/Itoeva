package com.notime.glyphsim.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Haelt die Texte an dem fest, was die App tatsaechlich tut.**
 *
 * Erklaerungen verrotten leise. Ein Satz wie "tipp oben auf meine Mahlzeiten" war jahrelang
 * richtig und wurde in dem Moment falsch, in dem der Streifen am oberen Rand verschwand - ohne
 * dass irgendetwas nicht mehr uebersetzte, abstuerzte oder auffiel. Falsche Hilfe ist schlimmer
 * als keine: Wer ihr folgt, sucht anschliessend an einer Stelle, die es nicht gibt, und haelt sich
 * selbst fuer begriffsstutzig.
 *
 * Automatisch pruefbar ist daran nicht, ob ein Satz stimmt - wohl aber, ob er sich auf Dinge
 * beruft, die es noch gibt: Jeder Modus braucht seine Erklaerung, jede Kategorie ihre Eintraege,
 * und beide Sprachen muessen dasselbe kennen.
 */
class HelpTextsTest {

    private val en = File("src/main/res/values/strings.xml").readText()
    private val de = File("src/main/res/values-de/strings.xml").readText()

    private fun names(xml: String): Set<String> =
        Regex("""<string name="(\w+)"""").findAll(xml).map { it.groupValues[1] }.toSet()

    private fun valueOf(xml: String, name: String): String? =
        Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)

    @Test
    fun `jeder Modus hat eine Erklaerung, wo man gerade ist`() {
        // Fiele eine davon weg, staende beim Antippen des Avatars in genau einem Modus nichts -
        // und zwar nicht sichtbar leer, sondern mit einem Absturz beim Aufloesen der Ressource.
        for (name in listOf(
            "assistant_here_watch", "assistant_here_reminder", "assistant_here_play"
        )) {
            assertTrue("$name fehlt", name in names(en))
            assertTrue("$name fehlt auf Deutsch", name in names(de))
        }
    }

    @Test
    fun `jeder Modus wird in den Fragen und Antworten erklaert`() {
        for (name in listOf(
            "faq_a_modes", "faq_a_mode_watch", "faq_a_mode_reminder", "faq_a_mode_play"
        )) {
            assertTrue("$name fehlt", name in names(en))
        }
    }

    /**
     * Was absichtlich nur auf Englisch dasteht - und warum.
     *
     * Ausdruecklich aufgezaehlt statt ueber ein Namensmuster ausgenommen: Ein Muster wuerde
     * kuenftige Luecken gleich mit verstecken. Wer hier etwas eintraegt, muss es begruenden
     * koennen.
     */
    private val deliberatelyUntranslated = mapOf(
        // Der Produktname wird nicht uebersetzt.
        "app_name" to "Produktname",
        "home_title" to "Produktname",
        // Die sechs Kreaturen heissen in jeder Sprache gleich; ihre BESCHREIBUNGEN sind
        // uebersetzt, nur die Namen selbst nicht.
        "avatar_puffling" to "Eigenname",
        "avatar_starlet" to "Eigenname",
        "avatar_wyrmling" to "Eigenname",
        "avatar_fennec" to "Eigenname",
        "avatar_gloop" to "Eigenname",
        "avatar_hootlet" to "Eigenname"
    )

    @Test
    fun `beide Sprachen kennen dieselben Texte`() {
        val missingInGerman = (names(en) - names(de) - deliberatelyUntranslated.keys).sorted()
        assertEquals(
            "Diese Texte gibt es nur auf Englisch - entweder uebersetzen oder in " +
                "deliberatelyUntranslated eintragen und begruenden",
            emptyList<String>(), missingInGerman
        )

        val onlyGerman = (names(de) - names(en)).sorted()
        assertEquals("Diese Texte gibt es nur auf Deutsch", emptyList<String>(), onlyGerman)
    }

    /**
     * **Genau die Modi, die wirklich still sind, duerfen das auch sagen.**
     *
     * Ob eine Erinnerung kommt oder nicht, ist die Kernzusage dieser App - und der einzige Punkt,
     * an dem ein falscher Satz echten Schaden anrichtet. Wer glaubt, seine Erinnerungen ruhten,
     * rechnet nicht mehr mit ihnen und bekommt sie trotzdem; wer das Gegenteil glaubt, verlaesst
     * sich auf etwas, das nicht kommt.
     *
     * Der Stand nach Phase 5b:
     *
     * - **Spielmodus**: die eigenen Routinen laufen weiter, die gewuerfelte Zeile kommt hinzu
     *   (Phase 3). Ein Text, der hier "ruhen" oder "statt deiner Erinnerungen" sagt, ist falsch.
     * - **Uhr-Modus**: wirklich still, es wird gar nichts ausgeloest (Phase 5b, [QuietModeState]).
     *   Ein Text, der das VERSCHWEIGT, ist ebenso irrefuehrend - man wuerde stumme Stunden fuer
     *   einen Fehler halten.
     * - **Pause**: still, mit Ende (`ReminderPause`).
     *
     * Beide Richtungen werden geprueft. Als der Uhr-Modus noch nicht still war, hat genau die
     * erste Haelfte dieser Pruefung acht falsche Texte gefunden; als er es wurde, haette ohne die
     * zweite Haelfte niemand gemerkt, dass die Texte nun das Gegenteil verschweigen.
     */
    @Test
    fun `nur die wirklich stillen Modi behaupten Stille`() {
        val stilleBehauptet = listOf(
            Regex("""Erinnerungen ruhen"""),
            Regex("""eigenen ruhen"""),
            Regex("""[Ss]tatt deiner Erinnerungen"""),
            Regex("""übernehme ich .{0,20}die Erinnerungen""")
        )

        // Der Spielmodus legt nichts stumm.
        val spielTexte = names(de).filter {
            it.startsWith("playmode_intro") || it == "assistant_here_play" || it == "faq_a_mode_play"
        }
        for (name in spielTexte) {
            val text = valueOf(de, name) ?: continue
            for (behauptung in stilleBehauptet) {
                assertTrue(
                    "$name behauptet, der Spielmodus lege die eigenen Erinnerungen still " +
                        "(\"${behauptung.pattern}\") - seit Phase 3 laufen sie weiter",
                    !behauptung.containsMatchIn(text)
                )
            }
        }

        // Der Uhr-Modus tut es und muss es sagen.
        for (name in listOf("watch_only_note", "assistant_here_watch", "faq_a_mode_watch")) {
            val text = valueOf(de, name) ?: continue
            assertTrue(
                "$name muss sagen, dass im Uhr-Modus nichts kommt - sonst haelt man stumme " +
                    "Stunden fuer einen Fehler (siehe QuietModeState)",
                stilleBehauptet.any { it.containsMatchIn(text) }
            )
        }
    }

    @Test
    fun `die Hilfe verweist nicht mehr auf entfernte Bedienelemente`() {
        // Namentlich die drei, die es nicht mehr gibt: der Tagesstand-Streifen am oberen Rand,
        // das Zahnrad und der frueher in den Einstellungen ausgeloeste Mitschnitt. Geprueft wird
        // an den Texten, die tatsaechlich erklaeren - nicht an allen, sonst schlaegt es bei jeder
        // beilaeufigen Erwaehnung von "oben" an.
        val explaining = names(de).filter {
            it.startsWith("assistant_intro") || it.startsWith("faq_a_") || it.startsWith("assistant_here")
        }
        val gone = listOf("Zahnrad", "meine Mahlzeiten", "Mahlzeiten-Streifen")
        for (name in explaining) {
            val text = valueOf(de, name) ?: continue
            for (phrase in gone) {
                assertTrue(
                    "$name verweist auf \"$phrase\" - das gibt es nicht mehr",
                    !text.contains(phrase)
                )
            }
        }
    }
}
