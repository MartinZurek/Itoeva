package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.PlayPantry
import com.notime.glyphsim.matrix.PlayWallet
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * **Was der Avatar auf eine Frage antworten kann - und zwar ohne eine Zeile Text zu erfinden.**
 *
 * Jede Antwort hier ist eine Auskunft ueber Daten, die ohnehin schon vorliegen: was heute gefuettert
 * wurde, welche Erinnerungen eingerichtet sind, welche davon heute noch offen sind. Nichts davon
 * wird gerechnet, um eine Antwort zu haben - es wird nur endlich ZUGAENGLICH gemacht.
 *
 * **Warum das der bessere Weg ist als ein Sprachmodell.** Ein Wesen, das plaudert, sagt Dinge, die
 * es nicht wissen kann, und klingt dabei wie jedes andere Plauderprogramm. Ein Wesen, das auf
 * "Was hast du dir vorgenommen?" den tatsaechlichen Plan aufsagt, sagt etwas, das nur DIESER
 * Begleiter wissen kann. Das ist keine Sparversion einer Unterhaltung, sondern eine andere und
 * ehrlichere: Er weiss wenig, aber was er weiss, stimmt.
 *
 * **Der Punkt, an dem es rund wird**, ist [steering]: Offene Gewohnheiten beeinflussen laengst,
 * was der Avatar von sich aus tut (siehe [PlayHabitSignal] und
 * [com.notime.glyphsim.matrix.PlayAmbientActivity]) - nur konnte man das bisher nirgends ablesen.
 * Wer jetzt fragt, bekommt gesagt, worauf er heute achtet, und sieht ihn anschliessend genau das
 * tun. Erst dadurch wird aus einer verborgenen Gewichtung eine Beziehung.
 */
object PlayTalk {

    /** Was heute mit einer Erinnerung geschehen ist. */
    data class PlanEntry(
        val reminder: GlyphReminder,
        /** Wie oft heute darauf reagiert wurde. */
        val doneToday: Int
    ) {
        val goal: Int get() = reminder.dailyGoal
        val hasGoal: Boolean get() = goal > 0
        val reached: Boolean get() = hasGoal && doneToday >= goal
    }

    /**
     * Alles, was der Avatar ueber den heutigen Tag sagen kann.
     *
     * In EINEM Zug aus der Datenbank geholt statt je Frage neu: Der Nutzer stellt die Fragen
     * nacheinander, und Antworten, die sich zwischendurch widersprechen ("drei erledigt" / "nichts
     * erledigt"), waeren schlimmer als eine Sekunde Wartezeit am Anfang.
     */
    /**
     * Die Rueckschau ueber die laufende Woche.
     *
     * **Bewusst nur drei Zahlen, und keine davon ist eine Quote.** Eine Prozentangabe ueber eine
     * Woche liest sich wie ein Zeugnis, und ein Begleiter, der Zeugnisse ausstellt, ist kein
     * Begleiter mehr. "An vier von sieben Tagen war etwas" sagt dasselbe, ohne zu bewerten - und
     * [bestDay] gibt es ueberhaupt nur, damit die Rueckschau etwas Gutes zu berichten hat.
     */
    data class Week(
        /** Wie oft in dieser Woche insgesamt reagiert wurde. */
        val total: Int,
        /** An wievielen Tagen der Woche ueberhaupt etwas geschah. */
        val activeDays: Int,
        /** Wieviele Tage die Woche bisher hat - Montag zaehlt als ein Tag, nicht als sieben. */
        val daysSoFar: Int,
        /** Die hoechste Tageszahl der Woche. */
        val bestDay: Int
    )

    /**
     * Der Spielstand - nur im Spielmodus ueberhaupt gefuellt.
     *
     * **Warum die beiden Modi verschiedene Zahlen brauchen.** Im Normalbetrieb geht es um DICH:
     * deine Gewohnheiten, deine Tagesziele, deine Woche. Im Spiel geht es um IHN: wie weit er
     * gekommen ist, was er verdient hat, ob noch etwas zu essen da ist. Dieselben Zahlen fuer
     * beides zu zeigen war die eigentliche Unstimmigkeit - der Streifen am oberen Rand tat genau
     * das und blieb in beiden Modi derselbe, obwohl sie voellig Verschiedenes bedeuten.
     *
     * Und im Spiel sind diese Zahlen keine Auswertung, sondern eine ERKLAERUNG: Wer sieht, dass
     * der Vorrat leer ist, versteht, warum er gleich in den Laden geht.
     */
    data class Game(
        val level: Int,
        val xp: Int,
        val coins: Int,
        val pantry: Int
    ) {
        val pantryEmpty: Boolean get() = pantry <= 0
        val brokeAndHungry: Boolean get() = pantryEmpty && coins < PlayWallet.GROCERY_COST
    }

    data class Knowledge(
        val plan: List<PlanEntry>,
        /** Wie oft heute insgesamt reagiert wurde. */
        val fedToday: Int,
        /** Themen, die heute noch offen sind und deshalb seinen Tag mitbestimmen. */
        val steering: Set<AnimationType>,
        /** Themen, fuer die es noch gar keine Erinnerung gibt - daraus entstehen die Vorschlaege. */
        val missing: List<AnimationType>,
        /** Die laufende Woche - siehe [Week]. */
        val week: Week,
        /** Der Spielstand, oder `null` im Normalbetrieb - siehe [Game]. */
        val game: Game? = null
    ) {
        val goalsTotal: Int get() = plan.count { it.hasGoal }
        val goalsReached: Int get() = plan.count { it.reached }
        val hasPlan: Boolean get() = plan.isNotEmpty()
    }

    /**
     * Verteilt Zeitpunkte auf Kalendertage und verdichtet sie zur Wochen-Rueckschau.
     *
     * Ausgelagert und ohne Datenbank, damit sich genau die Faelle pruefen lassen, die sonst
     * niemand von Hand nachstellt: eine Woche, die erst am Mittwoch anfaengt; zwei Ereignisse am
     * selben Abend; ein Ereignis kurz vor Mitternacht.
     */
    fun summariseWeek(
        timestamps: List<Long>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Week {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val perDay = timestamps.groupingBy {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }.eachCount()
        return Week(
            total = timestamps.size,
            activeDays = perDay.count { (day, count) -> count > 0 && !day.isBefore(weekStart) },
            daysSoFar = (ChronoUnit.DAYS.between(weekStart, today).toInt() + 1).coerceIn(1, 7),
            bestDay = perDay.values.maxOrNull() ?: 0
        )
    }

    /**
     * Themen, die als Vorschlag taugen.
     *
     * Bewusst eine kurze, feste Liste statt "alles, was noch fehlt": Von zwoelf Themen sind die
     * meisten keine Gewohnheit, die man sich vornimmt (LOVE, GENERAL, SLEEP). Vorgeschlagen wird
     * nur, was man tatsaechlich regelmaessig tun kann - und wovon der Avatar im Spiel auch etwas
     * hat, weil es seinen Tagesablauf sichtbar veraendert.
     */
    val SUGGESTABLE = listOf(
        AnimationType.MOVE,
        AnimationType.DRINK,
        AnimationType.MINDFULNESS,
        AnimationType.FOCUS,
        AnimationType.BOOK
    )

    suspend fun gather(
        context: Context,
        profileId: String,
        /** Im Spielmodus kommt der Spielstand dazu - siehe [Game]. */
        includeGame: Boolean = false
    ): Knowledge {
        val db = AppDatabase.getInstance(context)
        val since = FeedStatsPeriod.TODAY.startMillis()

        // isPlayMode ausgeschlossen: Die Spiel-Erinnerung ist ein technisches Hilfsmittel und
        // gehoert nicht in einen Plan, den sich jemand vorgenommen hat.
        val reminders = db.glyphReminderDao().getEnabledForProfile(profileId)
            .filterNot { it.isPlayMode }
            .sortedBy { it.startMinuteOfDay }

        val fedByReminder = db.avatarFeedEventDao()
            .countFedPerReminderSince(profileId, since)
            .associate { it.reminderId to it.count }

        val plan = reminders.map { PlanEntry(it, fedByReminder[it.id] ?: 0) }
        val covered = reminders.map { it.animationType }.toSet()

        val weekStart = FeedStatsPeriod.WEEK.startMillis()
        val week = summariseWeek(
            db.avatarFeedEventDao().fedTimestampsSince(profileId, weekStart)
        )

        val game = if (includeGame) {
            // Die Erfahrung steht in der Datenbank, Geld und Vorrat in den Einstellungen - beides
            // hier zusammengefuehrt, damit das Gespraech nur EINE Quelle kennt.
            val xp = db.avatarPlayStateDao().getForProfile(profileId)?.xp ?: 0
            Game(
                level = PlayModeXp.levelFor(xp),
                xp = xp,
                coins = PlayWallet.coins(context),
                pantry = PlayPantry.level(context)
            )
        } else {
            null
        }

        return Knowledge(
            plan = plan,
            fedToday = fedByReminder.values.sum(),
            steering = PlayHabitSignal.underfulfilledTopics(context, profileId),
            missing = SUGGESTABLE.filterNot { it in covered },
            week = week,
            game = game
        )
    }

    /**
     * Der Vorschlag, den der Avatar von sich aus macht - hoechstens einer.
     *
     * **Einer, nicht fuenf.** Eine Liste von Vorschlaegen ist eine Liste von Vorwuerfen; ein
     * einzelner ist ein Angebot. Und er kommt nur, wenn es wirklich etwas anzubieten gibt.
     */
    fun nextSuggestion(knowledge: Knowledge): AnimationType? = knowledge.missing.firstOrNull()

    /**
     * Voreinstellungen fuer eine so angelegte Erinnerung: Zeitfenster, Abstand und Tagesziel.
     *
     * **Bewusst je Thema verschieden und bewusst zurueckhaltend.** Wer aus einem Gespraech heraus
     * etwas anlegt, will nicht anschliessend einen Dialog mit acht Feldern ausfuellen - er will,
     * dass es einfach da ist. Damit das gutgeht, muessen die Vorgaben so sein, dass sie niemanden
     * bedraengen: lieber ein Tagesziel zu niedrig als eine App, die einen den ganzen Tag anstupst.
     * Aendern laesst sich alles hinterher unter Erinnerungen.
     */
    data class Preset(
        val startMinuteOfDay: Int,
        val endMinuteOfDay: Int,
        val intervalMinutes: Int,
        val dailyGoal: Int
    )

    fun presetFor(topic: AnimationType): Preset = when (topic) {
        // Trinken: ueber den ganzen Tag verteilt, oft, kleines Ziel je Anstupser.
        AnimationType.DRINK -> Preset(8 * 60, 20 * 60, 90, 6)
        // Bewegung: einmal am Vormittag, einmal am spaeten Nachmittag.
        AnimationType.MOVE -> Preset(9 * 60, 18 * 60, 120, 2)
        // Stille: abends, selten, ein einziges Mal.
        AnimationType.MINDFULNESS -> Preset(18 * 60, 22 * 60, 120, 1)
        // Konzentration: im Arbeitsfenster.
        AnimationType.FOCUS -> Preset(9 * 60, 17 * 60, 120, 2)
        // Lesen: abends.
        AnimationType.BOOK -> Preset(19 * 60, 22 * 60, 60, 1)
        else -> Preset(9 * 60, 20 * 60, 120, 1)
    }

    /**
     * Alle Wochentage - eine Gewohnheit, die man sich vornimmt, gilt zunaechst taeglich.
     *
     * Ueber [DaysOfWeekMask] gerechnet statt als Zahl hingeschrieben: Die Bitfolge haengt an der
     * Nummerierung von [java.time.DayOfWeek], und eine handgeschriebene 127 waere genau die Art
     * Annahme, die still bricht, wenn sich an der Kodierung je etwas aendert.
     */
    val EVERY_DAY_MASK: Int = DaysOfWeekMask.toMask(java.time.DayOfWeek.entries.toSet())
}
