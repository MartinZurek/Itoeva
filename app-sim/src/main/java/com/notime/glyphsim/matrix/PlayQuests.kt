package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.matrix.PlayQuestEffects.Effect
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.RoutineStep.GoToPlace
import com.notime.glyphsim.matrix.RoutineStep.Linger
import com.notime.glyphsim.matrix.RoutineStep.Quest
import com.notime.glyphsim.matrix.RoutineStep.Stir
import com.notime.glyphsim.matrix.RoutineStep.Stroll
import com.notime.glyphsim.matrix.RoutineStep.Take

/**
 * **Die Quests als Reisen: Das Wesen ist wirklich den ganzen Tag unterwegs.**
 *
 * Entscheidung des Nutzers vom 28.09.2026: Es soll eine Geschichte erzaehlt werden, "einmal am Tag
 * etwas Neues", und zwar mit Expeditionen, Schatzsuche, Zaubern lernen und einem Drachenei, das
 * schluepft. Gewaehlt wurden **feste Questketten** und alle vier Belohnungsarten. Damit ist die in
 * LIVING_AGENT.md festgehaltene Regel "kein StoryManager, keine Plot-Skripte" fuer diesen einen
 * Bereich aufgehoben, bewusst und vom Produktverantwortlichen (siehe EVOLUTION.md).
 *
 * **Nachgeschaerft am selben Abend:** Die erste Fassung erzaehlte jede Quest in drei Auftritten
 * von je einer halben bis anderthalb Minuten. Das war nicht gemeint: "keine Animation, sondern
 * eine realistische Geschichte ueber den Tag, wo der Avatar reisen geht, dort Sachen erlebt, wie
 * im wahren Leben, ueber einen Tag und sogar mehrere Tage". Entschieden: **zwei Reisetage, dann
 * ein Tag daheim.**
 *
 * ## Ein Reisetag
 *
 * Morgens bricht das Wesen auf ([StepKind.DEPART]), zieht ueber **Stationen**, an denen es jeweils
 * mehrere Stunden bleibt ([Station]), erlebt dort zu festen Zeiten die Momente der Geschichte
 * ([StepKind.EXPERIENCE]) und kommt abends heim ([StepKind.RETURN]). Zwischen den Momenten lebt
 * es an der Station weiter: Rast, Proviant, sich umsehen, den Drachen steigen lassen ([wayside]).
 * Die Geschichte wird ohne ein Wort erzaehlt, wie der Nutzer es fuer den Stream festgelegt hat.
 *
 * ## Was bleibt
 *
 * Jede Reise hinterlaesst etwas ([Reward]): die Truhe in der Werkstatt, den Zauberstab und die
 * Faehigkeit zu zaubern, die Kristallgrotte als neuen Ort, und ein Drachenei. Das Ei wird an den
 * folgenden Tagen morgens und abends gewaermt ([StepKind.CARE]), bekommt Risse und schluepft -
 * die Geschichte zieht sich ueber mehrere Tage.
 *
 * ## Verpasstes
 *
 * Laeuft die App nicht, wird nachgeholt, was die Geschichte braucht, und uebersprungen, was sich
 * erledigt hat: Ein Moment ohne Belohnung faellt weg, sobald der naechste faellig ist; ein
 * Aufbruch nach der Heimkehrzeit findet nicht mehr statt, die Reise wird dann am naechsten
 * Reisetag angetreten.
 *
 * Alles hier ist reine Rechnung ueber [Progress]; gespeichert wird in PlayQuestLog.
 */
object PlayQuests {

    /** Die Reisen, in dieser Reihenfolge. */
    enum class Quest { TREASURE, MAGIC, EXPEDITION, DRAGON_EGG }

    /** Was eine Reise hinterlaesst. */
    enum class Reward {
        /** Die Truhe in der Werkstatt. */
        TREASURE_CHEST,

        /** Der Zauberstab - und die Faehigkeit zu zaubern. */
        MAGIC,

        /** Die Kristallgrotte als neuer Ort. */
        GROTTO,

        /** Das Ei im Nest, dann mit Rissen, dann das Junge. */
        DRAGON_EGG,
        DRAGON_EGG_CRACKED,
        DRAGONLING
    }

    /** Zwei Reisetage, dann ein Tag daheim (Entscheidung des Nutzers). */
    enum class DayKind { TRAVEL, HOME }

    enum class StepKind {
        /** Aufbruch am Morgen. */
        DEPART,

        /** Ein Moment der Geschichte an einer Station. */
        EXPERIENCE,

        /**
         * Eine Begegnung unterwegs: Das Wesen bleibt stehen, und ein Bewohner kommt dazu (der
         * Besuch selbst laeuft ueber denselben Weg wie jeder Besuch, siehe DockScreen.runVisit).
         */
        ENCOUNTER,

        /** Heimkehr am Abend. */
        RETURN,

        /** Das Ei waermen - zu Hause, morgens und abends. */
        CARE
    }

    /** Hier ist das Wesen ab [fromMinute] (Minute des Questtags, siehe [questMinute]). */
    data class Station(val place: Place, val fromMinute: Int)

    /** Ein Schritt der Geschichte - ab [atMinute] faellig. */
    data class Step(
        val kind: StepKind,
        val atMinute: Int,
        val routine: PlayRoutine,
        val reward: Reward? = null
    )

    /** Der Plan eines Tages. Auf Reisen mit Stationen, daheim nur mit der Pflege des Eis. */
    data class DayPlan(
        val kind: DayKind,
        val quest: Quest?,
        val stations: List<Station>,
        val steps: List<Step>
    ) {
        val returnMinute: Int? get() = steps.firstOrNull { it.kind == StepKind.RETURN }?.atMinute
    }

    /**
     * Der Stand. [stepsDone] zaehlt die erledigten (oder uebersprungenen) Schritte des Tages
     * [questDayNumber]; [departed] und [journeyDone] gelten fuer diesen Tag.
     */
    data class Progress(
        val quest: Quest = ORDER.first(),
        val questDayNumber: Long = Long.MIN_VALUE,
        val stepsDone: Int = 0,
        val departed: Boolean = false,
        val journeyDone: Boolean = false,
        val rewards: Set<Reward> = emptySet(),
        /** Wie oft alle Reisen schon durchlaufen sind - danach wiederholen sie sich. */
        val round: Int = 0,
        /** Der Questtag, an dem das Ei nach Hause kam. */
        val eggFoundDay: Long = Long.MIN_VALUE
    )

    /** Was jetzt zu tun ist: einen Schritt spielen, oder einen verpassten ueberspringen. */
    sealed interface Next {
        data class Run(val step: Step) : Next
        data object Skip : Next
    }

    val ORDER = listOf(Quest.TREASURE, Quest.MAGIC, Quest.EXPEDITION, Quest.DRAGON_EGG)

    /** Ein Questtag beginnt um sechs Uhr, nicht um Mitternacht: Bis zwei ist ja noch Abend. */
    private const val DAY_STARTS_AT_MINUTE = 6 * 60

    /** Ab hier (zwei Uhr) wird geschlafen, bis der naechste Questtag beginnt. */
    private const val BEDTIME = (24 + PlayAmbientActivity.BEDTIME_HOUR) * 60

    private fun h(hour: Int, minute: Int = 0) = hour * 60 + minute

    /** Der Questtag zur absoluten Minute [absoluteMinute] (siehe PlayTimeLapse.absoluteMinute). */
    fun questDayOf(absoluteMinute: Long): Long =
        Math.floorDiv(absoluteMinute - DAY_STARTS_AT_MINUTE, 24L * 60L)

    /** Reisetag oder Tag daheim: zwei Reisetage, dann einer daheim. */
    fun dayKind(questDayNumber: Long): DayKind =
        if (Math.floorMod(questDayNumber, 3L) == 2L) DayKind.HOME else DayKind.TRAVEL

    /** Die Minute des Tages im Questtag: nach Mitternacht 24:00 und mehr, damit sie weiterzaehlt. */
    fun questMinute(minuteOfDay: Int): Int {
        val m = Math.floorMod(minuteOfDay, 24 * 60)
        return if (m < DAY_STARTS_AT_MINUTE) m + 24 * 60 else m
    }

    /**
     * **Ein neuer Tag.** War die Reise des Vortags vollendet, geht es mit der naechsten weiter; war
     * sie es nicht, wird sie am naechsten Reisetag angetreten - eine Geschichte wird zu Ende
     * erzaehlt, nicht uebersprungen.
     */
    fun rollTo(progress: Progress, questDayNumber: Long): Progress {
        if (progress.questDayNumber == Long.MIN_VALUE) {
            return progress.copy(questDayNumber = questDayNumber, stepsDone = 0, departed = false, journeyDone = false)
        }
        if (questDayNumber <= progress.questDayNumber) return progress
        val fresh = progress.copy(
            questDayNumber = questDayNumber,
            stepsDone = 0,
            departed = false,
            journeyDone = false
        )
        if (!progress.journeyDone) return fresh
        val (next, round) = nextQuest(progress.quest, progress.round)
        return fresh.copy(quest = next, round = round)
    }

    /**
     * Die naechste Reise. Nach der ersten Runde wiederholen sich Schatz, Zauber und Expedition;
     * das Drachenei nicht - es gibt nur ein Drachenjunges.
     */
    private fun nextQuest(current: Quest, round: Int): Pair<Quest, Int> {
        val order = if (round == 0) ORDER else ORDER - Quest.DRAGON_EGG
        val i = order.indexOf(current)
        return if (i < 0 || i + 1 >= order.size) {
            (ORDER - Quest.DRAGON_EGG).first() to round + 1
        } else {
            order[i + 1] to round
        }
    }

    /** Der Plan des Tages, auf dem [progress] steht. */
    fun planFor(progress: Progress): DayPlan {
        val kind = dayKind(progress.questDayNumber)
        val care = careSteps(progress, kind)
        if (kind == DayKind.HOME) return DayPlan(kind, null, emptyList(), care)
        val journey = journeyFor(progress.quest)
        return DayPlan(
            kind,
            progress.quest,
            journey.stations,
            (journey.steps + care).sortedBy { it.atMinute }
        )
    }

    /**
     * **Was jetzt dran ist** - oder `null`, wenn gerade nichts zur Geschichte gehoert.
     *
     * Zwischen zwei und sechs Uhr ist nichts faellig: Dann wird geschlafen.
     */
    fun next(progress: Progress, minuteOfDay: Int): Next? {
        val plan = planFor(progress)
        val i = progress.stepsDone
        if (i >= plan.steps.size) return null
        val m = questMinute(minuteOfDay)
        if (m >= BEDTIME) return null
        val step = plan.steps[i]
        if (m < step.atMinute) return null
        val nextAt = plan.steps.getOrNull(i + 1)?.atMinute
        // Ueberholt ist ein Moment, sobald der naechste faellig ist - oder sobald die Reise zu
        // dieser Zeit schon an einer anderen Station ist (sonst liefe er zurueck in den Wald).
        val stationNow = plan.stations.lastOrNull { it.fromMinute <= m }?.place
        val placeOfStep = step.routine.steps.filterIsInstance<GoToPlace>().firstOrNull()?.place
        val leftBehind = (step.kind == StepKind.EXPERIENCE || step.kind == StepKind.ENCOUNTER) &&
            stationNow != null && placeOfStep != stationNow
        val overtaken = step.reward == null && (nextAt != null && m >= nextAt || leftBehind)
        return when (step.kind) {
            StepKind.DEPART -> if (m >= (plan.returnMinute ?: Int.MAX_VALUE)) Next.Skip else Next.Run(step)
            StepKind.EXPERIENCE, StepKind.ENCOUNTER ->
                if (!progress.departed || overtaken) Next.Skip else Next.Run(step)
            StepKind.RETURN -> if (!progress.departed) Next.Skip else Next.Run(step)
            StepKind.CARE -> if (overtaken) Next.Skip else Next.Run(step)
        }
    }

    /** Nach einem gespielten Schritt: eins weiter, und was er hinterlaesst, bleibt. */
    fun completed(progress: Progress, step: Step): Progress {
        val plan = planFor(progress)
        if (plan.steps.getOrNull(progress.stepsDone) != step) return progress
        val reward = step.reward
        return progress.copy(
            stepsDone = progress.stepsDone + 1,
            departed = progress.departed || step.kind == StepKind.DEPART,
            journeyDone = progress.journeyDone || step.kind == StepKind.RETURN,
            rewards = reward?.let { progress.rewards + it } ?: progress.rewards,
            eggFoundDay = if (reward == Reward.DRAGON_EGG) progress.questDayNumber else progress.eggFoundDay
        )
    }

    /** Ein verpasster Schritt faellt weg - ohne Belohnung, ohne Aufbruch. */
    fun skipped(progress: Progress): Progress = progress.copy(stepsDone = progress.stepsDone + 1)

    /**
     * **Wo das Wesen gerade sein soll** - die Station der Reise zu dieser Minute, oder `null`,
     * wenn es nicht unterwegs ist (noch nicht aufgebrochen, schon daheim, Tag daheim).
     */
    fun stationAt(progress: Progress, minuteOfDay: Int): Station? {
        if (!progress.departed || progress.journeyDone) return null
        val plan = planFor(progress)
        if (plan.kind != DayKind.TRAVEL) return null
        val m = questMinute(minuteOfDay)
        return plan.stations.lastOrNull { it.fromMinute <= m } ?: plan.stations.firstOrNull()
    }

    /**
     * **Was das Wesen unterwegs in der Hand traegt** - die Karte der Schatzsuche, nach dem Fund die
     * Truhe, auf der Dracheneireise das Ei. Sonst verschwaende es zwischen Fund und Heimkehr, als
     * haette es nie etwas gefunden.
     */
    fun carriedOnJourney(progress: Progress): PlayEffects.Carried? {
        if (!progress.departed || progress.journeyDone) return null
        val plan = planFor(progress)
        if (plan.kind != DayKind.TRAVEL) return null
        val done = plan.steps.take(progress.stepsDone)
        fun found(effect: Effect) = done.any { step -> step.routine.steps.any { it is RoutineStep.Quest && it.effect == effect } }
        return when (progress.quest) {
            Quest.TREASURE -> if (found(Effect.CHEST_FOUND)) PlayEffects.Carried.CHEST else PlayEffects.Carried.MAP
            Quest.DRAGON_EGG -> if (found(Effect.EGG_FOUND)) PlayEffects.Carried.EGG else null
            else -> null
        }
    }

    /**
     * **Das Wetter haelt auf.** Regnet oder schneit es, wenn die Reise zur naechsten Station
     * weiterziehen will, stellt sich das Wesen unter und wartet - hoechstens [WEATHER_DELAY_MINUTES]
     * Minuten, dann geht es trotzdem weiter.
     */
    fun waitsOutWeather(station: Station, minuteOfDay: Int, falling: Boolean): Boolean =
        falling && questMinute(minuteOfDay) - station.fromMinute < WEATHER_DELAY_MINUTES

    /**
     * Ob das Wesen wegen des Wetters an der vorigen Station bleibt, statt weiterzuziehen - nur
     * wenn es tatsaechlich an einer Station dieser Reise steht.
     */
    fun holdsForWeather(progress: Progress, minuteOfDay: Int, currentPlace: Place, falling: Boolean): Boolean {
        val station = stationAt(progress, minuteOfDay) ?: return false
        if (currentPlace == station.place) return false
        if (planFor(progress).stations.none { it.place == currentPlace }) return false
        return waitsOutWeather(station, minuteOfDay, falling)
    }

    /** Wie lange schlechtes Wetter das Weiterziehen hoechstens aufhaelt. */
    const val WEATHER_DELAY_MINUTES = 60

    /** Die Faehigkeit zu zaubern - seit der Zauberlehre. */
    fun canCastMagic(rewards: Set<Reward>): Boolean = Reward.MAGIC in rewards

    /** Ob die Kristallgrotte entdeckt ist. */
    fun grottoDiscovered(rewards: Set<Reward>): Boolean = Reward.GROTTO in rewards

    /**
     * **Was davon zu Hause steht.** Beim Drachen immer nur der juengste Stand - erst das Ei, dann
     * das Ei mit Rissen, dann das Junge -, nie zwei davon nebeneinander.
     */
    fun acquisitions(rewards: Set<Reward>): Set<PlayScene.Acquisition> = buildSet {
        if (Reward.TREASURE_CHEST in rewards) add(PlayScene.Acquisition.TREASURE_CHEST)
        if (Reward.MAGIC in rewards) add(PlayScene.Acquisition.MAGIC_WAND)
        when {
            Reward.DRAGONLING in rewards -> add(PlayScene.Acquisition.DRAGONLING)
            Reward.DRAGON_EGG_CRACKED in rewards -> add(PlayScene.Acquisition.DRAGON_EGG_CRACKED)
            Reward.DRAGON_EGG in rewards -> add(PlayScene.Acquisition.DRAGON_EGG)
        }
    }

    // ---- Ablage ----

    /**
     * **Der Stand als eine Zeile** - fuer die Ablage (siehe PlayQuestLog). Unbekanntes oder
     * Beschaedigtes liest sich als frischer Anfang statt als Absturz; der Stand der ersten Fassung
     * (sechs Felder) behaelt Quest, Belohnungen und Runde.
     */
    fun encode(progress: Progress): String = listOf(
        progress.quest.name,
        progress.questDayNumber.toString(),
        progress.stepsDone.toString(),
        progress.departed.toString(),
        progress.journeyDone.toString(),
        progress.rewards.joinToString(",") { it.name },
        progress.round.toString(),
        progress.eggFoundDay.toString()
    ).joinToString(";")

    fun decode(text: String?): Progress {
        val parts = text?.split(";") ?: return Progress()
        val quest = Quest.entries.firstOrNull { it.name == parts[0] } ?: return Progress()
        fun rewardsOf(field: String) =
            field.split(",").mapNotNull { n -> Reward.entries.firstOrNull { it.name == n } }.toSet()
        return when (parts.size) {
            8 -> Progress(
                quest = quest,
                questDayNumber = parts[1].toLongOrNull() ?: Long.MIN_VALUE,
                stepsDone = parts[2].toIntOrNull()?.coerceAtLeast(0) ?: 0,
                departed = parts[3] == "true",
                journeyDone = parts[4] == "true",
                rewards = rewardsOf(parts[5]),
                round = parts[6].toIntOrNull()?.coerceAtLeast(0) ?: 0,
                eggFoundDay = parts[7].toLongOrNull() ?: Long.MIN_VALUE
            )
            // Die erste Fassung (drei Stufen je Tag): Quest, Tag der Quest, Questtag, Stufen,
            // Belohnungen, Runde. Der Tagesstand passt nicht mehr - er beginnt neu.
            6 -> Progress(
                quest = quest,
                rewards = rewardsOf(parts[4]),
                round = parts[5].toIntOrNull()?.coerceAtLeast(0) ?: 0
            )
            else -> Progress()
        }
    }

    // ---- Unterwegs ----

    /** Ein Ablauf an der Station und was er an Beduerfnissen stillt (fuer den Living-Kern). */
    data class Wayside(val routine: PlayRoutine, val relief: Map<NeedKind, Double>)

    /**
     * **Das Leben an der Station** zwischen den Momenten der Geschichte. Wer hungrig ist, isst
     * vom Proviant; wer muede ist, rastet; sonst wird die Gegend erkundet - je nach Ort mit einem
     * Blick in die Weite, einem Skizzenbuch, dem Drachen oder der Angel.
     *
     * [hunger] und [energy] sind die Drucke des Living-Kerns (0 = satt/ausgeruht); [roll] waehlt
     * unter dem, was an diesem Ort passt.
     */
    fun wayside(
        place: Place,
        hunger: Double,
        energy: Double,
        rainy: Boolean,
        roll: Int,
        /** Ob schon etwas in der Hand ist (Karte, Ei, Truhe) - dann wird nichts anderes genommen. */
        holding: Boolean = false
    ): Wayside {
        val bench = PlayScene.Station.BENCH in PlayScene.stationsAt(place)
        fun sitDown(): List<RoutineStep> = if (bench) {
            listOf(RoutineStep.GoTo(PlayScene.Station.BENCH), RoutineStep.Occupy(PlayScene.Station.BENCH))
        } else {
            listOf(Stroll(0.3f))
        }
        fun standUp(): List<RoutineStep> = if (bench) listOf(RoutineStep.Rise) else emptyList()

        if (hunger >= HUNGRY) {
            return Wayside(
                r(*(sitDown() + (if (holding) emptyList() else listOf(Take(PlayEffects.Carried.FOOD))) +
                    listOf(RoutineStep.Act(AnimationType.DRINK), Linger(8_000L)) +
                    (if (holding) emptyList() else listOf(RoutineStep.Drop)) + standUp() +
                    Stir(AvatarAnimations.Fidget.STRETCH)).toTypedArray()),
                mapOf(NeedKind.HUNGER to 0.6)
            )
        }
        if (energy >= TIRED || (rainy && Math.floorMod(roll, 2) == 0)) {
            return Wayside(
                r(*(sitDown() + listOf(Linger(14_000L), RoutineStep.Daydream) + standUp() +
                    Stir(AvatarAnimations.Fidget.LOOK_AROUND)).toTypedArray()),
                mapOf(NeedKind.ENERGY to 0.35, NeedKind.COMFORT to 0.2)
            )
        }
        val choices = buildList {
            add(explore(roll))
            add(explore(roll + 1))
            if (place in WIDE_VIEWS) add(view())
            if (place in SKETCH_PLACES) add(sketch())
            if (place in KITE_PLACES && !rainy) add(kite())
            if (place == Place.POND) add(fishing())
            if (bench && !holding) add(read(sitDown(), standUp()))
            if (rainy) add(shelter(sitDown(), standUp()))
        }
        return choices[Math.floorMod(roll, choices.size)]
    }

    /** Ab diesem Hungerdruck wird Proviant ausgepackt. */
    private const val HUNGRY = 0.55

    /** Ab dieser Muedigkeit wird gerastet. */
    private const val TIRED = 0.65

    private val WIDE_VIEWS = setOf(Place.MOUNTAINS, Place.BEACH, Place.PLAINS, Place.GROTTO)
    private val SKETCH_PLACES = setOf(Place.MEADOW, Place.PARK, Place.FOREST, Place.BEACH, Place.POND)
    private val KITE_PLACES = setOf(Place.MEADOW, Place.PLAINS, Place.BEACH)

    private fun explore(roll: Int): Wayside {
        val here = listOf(0.2f, 0.45f, 0.7f, 0.85f)
        val a = here[Math.floorMod(roll, here.size)]
        val b = here[Math.floorMod(roll + 2, here.size)]
        return Wayside(
            r(
                Stroll(a), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(6_000L),
                Stroll(b), Stir(AvatarAnimations.Fidget.STRETCH), Linger(5_000L),
                Stir(AvatarAnimations.Fidget.LOOK_AROUND)
            ),
            mapOf(NeedKind.CURIOSITY to 0.3, NeedKind.FUN to 0.1)
        )
    }

    private fun view() = Wayside(
        r(Stroll(0.6f), RoutineStep.Act(AnimationType.MINDFULNESS), Linger(10_000L),
            Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(4_000L)),
        mapOf(NeedKind.COMFORT to 0.2, NeedKind.CURIOSITY to 0.15)
    )

    private fun sketch() = Wayside(
        r(Stroll(0.4f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), RoutineStep.Act(AnimationType.CREATIVITY),
            Linger(9_000L), Stir(AvatarAnimations.Fidget.STRETCH)),
        mapOf(NeedKind.FUN to 0.2, NeedKind.GROWTH to 0.2)
    )

    private fun kite() = Wayside(
        r(
            Stroll(0.36f),
            RoutineStep.Kite(PlayEffects.KitePhase.PREPARE), Linger(2_500L),
            RoutineStep.Kite(PlayEffects.KitePhase.LAUNCH),
            RoutineStep.Kite(PlayEffects.KitePhase.FLY), Linger(24_000L),
            Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(8_000L),
            RoutineStep.Kite(PlayEffects.KitePhase.LAND), Linger(2_500L)
        ),
        mapOf(NeedKind.FUN to 0.4)
    )

    private fun fishing() = Wayside(
        PlayRoutine(PlayRoutines.fishingRoutine().steps.filterNot { it is GoToPlace }),
        mapOf(NeedKind.FUN to 0.3, NeedKind.COMFORT to 0.1)
    )

    /** Unterstellen: sitzen, in den Regen sehen, sich danach abschuetteln. */
    private fun shelter(sitDown: List<RoutineStep>, standUp: List<RoutineStep>) = Wayside(
        r(*(sitDown + listOf(Linger(10_000L), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(8_000L)) +
            standUp + Stir(AvatarAnimations.Fidget.SHAKE)).toTypedArray()),
        mapOf(NeedKind.COMFORT to 0.15, NeedKind.ENERGY to 0.15)
    )

    private fun read(sitDown: List<RoutineStep>, standUp: List<RoutineStep>) = Wayside(
        r(*(listOf<RoutineStep>(Take(PlayEffects.Carried.BOOK)) + sitDown +
            listOf(RoutineStep.Act(AnimationType.BOOK), Linger(12_000L)) + standUp + RoutineStep.Drop).toTypedArray()),
        mapOf(NeedKind.GROWTH to 0.3, NeedKind.COMFORT to 0.1)
    )

    /**
     * **Weiterziehen** zur naechsten Station: aus der Wohnung ueber die Strasse hinaus, draussen
     * vom einen Ort direkt zum naechsten - zum Rand hinaus, drueben ankommen, sich umsehen.
     */
    fun travel(from: Place, to: Place): PlayRoutine =
        if (PlayScene.isOutdoors(from) && from != Place.STREET) {
            r(Stroll(0.92f), GoToPlace(to), Stroll(0.4f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L))
        } else {
            r(
                GoToPlace(Place.STREET), Stroll(0.7f), Linger(2_500L),
                GoToPlace(to), Stroll(0.4f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
            )
        }

    // ---- Die Reisen ----

    private data class Journey(val stations: List<Station>, val steps: List<Step>)

    private fun r(vararg steps: RoutineStep) = PlayRoutine(steps.toList())

    private fun depart(at: Int, routine: PlayRoutine) = Step(StepKind.DEPART, at, routine)
    private fun moment(at: Int, place: Place, vararg steps: RoutineStep, reward: Reward? = null) =
        Step(StepKind.EXPERIENCE, at, PlayRoutine(listOf(GoToPlace(place)) + steps), reward)
    /**
     * Eine Begegnung: stehen bleiben, sich umsehen, warten - in diesem Verweilen kommt der
     * Bewohner dazu, und solange er bleibt, wartet der Ablauf (siehe PlayVisitWindow).
     */
    private fun encounter(at: Int, place: Place) = Step(
        StepKind.ENCOUNTER, at,
        r(
            GoToPlace(place), Stroll(0.34f), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
            Linger(ENCOUNTER_WAIT_MS), Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L)
        )
    )

    /** So lange wartet das Wesen bei einer Begegnung auf den anderen. */
    const val ENCOUNTER_WAIT_MS = 24_000L

    private fun homecoming(at: Int, routine: PlayRoutine, reward: Reward? = null) =
        Step(StepKind.RETURN, at, routine, reward)

    /** Morgens: den Rucksack packen, ueber die Strasse hinaus zur ersten Station. */
    private fun packAndLeave(first: Place, vararg before: RoutineStep) = r(
        *before,
        GoToPlace(Place.KITCHEN), Stroll(0.5f), Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_500L),
        Stir(AvatarAnimations.Fidget.LOOK_AROUND),
        GoToPlace(Place.STREET), Stroll(0.7f), Linger(3_000L),
        GoToPlace(first), Stroll(0.4f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(2_000L)
    )

    private fun journeyFor(quest: Quest): Journey = when (quest) {
        Quest.TREASURE -> treasure()
        Quest.MAGIC -> magic()
        Quest.EXPEDITION -> expedition()
        Quest.DRAGON_EGG -> dragonEgg()
    }

    /**
     * **Die Schatzsuche.** Beim Fruehstueck flattert eine Karte aus einem Buch. Ueber die Ebene in
     * den Wald - dort wird zweimal vergeblich gegraben -, auf den Berg, von dem aus man das Meer
     * sieht, und am Abend am Strand: Die Karte stimmt, die Truhe steigt aus dem Sand.
     */
    private fun treasure() = Journey(
        stations = listOf(
            Station(Place.PLAINS, h(8)),
            Station(Place.FOREST, h(11)),
            Station(Place.MOUNTAINS, h(14)),
            Station(Place.BEACH, h(17, 30))
        ),
        steps = listOf(
            depart(
                h(8),
                packAndLeave(
                    Place.PLAINS,
                    GoToPlace(Place.NOOK), Stroll(0.5f), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    Quest(Effect.MAP_FOUND), Linger(4_500L), Quest(Effect.MAP_STUDY), Linger(6_000L), Quest(null)
                )
            ),
            moment(
                h(9, 30), Place.PLAINS,
                Take(PlayEffects.Carried.MAP), Stroll(0.6f), Quest(Effect.MAP_STUDY), Linger(5_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
            ),
            moment(
                h(12), Place.FOREST,
                Take(PlayEffects.Carried.MAP), Stroll(0.35f), Quest(Effect.MAP_STUDY), Linger(3_000L),
                Quest(Effect.DIG), Linger(7_000L), Quest(null), Stir(AvatarAnimations.Fidget.SHAKE)
            ),
            moment(
                h(13, 15), Place.FOREST,
                Stroll(0.7f), Quest(Effect.DIG), Linger(6_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(2_000L), Stir(AvatarAnimations.Fidget.SHAKE)
            ),
            moment(
                h(15, 30), Place.MOUNTAINS,
                Stroll(0.7f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(4_000L),
                Take(PlayEffects.Carried.MAP), Quest(Effect.MAP_STUDY), Linger(5_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.STRETCH)
            ),
            encounter(h(16, 45), Place.MOUNTAINS),
            moment(
                h(18, 30), Place.BEACH,
                Take(PlayEffects.Carried.MAP), Stroll(0.5f), Quest(Effect.MAP_STUDY), Linger(3_000L),
                Quest(Effect.DIG), Linger(6_000L), Quest(null), Stir(AvatarAnimations.Fidget.SHAKE)
            ),
            moment(
                h(19, 30), Place.BEACH,
                Stroll(0.3f), Quest(Effect.DIG), Linger(5_000L),
                Quest(Effect.CHEST_FOUND), Linger(8_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.SHAKE), Linger(2_000L)
            ),
            homecoming(
                h(20, 30),
                r(
                    GoToPlace(Place.BEACH), Take(PlayEffects.Carried.CHEST), Stroll(0.92f),
                    GoToPlace(Place.STREET), Stroll(0.5f),
                    GoToPlace(Place.CRAFT), Stroll(0.5f), RoutineStep.Drop,
                    Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L)
                ),
                Reward.TREASURE_CHEST
            )
        )
    )

    /**
     * **Die Zauberlehre.** Morgens ein Buch und ein Versuch, der nur qualmt. Auf der Wiese weiter
     * Rauch, im Wald die ersten Funken, am Teich schon sicherer, und abends im Park gelingt der
     * Sternenregen. Den Zauberstab haengt er zu Hause ueber den Schreibtisch.
     */
    private fun magic() = Journey(
        stations = listOf(
            Station(Place.MEADOW, h(8, 30)),
            Station(Place.FOREST, h(12)),
            Station(Place.POND, h(15)),
            Station(Place.PARK, h(18))
        ),
        steps = listOf(
            depart(
                h(8, 30),
                packAndLeave(
                    Place.MEADOW,
                    GoToPlace(Place.NOOK), Take(PlayEffects.Carried.BOOK), Stroll(0.4f),
                    RoutineStep.Act(AnimationType.BOOK), Linger(3_000L), RoutineStep.Drop,
                    Quest(Effect.SPELL_FIZZLE), Linger(4_000L), Quest(null), Stir(AvatarAnimations.Fidget.SHAKE)
                )
            ),
            moment(
                h(10), Place.MEADOW,
                Stroll(0.3f), Quest(Effect.SPELL_FIZZLE), Linger(5_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.SHAKE), Linger(2_000L)
            ),
            moment(
                h(11, 15), Place.MEADOW,
                Stroll(0.6f), RoutineStep.Act(AnimationType.MINDFULNESS), Linger(3_000L),
                Quest(Effect.SPELL_FIZZLE), Linger(3_000L), Quest(null), Stir(AvatarAnimations.Fidget.LOOK_AROUND)
            ),
            moment(
                h(13), Place.FOREST,
                Stroll(0.4f), RoutineStep.Act(AnimationType.MINDFULNESS), Linger(2_000L),
                Quest(Effect.SPELL_SPARKS), Linger(6_000L), Quest(null), Stir(AvatarAnimations.Fidget.SHAKE)
            ),
            encounter(h(14, 15), Place.FOREST),
            moment(
                h(16), Place.POND,
                Stroll(0.5f), Quest(Effect.SPELL_SPARKS), Linger(6_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.LOOK_AROUND)
            ),
            moment(
                h(17, 15), Place.POND,
                Stroll(0.3f), Quest(Effect.SPELL_FIZZLE), Linger(2_000L),
                Quest(Effect.SPELL_SPARKS), Linger(5_000L), Quest(null)
            ),
            moment(
                h(19, 15), Place.PARK,
                Stroll(0.45f), Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_000L),
                Quest(Effect.SPELL_STARBURST), Linger(10_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L)
            ),
            homecoming(
                h(20, 15),
                r(
                    Stroll(0.92f), GoToPlace(Place.STREET), Stroll(0.5f),
                    GoToPlace(Place.DESK), Stroll(0.5f), Stir(AvatarAnimations.Fidget.STRETCH),
                    Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
                ),
                Reward.MAGIC
            )
        )
    )

    /**
     * **Die Expedition.** Frueh los, weit ueber die Ebene, durch den Sumpf, ins Gebirge - wo es
     * aus dem Fels blitzt -, und am Abend der Eingang zu einer Grotte voller Kristalle. Dort bleibt
     * er eine Weile sitzen, bevor er heimgeht.
     */
    private fun expedition() = Journey(
        stations = listOf(
            Station(Place.PLAINS, h(7, 45)),
            Station(Place.SWAMP, h(10, 30)),
            Station(Place.MOUNTAINS, h(13)),
            Station(Place.GROTTO, h(17))
        ),
        steps = listOf(
            depart(h(7, 45), packAndLeave(Place.PLAINS)),
            moment(
                h(9), Place.PLAINS,
                Stroll(0.8f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(5_000L),
                Stroll(0.3f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
            ),
            encounter(h(10), Place.PLAINS),
            moment(
                h(11, 30), Place.SWAMP,
                Stroll(0.5f), Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L),
                Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
            ),
            moment(
                h(14, 30), Place.MOUNTAINS,
                Stroll(0.4f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L),
                Quest(Effect.CRYSTAL_GLINT), Linger(8_000L), Quest(null),
                Stroll(0.7f), Stir(AvatarAnimations.Fidget.LOOK_AROUND)
            ),
            moment(
                h(16, 15), Place.MOUNTAINS,
                Stroll(0.8f), Quest(Effect.CRYSTAL_GLINT), Linger(6_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_000L)
            ),
            moment(
                h(17, 15), Place.GROTTO,
                Quest(Effect.DISCOVERY), Linger(7_000L), Quest(null), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                reward = Reward.GROTTO
            ),
            moment(
                h(18, 30), Place.GROTTO,
                RoutineStep.GoTo(PlayScene.Station.BENCH), RoutineStep.Occupy(PlayScene.Station.BENCH),
                Linger(8_000L), RoutineStep.Daydream, RoutineStep.Rise, Linger(2_000L)
            ),
            homecoming(
                h(20),
                r(
                    Stroll(0.92f), GoToPlace(Place.MOUNTAINS), Stroll(0.9f),
                    GoToPlace(Place.STREET), Stroll(0.5f),
                    GoToPlace(Place.LIVING), Stroll(0.4f), Stir(AvatarAnimations.Fidget.STRETCH)
                )
            )
        )
    )

    /**
     * **Das Drachenei.** Durch Ebene und Wald ins Gebirge; am Nachmittag liegt dort ein
     * schimmerndes Ei. Er waermt es, und am Abend traegt er es heim ins Nest. In den Tagen danach
     * wird es gewaermt ([careSteps]), bekommt Risse und schluepft.
     */
    private fun dragonEgg() = Journey(
        stations = listOf(
            Station(Place.PLAINS, h(8)),
            Station(Place.FOREST, h(10)),
            Station(Place.MOUNTAINS, h(12, 30))
        ),
        steps = listOf(
            depart(h(8), packAndLeave(Place.PLAINS)),
            moment(
                h(11), Place.FOREST,
                Stroll(0.5f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(4_000L),
                Stir(AvatarAnimations.Fidget.STRETCH)
            ),
            encounter(h(12), Place.FOREST),
            moment(
                h(13, 30), Place.MOUNTAINS,
                Stroll(0.3f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(4_000L),
                Stroll(0.8f), Stir(AvatarAnimations.Fidget.LOOK_AROUND)
            ),
            moment(
                h(15, 30), Place.MOUNTAINS,
                Stroll(0.5f), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                Quest(Effect.EGG_FOUND), Linger(7_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.SHAKE), Linger(2_000L)
            ),
            moment(
                h(16, 30), Place.MOUNTAINS,
                Stroll(0.5f), Quest(Effect.EGG_WARM), Linger(6_000L), Quest(null)
            ),
            homecoming(
                h(18, 30),
                r(
                    GoToPlace(Place.MOUNTAINS), Take(PlayEffects.Carried.EGG), Stroll(0.92f),
                    GoToPlace(Place.STREET), Stroll(0.5f),
                    GoToPlace(Place.BEDROOM), Stroll(0.5f), RoutineStep.Drop,
                    Quest(Effect.EGG_WARM), Linger(5_000L), Quest(null)
                ),
                Reward.DRAGON_EGG
            )
        )
    )

    /** Beim Nest im Schlafzimmer (siehe PlayScene.Acquisition.DRAGON_EGG) waermen. */
    private fun warm(vararg extra: RoutineStep) = r(
        GoToPlace(Place.BEDROOM), Stroll(0.5f),
        Quest(Effect.EGG_WARM), Linger(8_000L), Quest(null),
        *extra, Stir(AvatarAnimations.Fidget.LOOK_AROUND)
    )

    private fun hatch() = r(
        GoToPlace(Place.BEDROOM), Stroll(0.5f),
        Quest(Effect.EGG_WARM), Linger(3_000L),
        Quest(Effect.EGG_HATCH), Linger(12_000L), Quest(null),
        Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L)
    )

    /**
     * **Das Ei zu Hause:** morgens vor dem Aufbruch und abends nach der Heimkehr gewaermt, an
     * Tagen daheim um neun und um acht. Am Abend nach dem ersten ganzen Tag bekommt es Risse, am
     * Abend danach schluepft es.
     */
    private fun careSteps(progress: Progress, kind: DayKind): List<Step> {
        val rewards = progress.rewards
        if (Reward.DRAGON_EGG !in rewards || Reward.DRAGONLING in rewards) return emptyList()
        if (progress.eggFoundDay == Long.MIN_VALUE || progress.questDayNumber <= progress.eggFoundDay) {
            return emptyList()
        }
        val morning = if (kind == DayKind.TRAVEL) h(7, 30) else h(9)
        val evening = if (kind == DayKind.TRAVEL) h(21, 15) else h(20)
        val eveningStep = if (Reward.DRAGON_EGG_CRACKED in rewards) {
            Step(StepKind.CARE, evening, hatch(), Reward.DRAGONLING)
        } else {
            Step(StepKind.CARE, evening, warm(Stir(AvatarAnimations.Fidget.SHAKE), RoutineStep.Daydream), Reward.DRAGON_EGG_CRACKED)
        }
        return listOf(Step(StepKind.CARE, morning, warm()), eveningStep)
    }
}
