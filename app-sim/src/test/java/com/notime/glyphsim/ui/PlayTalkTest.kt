package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.PlayPantry
import com.notime.glyphsim.matrix.PlayWallet
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Vorgaben, mit denen aus einem Gespraech heraus eine Gewohnheit entsteht.
 *
 * **Warum das nicht nur Zahlen sind.** Wer im Gespraech "richte es ein" antippt, bekommt keine
 * Maske zum Nachjustieren - er bekommt eine fertige Erinnerung. Sitzt darin ein Tagesziel, das im
 * gewaehlten Zeitfenster gar nicht erreichbar ist, dann fordert die App etwas Unmoegliches, meldet
 * das nirgends und laesst den Nutzer taeglich scheitern. Das ist der unangenehmste Fehler, den
 * diese App ueberhaupt machen kann, und er faellt beim Lesen des Codes nicht auf: Alle vier Zahlen
 * sehen fuer sich vernuenftig aus, erst ihr Zusammenspiel entscheidet.
 */
class PlayTalkTest {

    @Test
    fun `jedes vorschlagbare Thema hat brauchbare Vorgaben`() {
        for (topic in PlayTalk.SUGGESTABLE) {
            val preset = PlayTalk.presetFor(topic)
            assertTrue(
                "$topic: Das Zeitfenster endet nicht nach seinem Anfang",
                preset.endMinuteOfDay > preset.startMinuteOfDay
            )
            assertTrue("$topic: Abstand muss positiv sein", preset.intervalMinutes > 0)
            assertTrue("$topic: ohne Tagesziel steuert es seinen Tag nicht", preset.dailyGoal > 0)
            assertTrue(
                "$topic: Zeitfenster liegt ausserhalb des Tages",
                preset.startMinuteOfDay >= 0 && preset.endMinuteOfDay <= 24 * 60
            )
        }
    }

    @Test
    fun `das Tagesziel ist im Zeitfenster ueberhaupt erreichbar`() {
        for (topic in PlayTalk.SUGGESTABLE) {
            val preset = PlayTalk.presetFor(topic)
            // Anzahl der Anstupser: Beim Fensterbeginn einer, danach alle intervalMinutes einer.
            val slots = (preset.endMinuteOfDay - preset.startMinuteOfDay) / preset.intervalMinutes + 1
            assertTrue(
                "$topic verlangt ${preset.dailyGoal} am Tag, kommt im Fenster aber nur $slots mal - " +
                    "das waere ein Ziel, das sich beim besten Willen nicht erreichen laesst.",
                preset.dailyGoal <= slots
            )
        }
    }

    @Test
    fun `Vorgaben lassen Luft, statt den Tag vollzustellen`() {
        // Die Gegenprobe zum Test darueber: Ein Ziel, das GENAU der Zahl der Anstupser entspricht,
        // waere zwar erreichbar, verlangte aber, dass wirklich jeder einzelne befolgt wird. Diese
        // App soll begleiten und nicht treiben - deshalb hoechstens drei Viertel.
        for (topic in PlayTalk.SUGGESTABLE) {
            val preset = PlayTalk.presetFor(topic)
            val slots = (preset.endMinuteOfDay - preset.startMinuteOfDay) / preset.intervalMinutes + 1
            assertTrue(
                "$topic: ${preset.dailyGoal} von $slots moeglichen ist zu eng getaktet",
                preset.dailyGoal <= slots * 3 / 4
            )
        }
    }

    @Test
    fun `eine neue Gewohnheit gilt an allen sieben Tagen`() {
        assertEquals(7, DaysOfWeekMask.toSet(PlayTalk.EVERY_DAY_MASK).size)
    }

    @Test
    fun `vorgeschlagen wird nur, was noch fehlt - und hoechstens eines`() {
        val nothingMissing = PlayTalk.Knowledge(
            plan = emptyList(), fedToday = 0, steering = emptySet(), missing = emptyList(),
            week = PlayTalk.Week(0, 0, 1, 0)
        )
        assertNull(
            "Ohne Luecke darf er nichts vorschlagen - sonst wird aus dem Angebot eine Ermahnung",
            PlayTalk.nextSuggestion(nothingMissing)
        )

        val twoMissing = PlayTalk.Knowledge(
            plan = emptyList(), fedToday = 0, steering = emptySet(),
            missing = listOf(AnimationType.MOVE, AnimationType.DRINK),
            week = PlayTalk.Week(0, 0, 1, 0)
        )
        assertEquals(AnimationType.MOVE, PlayTalk.nextSuggestion(twoMissing))
    }

    @Test
    fun `vorgeschlagen wird nur, was der Avatar auch sichtbar tut`() {
        // Ein Vorschlag, der seinen Tagesablauf nicht veraendert, waere eine leere Zusage: Der
        // Nutzer legt etwas an, weil der Begleiter danach gefragt hat, und sieht anschliessend
        // keinen Unterschied. Jedes vorschlagbare Thema muss deshalb einen eigenen Ablauf haben.
        for (topic in PlayTalk.SUGGESTABLE) {
            val routines = com.notime.glyphsim.matrix.PlayRoutines.allFor(topic)
            assertTrue("$topic hat keinen Tagesablauf", routines.isNotEmpty())
            assertTrue(
                "$topic fuehrt zu keinem Ablauf, in dem sich etwas bewegt",
                routines.any { it.steps.size > 1 }
            )
        }
    }

    // ---- Rueckschau auf die Woche ----

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    /** Ein Zeitpunkt an einem bestimmten Tag zu einer bestimmten Stunde. */
    private fun at(date: LocalDate, hour: Int): Long =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `die Woche zaehlt Tage, nicht Ereignisse`() {
        // Mittwoch: Die Woche hat bisher drei Tage, nicht sieben. Wer am Mittwoch fragt, soll
        // nicht lesen "an 2 von 7 Tagen" und sich fuer vier Tage schaemen, die es noch gar
        // nicht gibt.
        val wednesday = LocalDate.of(2026, 8, 5)
        val monday = LocalDate.of(2026, 8, 3)

        val week = PlayTalk.summariseWeek(
            listOf(at(monday, 9), at(monday, 18), at(wednesday, 8)),
            today = wednesday, zone = zone
        )

        assertEquals("drei Ereignisse", 3, week.total)
        assertEquals("an zwei Tagen", 2, week.activeDays)
        assertEquals("Montag bis Mittwoch sind drei Tage", 3, week.daysSoFar)
        assertEquals("der Montag war der beste Tag", 2, week.bestDay)
    }

    @Test
    fun `mehrere Ereignisse am selben Abend sind EIN Tag`() {
        val friday = LocalDate.of(2026, 8, 7)
        val week = PlayTalk.summariseWeek(
            List(5) { at(friday, 20) }, today = friday, zone = zone
        )
        assertEquals(5, week.total)
        assertEquals("fuenfmal am selben Abend bleibt ein Tag", 1, week.activeDays)
    }

    @Test
    fun `kurz vor Mitternacht gehoert noch zum selben Tag`() {
        // Die Falle, wegen der die Tagesgrenze in der Zeitzone des Nutzers gezogen wird und nicht
        // in SQL: In UTC gerechnet fiele 23 Uhr deutscher Zeit bereits auf den Folgetag, und die
        // Woche haette stillschweigend einen Tag zu viel.
        val tuesday = LocalDate.of(2026, 8, 4)
        val week = PlayTalk.summariseWeek(
            listOf(at(tuesday, 9), at(tuesday, 23)), today = tuesday, zone = zone
        )
        assertEquals("ein Tag, nicht zwei", 1, week.activeDays)
    }

    @Test
    fun `am Montag hat die Woche genau einen Tag`() {
        val monday = LocalDate.of(2026, 8, 3)
        val week = PlayTalk.summariseWeek(listOf(at(monday, 10)), today = monday, zone = zone)
        assertEquals(1, week.daysSoFar)
        assertEquals(1, week.activeDays)
    }

    @Test
    fun `eine leere Woche meldet Nullen statt zu stolpern`() {
        val sunday = LocalDate.of(2026, 8, 9)
        val week = PlayTalk.summariseWeek(emptyList(), today = sunday, zone = zone)
        assertEquals(0, week.total)
        assertEquals(0, week.activeDays)
        assertEquals(0, week.bestDay)
        assertEquals("Montag bis Sonntag sind sieben Tage", 7, week.daysSoFar)
    }

    // ---- Die Stimme der sechs Kreaturen ----

    @Test
    fun `jede Kreatur hat ihre eigene Stimme`() {
        // Sechs Stimmen, die versehentlich auf dieselben Texte zeigen, waeren schlimmer als gar
        // keine: Der Aufwand waere da, die Wirkung nicht, und niemand wuerde es bemerken.
        val voices = AvatarSpecies.entries.map { PlayVoice.forSpecies(it) }
        assertEquals(
            "zwei Kreaturen begruessen mit demselben Text",
            AvatarSpecies.entries.size, voices.map { it.greeting }.toSet().size
        )
        assertEquals(
            "zwei Kreaturen troesten mit demselben Text",
            AvatarSpecies.entries.size, voices.map { it.emptyDay }.toSet().size
        )
        assertEquals(
            "zwei Kreaturen freuen sich mit demselben Text",
            AvatarSpecies.entries.size, voices.map { it.allDone }.toSet().size
        )
    }

    @Test
    fun `keine Stimme laesst eine Zeile offen`() {
        // Eine nicht gesetzte Ressourcen-Nummer waere 0 und faellt erst beim Anzeigen auf - dann
        // aber mit einem Absturz mitten im Gespraech.
        for (species in AvatarSpecies.entries) {
            val voice = PlayVoice.forSpecies(species)
            for ((name, res) in listOf(
                "greeting" to voice.greeting,
                "emptyDay" to voice.emptyDay,
                "allDone" to voice.allDone,
                "offering" to voice.offering,
                "farewell" to voice.farewell
            )) {
                assertTrue("$species hat keinen Text fuer $name", res != 0)
            }
        }
    }

    // ---- Spielstand: die Zahlen, die nur im Spiel etwas bedeuten ----

    private fun knowledge(game: PlayTalk.Game?) = PlayTalk.Knowledge(
        plan = emptyList(), fedToday = 0, steering = emptySet(), missing = emptyList(),
        week = PlayTalk.Week(0, 0, 1, 0), game = game
    )

    @Test
    fun `ohne Spiel gibt es keinen Spielstand`() {
        // Der Normalbetrieb fuehrt weder Stufe noch Muenzen. Eine 0 anzuzeigen waere schlimmer
        // als nichts: Sie sieht aus wie ein Ergebnis, obwohl gar nicht gespielt wird.
        assertNull(knowledge(null).game)
    }

    @Test
    fun `leerer Vorrat mit Geld heisst einkaufen, ohne Geld heisst arbeiten`() {
        // Diese Unterscheidung ist der Grund, warum der Spielstand ueberhaupt erklaert wird: Sie
        // sagt voraus, was er als naechstes tut. Steht sie falsch herum, schickt das Gespraech
        // ihn in den Laden, obwohl er dort nichts bezahlen kann - und er kaeme mit leeren Haenden
        // zurueck.
        val canShop = PlayTalk.Game(level = 1, xp = 0, coins = PlayWallet.GROCERY_COST, pantry = 0)
        assertTrue("leerer Vorrat wird nicht erkannt", canShop.pantryEmpty)
        assertTrue("mit genug Geld ist er nicht mittellos", !canShop.brokeAndHungry)

        val broke = PlayTalk.Game(level = 1, xp = 0, coins = PlayWallet.GROCERY_COST - 1, pantry = 0)
        assertTrue("mittellos und hungrig wird nicht erkannt", broke.brokeAndHungry)
    }

    @Test
    fun `voller Vorrat ist nie ein Notfall`() {
        val fine = PlayTalk.Game(level = 3, xp = 120, coins = 0, pantry = PlayPantry.FULL)
        assertTrue(!fine.pantryEmpty)
        assertTrue("ohne Hunger ist auch kein Geld noetig", !fine.brokeAndHungry)
    }

    // ---- Was er von sich aus sagt ----

    private fun known(
        plan: List<PlayTalk.PlanEntry> = emptyList(),
        fedToday: Int = 0,
        steering: Set<AnimationType> = emptySet(),
        missing: List<AnimationType> = emptyList(),
        week: PlayTalk.Week = PlayTalk.Week(0, 0, 1, 0),
        game: PlayTalk.Game? = null
    ) = PlayTalk.Knowledge(plan, fedToday, steering, missing, week, game)

    /** Eine Erinnerung, damit `hasPlan` stimmt - der Inhalt spielt hier keine Rolle. */
    private fun somePlan(goal: Int = 1, done: Int = 0) = listOf(
        PlayTalk.PlanEntry(
            reminder = GlyphReminder(
                id = 1, label = "Test", animationType = AnimationType.DRINK,
                daysOfWeekMask = PlayTalk.EVERY_DAY_MASK,
                startMinuteOfDay = 8 * 60, endMinuteOfDay = 20 * 60,
                intervalMinutes = 60, dailyGoal = goal
            ),
            doneToday = done
        )
    )

    @Test
    fun `hoechstens zwei Angebote - sonst ist es wieder ein Menue`() {
        // Der Grund, warum es diese Funktion ueberhaupt gibt: Sechs gleich aussehende Zeilen
        // sahen aus wie ein Menue. Waechst die Zahl je wieder, ist genau das zurueck.
        val everything = known(
            plan = somePlan(goal = 3, done = 0),
            fedToday = 5,
            steering = setOf(AnimationType.MOVE, AnimationType.DRINK, AnimationType.BOOK),
            missing = listOf(AnimationType.FOCUS, AnimationType.MINDFULNESS),
            week = PlayTalk.Week(total = 20, activeDays = 4, daysSoFar = 5, bestDay = 8),
            game = PlayTalk.Game(level = 3, xp = 120, coins = 0, pantry = 0)
        )
        assertTrue(
            "zu viele Angebote: ${PlayTalk.focus(everything).offers.size}",
            PlayTalk.focus(everything).offers.size <= 2
        )
    }

    @Test
    fun `die dringendste Lage kommt zuerst`() {
        // Mittellos und hungrig geht jeder Statistik vor - es erklaert, was gleich zu sehen ist.
        val broke = known(
            plan = somePlan(),
            steering = setOf(AnimationType.MOVE),
            game = PlayTalk.Game(level = 1, xp = 0, coins = 0, pantry = 0)
        )
        val focus = PlayTalk.focus(broke)
        assertEquals(PlayTalk.Headline.BROKE, focus.headline)
        assertEquals(
            "Bei leerem Beutel muss die Arbeit das erste Angebot sein",
            PlayTalk.Offer.Ask(AnimationType.WORK), focus.offers.first()
        )
    }

    @Test
    fun `eine offene Gewohnheit wird zur Bitte, nicht zur Aufzaehlung`() {
        val open = known(
            plan = somePlan(),
            steering = setOf(AnimationType.MOVE, AnimationType.DRINK, AnimationType.BOOK)
        )
        val focus = PlayTalk.focus(open)
        assertEquals(PlayTalk.Headline.OPEN_TOPICS, focus.headline)
        val asks = focus.offers.filterIsInstance<PlayTalk.Offer.Ask>()
        assertEquals("Nur EINE Bitte, sonst ist die Liste wieder da", 1, asks.size)
    }

    @Test
    fun `ohne Plan bietet er nichts an, was einen Plan voraussetzt`() {
        // Ein "Zeig mir den Plan", wenn es keinen gibt, waere eine Zeile, die ins Leere fuehrt.
        val fresh = known()
        val focus = PlayTalk.focus(fresh)
        assertEquals(PlayTalk.Headline.NO_PLAN, focus.headline)
        assertTrue(
            "ohne Plan darf der Plan nicht angeboten werden",
            focus.offers.none { it is PlayTalk.Offer.ShowPlan }
        )
    }

    @Test
    fun `eine leere Woche wird nicht angeboten`() {
        val quiet = known(plan = somePlan(goal = 0), fedToday = 0)
        assertTrue(
            "eine Woche ohne einen einzigen Eintrag ist keine Rueckschau wert",
            PlayTalk.focus(quiet).offers.none { it is PlayTalk.Offer.ShowWeek }
        )
    }

    @Test
    fun `jede Lage bekommt eine Aussage - keine bleibt stumm`() {
        // Faellt eine Kombination durch alle Zweige, staende das Feld leer da. Deshalb hier
        // einmal quer durch die Faelle.
        val cases = listOf(
            known(),
            known(plan = somePlan()),
            known(plan = somePlan(), fedToday = 3),
            known(plan = somePlan(goal = 1, done = 1), fedToday = 1),
            known(plan = somePlan(goal = 0), fedToday = 2),
            known(plan = somePlan(), game = PlayTalk.Game(1, 0, 5, 3)),
            known(plan = somePlan(), game = PlayTalk.Game(1, 0, 0, 0))
        )
        for (case in cases) {
            assertTrue("keine Aussage fuer $case", PlayTalk.focus(case).headline != null)
        }
    }
}
