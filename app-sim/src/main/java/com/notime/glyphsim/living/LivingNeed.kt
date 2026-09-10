package com.notime.glyphsim.living

import com.notime.glyphsim.matrix.AvatarSpecies

/**
 * **Was ein Wesen von sich aus will** - der Druck, aus dem alles Weitere folgt.
 *
 * Ein Beduerfnis ist eine Zahl zwischen 0 (gestillt) und 1 (draengend). Sie waechst mit der
 * Zeit und faellt durch Handlungen. Nichts an dieser Datei entscheidet etwas; sie liefert nur
 * den Druck, den [UtilitySelector] in ein Ziel uebersetzt.
 *
 * ## Warum die Liste laenger ist als der erste Schnitt braucht
 *
 * Heute bewegt der Kern nur [NeedKind.HUNGER], [NeedKind.ENERGY] und [NeedKind.FUN]. Die
 * uebrigen stehen trotzdem schon da, weil eine spaeter angehaengte Art von Beduerfnis sonst
 * jeden Aufrufer aendern muesste, der ueber alle iteriert. Sie wachsen mit, sie bleiben ohne
 * Wirkung, und die erste Handlung, die sie stillt, kostet dann keine Umbauarbeit.
 */
enum class NeedKind {
    /** Hunger. Der einzige, der heute einen mehrschrittigen Plan erzwingen kann. */
    HUNGER,

    /** Muedigkeit. Waechst langsamer als Hunger und wird durch Arbeit zusaetzlich getrieben. */
    ENERGY,

    /** Langeweile. */
    FUN,

    /** Naehe zu anderen Wesen. Wirkt erst, wenn es echte soziale Handlungen gibt (NT-063b). */
    SOCIAL,

    /** Behaglichkeit und Sicherheit. */
    COMFORT,

    /** Neugier - der Antrieb hinter spaeterem Erkunden und Entdecken. */
    CURIOSITY,

    /** Persoenliche Entwicklung: lernen, besser werden. */
    GROWTH
}

/**
 * Der Beduerfnisstand eines Agenten.
 *
 * Absichtlich eine unveraenderliche Karte und keine sechs Felder: Ein siebtes Beduerfnis soll
 * eine Zeile im Enum kosten und keine im Rest des Kerns.
 */
data class Needs(private val values: Map<NeedKind, Double>) {

    /** Der Druck eines Beduerfnisses, immer zwischen 0 und 1. Unbekannt heisst gestillt. */
    fun pressure(kind: NeedKind): Double = (values[kind] ?: 0.0).coerceIn(0.0, 1.0)

    /** Das draengendste Beduerfnis. Bei Gleichstand entscheidet die Reihenfolge im Enum. */
    fun strongest(): NeedKind = NeedKind.entries.maxBy { pressure(it) * 1000 - it.ordinal }

    /**
     * Der Stand [minutes] Minuten spaeter.
     *
     * Linear und nicht exponentiell: Ein Hunger, der sich der Eins nur naehert, wird nie
     * dringend genug, um einen Plan auszuloesen - und genau das soll er.
     */
    fun advanced(minutes: Int, personality: Personality): Needs {
        if (minutes <= 0) return this
        val stunden = minutes / 60.0
        return Needs(
            NeedKind.entries.associateWith { kind ->
                (pressure(kind) + personality.growthPerHour(kind) * stunden).coerceIn(0.0, 1.0)
            }
        )
    }

    /** Derselbe Stand, ein Beduerfnis um [amount] erleichtert. */
    fun relieved(kind: NeedKind, amount: Double): Needs =
        Needs(values + (kind to (pressure(kind) - amount).coerceIn(0.0, 1.0)))

    /** Derselbe Stand, mehrere Beduerfnisse auf einmal veraendert. Negatives Mass belastet. */
    fun relieved(amounts: Map<NeedKind, Double>): Needs =
        amounts.entries.fold(this) { stand, (kind, amount) -> stand.relieved(kind, amount) }

    companion object {
        /** Alles gestillt - der Ausgangspunkt eines frisch erdachten Agenten im Test. */
        fun calm(): Needs = Needs(emptyMap())

        fun of(vararg pairs: Pair<NeedKind, Double>): Needs = Needs(pairs.toMap())
    }
}

/**
 * **Der Startbias eines Wesens - ausdruecklich nicht sein Schicksal.**
 *
 * Die Persoenlichkeit liegt als Datum im [AgentState] und wird nicht bei jeder Entscheidung aus
 * [AvatarSpecies] nachgeschlagen. Der Unterschied ist der ganze Punkt: Ein Wert, der im Agenten
 * steht, kann sich durch Erlebtes veraendern; einer, der aus einem Enum kommt, kann es nie. Die
 * Handlung, die ihn veraendert, gibt es noch nicht - der Platz dafuer ist da.
 *
 * [goalBias] verschiebt die Zielwahl, [needGrowthPerHour] das Tempo der Beduerfnisse. Beides
 * sind kleine Zahlen: Ein Wesen, dessen Bias den Beduerfnisdruck ueberstimmt, waere kein
 * Charakter, sondern ein Automat mit fester Lieblingsbeschaeftigung.
 */
data class Personality(
    val goalBias: Map<GoalKind, Double> = emptyMap(),
    val needGrowthPerHour: Map<NeedKind, Double> = emptyMap()
) {

    fun bias(goal: GoalKind): Double = goalBias[goal] ?: 0.0

    fun growthPerHour(kind: NeedKind): Double = needGrowthPerHour[kind] ?: DEFAULT_GROWTH[kind] ?: 0.0

    companion object {
        /**
         * Wie schnell ein Beduerfnis ohne persoenliche Abweichung waechst.
         *
         * Hunger mit 0,07 je Stunde heisst: aus einem gestillten Wesen wird in gut vierzehn
         * Stunden ein draengend hungriges. Das ist keine gerundete Zahl, sondern die Absicht,
         * dass ein Tag zwei bis drei Mahlzeiten traegt und nicht zehn.
         */
        val DEFAULT_GROWTH: Map<NeedKind, Double> = mapOf(
            NeedKind.HUNGER to 0.07,
            NeedKind.ENERGY to 0.05,
            NeedKind.FUN to 0.04,
            NeedKind.SOCIAL to 0.03,
            NeedKind.COMFORT to 0.02,
            NeedKind.CURIOSITY to 0.03,
            NeedKind.GROWTH to 0.02
        )

        /**
         * Der Startbias je Spezies, abgeleitet aus dem vorhandenen
         * [AvatarSpecies.signatureTopic] - kein zweiter Charakterkatalog.
         *
         * Bewusst hier und nicht in [AvatarSpecies]: Die Domaene soll die Spezies kennen
         * duerfen, die Spezies aber nichts von Zielen wissen muessen.
         */
        fun of(species: AvatarSpecies): Personality = when (species) {
            // Der neugierige Optimist: schneller gelangweilt, staerker auf Entwicklung aus.
            AvatarSpecies.PUFFLING -> Personality(
                goalBias = mapOf(GoalKind.DEVELOP to 0.06, GoalKind.HAVE_FUN to 0.04),
                needGrowthPerHour = mapOf(NeedKind.CURIOSITY to 0.05)
            )
            // Die Traeumerin: ruhig, braucht mehr Erholung.
            AvatarSpecies.STARLET -> Personality(
                goalBias = mapOf(GoalKind.REST to 0.06),
                needGrowthPerHour = mapOf(NeedKind.ENERGY to 0.06)
            )
            // Der Motivator: viel unterwegs, entsprechend hungriger.
            AvatarSpecies.WYRMLING -> Personality(
                goalBias = mapOf(GoalKind.HAVE_FUN to 0.06),
                needGrowthPerHour = mapOf(NeedKind.HUNGER to 0.09)
            )
            // Der Beschuetzer: haelt den Vorrat gern voll, kuemmert sich frueh.
            AvatarSpecies.FENNEC -> Personality(
                goalBias = mapOf(GoalKind.GET_FOOD to 0.05)
            )
            // Der Entschleuniger: nimmt sich Zeit, wird langsamer muede.
            AvatarSpecies.GLOOP -> Personality(
                goalBias = mapOf(GoalKind.REST to 0.08),
                needGrowthPerHour = mapOf(NeedKind.ENERGY to 0.03)
            )
            // Der Beobachter: still, lernt lieber, als sich zu vergnuegen.
            AvatarSpecies.HOOTLET -> Personality(
                goalBias = mapOf(GoalKind.DEVELOP to 0.08),
                needGrowthPerHour = mapOf(NeedKind.CURIOSITY to 0.04, NeedKind.FUN to 0.02)
            )
        }
    }
}
