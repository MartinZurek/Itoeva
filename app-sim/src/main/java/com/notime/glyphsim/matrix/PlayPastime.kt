package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.RoutineStep.Linger
import com.notime.glyphsim.matrix.RoutineStep.Stir
import com.notime.glyphsim.matrix.RoutineStep.Stroll
import com.notime.glyphsim.matrix.RoutineStep.Watch
import com.notime.glyphsim.matrix.PlayEffects.WatchKind
import com.notime.glyphsim.matrix.PlayEffects.WatchPhase

/**
 * **Die ruhigen Beschaeftigungen zwischen den grossen.**
 *
 * Gemeldet am 30.09.: "Wenn ich reinschalte, macht er ab und zu etwas, zum Beispiel Training -
 * und danach steht da nur ein Bild, der Avatar macht gar nichts, als haette er kein Ziel." Und:
 * "Entspannen ist in Ordnung, aber dann soll man sehen, WAS er tut - einen Vogel beobachten, die
 * Aussicht, sich kurz hinsetzen und nachdenken."
 *
 * Nachgemessen stimmte das Gefuehl: Zwischen zwei Regungen lag eine feste Pause von 18 bis 36
 * Sekunden (abends 25 bis 50), und nur jede zweite Regung war eine Handlung - gut die Haelfte der
 * Zeit stand das Wesen einfach da. Diese Pausen sind jetzt kurz, und statt eines Leerlaufs kommt
 * eine dieser Szenen: klein, ruhig, ohne Ortswechsel und ohne Wirkung im Living-Kern - aber mit
 * erkennbarem Inhalt.
 */
object PlayPastime {

    /** Eine ruhige Szene an [place], passend zu Tageszeit und Wetter. [roll] waehlt. */
    fun scene(place: Place, phase: DayPhase, falling: Boolean, roll: Int): PlayRoutine {
        val choices = choicesFor(place, phase, falling)
        return choices[Math.floorMod(roll, choices.size)]
    }

    /** Alle Szenen, die an diesem Ort zu dieser Zeit passen - nie leer. */
    fun choicesFor(place: Place, phase: DayPhase, falling: Boolean): List<PlayRoutine> {
        val stations = PlayScene.stationsAt(place)
        val sitStation = when {
            PlayScene.Station.BENCH in stations -> PlayScene.Station.BENCH
            PlayScene.Station.SEAT in stations -> PlayScene.Station.SEAT
            else -> null
        }
        val draussen = PlayScene.isOutdoors(place)
        val hell = phase == DayPhase.MORNING || phase == DayPhase.MIDDAY
        return buildList {
            add(thinking(sitStation))
            if (draussen && !falling) {
                if (hell) {
                    add(birdWatching())
                    add(birdWatching())
                    if (place in FLOWERY) add(butterfly())
                    add(view())
                } else {
                    add(stars())
                    add(view())
                }
            }
            if (draussen && falling) add(rainWatching(sitStation))
            if (!draussen) {
                add(windowGaze())
                add(tidyUp())
            }
            if (hell) add(stretching())
        }
    }

    /** Wo Schmetterlinge fliegen. */
    private val FLOWERY = setOf(
        Place.PARK, Place.MEADOW, Place.POND, Place.FOREST, Place.PLAINS, Place.JUNGLE, Place.CAMP
    )

    private fun r(vararg steps: RoutineStep) = PlayRoutine(steps.toList())

    /** Ein Vogel landet daneben: stillhalten, zusehen, wie er pickt, ihm nachsehen. */
    private fun birdWatching() = r(
        Stroll(0.34f),
        Watch(WatchKind.BIRD, WatchPhase.COME), Linger(2_600L),
        Stir(AvatarAnimations.Fidget.LOOK_AROUND),
        Watch(WatchKind.BIRD, WatchPhase.STAY), Linger(9_000L),
        Watch(WatchKind.BIRD, WatchPhase.GO), Linger(2_600L),
        Stir(AvatarAnimations.Fidget.STRETCH)
    )

    /** Ein Schmetterling umkreist den Kopf. */
    private fun butterfly() = r(
        Stroll(0.5f),
        Watch(WatchKind.BUTTERFLY, WatchPhase.COME), Linger(2_600L),
        Watch(WatchKind.BUTTERFLY, WatchPhase.STAY), Linger(4_000L),
        Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(4_000L),
        Watch(WatchKind.BUTTERFLY, WatchPhase.GO), Linger(2_600L)
    )

    /** In die Weite sehen, durchatmen. */
    private fun view() = r(
        Stroll(0.62f), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
        RoutineStep.Act(AnimationType.MINDFULNESS), Linger(8_000L),
        Stir(AvatarAnimations.Fidget.LOOK_AROUND)
    )

    /** Abends nach oben sehen - zu den Sternen, dem Mond. */
    private fun stars() = r(
        Stroll(0.5f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(6_000L),
        RoutineStep.Daydream, Linger(4_000L), Stir(AvatarAnimations.Fidget.YAWN)
    )

    /** Sich hinsetzen und ueber etwas nachdenken - die Gedankenblase zeigt, woran. */
    private fun thinking(sitStation: PlayScene.Station?) = PlayRoutine(
        (if (sitStation != null) {
            listOf(RoutineStep.GoTo(sitStation), RoutineStep.Occupy(sitStation))
        } else {
            listOf(Stroll(0.4f))
        }) + listOf(Linger(4_000L), RoutineStep.Daydream, Linger(5_000L)) +
            (if (sitStation != null) listOf(RoutineStep.Rise) else emptyList()) +
            Stir(AvatarAnimations.Fidget.STRETCH)
    )

    /** Draussen im Regen: unterstellen, zusehen, abschuetteln. */
    private fun rainWatching(sitStation: PlayScene.Station?) = PlayRoutine(
        (if (sitStation != null) {
            listOf(RoutineStep.GoTo(sitStation), RoutineStep.Occupy(sitStation))
        } else {
            listOf(Stroll(0.3f))
        }) + listOf(Linger(8_000L), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(5_000L)) +
            (if (sitStation != null) listOf(RoutineStep.Rise) else emptyList()) +
            Stir(AvatarAnimations.Fidget.SHAKE)
    )

    /** Drinnen ans Fenster (links im Bild) und hinaussehen. */
    private fun windowGaze() = r(
        Stroll(0.02f), Linger(4_000L), Stir(AvatarAnimations.Fidget.LOOK_AROUND),
        Linger(5_000L), RoutineStep.Daydream, Linger(2_000L)
    )

    /** Ein bisschen aufraeumen: hin, her, sich strecken. */
    private fun tidyUp() = r(
        Stroll(0.7f), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(2_000L),
        Stroll(0.25f), Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_000L),
        Stroll(0.5f), Stir(AvatarAnimations.Fidget.SHAKE)
    )

    /** Dehnen, gaehnen, umsehen - der kleine Morgen- und Mittagsbeweger. */
    private fun stretching() = r(
        Stroll(0.45f), Stir(AvatarAnimations.Fidget.STRETCH), Linger(2_500L),
        Stir(AvatarAnimations.Fidget.STRETCH), Stir(AvatarAnimations.Fidget.LOOK_AROUND), Linger(3_000L)
    )
}
