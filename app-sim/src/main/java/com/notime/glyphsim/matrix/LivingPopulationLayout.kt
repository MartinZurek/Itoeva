package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.ActionKind
import kotlin.math.max

/**
 * Eine kleine, lesbare Projektion der wirklichen Bevoelkerung in die Pixelwelt (NT-089).
 *
 * Der Renderer entscheidet nicht, WER an einem Ort ist. Das steht bereits im
 * [ResidentSnapshot]. Er beantwortet nur die Darstellungsfrage, wo die anwesenden Wesen Platz
 * finden, ohne den Hauptavatar zu verdecken. Auf der normalen Breite von vierzig Szenenzellen
 * passen mehrere Figuren nur verkleinert; deshalb werden hoechstens zwei ruhige
 * Hintergrundwesen gezeigt.
 */
object LivingPopulationLayout {

    const val MAX_VISIBLE_RESIDENTS = 2

    /** Lage und Groesse als Bruchteil der Bildbreite - damit Bildschirm und Clip gleich bauen. */
    data class Placement(
        val resident: ResidentSnapshot,
        val leftFraction: Float,
        val widthFraction: Float
    )

    /**
     * Waehlt ausschliesslich Einwohner, deren EIGENER Zeitstand sie hier als anwesend meldet.
     *
     * [ResidentSnapshot.minuteOfDay] wird absichtlich nicht gegen eine gemeinsame Uhr ersetzt:
     * Eine lange Handlung kann einen Einwohner bis zu drei Stunden weiter tragen als einen
     * anderen. `publiclyPresent` ist die bereits aus genau diesem eigenen Stand abgeleitete
     * Wahrheit. Ein laufender Besuch wird ausgeschlossen, damit dasselbe Wesen nicht zweimal im
     * Bild steht.
     */
    fun place(
        snapshots: List<ResidentSnapshot>,
        place: PlayScene.Place,
        hostLeftFraction: Float,
        hostWidthFraction: Float,
        visitingProfileId: String? = null
    ): List<Placement> {
        val residents = presentAt(snapshots, place)
            .asSequence()
            .filterNot { it.profileId == visitingProfileId }
            .toList()
        if (residents.isEmpty()) return emptyList()

        val hostLeft = hostLeftFraction.coerceIn(0f, 1f)
        val hostWidth = hostWidthFraction.coerceIn(0f, 1f - hostLeft)
        val residentWidth = (hostWidth * RESIDENT_TO_HOST)
            .coerceIn(MIN_RESIDENT_WIDTH, MAX_RESIDENT_WIDTH)
        val hostRight = hostLeft + hostWidth
        val lastStart = 1f - residentWidth
        val candidates = (0 until LANE_COUNT).map { lane ->
            lastStart * lane / (LANE_COUNT - 1)
        }

        val chosen = mutableListOf<Placement>()
        for (resident in residents) {
            val left = candidates.firstOrNull { candidate ->
                val right = candidate + residentWidth
                separated(candidate, right, hostLeft, hostRight) && chosen.all { existing ->
                    separated(
                        candidate,
                        right,
                        existing.leftFraction,
                        existing.leftFraction + existing.widthFraction
                    )
                }
            } ?: continue
            chosen += Placement(resident, left, residentWidth)
            if (chosen.size == MAX_VISIBLE_RESIDENTS) break
        }
        return chosen
    }

    /** Der naechste wirkliche Gast in fester Rotation - aus derselben Wahrheit wie das Bild. */
    fun nextVisitor(
        snapshots: List<ResidentSnapshot>,
        place: PlayScene.Place,
        previousProfileId: String?
    ): ResidentSnapshot? {
        val candidates = presentAt(snapshots, place)
        if (candidates.isEmpty()) return null
        val previousIndex = candidates.indexOfFirst { it.profileId == previousProfileId }
        return candidates[(previousIndex + 1).mod(candidates.size)]
    }

    /**
     * Findet einen wirklichen Partner fuer eine gemeinsame Sportplatz-Aktivitaet (NT-091, seit
     * NT-092 auch Basketball).
     *
     * Weder ATHLETE noch WYRMLING noch der Ort allein reichen aus. Der Hauptavatar muss gerade
     * wirklich `MOVE_BODY` abschliessen, die vorhandene Routine muss eine der beiden hier
     * gefuehrten Sonderaktivitaeten sein, und der Einwohner muss am selben Sportplatz oeffentlich
     * anwesend sein und dieselbe ungehinderte Handlung als naechstes geplant haben. So beschreibt
     * das Bild zwei Handlungen und nicht zwei Etiketten.
     *
     * **Warum nur diese zwei und nicht auch Drachen, Fussball oder Angeln.** Der Einwohner traegt
     * im Kern ausschliesslich das allgemeine `ActionKind.MOVE_BODY` - der Living-Agent-Kern kennt
     * keine einzelne Sportart. Was den Hauptavatar unterscheidet, ist ausschliesslich der bereits
     * wirklich gewaehlte, gleich rendernde Ablauf; das gilt fuer jede der fuenf Sonderaktivitaeten
     * gleichermassen. Der Auftrag verlangt aber hoechstens eine weitere neben TRAINING (NT-092).
     * BASKETBALL ist die naheliegendste: gleicher Ort (SPORT, keine neue Ortszuordnung), gleicher
     * Ablaufaufbau ohne zusaetzlichen Zustand wie den gelernten Fussballtrick. Drachen (PARK) und
     * Angeln (POND) blieben deshalb bewusst aussen vor, nicht weil sie unmoeglich waeren.
     */
    fun sharedSportPartner(
        snapshots: List<ResidentSnapshot>,
        place: PlayScene.Place,
        hostCompletedActions: List<ActionKind>,
        specialActivity: PlayRoutines.SpecialActivity?
    ): ResidentSnapshot? {
        if (place != PlayScene.Place.SPORT ||
            ActionKind.MOVE_BODY !in hostCompletedActions ||
            specialActivity !in SHARED_SPORT_ACTIVITIES
        ) {
            return null
        }
        return presentAt(snapshots, place).firstOrNull {
            it.nextAction == ActionKind.MOVE_BODY && it.blockedBy == null
        }
    }

    /** Die einzigen zwei Sonderaktivitaeten, die heute eine gemeinsame Szene tragen koennen. */
    private val SHARED_SPORT_ACTIVITIES = setOf(
        PlayRoutines.SpecialActivity.TRAINING,
        PlayRoutines.SpecialActivity.BASKETBALL
    )

    /**
     * Waehlt ein Ruhebild mit den echten Haltezeiten und einem Versatz aus der Einwohnerzeit.
     * Dadurch atmen zwei Wesen nicht im Gleichschritt und ihr eigener Zeitstand bleibt auch in
     * der Darstellung wirksam, ohne Zufall oder eine zweite Uhr einzufuehren.
     */
    fun idleFrameIndex(
        holdsMs: List<Long>,
        scenePhase: Int,
        minuteOfDay: Int,
        phaseTickMs: Int
    ): Int {
        if (holdsMs.isEmpty()) return 0
        val duration = holdsMs.sumOf { max(1L, it) }
        val elapsed = ((scenePhase.toLong() + minuteOfDay.toLong()) * max(1, phaseTickMs))
            .mod(duration)
        var boundary = 0L
        for ((index, hold) in holdsMs.withIndex()) {
            boundary += max(1L, hold)
            if (elapsed < boundary) return index
        }
        return holdsMs.lastIndex
    }

    private fun separated(left: Float, right: Float, otherLeft: Float, otherRight: Float): Boolean =
        right + GAP_FRACTION <= otherLeft || left >= otherRight + GAP_FRACTION

    private fun presentAt(
        snapshots: List<ResidentSnapshot>,
        place: PlayScene.Place
    ): List<ResidentSnapshot> = snapshots
        .asSequence()
        .filter { it.publiclyPresent && it.place == place }
        .distinctBy { it.profileId }
        .sortedWith(compareBy<ResidentSnapshot> { it.role.ordinal }.thenBy { it.profileId })
        .toList()

    /** Fuenf feste Bahnen vermeiden Springen, wenn sich die Bildbreite leicht aendert. */
    private const val LANE_COUNT = 5
    private const val RESIDENT_TO_HOST = 0.5f
    private const val MIN_RESIDENT_WIDTH = 0.12f
    private const val MAX_RESIDENT_WIDTH = 0.18f
    private const val GAP_FRACTION = 0.01f
}
