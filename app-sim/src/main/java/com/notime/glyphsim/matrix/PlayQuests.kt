package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayQuestEffects.Effect
import com.notime.glyphsim.matrix.RoutineStep.GoToPlace
import com.notime.glyphsim.matrix.RoutineStep.Linger
import com.notime.glyphsim.matrix.RoutineStep.Quest
import com.notime.glyphsim.matrix.RoutineStep.Stir
import com.notime.glyphsim.matrix.RoutineStep.Stroll
import com.notime.glyphsim.matrix.RoutineStep.Take

/**
 * **Die Quests: jeden Tag eine Geschichte, die sich durch den Tag zieht.**
 *
 * Entscheidung des Nutzers vom 28.09.2026: Der Abend war langweilig, und auch sonst fehlte etwas,
 * worauf man beim Einschalten neugierig ist - "Expeditionen, einen Schatz suchen, zaubern lernen,
 * ein Drachenei, das schluepft ... es sollte eine Geschichte erzaehlt werden, einmal am Tag etwas
 * Neues." Auf die Rueckfrage, ob die Quests aus der Simulation entstehen oder feste Ketten sein
 * sollen, fiel die Wahl ausdruecklich auf **feste Questketten**. Damit ist die in LIVING_AGENT.md
 * festgehaltene Regel "kein StoryManager, keine Plot-Skripte" fuer diesen einen Bereich
 * aufgehoben - bewusst, vom Produktverantwortlichen, und in EVOLUTION.md dokumentiert.
 *
 * ## Wie eine Quest aufgebaut ist
 *
 * Jede Quest hat je Tag **drei Stufen** mit festen Anfangszeiten - ab 9, ab 14, ab 20 Uhr - und
 * jede Stufe ist ein gewoehnlicher [PlayRoutine]: dieselben Schritte wie jeder andere Ablauf,
 * dazu die Quest-Bilder ([PlayQuestEffects]). Eine Stufe wird faellig, sobald ihre Stunde
 * gekommen und die vorige erledigt ist; sie bleibt faellig bis zur Schlafenszeit um zwei Uhr.
 * Dazwischen lebt das Wesen seinen Tag wie bisher - die Quest ist ein roter Faden, kein Ersatz.
 *
 * Erzaehlt wird **ohne ein Wort**, wie der Nutzer es fuer den Stream festgelegt hat: Eine Karte
 * flattert aus einem Buch, es wird vergeblich im Wald gegraben, abends steigt am Strand die Truhe
 * aus dem Sand und wird heimgetragen.
 *
 * ## Was bleibt
 *
 * Jede Quest hinterlaesst etwas ([Reward]): die Truhe im Wohnzimmer, den Zauberstab und die
 * Faehigkeit zu zaubern, einen neuen Ort, ein Drachenjunges. Das Drachenei ist die einzige Quest
 * ueber drei Tage - es muss gewaermt werden, bekommt Risse und schluepft.
 *
 * Alles hier ist reine Rechnung ueber [Progress]; gespeichert wird in der Oberflaeche.
 */
object PlayQuests {

    /** Die Quests - [days] Tage lang, in dieser Reihenfolge. */
    enum class Quest(val days: Int) {
        TREASURE(1),
        MAGIC(1),
        EXPEDITION(1),
        DRAGON_EGG(3)
    }

    /** Was eine Quest hinterlaesst. */
    enum class Reward {
        /** Die Truhe im Wohnzimmer. */
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

    /** Eine Stufe: welche Quest, welcher Tag, welche Stufe, ab wann, was geschieht, was bleibt. */
    data class Stage(
        val quest: Quest,
        val day: Int,
        val index: Int,
        val fromHour: Int,
        val routine: PlayRoutine,
        val reward: Reward? = null
    )

    /**
     * Der Stand - [questDayNumber] ist der Kalendertag der Quest (siehe [questDayOf]), an dem
     * [stagesDone] Stufen des Tages [dayOfQuest] erledigt sind.
     */
    data class Progress(
        val quest: Quest = ORDER.first(),
        val dayOfQuest: Int = 0,
        val questDayNumber: Long = Long.MIN_VALUE,
        val stagesDone: Int = 0,
        val rewards: Set<Reward> = emptySet(),
        /** Wie oft alle Quests schon durchlaufen sind - danach wiederholen sie sich. */
        val round: Int = 0
    )

    val ORDER = listOf(Quest.TREASURE, Quest.MAGIC, Quest.EXPEDITION, Quest.DRAGON_EGG)

    /** Ab diesen Stunden werden die drei Stufen eines Tages faellig. */
    val STAGE_HOURS = listOf(9, 14, 20)

    /** Bis hierhin (zwei Uhr) darf eine Stufe noch laufen - dann wird geschlafen. */
    private const val LAST_HOUR = PlayAmbientActivity.BEDTIME_HOUR

    /** Ein Questtag beginnt um sechs Uhr, nicht um Mitternacht: Bis zwei ist ja noch Abend. */
    private const val DAY_STARTS_AT_MINUTE = 6 * 60

    /** Der Questtag zur absoluten Minute [absoluteMinute] (siehe PlayTimeLapse.absoluteMinute). */
    fun questDayOf(absoluteMinute: Long): Long =
        Math.floorDiv(absoluteMinute - DAY_STARTS_AT_MINUTE, 24L * 60L)

    /**
     * **Ein neuer Tag.** Ist die Quest des Vortags ganz erledigt, geht es mit ihrem naechsten Tag
     * oder der naechsten Quest weiter; ist sie es nicht, bleibt sie - eine angefangene Geschichte
     * wird zu Ende erzaehlt, nicht uebersprungen.
     */
    fun rollTo(progress: Progress, questDayNumber: Long): Progress {
        if (progress.questDayNumber == Long.MIN_VALUE) {
            return progress.copy(questDayNumber = questDayNumber, stagesDone = 0)
        }
        if (questDayNumber <= progress.questDayNumber) return progress
        val finishedDay = progress.stagesDone >= STAGE_HOURS.size
        if (!finishedDay) return progress.copy(questDayNumber = questDayNumber)
        return if (progress.dayOfQuest + 1 < progress.quest.days) {
            progress.copy(questDayNumber = questDayNumber, dayOfQuest = progress.dayOfQuest + 1, stagesDone = 0)
        } else {
            val (next, round) = nextQuest(progress.quest, progress.round)
            progress.copy(
                quest = next,
                dayOfQuest = 0,
                questDayNumber = questDayNumber,
                stagesDone = 0,
                round = round
            )
        }
    }

    /**
     * Die naechste Quest. Nach der ersten Runde wiederholen sich Schatz, Zauber und Expedition;
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

    /**
     * **Die faellige Stufe** zur Minute des Tages [minuteOfDay] - oder `null`.
     *
     * Faellig ist die naechste unerledigte Stufe des Tages, sobald ihre Stunde gekommen ist, bis
     * zwei Uhr nachts. Frueh am Morgen (zwei bis neun) ist nichts faellig.
     */
    fun due(progress: Progress, minuteOfDay: Int): Stage? {
        if (progress.stagesDone >= STAGE_HOURS.size) return null
        val stunde = Math.floorMod(minuteOfDay, 24 * 60) / 60
        // Nach Mitternacht zaehlt die Stunde als 24, 25 - sie gehoert noch zum Abend davor.
        val abend = if (stunde < LAST_HOUR) stunde + 24 else stunde
        if (stunde in LAST_HOUR until STAGE_HOURS.first()) return null
        val stage = stagesFor(progress.quest, progress.dayOfQuest)[progress.stagesDone]
        return stage.takeIf { abend >= it.fromHour }
    }

    /** Nach einer erledigten Stufe: eins weiter, und was sie hinterlaesst, bleibt. */
    fun completed(progress: Progress, stage: Stage): Progress {
        if (stage.quest != progress.quest || stage.day != progress.dayOfQuest ||
            stage.index != progress.stagesDone
        ) {
            return progress
        }
        return progress.copy(
            stagesDone = progress.stagesDone + 1,
            rewards = stage.reward?.let { progress.rewards + it } ?: progress.rewards
        )
    }

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

    /** Die drei Stufen einer Quest an ihrem Tag [day]. */
    fun stagesFor(quest: Quest, day: Int): List<Stage> {
        val routines = when (quest) {
            Quest.TREASURE -> treasure()
            Quest.MAGIC -> magic()
            Quest.EXPEDITION -> expedition()
            Quest.DRAGON_EGG -> dragonEgg(day.coerceIn(0, quest.days - 1))
        }
        return routines.mapIndexed { i, (routine, reward) ->
            Stage(quest, day, i, STAGE_HOURS[i], routine, reward)
        }
    }

    private fun r(vararg steps: RoutineStep) = PlayRoutine(steps.toList())

    // ---- Die Schatzsuche ----

    private fun treasure(): List<Pair<PlayRoutine, Reward?>> = listOf(
        // Morgens in der Leseecke: Aus einem Buch flattert eine Karte. Er studiert sie.
        r(
            GoToPlace(PlayScene.Place.NOOK), Stroll(0.5f), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
            Quest(Effect.MAP_FOUND), Linger(4_500L),
            Quest(Effect.MAP_STUDY), Linger(7_000L), Quest(null),
            Take(PlayEffects.Carried.MAP), Stir(AvatarAnimations.Fidget.STRETCH), Linger(3_000L)
        ) to null,
        // Nachmittags im Wald: zweimal gegraben, zweimal nichts.
        r(
            GoToPlace(PlayScene.Place.STREET), Take(PlayEffects.Carried.MAP), Stroll(0.7f),
            GoToPlace(PlayScene.Place.FOREST), Stroll(0.35f), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
            Quest(Effect.MAP_STUDY), Linger(3_000L),
            Quest(Effect.DIG), Linger(6_000L), Quest(null), Stir(AvatarAnimations.Fidget.SHAKE),
            Stroll(0.7f), Quest(Effect.DIG), Linger(5_000L), Quest(null),
            Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
        ) to null,
        // Abends am Strand: die Karte stimmt. Die Truhe steigt aus dem Sand und wird heimgetragen.
        r(
            GoToPlace(PlayScene.Place.STREET), Take(PlayEffects.Carried.MAP),
            GoToPlace(PlayScene.Place.BEACH), Stroll(0.5f),
            Quest(Effect.MAP_STUDY), Linger(3_000L),
            Quest(Effect.DIG), Linger(6_000L),
            Quest(Effect.CHEST_FOUND), Linger(8_000L), Quest(null),
            Stir(AvatarAnimations.Fidget.SHAKE), Take(PlayEffects.Carried.CHEST),
            GoToPlace(PlayScene.Place.STREET), GoToPlace(PlayScene.Place.LIVING), Stroll(0.4f),
            RoutineStep.Drop, Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L)
        ) to Reward.TREASURE_CHEST
    )

    // ---- Die Zauberlehre ----

    private fun magic(): List<Pair<PlayRoutine, Reward?>> = listOf(
        // Morgens: ein Buch, ein Versuch - es qualmt nur.
        r(
            GoToPlace(PlayScene.Place.NOOK), Take(PlayEffects.Carried.BOOK), Stroll(0.4f),
            RoutineStep.Act(com.notime.glyphcore.data.AnimationType.BOOK), Linger(3_000L),
            RoutineStep.Drop,
            Quest(Effect.SPELL_FIZZLE), Linger(5_000L), Quest(null),
            Stir(AvatarAnimations.Fidget.SHAKE), Linger(2_000L)
        ) to null,
        // Nachmittags auf der Wiese: erst wieder Rauch, dann kreisen die ersten Funken.
        r(
            GoToPlace(PlayScene.Place.MEADOW), Stroll(0.3f),
            Quest(Effect.SPELL_FIZZLE), Linger(3_000L),
            Quest(Effect.SPELL_SPARKS), Linger(6_000L), Quest(null),
            Stir(AvatarAnimations.Fidget.LOOK_AROUND),
            Quest(Effect.SPELL_SPARKS), Linger(6_000L), Quest(null)
        ) to null,
        // Abends im Park unter dem Himmel: Es gelingt - ein Sternenregen.
        r(
            GoToPlace(PlayScene.Place.PARK), Stroll(0.45f), Stir(AvatarAnimations.Fidget.STRETCH),
            Linger(2_000L), Quest(Effect.SPELL_STARBURST), Linger(10_000L), Quest(null),
            Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L)
        ) to Reward.MAGIC
    )

    // ---- Die Expedition ----

    private fun expedition(): List<Pair<PlayRoutine, Reward?>> = listOf(
        // Morgens hinaus in die Ebene - weit sehen.
        r(
            GoToPlace(PlayScene.Place.STREET), Stroll(0.7f), GoToPlace(PlayScene.Place.PLAINS),
            Stroll(0.5f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(6_000L),
            Stroll(0.8f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(4_000L)
        ) to null,
        // Nachmittags im Gebirge: Da hinten blitzt etwas.
        r(
            GoToPlace(PlayScene.Place.STREET), GoToPlace(PlayScene.Place.MOUNTAINS), Stroll(0.4f),
            Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L),
            Quest(Effect.CRYSTAL_GLINT), Linger(8_000L), Quest(null),
            Stroll(0.7f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
        ) to null,
        // Abends: hinter dem Gebirge die Grotte - Licht bricht aus dem Fels. Dort bleibt er.
        r(
            GoToPlace(PlayScene.Place.STREET), GoToPlace(PlayScene.Place.MOUNTAINS), Stroll(0.8f),
            GoToPlace(PlayScene.Place.GROTTO),
            Quest(Effect.DISCOVERY), Linger(6_000L), Quest(null),
            Stir(AvatarAnimations.Fidget.LOOK_AROUND),
            RoutineStep.GoTo(PlayScene.Station.BENCH), RoutineStep.Occupy(PlayScene.Station.BENCH),
            Linger(8_000L), RoutineStep.Daydream, RoutineStep.Rise, Linger(2_000L)
        ) to Reward.GROTTO
    )

    // ---- Das Drachenei ----

    /** Beim Nest im Schlafzimmer (siehe PlayScene.Acquisition.DRAGON_EGG) waermen. */
    private fun warm(extra: Array<RoutineStep> = emptyArray()) = r(
        GoToPlace(PlayScene.Place.BEDROOM), Stroll(0.22f),
        Quest(Effect.EGG_WARM), Linger(8_000L), Quest(null),
        *extra, Stir(AvatarAnimations.Fidget.LOOK_AROUND)
    )

    private fun dragonEgg(day: Int): List<Pair<PlayRoutine, Reward?>> = when (day) {
        // Erster Tag: Im Gebirge liegt ein schimmerndes Ei. Es kommt nach Hause, ins Nest.
        0 -> listOf(
            r(
                GoToPlace(PlayScene.Place.STREET), GoToPlace(PlayScene.Place.MOUNTAINS), Stroll(0.5f),
                Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                Quest(Effect.EGG_FOUND), Linger(6_000L), Quest(null),
                Take(PlayEffects.Carried.EGG),
                GoToPlace(PlayScene.Place.STREET), GoToPlace(PlayScene.Place.BEDROOM), Stroll(0.22f),
                RoutineStep.Drop, Quest(Effect.EGG_WARM), Linger(4_000L), Quest(null)
            ) to Reward.DRAGON_EGG,
            warm() to null,
            warm(arrayOf(RoutineStep.Daydream)) to null
        )
        // Zweiter Tag: waermen, waermen - und abends die ersten Risse.
        1 -> listOf(
            warm() to null,
            warm() to null,
            warm(arrayOf(RoutineStep.Daydream)) to Reward.DRAGON_EGG_CRACKED
        )
        // Dritter Tag: abends schluepft es.
        else -> listOf(
            warm() to null,
            warm() to null,
            r(
                GoToPlace(PlayScene.Place.BEDROOM), Stroll(0.22f),
                Quest(Effect.EGG_WARM), Linger(3_000L),
                Quest(Effect.EGG_HATCH), Linger(12_000L), Quest(null),
                Stir(AvatarAnimations.Fidget.SHAKE), Linger(3_000L)
            ) to Reward.DRAGONLING
        )
    }
}
