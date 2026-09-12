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

            // Gemessen wird gegen das, was bis JETZT anstand - nicht gegen den ganzen Tag.
            val target = withGoal.sumOf { it.dueByNow }
            val reached = withGoal.sumOf { minOf(it.achieved, it.dueByNow) }

            // Noch nichts faellig. Wer trotzdem schon etwas getan hat, hat etwas Gutes getan -
            // wer noch nichts getan hat, hat nichts versaeumt.
            if (target <= 0) return if (withGoal.any { it.achieved > 0 }) HAPPY else NEUTRAL

            val rate = reached * 100 / target
            return when {
                rate >= 80 -> HAPPY
                rate >= 50 -> CONTENT
                rate >= 20 -> HUNGRY
                else -> SAD
            }
        }

        /**
         * **Wie es dem Wesen geht - aus beiden Quellen** (NT-074).
         *
         * Bis hierher stammte die Stimmung ausschliesslich aus dem Pflegebuch, also daraus, wie
         * der NUTZER seinen Tag gemacht hat. Das war als Erinnerungshilfe richtig gedacht, hatte
         * aber zwei Folgen, die beim Zusehen stoerten:
         *
         * 1. Wer keine Tagesziele gesetzt hat, sah **immer** [NEUTRAL] - ein Wesen ohne jede
         *    Regung, egal was es gerade erlebt.
         * 2. Der Living Agent fuehrt laengst einen vollstaendigen Zustand mit sieben
         *    Beduerfnissen. Dass ein hungriges, muedes, einsames Wesen trotzdem gut gelaunt
         *    aussah, solange die Haekchen stimmten, war der sichtbarste Bruch zwischen dem, was
         *    das Modell weiss, und dem, was das Bild zeigt.
         *
         * [wellbeing] ist [com.notime.glyphsim.living.Needs.wellbeing]: 1 heisst "nichts
         * draengt". Beide Quellen zaehlen gleich viel - die eine sagt, wie der Tag des Nutzers
         * lief, die andere, wie es dem Wesen dabei ergangen ist.
         *
         * **Ohne Tagesziele entscheidet allein das Wohlbefinden.** Das ist der groessere Teil
         * der Aenderung: Aus einem dauerhaft neutralen Gesicht wird eines, das seinem eigenen
         * Zustand folgt.
         *
         * Am Grundsatz aendert das nichts: Es gibt weiterhin kein Verhungern und keine Strafe.
         * Ein Wohlbefinden von 0 ergibt [SAD], nicht mehr - trueber, nie verloren.
         */
        fun of(progress: List<GoalProgress>, wellbeing: Double): AvatarMood {
            val ausDemPflegebuch = fromGoals(progress).takeIf { it != NEUTRAL }?.let(::scoreOf)
            val ausDemWesen = wellbeing.coerceIn(0.0, 1.0)
            val gesamt = ausDemPflegebuch?.let { (it + ausDemWesen) / 2.0 } ?: ausDemWesen
            return moodOf(gesamt)
        }

        /** Die Stimmungsstufen als Zahl, damit sich beide Quellen ueberhaupt mitteln lassen. */
        private fun scoreOf(mood: AvatarMood): Double = when (mood) {
            HAPPY -> 0.9
            CONTENT -> 0.65
            HUNGRY -> 0.35
            SAD -> 0.1
            // fromGoals liefert NEUTRAL nur, wenn es nichts zu bewerten gibt; der Aufrufer
            // oben filtert das vorher heraus, damit "keine Ziele" nicht als "mittelmaessig"
            // in den Mittelwert eingeht.
            NEUTRAL -> 0.5
        }

        /** Dieselben Schwellen wie in [fromGoals], damit beide Wege dasselbe bedeuten. */
        private fun moodOf(score: Double): AvatarMood = when {
            score >= 0.8 -> HAPPY
            score >= 0.5 -> CONTENT
            score >= 0.2 -> HUNGRY
            else -> SAD
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

/**
 * Wie weit ein einzelnes Tagesziel erfuellt ist - **und wie viel davon bis jetzt ueberhaupt
 * anstand.**
 *
 * [expected] ist der Teil, der zum Bewertungszeitpunkt faellig war (siehe [expectedByNow]).
 * Standardmaessig das ganze Ziel, also die Sicht am Tagesende.
 */
data class GoalProgress(val goal: Int, val achieved: Int, val expected: Int = goal) {
    /** [expected], sicherheitshalber in den sinnvollen Bereich gezogen. */
    val dueByNow: Int get() = expected.coerceIn(0, goal)
}

/**
 * Wie viel eines Tagesziels zum Zeitpunkt [minuteOfDay] vernuenftigerweise schon getan sein
 * konnte - anteilig am Zeitfenster der Erinnerung.
 *
 * ## Warum es das braucht
 *
 * Vorher wurde den ganzen Tag gegen das VOLLE Tagesziel gerechnet, ab Mitternacht. Damit stand
 * jeder Morgen bei null Prozent, und selbst wer alles puenktlich tat, hatte bis in den Nachmittag
 * hinein ein truebes Wesen. Ziel 6x trinken, Fenster 8-20 Uhr, stuendlich puenktlich getrunken:
 *
 *     07:00   0 von 6  ->    0%  ->  traurig
 *     09:00   1 von 6  ->   16%  ->  traurig
 *     12:00   2 von 6  ->   33%  ->  hungrig
 *     15:00   3 von 6  ->   50%  ->  zufrieden
 *     20:00   6 von 6  ->  100%  ->  gluecklich
 *
 * Das ist eine Bestrafung dafuer, dass der Tag noch nicht vorbei ist. Wer morgens die App
 * oeffnet, sieht ein trauriges Wesen und hat noch gar nichts falsch gemacht - genau das
 * Schuldgefuehl, das diese App ausdruecklich nicht erzeugen soll (siehe Klassendoku von
 * [AvatarMood]).
 *
 * Anteilig gerechnet ist derselbe Tag durchgehend gut, und ein wirklich uebergangener Tag wird
 * trotzdem noch als solcher sichtbar - nur eben abends und nicht schon beim Aufwachen.
 *
 * ## Die Regeln im Einzelnen
 *
 * Vor Fensterbeginn steht **nichts** an - dann gibt es auch nichts zu bewerten. Nach Fensterende
 * steht das ganze Ziel an. Dazwischen wird abgerundet, also zugunsten des Nutzers: Wer bei "1,8
 * faellig" eines geschafft hat, gilt als vollstaendig auf Kurs.
 *
 * Ein Fenster ohne Dauer (Ende nicht nach dem Anfang - moeglich, weil hier nichts validiert wird)
 * gilt ab seinem Beginn als ganztaegig. Das ganze Ziel schon in derselben Minute zu erwarten,
 * waere die haerteste denkbare Auslegung eines vermutlichen Eingabefehlers.
 */
fun expectedByNow(goal: Int, startMinuteOfDay: Int, endMinuteOfDay: Int, minuteOfDay: Int): Int {
    if (goal <= 0) return 0
    val end = if (endMinuteOfDay > startMinuteOfDay) endMinuteOfDay else END_OF_DAY_MINUTE
    if (minuteOfDay <= startMinuteOfDay) return 0
    if (minuteOfDay >= end) return goal
    return goal * (minuteOfDay - startMinuteOfDay) / (end - startMinuteOfDay)
}

private const val END_OF_DAY_MINUTE = 23 * 60 + 59
