package com.notime.glyphsim.matrix

/**
 * Wie es dem Avatar geht - abgeleitet daraus, wie zuverlaessig zuletzt auf seine Erinnerungen
 * reagiert wurde.
 *
 * **Warum abgeleitet und nicht gespeichert:** ein gespeicherter Hunger-Wert muesste laufend
 * heruntergezaehlt werden, auch waehrend die App gar nicht laeuft. Das braeuchte einen eigenen
 * Zeitgeber, ginge bei jedem verpassten Tick daneben und koennte nach laengerer Pause absurde
 * Werte annehmen. Aus den ohnehin vorhandenen Ereignissen zu rechnen ist immer korrekt, kommt
 * ohne zusaetzlichen Zustand aus und ueberlebt jeden Neustart von selbst.
 *
 * Bewusst KEIN Verhungern: der Avatar wird trueber, nie tot oder verloren. Die App soll an
 * Gewohnheiten erinnern, nicht mit Schuldgefuehlen arbeiten - ein verlorenes Haustier waere ein
 * Grund, die App zu deinstallieren, statt es am naechsten Tag besser zu machen.
 */
enum class AvatarMood {
    /** Nichts ausgeloest, worauf man haette reagieren koennen - neutrale Ausgangslage. */
    NEUTRAL,
    HAPPY,
    CONTENT,
    HUNGRY,
    SAD;

    companion object {
        /**
         * Leitet die Stimmung aus der Erreichung der TAGESZIELE ab.
         *
         * **Warum nicht aus dem Verhaeltnis beantworteter zu ausgeloesten Erinnerungen**, wie es
         * zuerst gerechnet wurde: das unterstellt, jede Ausloesung sei eine Verpflichtung. Eine
         * Erinnerung alle fuenf Minuten ist aber kein hundertfacher Auftrag, sondern ein Netz, das
         * einen guten Moment einfangen soll. Wer viermal am Tag kurz Sport macht, hat sein Ziel
         * voll erreicht - nach der alten Rechnung waren das 4 % und ein dauerhaft trauriger Avatar.
         * Gemessen wird deshalb gegen das, was sich der Nutzer vorgenommen hat, nicht gegen die
         * Stupsfrequenz.
         *
         * Erinnerungen ohne Ziel ([com.notime.glyphcore.data.NO_GOAL]) filtert der Aufrufer
         * heraus; bleibt danach nichts uebrig, gibt es nichts zu bewerten und es bleibt bei
         * [NEUTRAL]. Uebererfuellung zaehlt nicht doppelt (`min(erreicht, ziel)`) - sonst koennte
         * ein uebererfuelltes Ziel ein vernachlaessigtes ausgleichen und die Anzeige waere
         * beschoenigt.
         */
        fun fromGoals(progress: List<GoalProgress>): AvatarMood {
            val withGoal = progress.filter { it.goal > 0 }
            if (withGoal.isEmpty()) return NEUTRAL
            val target = withGoal.sumOf { it.goal }
            val reached = withGoal.sumOf { minOf(it.achieved, it.goal) }
            val rate = reached * 100 / target
            return when {
                rate >= 80 -> HAPPY
                rate >= 50 -> CONTENT
                rate >= 20 -> HUNGRY
                else -> SAD
            }
        }

        /**
         * Bewertungszeitraum: der laufende KALENDERTAG, nicht die letzten 24 Stunden.
         *
         * Ein Tagesziel endet um Mitternacht - ein gleitendes Fenster wuerde die Erfolge von
         * gestern Abend in den heutigen Vormittag mitschleppen und den Avatar am naechsten Morgen
         * grundlos gut dastehen lassen.
         */
        const val WINDOW_MILLIS = 24 * 60 * 60 * 1000L
    }
}

/** Wie weit ein einzelnes Tagesziel erfuellt ist. */
data class GoalProgress(val goal: Int, val achieved: Int)
