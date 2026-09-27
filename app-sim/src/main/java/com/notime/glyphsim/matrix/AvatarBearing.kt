package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs

/**
 * **Wie es dem Wesen geht, sieht man an seinem Gang** - ohne ein einziges Wort.
 *
 * Bis hierhin ging jede Figur zu jeder Stunde im selben Tempo (nur leicht gestreut, siehe
 * DockScreen.walkDurationMs), und im Stehen lief immer dieselbe Ruhe-Schleife. Die Simulation
 * wusste dabei laengst, ob das Wesen muede, hungrig, gelangweilt oder rundum zufrieden ist - nur
 * das Bild erzaehlte es nicht. Fuer jemanden, der im Stream zusieht, ist das die billigste
 * Charakterzeichnung, die es gibt: Ein Wesen, das um Mitternacht langsamer heimschlurft und im
 * Stehen gaehnt, braucht keine Zeile Text, um "muede" zu sagen.
 *
 * Zwei Hebel, beide reine Rechnung und damit offline pruefbar:
 *
 * - [paceFactor] - das Gehtempo. Muedigkeit bremst, Wohlbefinden beschwingt. Der Ausschlag ist
 *   mit 0,72 bis 1,14 bewusst klein: Er soll auffallen, wenn man zwei Gaenge vergleicht, aber
 *   nie wie ein Zeitlupen- oder Zeitraffereffekt wirken.
 * - [idleFidget] - eine spontane Regung zwischen zwei Durchlaeufen der Ruhe-Schleife: gaehnen,
 *   sich strecken, sich umsehen, sich vor Freude schuetteln. Welche, haengt am draengendsten
 *   Zustand; wie oft, an einem Mindestabstand, damit aus Charakter kein Tick wird.
 *
 * **Nichts davon aendert, WAS das Wesen tut.** Entscheidungen, Ablaeufe und Dauer der
 * Handlungen bleiben unberuehrt - es aendert sich nur, WIE es aussieht, waehrend es sie tut.
 */
object AvatarBearing {

    /** Langsamster Gang (sehr muede, mitten in der Nacht). */
    const val MIN_PACE = 0.72f

    /** Beschwingtester Gang (alles gestillt, heller Tag). */
    const val MAX_PACE = 1.14f

    /**
     * Wie viele Ruhe-Durchlaeufe mindestens zwischen zwei spontanen Regungen liegen. Eine
     * Ruhe-Schleife dauert je nach Wesen drei bis fuenf Sekunden - drei Durchlaeufe sind also gut
     * zehn Sekunden Ruhe, bevor wieder etwas geschieht.
     */
    const val MIN_LOOPS_BETWEEN_FIDGETS = 3

    /**
     * **Wie muede das Wesen wirkt**, 0 bis 1.
     *
     * Der Energiedruck allein reicht nicht: Ein Wesen, das tagsueber geschlafen hat, waere nachts
     * sonst hellwach unterwegs - und genau das liest man im Bild als falsch. Die Tageszeit legt
     * deshalb einen Boden unter die Muedigkeit, ohne sie zu bestimmen.
     */
    fun tiredness(needs: Needs?, dayPhase: PlayAmbientActivity.DayPhase): Double {
        val energy = needs?.pressure(NeedKind.ENERGY) ?: 0.0
        val floor = when (dayPhase) {
            PlayAmbientActivity.DayPhase.NIGHT -> 0.62
            PlayAmbientActivity.DayPhase.EVENING -> 0.30
            PlayAmbientActivity.DayPhase.MORNING, PlayAmbientActivity.DayPhase.MIDDAY -> 0.0
        }
        return maxOf(energy, floor).coerceIn(0.0, 1.0)
    }

    /**
     * **Das Gehtempo relativ zum gewohnten**, zwischen [MIN_PACE] und [MAX_PACE].
     *
     * `null` fuer [needs] heisst: Die Simulation ist noch nicht geladen - dann entscheidet allein
     * die Tageszeit, und tagsueber ist das genau 1.
     */
    fun paceFactor(needs: Needs?, dayPhase: PlayAmbientActivity.DayPhase): Float {
        val tired = tiredness(needs, dayPhase)
        // Unter 0,45 merkt man Muedigkeit noch nicht im Gang - erst darueber wird er schwerer.
        val drag = 0.28 * smoothstep(0.45, 0.95, tired)
        val wellbeing = needs?.wellbeing() ?: 0.5
        val lift = when {
            wellbeing > 0.75 -> 0.12 * ((wellbeing - 0.75) / 0.25)
            wellbeing < 0.35 -> -0.08 * ((0.35 - wellbeing) / 0.35)
            else -> 0.0
        }
        return (1.0 - drag + lift).toFloat().coerceIn(MIN_PACE, MAX_PACE)
    }

    /**
     * **Eine spontane Regung, oder `null`.**
     *
     * [loopsSinceLast] zaehlt die Ruhe-Durchlaeufe seit der letzten Regung, [roll] ist ein
     * Zufallswert in [0, 1) - von aussen hereingereicht, damit sich jede Verzweigung pruefen
     * laesst. Die Reihenfolge der Faelle ist eine Rangfolge: Wer todmuede ist, gaehnt, auch wenn
     * er zugleich neugierig waere.
     */
    fun idleFidget(
        needs: Needs?,
        dayPhase: PlayAmbientActivity.DayPhase,
        loopsSinceLast: Int,
        roll: Float
    ): AvatarAnimations.Fidget? {
        if (loopsSinceLast < MIN_LOOPS_BETWEEN_FIDGETS) return null
        val tired = tiredness(needs, dayPhase)
        val wellbeing = needs?.wellbeing() ?: 0.5
        val restless = maxOf(
            needs?.pressure(NeedKind.CURIOSITY) ?: 0.0,
            needs?.pressure(NeedKind.FUN) ?: 0.0,
            needs?.pressure(NeedKind.HUNGER) ?: 0.0
        )
        return when {
            tired >= 0.65 && roll < 0.40f -> AvatarAnimations.Fidget.YAWN
            // Morgens ausgeruht: der Moment, in dem man sich streckt. Ein Wesen, das das jeden
            // Morgen tut, bekommt fuer Wiederkehrende ein Ritual.
            dayPhase == PlayAmbientActivity.DayPhase.MORNING && tired < 0.4 && roll < 0.30f ->
                AvatarAnimations.Fidget.STRETCH
            // Unruhig - gelangweilt, neugierig oder hungrig: Es sieht sich um, sucht etwas.
            restless >= 0.6 && roll < 0.30f -> AvatarAnimations.Fidget.LOOK_AROUND
            // Rundum zufrieden: ein kleines Schuetteln vor Freude - selten, sonst nutzt es sich ab.
            wellbeing >= 0.8 && roll < 0.15f -> AvatarAnimations.Fidget.SHAKE
            else -> null
        }
    }

    private fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }
}
