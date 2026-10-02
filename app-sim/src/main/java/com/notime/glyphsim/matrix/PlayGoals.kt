package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import com.notime.glyphsim.matrix.RoutineStep.Act
import com.notime.glyphsim.matrix.RoutineStep.GoTo
import com.notime.glyphsim.matrix.RoutineStep.GoToPlace
import com.notime.glyphsim.matrix.RoutineStep.Linger
import com.notime.glyphsim.matrix.RoutineStep.Stir
import com.notime.glyphsim.matrix.RoutineStep.Stroll
import com.notime.glyphsim.matrix.RoutineStep.Take

/**
 * **Vorhaben: was sich das Wesen vornimmt - fuer heute und fuer die naechsten Tage.**
 *
 * Gemeldet am 30.09.2026: "Eigentlich braucht er staendig irgendwas, was er tut, ein Ziel.
 * Am besten manchmal langfristiges, manchmal kurzfristiges." Die Reisen (siehe [PlayQuests])
 * fuellen die Reisetage; dazwischen hatte das Wesen nur den Tagesablauf.
 *
 * ## Langfristig: Projekte ueber mehrere Tage
 *
 * Ein Projekt besteht aus mehreren Arbeitsgaengen, hoechstens einer am Tag - am Nachmittag,
 * oder nach einer Tagesreise am Abend, wenn der Arbeitsgang daheim stattfindet. Am Ende steht
 * etwas, das bleibt und das man sieht:
 *
 * - **Das Vogelhaus:** Holz im Wald holen, in der Werkstatt bauen, bemalen, auf der Wiese aufhaengen.
 * - **Der Kraeutertopf:** Samen kaufen, saeen, giessen, ernten - der Topf in der Kueche waechst mit.
 * - **Der eigene Drachen:** Gestell bauen, bespannen, auf der Wiese zum ersten Mal steigen lassen;
 *   danach haengt er in der Leseecke an der Wand.
 *
 * ## Kurzfristig: ein Vorhaben fuer den Tag
 *
 * An Tagen daheim nimmt sich das Wesen eine Sache vor - Pilze sammeln, angeln, den
 * Sonnenuntergang ansehen, Sterne gucken - und tut sie zu ihrer Zeit.
 *
 * Wie bei den Reisen: reine Rechnung, keine Oberflaeche. DockScreen fragt [due] und fuehrt aus.
 */
object PlayGoals {

    enum class Project { BIRDHOUSE, HERBS, KITE }

    enum class Intention { MUSHROOMS, FISHING, SUNSET, STARS }

    enum class Kind { PROJECT, INTENTION }

    /** Was jetzt dran ist: ein Arbeitsgang oder das Tagesvorhaben. */
    data class Step(val kind: Kind, val routine: PlayRoutine)

    data class Progress(
        /** Das laufende Projekt (Index in [Project]) und sein naechster Arbeitsgang. */
        val project: Int = 0,
        val session: Int = 0,
        /** An welchem Tag zuletzt am Projekt gearbeitet wurde - hoechstens einmal am Tag. */
        val lastProjectDay: Long = -1,
        /** An welchem Tag das Tagesvorhaben erledigt wurde. */
        val intentionDay: Long = -1,
        /** Fertige Projekte - ihr Ergebnis steht in der Welt. */
        val finished: Set<Project> = emptySet()
    )

    // ---- Zeiten ----

    /** Arbeitsgaenge draussen: nur bei Tageslicht. */
    private val OUTDOOR_WINDOW = 15 * 60 until 18 * 60

    /** Arbeitsgaenge daheim: auch noch nach einer Tagesreise. */
    private val INDOOR_WINDOW = 15 * 60 until 21 * 60 + 30

    fun windowOf(intention: Intention): IntRange = when (intention) {
        Intention.MUSHROOMS, Intention.FISHING -> 10 * 60 until 13 * 60
        Intention.SUNSET -> 18 * 60 + 30 until 19 * 60 + 30
        Intention.STARS -> 22 * 60 until 23 * 60 + 30
    }

    /** Das Vorhaben des Tages - an aufeinanderfolgenden Tagen jeweils ein anderes. */
    fun intentionFor(day: Long): Intention = Intention.entries[Math.floorMod(day, Intention.entries.size.toLong()).toInt()]

    fun currentProject(progress: Progress): Project =
        Project.entries[Math.floorMod(progress.project, Project.entries.size)]

    /** Visible progress uses the same session count as completion. */
    fun sessionCount(project: Project): Int = sessionsOf(project).size

    /**
     * **Was jetzt dran ist** - oder `null`.
     *
     * [homeDay]: ein Tag ohne Reise (siehe PlayQuests.DayKind.HOME) - nur dann gibt es ein
     * Tagesvorhaben. [away]: das Wesen ist gerade auf einer Reise unterwegs - dann ruht alles.
     */
    fun due(progress: Progress, day: Long, minuteOfDay: Int, homeDay: Boolean, away: Boolean): Step? {
        if (away) return null
        if (homeDay && progress.intentionDay != day) {
            val vorhaben = intentionFor(day)
            if (minuteOfDay in windowOf(vorhaben)) return Step(Kind.INTENTION, routineOf(vorhaben))
        }
        if (progress.lastProjectDay != day) {
            val gang = sessionsOf(currentProject(progress)).getOrNull(progress.session)
            if (gang != null) {
                val fenster = if (gang.outdoors) OUTDOOR_WINDOW else INDOOR_WINDOW
                if (minuteOfDay in fenster) return Step(Kind.PROJECT, gang.routine)
            }
        }
        return null
    }

    /** Nach einem vollstaendig gezeigten Schritt. */
    fun completed(progress: Progress, step: Step, day: Long): Progress = when (step.kind) {
        Kind.INTENTION -> progress.copy(intentionDay = day)
        Kind.PROJECT -> {
            val projekt = currentProject(progress)
            val naechster = progress.session + 1
            if (naechster >= sessionsOf(projekt).size) {
                // Fertig: das naechste Projekt beginnt morgen. Nach dem letzten geht es von vorn
                // los - das Vogelhaus wird neu gestrichen, der Topf neu bepflanzt.
                progress.copy(
                    project = progress.project + 1, session = 0, lastProjectDay = day,
                    finished = progress.finished + projekt
                )
            } else {
                progress.copy(session = naechster, lastProjectDay = day)
            }
        }
    }

    /**
     * Was von den Projekten in der Welt steht. Der Kraeutertopf waechst mit: nach dem Saeen ein
     * Keimling, fertig ein ganzer Busch.
     */
    fun acquisitions(progress: Progress): Set<PlayScene.Acquisition> = buildSet {
        if (Project.BIRDHOUSE in progress.finished) add(PlayScene.Acquisition.BIRDHOUSE)
        if (Project.KITE in progress.finished) add(PlayScene.Acquisition.KITE_WALL)
        when {
            Project.HERBS in progress.finished -> add(PlayScene.Acquisition.HERB_BUSH)
            currentProject(progress) == Project.HERBS && progress.session >= 2 -> add(PlayScene.Acquisition.HERB_SPROUT)
        }
    }

    // ---- Die Arbeitsgaenge ----

    private class Session(val outdoors: Boolean, val routine: PlayRoutine)

    private fun r(vararg steps: RoutineStep) = PlayRoutine(steps.toList())

    private fun sessionsOf(project: Project): List<Session> = when (project) {
        Project.BIRDHOUSE -> listOf(
            // Holz holen: im Wald suchen, aufheben, heim in die Werkstatt tragen.
            Session(
                outdoors = true,
                r(
                    GoToPlace(Place.FOREST), Stroll(0.3f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(4_000L),
                    Stroll(0.62f), Stir(AvatarAnimations.Fidget.STRETCH), Take(PlayEffects.Carried.WOOD), Linger(2_000L),
                    GoToPlace(Place.CRAFT), GoTo(Station.CRAFT), RoutineStep.Drop, Stir(AvatarAnimations.Fidget.STRETCH)
                )
            ),
            // Bauen: saegen und nageln an der Werkbank.
            Session(false, workbench(AnimationType.CREATIVITY, AnimationType.MOVE)),
            // Bemalen.
            Session(false, workbench(AnimationType.CREATIVITY, AnimationType.CREATIVITY)),
            // Aufhaengen auf der Wiese - danach steht es dort.
            Session(
                outdoors = true,
                r(
                    GoToPlace(Place.CRAFT), GoTo(Station.CRAFT), Take(PlayEffects.Carried.BIRDHOUSE),
                    GoToPlace(Place.MEADOW), Stroll(0.5f), Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_500L),
                    RoutineStep.Drop, Stroll(0.45f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(5_000L)
                )
            )
        )
        Project.HERBS -> listOf(
            // Samen kaufen und mit heimnehmen.
            Session(
                outdoors = true,
                r(
                    GoToPlace(Place.SHOP), GoTo(Station.RACK), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(5_000L),
                    Take(PlayEffects.Carried.SEEDS), GoTo(Station.CHECKOUT), Linger(4_000L),
                    GoToPlace(Place.KITCHEN), GoTo(Station.TABLE), RoutineStep.Drop
                )
            ),
            // Saeen und giessen.
            Session(false, kitchenCare(first = true)),
            // Giessen - der Keimling steht schon da.
            Session(false, kitchenCare(first = false)),
            // Ernten und probieren.
            Session(
                outdoors = false,
                r(
                    GoToPlace(Place.KITCHEN), GoTo(Station.TABLE), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    Take(PlayEffects.Carried.FOOD), Linger(2_000L), RoutineStep.Drop,
                    Act(AnimationType.DRINK), Linger(3_000L)
                )
            )
        )
        Project.KITE -> listOf(
            Session(false, workbench(AnimationType.CREATIVITY, AnimationType.FOCUS)),
            Session(false, workbench(AnimationType.CREATIVITY, AnimationType.CREATIVITY)),
            // Der erste Flug auf der Wiese.
            Session(
                outdoors = true,
                r(
                    GoToPlace(Place.MEADOW), Stroll(0.36f),
                    RoutineStep.Kite(PlayEffects.KitePhase.PREPARE), Linger(2_500L),
                    RoutineStep.Kite(PlayEffects.KitePhase.LAUNCH),
                    RoutineStep.Kite(PlayEffects.KitePhase.FLY), Linger(20_000L),
                    Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(6_000L),
                    RoutineStep.Kite(PlayEffects.KitePhase.LAND), Linger(2_500L),
                    Stir(AvatarAnimations.Fidget.STRETCH)
                )
            )
        )
    }

    /** An der Werkbank: zwei Arbeitsgaenge mit Pause dazwischen. */
    private fun workbench(first: AnimationType, second: AnimationType) = r(
        GoToPlace(Place.CRAFT), GoTo(Station.CRAFT),
        Act(first), Linger(6_000L), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
        Act(second), Linger(6_000L), Stir(AvatarAnimations.Fidget.STRETCH)
    )

    /** In der Kueche am Topf: Wasser holen, giessen, nachsehen. */
    private fun kitchenCare(first: Boolean) = r(
        GoToPlace(Place.KITCHEN), GoTo(Station.TABLE),
        *(if (first) arrayOf<RoutineStep>(Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_500L)) else emptyArray()),
        Take(PlayEffects.Carried.CUP), Linger(2_000L), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
        RoutineStep.Drop, Linger(3_000L), Stir(AvatarAnimations.Fidget.STRETCH)
    )

    // ---- Die Tagesvorhaben ----

    fun routineOf(intention: Intention): PlayRoutine = when (intention) {
        // Pilze: im Wald suchen, sich immer wieder buecken, den Korb heimtragen, kochen.
        Intention.MUSHROOMS -> r(
            GoToPlace(Place.FOREST), Stroll(0.25f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L),
            Stroll(0.5f), Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_000L),
            Stroll(0.72f), Stir(AvatarAnimations.Fidget.STRETCH), Take(PlayEffects.Carried.BASKET), Linger(2_000L),
            GoToPlace(Place.KITCHEN), GoTo(Station.TABLE), RoutineStep.Drop, Act(AnimationType.DRINK), Linger(3_000L)
        )
        Intention.FISHING -> PlayRoutine(listOf<RoutineStep>(GoToPlace(Place.POND)) +
            PlayRoutines.fishingRoutine().steps.filterNot { it is GoToPlace })
        // Den Sonnenuntergang von der Wiese aus ansehen.
        Intention.SUNSET -> r(
            GoToPlace(Place.MEADOW), Stroll(0.4f), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
            Act(AnimationType.MINDFULNESS), Linger(15_000L), RoutineStep.Daydream, Linger(4_000L)
        )
        // Sterne gucken auf der Wiese.
        Intention.STARS -> r(
            GoToPlace(Place.MEADOW), Stroll(0.5f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(10_000L),
            RoutineStep.Daydream, Linger(6_000L), Stir(AvatarAnimations.Fidget.YAWN)
        )
    }

    /** Alle Ablaeufe - fuer die Pruefung, dass jeder nur vorhandene Plaetze anspricht. */
    fun allRoutines(): List<PlayRoutine> =
        Project.entries.flatMap { p -> sessionsOf(p).map { it.routine } } + Intention.entries.map(::routineOf)

    // ---- Ablage ----

    fun encode(progress: Progress): String = listOf(
        progress.project, progress.session, progress.lastProjectDay, progress.intentionDay,
        progress.finished.joinToString("+") { it.name }
    ).joinToString(";")

    fun decode(text: String?): Progress {
        val teile = text?.split(";") ?: return Progress()
        if (teile.size != 5) return Progress()
        return runCatching {
            Progress(
                project = teile[0].toInt(),
                session = teile[1].toInt(),
                lastProjectDay = teile[2].toLong(),
                intentionDay = teile[3].toLong(),
                finished = teile[4].split("+").filter { it.isNotBlank() }.map { Project.valueOf(it) }.toSet()
            )
        }.getOrDefault(Progress())
    }
}
