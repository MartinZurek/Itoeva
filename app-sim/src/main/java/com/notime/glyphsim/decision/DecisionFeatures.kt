package com.notime.glyphsim.decision

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionCatalog
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayEffects
import com.notime.glyphsim.matrix.PlayGroupGame
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * **Was ein sichtbarer Ablauf IST** - abgeleitet aus seinen Schritten, seinem Thema und seiner
 * Kernwirkung, nicht aus einer gepflegten Liste.
 *
 * Das ist die Stelle, die neue Aktionen automatisch einbindet: Ein neuer Ablauf aus bekannten
 * Schritten bekommt seine Merkmale ohne eine Zeile Code. Ein neuer Schritt-TYP faellt in
 * [stepTraits] als fehlender `when`-Zweig beim Bauen auf - dort sagt man einmal, was er bedeutet
 * (Sport? Gegenstand? Ruhe?), und das Modell versteht ihn, ohne neu geformt zu werden.
 */
data class ActionTraits(
    val physical: Boolean,
    val mental: Boolean,
    val creative: Boolean,
    val game: Boolean,
    val sport: Boolean,
    val eat: Boolean,
    val drink: Boolean,
    val rest: Boolean,
    val sleep: Boolean,
    val explore: Boolean,
    val work: Boolean,
    val learn: Boolean,
    val social: Boolean,
    val music: Boolean,
    val calm: Boolean,
    val group: Boolean,
    val special: Boolean,
    val usesObject: Boolean,
    val media: Boolean,
    /** Wie viele verschiedene Orte der Ablauf betritt. */
    val places: Int,
    val outdoor: Boolean,
    /** Betritt der Ablauf irgendeinen Ort ausserhalb der Wohnung? */
    val leavesHome: Boolean,
    val durationSeconds: Double
) {
    companion object {

        fun of(candidate: ActionCandidate): ActionTraits {
            val routine = candidate.visibleRoutine
            val flags = mutableSetOf<Trait>()
            flags += topicTraits(candidate.topic)
            flags += coreTraits(candidate.coreAction)
            var dauer = 0.0
            for (step in routine.steps) {
                flags += stepTraits(step)
                dauer += stepSeconds(step)
            }
            if (Trait.EAT in flags && Trait.DRINK in flags && hasOnlyCup(routine)) flags -= Trait.EAT
            val orte = routine.steps.mapNotNull { (it as? RoutineStep.GoToPlace)?.place }.toSet()
            return ActionTraits(
                physical = Trait.PHYSICAL in flags,
                mental = Trait.MENTAL in flags,
                creative = Trait.CREATIVE in flags,
                game = Trait.GAME in flags,
                sport = Trait.SPORT in flags,
                eat = Trait.EAT in flags,
                drink = Trait.DRINK in flags,
                rest = Trait.REST in flags,
                sleep = Trait.SLEEP in flags,
                explore = Trait.EXPLORE in flags,
                work = Trait.WORK in flags,
                learn = Trait.LEARN in flags,
                social = Trait.SOCIAL in flags,
                music = Trait.MUSIC in flags,
                calm = Trait.CALM in flags,
                group = candidate.groupGame != null,
                special = PlayRoutines.specialOf(routine) != null || candidate.groupGame != null,
                usesObject = Trait.OBJECT in flags,
                media = Trait.MEDIA in flags,
                places = orte.size,
                outdoor = PlayScene.isOutdoors(candidate.place),
                leavesHome = orte.any { LivingRuntimeAdapterSites.isAway(it) },
                durationSeconds = dauer
            )
        }

        /** Ob ein Schritt etwas TUT (im Gegensatz zu Weg und Warten) - fuer den Hauptort. */
        fun isActivity(step: RoutineStep): Boolean = when (step) {
            is RoutineStep.Act, is RoutineStep.Kite, is RoutineStep.Football,
            is RoutineStep.Basketball, is RoutineStep.Training, is RoutineStep.Music,
            is RoutineStep.Painting, is RoutineStep.Fishing, is RoutineStep.GroupGame,
            is RoutineStep.Switch, is RoutineStep.Occupy, is RoutineStep.Take,
            RoutineStep.SleepUntilMorning, RoutineStep.Daydream -> true
            is RoutineStep.GoTo, is RoutineStep.Stroll, is RoutineStep.Stir,
            is RoutineStep.Linger, is RoutineStep.GoToPlace, RoutineStep.Rise,
            RoutineStep.Drop -> false
        }

        private fun hasOnlyCup(routine: PlayRoutine): Boolean {
            val items = routine.steps.mapNotNull { (it as? RoutineStep.Take)?.item }
            return items.isNotEmpty() && items.all { it == PlayEffects.Carried.CUP }
        }

        /** Grundzuege eines Themas. Vollstaendig: Ein neues Thema muss hier sagen, was es ist. */
        private fun topicTraits(topic: AnimationType): Set<Trait> = when (topic) {
            AnimationType.SLEEP -> setOf(Trait.REST, Trait.SLEEP, Trait.CALM)
            AnimationType.WORK -> setOf(Trait.WORK, Trait.MENTAL)
            AnimationType.DRINK -> setOf(Trait.EAT)
            AnimationType.MEDICINE -> setOf(Trait.CALM)
            AnimationType.REST -> setOf(Trait.REST)
            AnimationType.BOOK -> setOf(Trait.MENTAL, Trait.LEARN)
            AnimationType.FOCUS -> setOf(Trait.MENTAL, Trait.LEARN)
            AnimationType.CREATIVITY -> setOf(Trait.CREATIVE)
            AnimationType.MINDFULNESS -> setOf(Trait.CALM, Trait.REST)
            AnimationType.MOVE -> setOf(Trait.PHYSICAL)
            AnimationType.LOVE -> setOf(Trait.SOCIAL)
            AnimationType.GENERAL -> emptySet()
        }

        /** Was die Kernhandlung ueber den Ablauf sagt. */
        private fun coreTraits(kind: ActionKind): Set<Trait> = when (kind) {
            ActionKind.EXPLORE -> setOf(Trait.EXPLORE, Trait.PHYSICAL)
            ActionKind.MOVE_BODY -> setOf(Trait.PHYSICAL)
            ActionKind.WORK -> setOf(Trait.WORK)
            ActionKind.EAT, ActionKind.INSPECT_FOOD, ActionKind.BUY_FOOD -> setOf(Trait.EAT)
            ActionKind.REST -> setOf(Trait.REST)
            ActionKind.READ, ActionKind.CONCENTRATE -> setOf(Trait.LEARN, Trait.MENTAL)
            ActionKind.CREATE -> setOf(Trait.CREATIVE)
            ActionKind.SETTLE, ActionKind.TEND_SELF -> setOf(Trait.CALM)
            ActionKind.SHOW_AFFECTION, ActionKind.INVITE_TO_PLAY, ActionKind.RESPOND_TO_INVITE,
            ActionKind.RECEIVE_RESPONSE, ActionKind.TRAIN_TOGETHER -> setOf(Trait.SOCIAL)
            ActionKind.PURSUE_INTEREST, ActionKind.TRAVEL -> emptySet()
        }

        /** Was ein einzelner Schritt zeigt. Vollstaendig ueber alle Schritttypen. */
        private fun stepTraits(step: RoutineStep): Set<Trait> = when (step) {
            is RoutineStep.Kite -> setOf(Trait.PHYSICAL, Trait.GAME, Trait.OBJECT)
            is RoutineStep.Football, is RoutineStep.Basketball ->
                setOf(Trait.PHYSICAL, Trait.SPORT, Trait.GAME, Trait.OBJECT)
            is RoutineStep.Training -> setOf(Trait.PHYSICAL, Trait.SPORT, Trait.OBJECT)
            is RoutineStep.Fishing -> setOf(Trait.CALM, Trait.OBJECT)
            is RoutineStep.Music -> setOf(Trait.MUSIC, Trait.CREATIVE, Trait.OBJECT)
            is RoutineStep.Painting -> setOf(Trait.CREATIVE, Trait.OBJECT)
            is RoutineStep.GroupGame -> setOf(Trait.PHYSICAL, Trait.SPORT, Trait.GAME, Trait.SOCIAL)
            is RoutineStep.Switch -> when (step.device) {
                PlayScene.Station.ARCADE -> setOf(Trait.GAME, Trait.MEDIA, Trait.OBJECT)
                PlayScene.Station.TV -> setOf(Trait.MEDIA, Trait.OBJECT)
                else -> setOf(Trait.OBJECT)
            }
            is RoutineStep.Take -> when (step.item) {
                PlayEffects.Carried.FOOD -> setOf(Trait.EAT, Trait.OBJECT)
                PlayEffects.Carried.CUP -> setOf(Trait.DRINK, Trait.OBJECT)
                PlayEffects.Carried.BOOK -> setOf(Trait.LEARN, Trait.OBJECT)
                PlayEffects.Carried.GUITAR -> setOf(Trait.MUSIC, Trait.OBJECT)
                PlayEffects.Carried.EASEL -> setOf(Trait.CREATIVE, Trait.OBJECT)
            }
            is RoutineStep.Occupy -> if (step.station == PlayScene.Station.BED) {
                setOf(Trait.SLEEP, Trait.REST)
            } else {
                emptySet()
            }
            RoutineStep.SleepUntilMorning -> setOf(Trait.SLEEP, Trait.REST)
            RoutineStep.Daydream -> setOf(Trait.CALM)
            is RoutineStep.Act, is RoutineStep.GoTo, is RoutineStep.Stroll, is RoutineStep.Stir,
            is RoutineStep.Linger, is RoutineStep.GoToPlace, RoutineStep.Rise,
            RoutineStep.Drop -> emptySet()
        }

        /** Grobe Dauer je Schritt in Sekunden - fuer "wie lange bindet mich das". */
        private fun stepSeconds(step: RoutineStep): Double = when (step) {
            is RoutineStep.Linger -> step.millis / 1000.0
            is RoutineStep.GroupGame -> PlayGroupGame.DURATION_MS / 1000.0
            RoutineStep.SleepUntilMorning -> 120.0
            is RoutineStep.GoToPlace -> 4.0
            is RoutineStep.GoTo, is RoutineStep.Stroll -> 3.0
            is RoutineStep.Act, is RoutineStep.Kite, is RoutineStep.Football,
            is RoutineStep.Basketball, is RoutineStep.Training, is RoutineStep.Music,
            is RoutineStep.Painting, is RoutineStep.Fishing, RoutineStep.Daydream -> 3.0
            is RoutineStep.Stir, is RoutineStep.Switch, is RoutineStep.Occupy,
            is RoutineStep.Take, RoutineStep.Rise, RoutineStep.Drop -> 1.5
        }
    }

    private enum class Trait {
        PHYSICAL, MENTAL, CREATIVE, GAME, SPORT, EAT, DRINK, REST, SLEEP, EXPLORE, WORK, LEARN,
        SOCIAL, MUSIC, CALM, OBJECT, MEDIA
    }
}

/** Welche sichtbaren Orte "unterwegs" sind - ueber dieselbe Abbildung wie der Kern. */
internal object LivingRuntimeAdapterSites {
    fun isAway(place: PlayScene.Place): Boolean =
        com.notime.glyphsim.matrix.LivingRuntimeAdapter.siteFor(place) != LivingSite.HOME
}

/**
 * **Das Merkmalsschema der Decision Policy, Version [SCHEMA_VERSION].**
 *
 * Drei Bloecke in fester Reihenfolge: der Zustand des Wesens (fuer alle Kandidaten gleich), der
 * Kandidat selbst ([ActionTraits], Kernwirkung) und ihr Zusammenspiel (passt es zum Beduerfnis,
 * zur Persoenlichkeit, war es gerade erst dran, ist jemand da, mit dem man das schon einmal
 * gemacht hat). Keine Texte, keine Einbettungen - jede Zahl hat einen Namen, und die Namen stehen
 * mit im Modell. Ein Modell mit anderem Schema wird nicht geladen (siehe [OnnxDecisionPolicy]).
 *
 * **Wann die Version steigen muss:** sobald sich Anzahl, Reihenfolge oder Bedeutung eines
 * Merkmals aendert. Ein neuer Ablauf, Ort oder Gruppenspiel aendert das Schema NICHT - genau das
 * ist der Sinn der Bewertung je Kandidat.
 */
object DecisionFeatures {

    const val SCHEMA_VERSION = 1

    /** Die Merkmalsnamen in Modellreihenfolge. */
    val NAMES: List<String> by lazy { NameSink().also { write(it, ReferenceInput.state, ReferenceInput.candidate) }.names }

    val COUNT: Int get() = NAMES.size

    /** Ein Kandidat als Merkmalsvektor. */
    fun encode(state: DecisionState, candidate: ActionCandidate): FloatArray {
        val sink = ArraySink(COUNT)
        write(sink, state, candidate)
        return sink.values
    }

    /** Dieselben Werte mit Namen - fuer Lehrer, Datensatz und Erklaerung. */
    fun named(state: DecisionState, candidate: ActionCandidate): Map<String, Double> {
        val sink = MapSink()
        write(sink, state, candidate)
        return sink.map
    }

    private interface Sink {
        fun put(name: String, value: Double)
        fun flag(name: String, value: Boolean) = put(name, if (value) 1.0 else 0.0)
    }

    private class NameSink : Sink {
        val names = mutableListOf<String>()
        override fun put(name: String, value: Double) {
            names += name
        }
    }

    private class ArraySink(size: Int) : Sink {
        val values = FloatArray(size)
        private var i = 0
        override fun put(name: String, value: Double) {
            values[i++] = value.toFloat()
        }
    }

    private class MapSink : Sink {
        val map = LinkedHashMap<String, Double>()
        override fun put(name: String, value: Double) {
            map[name] = value
        }
    }

    private fun write(out: Sink, state: DecisionState, candidate: ActionCandidate) {
        val agent = state.agent
        val world = state.world
        val needs = agent.needs
        // ---- Zustand ----
        for (need in NeedKind.entries) out.put("need_${need.name.lowercase()}", needs.pressure(need))
        out.put("wellbeing", needs.wellbeing())
        for (goal in GoalKind.entries) {
            out.put("bias_${goal.name.lowercase()}", agent.personality.bias(goal) * 10.0)
        }
        for (goal in GoalKind.entries) {
            out.put("learned_${goal.name.lowercase()}", (agent.learnedPreferences[goal] ?: 0.0) * 5.0)
        }
        val goal = state.goal
        for (g in GoalKind.entries) out.flag("goal_${g.name.lowercase()}", goal == g)
        out.flag("goal_none", goal == null)
        for (phase in PlayAmbientActivity.DayPhase.entries) {
            out.flag("phase_${phase.name.lowercase()}", state.phase == phase)
        }
        val winkel = 2.0 * PI * world.minuteOfDay / WorldState.MINUTES_PER_DAY
        out.put("time_sin", sin(winkel))
        out.put("time_cos", cos(winkel))
        for (site in LivingSite.entries) out.flag("site_${site.name.lowercase()}", world.site == site)
        out.flag("here_outdoors", PlayScene.isOutdoors(state.currentPlace))
        val hier = state.othersAt(state.currentPlace)
        val alle = state.presence.values.flatten().toSet()
        out.put("others_here", (hier.size / 4.0).coerceAtMost(1.0))
        out.put("others_anywhere", (alle.size / 6.0).coerceAtMost(1.0))
        out.put("closeness_present", alle.maxOfOrNull { agent.relationships[it]?.closeness ?: 0.0 }?.coerceAtLeast(0.0) ?: 0.0)
        out.put("coins", (world.coins / 10.0).coerceAtMost(1.0))
        out.put("portions", (world.portions / 3.0).coerceAtMost(1.0))
        out.put("since_move", ((state.minutesSinceMove ?: 0L) / 240.0).coerceAtMost(1.0))
        out.flag("since_move_known", state.minutesSinceMove != null)
        out.put("since_outdoors", ((state.minutesSinceOutdoors ?: 0L) / 240.0).coerceAtMost(1.0))
        out.flag("hold_outdoors", state.signals.holdOutdoors)
        out.flag("impulse", state.impulseTopic != null)

        // ---- Kandidat ----
        val t = ActionTraits.of(candidate)
        out.flag("act_physical", t.physical)
        out.flag("act_mental", t.mental)
        out.flag("act_creative", t.creative)
        out.flag("act_game", t.game)
        out.flag("act_sport", t.sport)
        out.flag("act_eat", t.eat)
        out.flag("act_drink", t.drink)
        out.flag("act_rest", t.rest)
        out.flag("act_sleep", t.sleep)
        out.flag("act_explore", t.explore)
        out.flag("act_work", t.work)
        out.flag("act_learn", t.learn)
        out.flag("act_social", t.social)
        out.flag("act_music", t.music)
        out.flag("act_calm", t.calm)
        out.flag("act_group", t.group)
        out.flag("act_special", t.special)
        out.flag("act_object", t.usesObject)
        out.flag("act_media", t.media)
        out.flag("act_outdoor", t.outdoor)
        out.flag("act_leaves_home", t.leavesHome)
        out.put("act_places", (t.places / 3.0).coerceAtMost(1.0))
        out.put("act_duration", (t.durationSeconds / 90.0).coerceAtMost(2.0))
        val core = coreActionOf(candidate.coreAction)
        out.put("act_effort", core.effort * 3.0)
        out.put("act_minutes", (core.outcome.minutes / 180.0).coerceAtMost(1.0))
        for (need in NeedKind.entries) {
            out.put("relief_${need.name.lowercase()}", (core.outcome.needRelief[need] ?: 0.0).coerceIn(-1.0, 1.0))
        }

        // ---- Das Ziel, unter dem der Kandidat laeuft ----
        for (g in GoalKind.entries) out.flag("cand_goal_${g.name.lowercase()}", candidate.goal == g)
        out.put("goal_utility", candidate.goalUtility.coerceIn(-1.0, 1.5))
        out.put("goal_gap", candidate.goalGap.coerceIn(0.0, 1.0))
        out.flag("goal_is_core_choice", candidate.goal != null && candidate.goal == goal)
        out.put("goal_pressure", candidate.goal?.let { needs.pressure(it.drivenBy) } ?: 0.0)

        // ---- Zusammenspiel ----
        val needFit = NeedKind.entries.sumOf { needs.pressure(it) * (core.outcome.needRelief[it] ?: 0.0).coerceAtLeast(0.0) }
        out.put("need_fit", needFit)
        val muede = needs.pressure(NeedKind.ENERGY)
        out.put("fatigue_cost", muede * ((-(core.outcome.needRelief[NeedKind.ENERGY] ?: 0.0)).coerceAtLeast(0.0) * 4.0 + if (t.physical) 0.5 else 0.0))
        val servedGoal = candidate.goal ?: goalFor(candidate.coreAction)
        out.put(
            "personality_fit",
            servedGoal?.let { agent.personality.bias(it) * 10.0 + (agent.learnedPreferences[it] ?: 0.0) * 5.0 } ?: 0.0
        )
        val signals = state.signals
        out.flag("signature_match", signals.signatureTopic == candidate.topic)
        out.flag("leaning_match", candidate.topic in signals.leaning)
        out.flag("habit_match", candidate.topic in signals.boostedTopics)
        out.flag("plan_match", signals.plannedTopic == candidate.topic)
        out.put("afterglow", ((signals.afterglow[candidate.topic] ?: 0) / 4.0).coerceAtMost(1.0))
        out.flag("just_played", signals.justPlayed == candidate.topic)
        out.put("recent_topic", (signals.recentTopics.take(4).count { it == candidate.topic } / 4.0))
        val special = PlayRoutines.specialOf(candidate.visibleRoutine)
        out.flag("recent_special", special != null && special in state.recentSpecials)
        out.put("urge", if (t.physical) signals.movementUrge / PlayAmbientActivity.MAX_MOVEMENT_URGE.toDouble() else 0.0)
        val p = candidate.baselineProbability
        out.put("base_logp", if (p > 0.0) (ln(p).coerceAtLeast(-8.0) / 8.0) else -1.0)
        out.flag("base_zero", p <= 0.0)
        val summe = state.topicWeights.values.sum().coerceAtLeast(1)
        out.put("topic_share", (state.topicWeights[candidate.topic] ?: 0).toDouble() / summe)
        out.flag("stay_match", candidate.place == state.currentPlace)
        val zuletzt = state.history.lastShown(candidate.key)
        val seit = zuletzt?.let { (state.nowMinute - it).coerceAtLeast(0L) }
        out.put("novelty", seit?.let { 1.0 - exp(-it / 360.0) } ?: 1.0)
        out.flag("never_shown", zuletzt == null)
        out.put("repeat_key", (state.history.recentKeyCount(candidate.key) / 3.0).coerceAtMost(1.0))
        out.put("repeat_family", (state.history.recentFamilyCount(candidate.family) / 3.0).coerceAtMost(1.0))
        out.put("repeat_topic", (state.history.recentTopicCount(candidate.topic.name) / 3.0).coerceAtMost(1.0))
        out.flag("resumption", seit != null && seit >= RESUME_AFTER_MINUTES)
        val dort = state.othersAt(candidate.place)
        out.put("others_at_place", (dort.size / 4.0).coerceAtMost(1.0))
        out.put("social_opportunity", if (t.social || t.group) (dort.size / 2.0).coerceAtMost(1.0) else 0.0)
        val gemeinsam = state.history.lastWith(candidate.family, dort)
        out.put("continuity", gemeinsam?.let { exp(-((state.nowMinute - it).coerceAtLeast(0L)) / 1440.0) } ?: 0.0)
        out.put("partner_closeness", dort.maxOfOrNull { agent.relationships[it]?.closeness ?: 0.0 }?.coerceAtLeast(0.0) ?: 0.0)
        out.put(
            "shared_memory",
            (agent.episodes.count { it.valence > 0 && it.event.counterpartProfileId in dort } / 3.0).coerceAtMost(1.0)
        )
        out.put("outdoor_deficit", if (t.outdoor) ((state.minutesSinceOutdoors ?: 0L) / 240.0).coerceAtMost(1.0) else 0.0)
        out.flag("place_change", candidate.place != state.currentPlace)
    }

    /**
     * Die Kernhandlung als Katalogeintrag. Weg, Einladung und Antwort sind im Katalog Funktionen
     * eines Gegenuebers; fuer die Merkmale zaehlt nur ihre Wirkung, nicht wer gemeint ist.
     */
    fun coreActionOf(kind: ActionKind): com.notime.glyphsim.living.Action = when (kind) {
        ActionKind.TRAVEL -> ActionCatalog.travelTo(LivingSite.OUTSIDE)
        ActionKind.INVITE_TO_PLAY -> ActionCatalog.inviteToPlay(PLACEHOLDER_ID)
        ActionKind.TRAIN_TOGETHER -> ActionCatalog.trainTogether(PLACEHOLDER_ID)
        ActionKind.RESPOND_TO_INVITE -> ActionCatalog.respondToInvite(
            PLACEHOLDER_ID, setOf(com.notime.glyphsim.living.SymbolicIntent.YES), true
        )
        ActionKind.RECEIVE_RESPONSE -> ActionCatalog.receiveResponse(
            PLACEHOLDER_ID, setOf(com.notime.glyphsim.living.SymbolicIntent.YES), true
        )
        else -> ActionCatalog[kind]
    }

    private const val PLACEHOLDER_ID = "counterpart"

    /** Ab wann eine Wiederholung eine Wiederaufnahme ist: nach einem halben Tag. */
    const val RESUME_AFTER_MINUTES = 12 * 60L

    /** Welches Ziel eine Kernhandlung bedient - fuer die Passung zur Persoenlichkeit. */
    fun goalFor(kind: ActionKind): GoalKind? = when (kind) {
        ActionKind.EAT, ActionKind.INSPECT_FOOD, ActionKind.BUY_FOOD -> GoalKind.GET_FOOD
        ActionKind.REST -> GoalKind.REST
        ActionKind.WORK -> GoalKind.EARN_MONEY
        ActionKind.READ, ActionKind.CONCENTRATE, ActionKind.CREATE -> GoalKind.DEVELOP
        ActionKind.MOVE_BODY, ActionKind.PURSUE_INTEREST -> GoalKind.HAVE_FUN
        ActionKind.EXPLORE -> GoalKind.EXPLORE
        ActionKind.SETTLE, ActionKind.TEND_SELF -> GoalKind.SEEK_COMFORT
        ActionKind.SHOW_AFFECTION, ActionKind.INVITE_TO_PLAY, ActionKind.RESPOND_TO_INVITE,
        ActionKind.RECEIVE_RESPONSE, ActionKind.TRAIN_TOGETHER -> GoalKind.CONNECT_WITH
        ActionKind.TRAVEL -> null
    }

    /** Ein fester Moment, nur um die Namen einmal abzulaufen. */
    private object ReferenceInput {
        val state = DecisionState(
            agent = com.notime.glyphsim.living.AgentState(
                profileId = "reference",
                personality = com.notime.glyphsim.living.Personality(),
                needs = com.notime.glyphsim.living.Needs.calm()
            ),
            world = WorldState(0, 12 * 60, LivingSite.HOME, 0, 0, setOf(LivingSite.HOME)),
            phase = PlayAmbientActivity.DayPhase.MIDDAY,
            currentPlace = PlayScene.Place.LIVING,
            signals = TopicSignals()
        )
        val candidate = ActionCandidate(
            key = "reference",
            family = "reference",
            topic = AnimationType.GENERAL,
            interestTopic = AnimationType.GENERAL,
            routine = PlayRoutine(emptyList()),
            visibleRoutine = PlayRoutine(emptyList()),
            groupGame = null,
            place = PlayScene.Place.LIVING,
            coreAction = ActionKind.PURSUE_INTEREST,
            goal = GoalKind.HAVE_FUN,
            goalUtility = 0.0,
            goalGap = 0.0,
            baselineProbability = 1.0
        )
    }
}
