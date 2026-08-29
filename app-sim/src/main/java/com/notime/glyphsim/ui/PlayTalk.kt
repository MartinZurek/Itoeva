package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.CompanionChapter
import com.notime.glyphsim.matrix.PlayWeather
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.PlayPantry
import com.notime.glyphsim.matrix.PlayScene
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

        /**
         * Wie weit er in der laufenden Stufe ist - nicht die Gesamt-Erfahrung.
         *
         * Eine Zahl wie "180 Erfahrung" sagt niemandem etwas; "30 von 50 bis zur naechsten Stufe"
         * schon, und erst daraus laesst sich ein Balken zeichnen. Der Fortschritt ist das, was
         * man sehen will - nicht die Summe.
         */
        val xpInLevel: Int get() = xp % PlayModeXp.XP_PER_LEVEL
        val xpPerLevel: Int get() = PlayModeXp.XP_PER_LEVEL
        val progress: Float get() = xpInLevel.toFloat() / xpPerLevel
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
        val game: Game? = null,
        /**
         * Wie weit ihr beide seid (siehe [CompanionChapter]).
         *
         * Steht bewusst NEBEN den Zahlen und nicht zwischen ihnen: Alles andere in dieser Klasse
         * beschreibt, was heute oder diese Woche passiert ist. Das Kapitel beschreibt gar nichts
         * davon - es haengt allein daran, wie lange ihr euch kennt, und laesst sich weder
         * verbessern noch verlieren.
         */
        val chapter: CompanionChapter = CompanionChapter.ARRIVED,
        /** Was ihm an den bestehenden Erinnerungen auffaellt - siehe [adviceFor]. */
        val advice: List<Advice> = emptyList(),
        /** Serie, Vorwoche, Monat - siehe [History]. */
        val history: History = History(0, 0, 0, 0),
        /** Was du ihm ueber dich gesagt hast - siehe [UserAnswers]. */
        val answers: UserAnswers? = null,
        /** Ob er ueber sich selbst noch etwas zu erzaehlen hat - siehe [PlayLore]. */
        val hasMoreToTell: Boolean = false,
        /** Wohin er sich entwickelt - siehe [Development]. */
        val development: Development? = null
    ) {
        val goalsTotal: Int get() = plan.count { it.hasGoal }
        val goalsReached: Int get() = plan.count { it.reached }
        val hasPlan: Boolean get() = plan.isNotEmpty()
    }

    /**
     * **Nur die Entwicklung** - ohne den ganzen Rest der Auskunft.
     *
     * Noetig, weil der Pfad faerbt, was das Wesen von sich aus TUT (siehe DockScreen), und das
     * schon, bevor jemand das Gespraech oeffnet. Die vollstaendige Auskunft dafuer zu laden waere
     * die falsche Abkuerzung: Sie zieht Wochenrueckschau, Ratschlaege und Spielstand mit, von
     * denen hier nichts gebraucht wird - und sie liefe bei jeder Regung mit.
     */
    suspend fun developmentNow(context: Context, companionProfileId: String): Development {
        val db = AppDatabase.getInstance(context)
        val reminders = db.glyphReminderDao().getEnabledForProfile(RoutineOwner.current(context))
            .filterNot { it.isPlayMode }
        val fedEver = db.avatarFeedEventDao()
            .countFedPerReminderSince(companionProfileId, 0L)
            .associate { it.reminderId to it.count }
        val xp = db.avatarPlayStateDao().getForProfile(companionProfileId)?.xp ?: 0
        return developmentOf(
            plan = reminders.map { PlanEntry(it, 0) },
            fedEver = fedEver,
            level = PlayModeXp.levelFor(xp)
        )
    }

    /**
     * Verdichtet Antworten je Erinnerung zu Antworten je THEMA und leitet daraus den Pfad ab.
     *
     * Der Umweg ueber die Erinnerungen ist noetig, weil am Ereignis selbst nicht immer ein Thema
     * steht: Zeigt eine Erinnerung eine Bibliotheks-Animation, bleibt das Feld dort leer (siehe
     * ReminderTrigger). Die Erinnerung des Nutzers weiss ihr Thema dagegen immer - und sie ist
     * ohnehin das, worum es hier geht: was er sich vorgenommen hat.
     */
    private fun developmentOf(
        plan: List<PlanEntry>,
        fedEver: Map<Long, Int>,
        level: Int
    ): Development {
        val counts = mutableMapOf<AnimationType, Int>()
        for (entry in plan) {
            val type = entry.reminder.animationType ?: continue
            counts[type] = (counts[type] ?: 0) + (fedEver[entry.reminder.id] ?: 0)
        }
        val path = PlayPath.pathFor(counts)
        return Development(
            path = path,
            stage = PlayPath.stageFor(level),
            sharePercent = path?.let { (PlayPath.shareOf(counts, it) * 100).toInt() } ?: 0,
            enoughData = PlayPath.enoughData(counts),
            nextStageLevel = PlayPath.nextStageLevel(level)
        )
    }

    /**
     * **Ein Rat zu einer Erinnerung, die es schon gibt.**
     *
     * Bis hierher konnte der Begleiter genau eine Sache vorschlagen: eine NEUE Gewohnheit
     * anzulegen (siehe [Offer.Add]). Über das, was bereits eingerichtet ist, sagte er nie etwas -
     * dabei liegt genau dort das Wissen, das nur er hat: Er sieht jede Ausloesung und jede
     * Reaktion. Wer sich ein Tagesziel von sechs gesetzt hat und seit einer Woche bei zwei landet,
     * hat kein Willensproblem, sondern eine Einstellung, die nicht passt. Das zu sagen ist der
     * Unterschied zwischen einem Begleiter und einem Zaehlwerk.
     *
     * **Jeder Rat kommt mit einer fertigen Aenderung.** Ein Hinweis, der den Nutzer anschliessend
     * in eine Maske mit acht Feldern schickt, ist keine Hilfe - er ist eine Hausaufgabe. Deshalb
     * traegt jeder Fall bereits die geaenderte Erinnerung bei sich; annehmen ist ein Tippen.
     */
    sealed interface Advice {
        val reminder: GlyphReminder

        /** Wie die Erinnerung nach dem Rat aussaehe - oder `null`, wenn es nichts zu aendern gibt. */
        val changed: GlyphReminder?

        /**
         * Das Tagesziel wird seit Tagen nicht erreicht, aber es wird durchaus reagiert.
         *
         * [typical] ist die Zahl, die tatsaechlich zusammenkommt - der ehrliche Vorschlag ist
         * genau die, nicht eine erfundene Mitte.
         */
        data class LowerGoal(
            override val reminder: GlyphReminder,
            val typical: Int
        ) : Advice {
            override val changed: GlyphReminder get() = reminder.copy(dailyGoal = typical)
        }

        /** Das Ziel wird jeden Tag uebertroffen - es darf ruhig hoeher. */
        data class RaiseGoal(
            override val reminder: GlyphReminder,
            val typical: Int
        ) : Advice {
            override val changed: GlyphReminder get() = reminder.copy(dailyGoal = typical)
        }

        /**
         * Reagiert wird nur in einem Teil des Zeitfensters - der Rest stupst ins Leere.
         *
         * [fromHour]/[toHour] sind die Stunden, in denen tatsaechlich etwas passiert.
         */
        data class ShiftWindow(
            override val reminder: GlyphReminder,
            val fromHour: Int,
            val toHour: Int
        ) : Advice {
            override val changed: GlyphReminder get() = reminder.copy(
                startMinuteOfDay = fromHour * 60,
                endMinuteOfDay = ((toHour + 1) * 60).coerceAtMost(24 * 60 - 1)
            )
        }

        /**
         * Die Erinnerung wird durchweg uebergangen.
         *
         * **Der einzige Rat ohne fertige Aenderung**, und das mit Absicht: Ob etwas weg soll, ist
         * keine Rechenfrage. Er sagt, was er sieht, und ueberlaesst den Schluss dem Nutzer.
         */
        data class Ignored(
            override val reminder: GlyphReminder,
            val triggered: Int
        ) : Advice {
            override val changed: GlyphReminder? get() = null
        }
    }

    /**
     * **Wohin sich das Wesen entwickelt** (siehe [PlayPath]) - `null` im Normalbetrieb, wo es
     * kein Spiel und damit keine Stufe gibt.
     */
    data class Development(
        /** Der Pfad, oder `null`: dann sagt [enoughData], ob es an den Daten oder an der
         *  Gleichmaessigkeit liegt. */
        val path: PlayPath?,
        val stage: Int,
        /** Welchen Anteil der Pfad an allen Antworten hat, in Prozent. */
        val sharePercent: Int,
        /** Ob ueberhaupt genug beantwortet wurde. */
        val enoughData: Boolean,
        /** Die Stufe des Wesens, ab der die naechste Pfadstufe kommt - `null` am Ende. */
        val nextStageLevel: Int?
    )

    /**
     * **Was ER dich fragt** - der Teil des Gespraechs, in dem die Auskunft die Richtung wechselt.
     *
     * Bis hierher lief alles in eine Richtung: Er berichtete, der Nutzer las. Ein Gegenueber ist
     * aber jemand, der auch etwas wissen will. Zwei Fragen, und beide muessen sich AUSWIRKEN -
     * eine Antwort, die folgenlos in einer Datei liegt, ist eine Umfrage, und eine Umfrage ist das
     * Gegenteil eines Gespraechs (siehe [PlayUserProfile] fuer die Wirkung).
     *
     * Bewusst zwei und nicht zehn: Jede weitere waere eine Frage, deren Antwort nichts mehr
     * aendert - und damit genau die Sorte Fragebogen, die man wegtippt.
     */
    enum class Ask {
        /** "Worauf soll ich besonders achten?" - waehlbar sind Themen, die er sichtbar tut. */
        FOCUS,

        /** "Wann hast du am ehesten Zeit?" - verschiebt die Zeitfenster neuer Erinnerungen. */
        TIME,

        /**
         * "Wozu brauchst du mich eigentlich?" - die einzige Frage, die nach dem GRUND fragt statt
         * nach einer Einstellung.
         *
         * Sie aendert die Reihenfolge dessen, was er vorschlaegt (siehe [suggestableFor]): Wer
         * sagt "damit ich zur Ruhe komme", bekommt nicht als erstes Bewegung angeboten. Damit ist
         * auch sie folgenreich - sonst duerfte sie nicht gestellt werden.
         */
        PURPOSE,

        /**
         * "Auch am Wochenende?" - entscheidet ueber die Wochentage neuer Erinnerungen.
         *
         * Die unscheinbarste der vier und die mit der handfestesten Wirkung: Wer am Samstag um
         * neun angestupst wird, obwohl er ausschlafen wollte, schaltet die App ab.
         */
        WEEKEND
    }

    /**
     * **Wozu jemand einen Begleiter benutzt** - die Antwort auf [Ask.PURPOSE].
     *
     * Bewusst drei Moeglichkeiten und keine Freitextzeile: Drei kann er auseinanderhalten und in
     * eine Reihenfolge uebersetzen. Ein Satz, den er nicht versteht, waere eine Umfrage.
     */
    enum class Purpose { HEALTH, CALM, STRUCTURE }

    /**
     * Was er in welcher Reihenfolge vorschlaegt - abhaengig davon, wozu der Nutzer ihn benutzt.
     *
     * Dieselben fuenf Themen wie immer (siehe [SUGGESTABLE]), nur anders sortiert. Keines faellt
     * weg: Wer sich fuer Ruhe entschieden hat, darf trotzdem irgendwann Bewegung angeboten
     * bekommen - nur eben nicht als erstes.
     */
    fun suggestableFor(purpose: Purpose?): List<AnimationType> = when (purpose) {
        Purpose.HEALTH -> listOf(
            AnimationType.DRINK, AnimationType.MOVE, AnimationType.FOCUS,
            AnimationType.MINDFULNESS, AnimationType.BOOK
        )
        Purpose.CALM -> listOf(
            AnimationType.MINDFULNESS, AnimationType.BOOK, AnimationType.DRINK,
            AnimationType.MOVE, AnimationType.FOCUS
        )
        Purpose.STRUCTURE -> listOf(
            AnimationType.FOCUS, AnimationType.MOVE, AnimationType.DRINK,
            AnimationType.BOOK, AnimationType.MINDFULNESS
        )
        null -> SUGGESTABLE
    }

    /**
     * **Was er ueber dich weiss** - die Antworten, die du ihm gegeben hast (siehe
     * [PlayUserProfile]).
     *
     * Es gibt sie als eigenen Typ, damit er sie AUFSAGEN kann. Ein Begleiter, der Antworten
     * sammelt und sie danach nur noch heimlich verwendet, ist ein Formular mit Augen; einer, der
     * auf Nachfrage wiedergibt, was er sich gemerkt hat - und es auf Wunsch wieder vergisst -,
     * ist ein Gegenueber.
     */
    data class UserAnswers(
        val focusTopic: AnimationType?,
        val busyPhase: PlayAmbientActivity.DayPhase?,
        val purpose: Purpose?,
        val includesWeekend: Boolean,
        /** Ob ueberhaupt schon etwas beantwortet wurde. */
        val anyGiven: Boolean
    )

    /**
     * Die naechste Frage, die er stellen wuerde - `null`, wenn er alles gefragt hat.
     *
     * **Er fragt erst, wenn er etwas ueber dich weiss.** Beim allerersten Oeffnen kaeme die Frage
     * "worauf soll ich achten" von jemandem, der einen noch gar nicht kennt - das liest sich wie
     * ein Anmeldeformular. Deshalb erst, wenn ein Plan da ist und die Woche etwas hergibt.
     */
    fun pendingAsk(
        knowledge: Knowledge,
        alreadyAsked: Set<Ask>
    ): Ask? {
        if (!knowledge.hasPlan) return null
        return Ask.entries.firstOrNull { it !in alreadyAsked }
    }

    /** Eine Ausloesung, wie sie in die Auswertung eingeht. */
    data class Occurrence(val reminderId: Long, val epochMillis: Long, val fed: Boolean)

    /**
     * Wieviele Ausloesungen es mindestens braucht, bevor ueberhaupt geraten wird.
     *
     * **Der wichtigste Wert hier.** Ein Begleiter, der nach zwei Tagen anfaengt, Einstellungen
     * umzuwerfen, ist zudringlich - und er hat schlicht nicht genug gesehen, um recht zu haben.
     */
    private const val MIN_OCCURRENCES = 6

    /** Und so viele TAGE mit Reaktionen, bevor ueber ein Tagesziel geredet wird. */
    private const val MIN_ACTIVE_DAYS = 3

    /**
     * Was ihm an den bestehenden Erinnerungen auffaellt - hoechstens einer je Erinnerung, in der
     * Reihenfolge ihrer Dringlichkeit.
     *
     * Rein rechnerisch und ohne Datenbank, damit sich die Faelle mit erfundenen Wochen pruefen
     * lassen: Genau hier entscheidet sich, ob ein Rat hilfreich oder anmassend ist, und das laesst
     * sich nicht am Geraet ausprobieren - man muesste eine Woche lang echte Daten erzeugen.
     */
    fun adviceFor(
        plan: List<PlanEntry>,
        occurrences: List<Occurrence>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Advice> {
        val byReminder = occurrences.groupBy { it.reminderId }
        return plan.mapNotNull { entry ->
            val own = byReminder[entry.reminder.id].orEmpty()
            if (own.size < MIN_OCCURRENCES) return@mapNotNull null
            val fed = own.filter { it.fed }

            // Uebergangen: die dringendste Beobachtung, weil hier gar nichts wirkt.
            if (fed.isEmpty()) return@mapNotNull Advice.Ignored(entry.reminder, own.size)

            val perDay = fed.groupingBy {
                Instant.ofEpochMilli(it.epochMillis).atZone(zone).toLocalDate()
            }.eachCount()
            val activeDays = perDay.size
            val typical = perDay.values.sorted()[perDay.size / 2]

            if (activeDays < MIN_ACTIVE_DAYS) return@mapNotNull null
            val reachedDays = if (entry.hasGoal) perDay.count { it.value >= entry.goal } else 0

            // **Das unerreichbare Ziel geht dem zu weiten Fenster vor.** Beides kann gleichzeitig
            // zutreffen, und die Reihenfolge ist eine inhaltliche Entscheidung: Ein Fenster, das
            // zur Haelfte ins Leere stupst, kostet ein paar unbeachtete Anstupser. Ein Ziel, das
            // nie aufgeht, laesst jemanden jeden Abend an sich selbst zweifeln - obwohl nur eine
            // Zahl zu hoch steht.
            if (entry.hasGoal && reachedDays == 0 && typical in 1 until entry.goal) {
                return@mapNotNull Advice.LowerGoal(entry.reminder, typical)
            }

            // Zeitfenster: Reagiert wird nur in einem Teil davon.
            val hours = fed.map { Instant.ofEpochMilli(it.epochMillis).atZone(zone).hour }
            val fromHour = hours.min()
            val toHour = hours.max()
            val windowHours = (entry.reminder.endMinuteOfDay - entry.reminder.startMinuteOfDay) / 60
            val usedHours = toHour - fromHour + 1
            if (windowHours - usedHours >= WINDOW_SLACK_HOURS) {
                return@mapNotNull Advice.ShiftWindow(entry.reminder, fromHour, toHour)
            }

            // Jeden Tag uebertroffen - das Ziel darf hoeher.
            if (entry.hasGoal && reachedDays == activeDays && typical > entry.goal) {
                Advice.RaiseGoal(entry.reminder, typical)
            } else {
                null
            }
        }
    }

    /** Ab wieviel ungenutzter Fensterbreite (in Stunden) er das Fenster anspricht. */
    private const val WINDOW_SLACK_HOURS = 4

    /**
     * **Die laengere Rueckschau** - was sich ueber die Woche hinaus sagen laesst.
     *
     * Bewusst NEBEN [Week] und nicht darin: Die Woche ist die Auskunft auf "wie lief es", diese
     * hier ist die Antwort auf "und sonst so". Wer nur wissen will, wie die Woche stand, soll
     * nicht vier weitere Zahlen mitlesen muessen.
     *
     * **Und weiterhin keine Quote.** Auch ueber einen Monat gerechnet wird hier nichts bewertet -
     * "an 18 von 30 Tagen war etwas" sagt alles, was zu sagen ist, ohne eine Note zu vergeben.
     */
    data class History(
        /** Wie oft in der VORHERIGEN Woche reagiert wurde - der einzige Vergleich, den es gibt. */
        val previousWeekTotal: Int,
        /**
         * Tage in Folge mit mindestens einer Reaktion, bis heute.
         *
         * **Der heutige Tag zaehlt nicht gegen die Serie, solange er noch laeuft.** Wer morgens um
         * neun nachsieht, hat heute naturgemaess noch nichts getan - eine Serie, die dadurch auf
         * null faellt, waere schlicht falsch und obendrein entmutigend. Gezaehlt wird deshalb ab
         * gestern rueckwaerts, und der heutige Tag verlaengert sie, wenn schon etwas da ist.
         */
        val streakDays: Int,
        /** Reaktionen der letzten dreissig Tage. */
        val monthTotal: Int,
        /** An wievielen dieser Tage ueberhaupt etwas geschah. */
        val monthActiveDays: Int
    ) {
        /** Ob die laufende Woche besser steht als die vorherige - `null`, wenn es keine gab. */
        fun betterThanLastWeek(thisWeek: Int): Boolean? =
            if (previousWeekTotal <= 0) null else thisWeek > previousWeekTotal
    }

    /** Wie weit die lange Rueckschau zurueckreicht. */
    const val HISTORY_DAYS = 30

    /**
     * Rechnet Serie, Vorwoche und Monat aus denselben Zeitpunkten, aus denen auch [summariseWeek]
     * die Woche zieht.
     *
     * Eine eigene reine Funktion aus demselben Grund wie dort: Eine Serie ueber Tagesgrenzen
     * hinweg ist genau die Art Rechnung, die von Hand nie richtig ausprobiert wird - man muesste
     * dafuer drei Tage lang die App benutzen und dann einen Tag aussetzen.
     */
    fun summariseHistory(
        timestamps: List<Long>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): History {
        val perDay = timestamps.groupingBy {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }.eachCount()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val previousStart = weekStart.minusWeeks(1)
        val monthStart = today.minusDays(HISTORY_DAYS - 1L)

        var streak = 0
        // Ab heute, falls heute schon etwas war - sonst ab gestern (siehe [History.streakDays]).
        var cursor = if ((perDay[today] ?: 0) > 0) today else today.minusDays(1)
        while ((perDay[cursor] ?: 0) > 0) {
            streak++
            cursor = cursor.minusDays(1)
        }

        return History(
            previousWeekTotal = perDay.entries
                .filter { !it.key.isBefore(previousStart) && it.key.isBefore(weekStart) }
                .sumOf { it.value },
            streakDays = streak,
            monthTotal = perDay.entries.filter { !it.key.isBefore(monthStart) }.sumOf { it.value },
            monthActiveDays = perDay.entries.count { !it.key.isBefore(monthStart) && it.value > 0 }
        )
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

    /**
     * [companionProfileId] ist das Wesen, das gerade spricht. Was es ueber den Tag WEISS, steht in
     * seinem eigenen Pflegebuch und in seinem eigenen Spielstand; was du dir VORGENOMMEN hast,
     * steht in deinen Routinen ([RoutineOwner]). Ein Wesen soll ueber Dinge reden koennen, die es
     * miterlebt hat - und ueber Vorhaben, die dir gehoeren, auch wenn es neu ist.
     */
    suspend fun gather(
        context: Context,
        companionProfileId: String,
        /** Im Spielmodus kommt der Spielstand dazu - siehe [Game]. */
        includeGame: Boolean = false
    ): Knowledge {
        val db = AppDatabase.getInstance(context)
        val since = FeedStatsPeriod.TODAY.startMillis()

        // isPlayMode ausgeschlossen: Die Spiel-Erinnerung ist ein technisches Hilfsmittel und
        // gehoert nicht in einen Plan, den sich jemand vorgenommen hat.
        val reminders = db.glyphReminderDao().getEnabledForProfile(RoutineOwner.current(context))
            .filterNot { it.isPlayMode }
            .sortedBy { it.startMinuteOfDay }

        val fedByReminder = db.avatarFeedEventDao()
            .countFedPerReminderSince(companionProfileId, since)
            .associate { it.reminderId to it.count }

        val plan = reminders.map { PlanEntry(it, fedByReminder[it.id] ?: 0) }
        // **Ueber die GESAMTE Vergangenheit**, nicht ueber ein Fenster: Der Pfad soll langsam
        // mitwandern, wenn sich die Gewohnheiten aendern, und nie springen (siehe PlayPath).
        val fedEver = db.avatarFeedEventDao()
            .countFedPerReminderSince(companionProfileId, 0L)
            .associate { it.reminderId to it.count }
        val covered = reminders.map { it.animationType }.toSet()

        val weekStart = FeedStatsPeriod.WEEK.startMillis()
        // **Einmal die langen Zeitpunkte holen und zweimal auswerten.** Die Woche und die laengere
        // Rueckschau aus zwei getrennten Abfragen zu ziehen waere die naheliegende Loesung und die
        // falsche: Zwei Abfragen zu verschiedenen Zeitpunkten koennen sich widersprechen, und ein
        // Begleiter, der "diese Woche 12" und gleich darauf "die Serie ist gerissen" sagt, hat sich
        // gerade selbst widerlegt.
        val historyStart = System.currentTimeMillis() -
            HISTORY_DAYS * 24L * 60L * 60L * 1000L
        val longTimestamps = db.avatarFeedEventDao()
            .fedTimestampsSince(companionProfileId, minOf(historyStart, weekStart))
        val week = summariseWeek(longTimestamps.filter { it >= weekStart })
        val history = summariseHistory(longTimestamps)

        val game = if (includeGame) {
            // Die Erfahrung steht in der Datenbank, Geld und Vorrat in den Einstellungen - beides
            // hier zusammengefuehrt, damit das Gespraech nur EINE Quelle kennt.
            val xp = db.avatarPlayStateDao().getForProfile(companionProfileId)?.xp ?: 0
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
            steering = PlayHabitSignal.underfulfilledTopics(context, companionProfileId),
            chapter = CompanionChapter.chapterFor(
                db.avatarFeedEventDao().firstAnsweredMillis(companionProfileId),
                System.currentTimeMillis()
            ),
            missing = suggestableFor(PlayUserProfile.purpose(context)).filterNot { it in covered },
            week = week,
            game = game,
            // Ueber die ganze Woche, nicht ueber heute: Ein Rat zu einer Einstellung braucht
            // mehrere Tage, sonst raet er nach einem einzigen schlechten Vormittag.
            history = history,
            // Was der Nutzer selbst gesagt hat - damit er es auf Nachfrage aufsagen und auf
            // Wunsch wieder vergessen kann (siehe [UserAnswers]).
            hasMoreToTell = PlayLore.hasMore(context, AvatarSpeciesPrefs.get(context)),
            development = if (includeGame) developmentOf(plan, fedEver, game?.level ?: 1) else null,
            answers = UserAnswers(
                focusTopic = PlayUserProfile.focusTopic(context),
                busyPhase = PlayUserProfile.busyPhase(context),
                purpose = PlayUserProfile.purpose(context),
                includesWeekend = PlayUserProfile.includesWeekend(context),
                anyGiven = PlayUserProfile.focusTopic(context) != null ||
                    PlayUserProfile.busyPhase(context) != null ||
                    PlayUserProfile.purpose(context) != null ||
                    !PlayUserProfile.includesWeekend(context)
            ),
            advice = adviceFor(
                plan = plan,
                occurrences = db.avatarFeedEventDao()
                    .occurrencesSince(companionProfileId, weekStart)
                    .map { Occurrence(it.reminderId, it.epochMillis, it.fedAtMillis != null) }
            )
        )
    }

    /**
     * **Was er von sich aus sagt, wenn man ihn anspricht - und was er dazu anbietet.**
     *
     * Der erste Entwurf legte dem Nutzer eine Liste aus sechs Fragen vor. Das war ein Menue, kein
     * Gespraech: Sechs gleich aussehende Zeilen verlangen eine Auswahl, bevor irgendetwas gesagt
     * wurde, und die Frage "was will ich eigentlich wissen?" muss der Nutzer sich dann selbst
     * beantworten. Wer jemanden anspricht, erwartet aber, dass der andere ANFAENGT.
     *
     * Deshalb hier die Umkehrung: EINE Aussage, die zur Lage passt, und hoechstens zwei Angebote
     * dazu. Was gesagt wird, richtet sich nach Dringlichkeit - ein leerer Vorrat ohne Geld geht
     * jeder Statistik vor, und eine offene Gewohnheit geht einem Vorschlag vor, der noch gar keine
     * ist.
     *
     * **Als eigene, reine Funktion und nicht in der Oberflaeche verteilt**, weil sie eine
     * inhaltliche Entscheidung trifft ("worueber redet er zuerst?") und keine gestalterische.
     * Solche Entscheidungen gehoeren an eine Stelle, an der man sie nachlesen und pruefen kann.
     */
    enum class Headline {
        /** Vorrat leer und kein Geld - die dringendste Lage im Spiel. */
        BROKE,
        /** Vorrat leer, Geld da. */
        SHOPPING,
        /** Heute noch offene Gewohnheiten. */
        OPEN_TOPICS,
        /** Heute ist noch nichts geschehen. */
        NOTHING_TODAY,
        /** Alles erledigt, was man sich vorgenommen hat. */
        ALL_DONE,
        /** Es gibt noch gar keinen Plan. */
        NO_PLAN,
        /** Nichts Dringendes - dann erzaehlt er einfach, wie der Tag lief. */
        SMALL_TALK
    }

    /** Ein Angebot unter der Aussage - hoechstens zwei davon. */
    sealed interface Offer {
        /** Ihn um etwas bitten; er geht sofort los. */
        data class Ask(val topic: AnimationType) : Offer
        /** Eine Gewohnheit anlegen, die es noch nicht gibt. */
        data class Add(val topic: AnimationType) : Offer
        /** Den vollstaendigen Plan zeigen. */
        data object ShowPlan : Offer
        /** Die Rueckschau auf die Woche. */
        data object ShowWeek : Offer
        /** Stufe, Erfahrung, Muenzen, Vorrat - der Spielstand. */
        data object ShowGame : Offer
        /** "Was passiert hier eigentlich?" - die Erklaerung des Spielmodus. */
        data object Explain : Offer
        /** "Passt das so?" - was ihm an den bestehenden Erinnerungen auffaellt. */
        data object ShowAdvice : Offer
        /** "Was weisst du eigentlich ueber mich?" - und die Moeglichkeit, es zurueckzunehmen. */
        data object ShowProfile : Offer
        /** "Erzaehl mir etwas von dir" - siehe [PlayLore]. */
        data object Tell : Offer
        /** "Wohin entwickelst du dich?" - siehe [PlayPath]. */
        data object ShowPath : Offer
    }

    /**
     * **Was er GERADE tut** - Ort und Thema der laufenden Regung, oder `null`, wenn er noch nichts
     * getan hat (oder der Play-Modus aus ist).
     *
     * Gemeldet als "die Gespraeche sagen immer dasselbe". Das lag nicht an zu wenig Text, sondern
     * daran, WORUEBER er sprach: ueber Zahlen (Woche, Plan, Stufe) und ueber den Modus - also ueber
     * lauter Dinge, die sich beim zweiten Hinsehen nicht geaendert haben. Das Naechstliegende
     * fehlte: das, was gerade auf dem Bildschirm passiert. "Ich bin in der Kueche" ist bei jedem
     * Oeffnen ein anderer Satz, ohne dass dafuer ein einziger Text mehr noetig waere - er kommt
     * aus dem Zustand der Welt statt aus einer Liste.
     */
    data class Doing(val place: PlayScene.Place, val topic: AnimationType?)

    /**
     * **Was er von sich aus erzaehlt** - der Teil des Gespraechs, der nicht von Zahlen handelt.
     *
     * Gemeldet als "die Gespraeche bleiben immer gleich". Der Grund war nicht die Menge an Text,
     * sondern das Thema: Er sprach ueber die Woche, den Plan, die Stufe - alles Dinge, die sich
     * beim naechsten Oeffnen kaum geaendert haben. Woran sich dagegen STAENDIG etwas aendert, ist
     * die Welt, in der er gerade steht: Wetter, Tageszeit, wo er ist, wer vorbeikam, ob der
     * Vorrat voll ist.
     *
     * **Keine dieser Bemerkungen ist erfunden.** Jede haengt an einem Zustand, den es ohnehin
     * gibt, und ist damit ueberpruefbar wahr - dasselbe Versprechen wie beim Rest dieser Klasse
     * (siehe Klassendoku): Er weiss wenig, aber was er sagt, stimmt. Der Unterschied zu den
     * Zahlen ist nur, dass sich diese Wahrheiten staendig aendern.
     */
    enum class Remark {
        /** Es regnet. */
        RAIN,
        /** Es schneit. */
        SNOW,
        /** Klarer Morgen. */
        BRIGHT_MORNING,
        /** Es wird Abend. */
        EVENING,
        /** Nacht. */
        NIGHT,
        /** Wo er gerade ist und was er dort tut. */
        DOING,
        /** Vorhin war jemand da. */
        VISITOR,
        /** Er hat gearbeitet und etwas verdient. */
        EARNED,
        /** Der Vorrat ist gut gefuellt. */
        STOCKED,
        /** Wie lange man sich schon kennt. */
        TOGETHER,
        /**
         * Woran er sich haelt, weil der Nutzer es ihm gesagt hat (siehe [PlayUserProfile]).
         *
         * **Der Satz, der aus einer Antwort ein Gespraech macht.** Eine Frage zu stellen und die
         * Antwort danach nie wieder zu erwaehnen, waere schlimmer als gar nicht zu fragen: Es
         * saehe aus, als habe er zugehoert und es sofort vergessen.
         */
        FOCUS,

        /** Wenn sonst gerade nichts ist. */
        QUIET
    }

    /**
     * Der Zustand der Welt, ueber den er reden kann - alles Werte, die anderswo ohnehin gefuehrt
     * werden.
     */
    data class Mood(
        val doing: Doing?,
        val phase: PlayAmbientActivity.DayPhase,
        val weather: PlayWeather,
        /** Wer zuletzt vorbeikam, oder `null`, wenn heute noch niemand da war. */
        val lastVisitor: AvatarSpecies?,
        /** Worauf er achten soll, weil der Nutzer es gesagt hat - siehe [PlayUserProfile]. */
        val focusTopic: AnimationType? = null,
        val chapter: CompanionChapter,
        val game: Game?
    )

    /**
     * Alle Bemerkungen, die gerade ZUTREFFEN - in der Reihenfolge, in der sie sich lohnen.
     *
     * Vorn steht, was sich am haeufigsten aendert (wo er ist, wer da war, welches Wetter), hinten
     * das Bestaendige (wie lange man sich kennt). [QUIET] steht ganz am Ende und nur, damit die
     * Liste nie leer ist - ein Begleiter, der auf "wie geht's" gar nichts sagt, waere schlimmer
     * als einer, der zugibt, dass nichts los ist.
     */
    fun remarksFor(mood: Mood): List<Remark> = buildList {
        mood.doing?.let { add(Remark.DOING) }
        mood.lastVisitor?.let { add(Remark.VISITOR) }
        when (mood.weather) {
            PlayWeather.RAIN -> add(Remark.RAIN)
            PlayWeather.SNOW -> add(Remark.SNOW)
            PlayWeather.CLEAR -> if (mood.phase == PlayAmbientActivity.DayPhase.MORNING) {
                add(Remark.BRIGHT_MORNING)
            }
        }
        when (mood.phase) {
            PlayAmbientActivity.DayPhase.EVENING -> add(Remark.EVENING)
            PlayAmbientActivity.DayPhase.NIGHT -> add(Remark.NIGHT)
            else -> Unit
        }
        mood.game?.let { game ->
            if (game.coins >= PlayWallet.GROCERY_COST) add(Remark.EARNED)
            if (game.pantry >= PANTRY_COMFORTABLE) add(Remark.STOCKED)
        }
        mood.focusTopic?.let { add(Remark.FOCUS) }
        if (mood.chapter != CompanionChapter.ARRIVED) add(Remark.TOGETHER)
        add(Remark.QUIET)
    }

    /** Ab wieviel Vorrat er sich wohlfuehlt - siehe [Remark.STOCKED]. */
    private const val PANTRY_COMFORTABLE = 3

    /**
     * Eine davon, ueber [rotation] gewaehlt.
     *
     * **Ohne [Remark.QUIET], solange es etwas anderes gibt.** Sonst kaeme ausgerechnet die
     * Verlegenheitsantwort so haeufig wie alles andere - und die ist die einzige, die nichts
     * erzaehlt.
     */
    fun remarkFor(mood: Mood, rotation: Int = 0): Remark {
        val all = remarksFor(mood)
        val worthSaying = all.filterNot { it == Remark.QUIET }.ifEmpty { all }
        return worthSaying[rotation.mod(worthSaying.size)]
    }

    /**
     * Dieselbe Auswahl wie [remarkFor], aber ohne [Remark.DOING] im Topf.
     *
     * **Gemeldet als "die Gespraeche sagen immer dasselbe" - und dann, nachdem DOING eingefuehrt
     * war, als "der wichtigste Satz kommt nur manchmal".** Beides war derselbe Fehler: [remarkFor]
     * wuerfelt GLEICHBERECHTIGT unter allem, was zutrifft - Regen, Besuch, Verdienst, und eben auch,
     * was er gerade tut. Ausgerechnet der einzige Satz, der sich bei jedem Oeffnen aendert, ohne
     * dass dafuer ein neuer Text noetig waere, verschwand dadurch im Schnitt in drei von vier
     * Gespraechen hinter Kleinigkeiten wie dem Wetter.
     *
     * Die Loesung ist nicht, DOING haeufiger zu wuerfeln, sondern es aus dem Wuerfel herauszunehmen:
     * [PlayTalkPanel] zeigt es jetzt unbedingt, sobald [Mood.doing] gesetzt ist, und zieht diese
     * Funktion nur noch fuer die zweite, gedimmte Zeile daneben - das Nebenbei, nicht die Auskunft.
     */
    fun secondaryRemarkFor(mood: Mood, rotation: Int = 0): Remark? {
        // Kein Rueckfall auf QUIET: Steht die Doing-Zeile schon prominent daneben, waere "gerade
        // ist sonst nichts los" darunter keine zweite Auskunft, sondern eine Wiederholung derselben
        // Leere in anderen Worten.
        val rest = remarksFor(mood).filterNot { it == Remark.DOING || it == Remark.QUIET }
        if (rest.isEmpty()) return null
        return rest[rotation.mod(rest.size)]
    }

    data class Focus(val headline: Headline, val offers: List<Offer>)

    /** Hoechstens so viele Angebote - darueber wird aus dem Gespraech wieder eine Liste. */
    private const val MAX_OFFERS = 2

    /**
     * [rotation] verschiebt die Auswahl unter mehreren gleichrangigen Moeglichkeiten.
     *
     * **Gemeldet als "es bleibt immer bei demselben Vorschlag".** Und so war es: Unter den heute
     * offenen Gewohnheiten wurde immer die ERSTE gebeten, unter den fehlenden immer die erste
     * vorgeschlagen. Wer drei offene Themen hat, bekam trotzdem drei Tage lang denselben Satz -
     * und ein Gegenueber, das sich wiederholt, hoert man nach dem dritten Mal nicht mehr.
     *
     * Die Reihenfolge selbst bleibt, wie sie war (siehe [suggestable]): Es wird nicht gewuerfelt,
     * WAS wichtig ist, sondern nur, welches von mehreren gleich Wichtigen diesmal drankommt.
     *
     * [doing] ist optional (Standard: `null`, fuer Aufrufer ohne Weltzustand, z.B. Tests) - siehe
     * [nextSuggestion] fuer die einzige Stelle, an der es etwas aendert: eine laufende Taetigkeit,
     * die der Nutzer noch gar nicht als eigene Gewohnheit hat, wird bevorzugt statt der
     * durchrotierten Standardauswahl vorgeschlagen ("das mache ich gerade - auch was fuer dich?").
     */
    fun focus(knowledge: Knowledge, rotation: Int = 0, doing: Doing? = null): Focus {
        val game = knowledge.game
        val offers = mutableListOf<Offer>()
        fun <T> List<T>.rotated(): T? = if (isEmpty()) null else this[rotation.mod(size)]

        val headline = when {
            // Im Spiel hat seine eigene Lage Vorrang: Sie erklaert, was gleich zu sehen ist.
            game != null && game.brokeAndHungry -> {
                offers += Offer.Ask(AnimationType.WORK)
                Headline.BROKE
            }
            game != null && game.pantryEmpty -> {
                offers += Offer.Ask(AnimationType.DRINK)
                Headline.SHOPPING
            }
            !knowledge.hasPlan -> Headline.NO_PLAN
            knowledge.steering.isNotEmpty() -> {
                // Nur die erste offene Gewohnheit als Bitte - alle aufzulisten waere wieder die
                // Liste, die hier gerade abgeschafft wird.
                knowledge.steering.toList().rotated()?.let { offers += Offer.Ask(it) }
                Headline.OPEN_TOPICS
            }
            knowledge.fedToday == 0 -> Headline.NOTHING_TODAY
            knowledge.goalsTotal > 0 -> Headline.ALL_DONE
            else -> Headline.SMALL_TALK
        }

        // **Der Spielstand steht im Spiel immer zur Verfuegung** und kommt gleich nach einer
        // dringenden Bitte.
        //
        // Er stand frueher dauerhaft am unteren Bildrand. Das war die alte Bauweise "alles ist
        // immer zu sehen" - dieselbe, aus der auch der Tagesstand oben stammte. Beim Umbau auf
        // die kurze Fassung fiel er dann ganz heraus: Er tauchte nur noch auf, wenn der Vorrat
        // leer war. Wer einfach wissen wollte, wie weit sein Begleiter ist, fand es nirgends
        // mehr - weder unten noch im Gespraech.
        if (game != null && offers.size < MAX_OFFERS) offers += Offer.ShowGame

        // Aufgefuellt wird nur, wenn noch Platz ist, und in dieser Reihenfolge: erst ein
        // Vorschlag (er bringt etwas Neues), dann der Plan, dann die Woche.
        // **Das Erzaehlen steht weit vorn - und zwar hinter allem, was DRINGEND ist, aber vor
        // allem, was bloss nuetzlich ist.** Es ist das einzige Angebot, bei dem es nicht um den
        // Nutzer und seine Gewohnheiten geht, sondern um das Wesen selbst; und es ist der Grund,
        // warum jemand nach dem dritten Tag noch einmal auf die Figur tippt. Ein Plan laesst sich
        // auch morgen ansehen.
        if (offers.size < MAX_OFFERS && knowledge.hasMoreToTell) offers += Offer.Tell
        // Die Entwicklung gleich danach: Sie ist das, wofuer sich das Mitmachen lohnt, und
        // sie ist die einzige Auskunft, die sich ueber Wochen aendert statt ueber Stunden.
        if (offers.size < MAX_OFFERS && knowledge.development != null) offers += Offer.ShowPath

        // **Ein Rat zu etwas Bestehendem geht einem Vorschlag fuer etwas Neues vor.** Wer sein
        // Tagesziel seit einer Woche nicht erreicht, ist mit einer zusaetzlichen Gewohnheit nicht
        // geholfen - das waere die Antwort "dann nimm dir noch mehr vor".
        if (offers.size < MAX_OFFERS && knowledge.advice.isNotEmpty()) offers += Offer.ShowAdvice
        if (offers.size < MAX_OFFERS) {
            // **Nachahmen vor Vorschlagen.** Tut er gerade etwas, das der Nutzer selbst noch gar
            // nicht als Gewohnheit hat, ist das der bessere Anlass als eine durchrotierte
            // Standardauswahl - der Nutzer SIEHT gerade, was er ihm vorschlaegt, statt es nur zu
            // lesen. Trifft das nicht zu (er tut nichts Vorschlagbares, oder es gibt dafuer schon
            // eine Erinnerung), bleibt es bei der bisherigen Rotation.
            val mirrored = doing?.topic?.takeIf { it in knowledge.missing }
            (mirrored ?: nextSuggestion(knowledge, rotation))?.let { offers += Offer.Add(it) }
        }
        // Was er ueber den Nutzer weiss, bietet er erst an, wenn es etwas zu sagen gibt - und
        // hinter allem anderen: Es ist die Frage, die man einmal stellt, nicht jeden Tag.
        if (offers.size < MAX_OFFERS && knowledge.answers?.anyGiven == true) offers += Offer.ShowProfile
        if (offers.size < MAX_OFFERS && knowledge.hasPlan) offers += Offer.ShowPlan
        if (offers.size < MAX_OFFERS && knowledge.week.total > 0) offers += Offer.ShowWeek

        // **Die Erklaerung ganz zuletzt und nur, wenn sonst nichts zu sagen war.**
        //
        // Wer schon eine Woche spielt, braucht nicht jedes Mal zu lesen, was dieser Modus ist -
        // das waere die Art Hinweis, die man nach dem dritten Mal nicht mehr sieht. Wer dagegen
        // gerade erst hier gelandet ist, hat naturgemaess weder Plan noch Woche noch offene
        // Gewohnheiten, und genau dann steht sie da.
        if (offers.isEmpty() && knowledge.game != null) offers += Offer.Explain

        return Focus(headline, offers.take(MAX_OFFERS))
    }

    /**
     * Der Vorschlag, den der Avatar von sich aus macht - hoechstens einer.
     *
     * **Einer, nicht fuenf.** Eine Liste von Vorschlaegen ist eine Liste von Vorwuerfen; ein
     * einzelner ist ein Angebot. Und er kommt nur, wenn es wirklich etwas anzubieten gibt.
     */
    fun nextSuggestion(knowledge: Knowledge, rotation: Int = 0): AnimationType? =
        knowledge.missing.let { if (it.isEmpty()) null else it[rotation.mod(it.size)] }

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

    /**
     * [busyPhase] verschiebt das Zeitfenster dorthin, wo der Nutzer eigenen Angaben nach Zeit hat
     * (siehe [PlayUserProfile]) - der sichtbare Nutzen davon, dass er gefragt hat.
     *
     * Verschoben wird nur das FENSTER, nicht das Ziel und nicht der Abstand: Wer abends Zeit hat,
     * braucht kein anderes Pensum, sondern eine andere Uhrzeit.
     */
    fun presetFor(
        topic: AnimationType,
        busyPhase: PlayAmbientActivity.DayPhase?
    ): Preset {
        val base = presetFor(topic)
        val window = windowFor(busyPhase) ?: return base
        val (start, end) = window
        // Der Abstand wird notfalls gestaucht, damit das Tagesziel im engeren Fenster erreichbar
        // bleibt - sonst waere aus einer Auskunft ein unmoegliches Ziel geworden (siehe
        // PlayTalkTest: "das Tagesziel ist im Zeitfenster ueberhaupt erreichbar").
        val slots = base.dailyGoal.coerceAtLeast(1) * 4 / 3 + 1
        val interval = ((end - start) / slots).coerceIn(15, base.intervalMinutes)
        return base.copy(startMinuteOfDay = start, endMinuteOfDay = end, intervalMinutes = interval)
    }

    /** Das Zeitfenster zu einer Tageszeit - `null` heisst: keine Angabe, alles bleibt wie es ist. */
    private fun windowFor(phase: PlayAmbientActivity.DayPhase?): Pair<Int, Int>? = when (phase) {
        PlayAmbientActivity.DayPhase.MORNING -> 6 * 60 to 11 * 60
        PlayAmbientActivity.DayPhase.MIDDAY -> 11 * 60 to 17 * 60
        PlayAmbientActivity.DayPhase.EVENING -> 17 * 60 to 22 * 60
        // Nachts fragt niemand nach einem Zeitfenster - und wer es doch angibt, bekommt den Abend.
        PlayAmbientActivity.DayPhase.NIGHT -> 17 * 60 to 22 * 60
        null -> null
    }

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

    /**
     * Montag bis Freitag - fuer alle, die auf [Ask.WEEKEND] mit Nein geantwortet haben.
     *
     * Aus derselben Quelle gerechnet wie [EVERY_DAY_MASK] und aus demselben Grund: Wer hier eine
     * 31 hinschriebe, haette sich auf eine Nummerierung festgelegt, die woanders bestimmt wird.
     */
    val WEEKDAYS_MASK: Int = DaysOfWeekMask.toMask(
        java.time.DayOfWeek.entries.toSet() -
            setOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)
    )
}
