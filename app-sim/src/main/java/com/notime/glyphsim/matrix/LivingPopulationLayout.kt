package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
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

    /**
     * Der naechste wirkliche Gast in fester Rotation - aus derselben Wahrheit wie das Bild.
     *
     * [excludeProfileIds] nimmt die Bewohner heraus, die gerade schon woanders im Bild als
     * eigener Besuch laufen (siehe `visitingProfileIds` in DockScreen) - sonst koennte
     * derselbe Bewohner zweimal gleichzeitig als eigenstaendiger Gast auftauchen, sobald mehrere
     * Besuche parallel laufen duerfen.
     */
    fun nextVisitor(
        snapshots: List<ResidentSnapshot>,
        place: PlayScene.Place,
        previousProfileId: String?,
        excludeProfileIds: Set<String> = emptySet()
    ): ResidentSnapshot? {
        val candidates = presentAt(snapshots, place).filterNot { it.profileId in excludeProfileIds }
        if (candidates.isEmpty()) return null
        val previousIndex = candidates.indexOfFirst { it.profileId == previousProfileId }
        return candidates[(previousIndex + 1).mod(candidates.size)]
    }

    /**
     * Wie viele eigenstaendige, laufende Besucher an diesem Ort gleichzeitig auftreten duerfen.
     *
     * Offene Orte (Park, Sportplatz, Strasse, Stadt) bekommen mehr Platz als enge Raeume: das
     * bestehende Fuenf-Bahnen-Raster (siehe [LANE_COUNT]/[place]) zeigt schon fuer die kleineren,
     * gedimmten Hintergrundfiguren, wie viele Gestalten auf den vierzig Szenenzellen noch lesbar
     * bleiben - fuer die volle Groesse eigenstaendiger Besucher bleibt eine Bahn Puffer zum Wirt.
     */
    fun visitorCapFor(place: PlayScene.Place): Int =
        if (PlayScene.isOutdoors(place)) INTERACTIVE_VISITOR_CAP_OUTDOOR else INTERACTIVE_VISITOR_CAP_INDOOR

    const val INTERACTIVE_VISITOR_CAP_OUTDOOR = 4
    const val INTERACTIVE_VISITOR_CAP_INDOOR = 2

    /**
     * Zielort fuer einen NEUEN eigenstaendigen Besucher (volle Groesse, nicht die halbgrossen
     * Hintergrundfiguren aus [place]) - so, dass er weder den Wirt noch einen bereits
     * anwesenden Besucher ueberdeckt.
     *
     * **Eigenes Bahnenraster statt [LANE_COUNT].** Ein eigenstaendiger Besucher ist so breit wie
     * der Wirt selbst (siehe `VisitorState.sizeDp = host.sizeDp` in DockScreen), nicht nur halb
     * so breit wie die Hintergrundfiguren aus [place] - die fuenf dort fest verteilten Bahnen
     * waeren fuer volle Breite zu eng beieinander und liessen benachbarte Kandidaten selbst dann
     * ueberlappen, wenn noch niemand gewaehlt ist. Der Bahnenabstand richtet sich deshalb nach
     * der tatsaechlichen Gastbreite, gedeckelt auf [MAX_INTERACTIVE_CANDIDATES] Versuche.
     *
     * Liefert `null`, wenn gerade kein Kandidat frei ist - der Aufrufer faellt dann auf eine
     * einfachere, nur wirtsrelative Platzierung zurueck.
     */
    fun pickInteractiveSlot(
        hostLeftFraction: Float,
        hostWidthFraction: Float,
        occupied: List<Pair<Float, Float>>
    ): Float? {
        val hostLeft = hostLeftFraction.coerceIn(0f, 1f)
        val hostWidth = hostWidthFraction.coerceIn(0.01f, 1f)
        val hostRight = (hostLeft + hostWidth).coerceAtMost(1f)
        val guestWidth = hostWidth
        val lastStart = 1f - guestWidth
        if (lastStart < 0f) return null
        val pitch = guestWidth + GAP_FRACTION
        val laneCount = (lastStart / pitch).toInt().coerceIn(0, MAX_INTERACTIVE_CANDIDATES - 1) + 1
        val candidates = (0 until laneCount).map { lane -> (pitch * lane).coerceAtMost(lastStart) }
        return candidates.firstOrNull { candidate ->
            val right = candidate + guestWidth
            separated(candidate, right, hostLeft, hostRight) && occupied.all { (otherLeft, otherWidth) ->
                separated(candidate, right, otherLeft, otherLeft + otherWidth)
            }
        }
    }

    /** Genug Versuche fuer den groessten geplanten Deckel ([INTERACTIVE_VISITOR_CAP_OUTDOOR]) plus Puffer. */
    private const val MAX_INTERACTIVE_CANDIDATES = INTERACTIVE_VISITOR_CAP_OUTDOOR + 2

    /**
     * Findet einen wirklichen Partner fuer eine gemeinsame Sportplatz-Aktivitaet (NT-091, seit
     * dem NT-092-Folgeschnitt auch Basketball).
     *
     * Weder ATHLETE noch WYRMLING noch der Ort allein reichen aus. Der Hauptavatar muss gerade
     * wirklich `MOVE_BODY` abschliessen, die vorhandene Routine muss TRAINING oder BASKETBALL
     * sein, und der Einwohner muss am selben Sportplatz oeffentlich anwesend sein, dieselbe
     * ungehinderte Handlung als naechstes geplant haben - UND sein eigenes, deterministisch aus
     * [LivingPopulation.specialActivityFor] hergeleitetes `nextSpecialActivity` muss mit der
     * Aktivitaet des Hauptavatars UEBEREINSTIMMEN. So beschreibt das Bild zwei wirklich
     * zusammenpassende Handlungen und nicht zwei Etiketten auf einem zufaelligen MOVE_BODY.
     *
     * **Warum dieser letzte Vergleich noetig ist.** Ein frueherer Entwurf liess genau ihn weg und
     * pruefte nur generisches `MOVE_BODY` - dieselbe Bedingung haette damit jede der fuenf
     * Sonderaktivitaeten "belegt", weil der Einwohner nie wirklich sagte, WELCHE er meint. Das
     * hat ein automatisches Review (siehe `EVOLUTION.md`, NT-092) aufgedeckt; die Ruecknahme
     * steht dort. `nextSpecialActivity` schliesst diese Luecke: Der Einwohner traegt jetzt ein
     * eigenes, vom Hauptavatar unabhaengiges Signal, welche der beiden Sportplatz-Aktivitaeten
     * sein `MOVE_BODY` an diesem Simulationstag bedeutet.
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
            it.nextAction == ActionKind.MOVE_BODY &&
                it.blockedBy == null &&
                it.nextSpecialActivity == specialActivity
        }
    }

    /** Die einzigen zwei Sonderaktivitaeten, die heute eine gemeinsame Szene tragen koennen. */
    private val SHARED_SPORT_ACTIVITIES = setOf(
        PlayRoutines.SpecialActivity.TRAINING,
        PlayRoutines.SpecialActivity.BASKETBALL
    )

    /** Wie eine Hintergrundfigur gerade aussieht - siehe [poseFor]. */
    sealed interface ResidentPose {
        /** Sie tut sichtbar etwas: dieselbe Handlung, die der Hauptavatar zu diesem Thema spielt. */
        data class Doing(val topic: AnimationType) : ResidentPose

        /** Sie ist unterwegs und sieht sich um (Erkunden). */
        data object LookingAround : ResidentPose

        /** Sie steht einfach da - unterwegs, zwischen zwei Dingen, oder nichts Zeigbares. */
        data object Idle : ResidentPose
    }

    /**
     * **Was eine Hintergrundfigur gerade TUT - statt immer nur dazustehen.**
     *
     * Gemeldet: "Man erkennt nicht, was die tatsaechlich machen." Die Bevoelkerung wusste es
     * laengst - ein Einwohner im Park liest, sammelt sich oder zeigt Zuneigung -, gezeichnet
     * wurde trotzdem jeder mit derselben Ruhe-Animation. Gemessen (Woche, 8-20 Uhr) hatten die
     * sichtbaren Einwohner zu gut zwei Dritteln eine zeigbare Handlung.
     *
     * Die Zuordnung benutzt dieselben Themen wie der Hauptavatar (siehe
     * `LivingRuntimeAdapter.prepare`), damit Lesen im Hintergrund genauso aussieht wie Lesen im
     * Vordergrund. Medizin bleibt aussen vor - das ist eine Erinnerungsfunktion des Nutzers und
     * nichts, was man einem Nachbarn ansieht.
     */
    fun poseFor(snapshot: ResidentSnapshot): ResidentPose = when (snapshot.currentAction) {
        ActionKind.READ -> ResidentPose.Doing(AnimationType.BOOK)
        ActionKind.CREATE -> ResidentPose.Doing(AnimationType.CREATIVITY)
        ActionKind.CONCENTRATE -> ResidentPose.Doing(AnimationType.FOCUS)
        ActionKind.SETTLE -> ResidentPose.Doing(AnimationType.MINDFULNESS)
        ActionKind.MOVE_BODY -> ResidentPose.Doing(AnimationType.MOVE)
        ActionKind.SHOW_AFFECTION,
        ActionKind.INVITE_TO_PLAY -> ResidentPose.Doing(AnimationType.LOVE)
        ActionKind.WORK -> ResidentPose.Doing(AnimationType.WORK)
        ActionKind.PURSUE_INTEREST -> ResidentPose.Doing(AnimationType.GENERAL)
        ActionKind.REST -> ResidentPose.Doing(AnimationType.REST)
        ActionKind.EAT -> ResidentPose.Doing(AnimationType.DRINK)
        ActionKind.EXPLORE -> ResidentPose.LookingAround
        ActionKind.TEND_SELF,
        ActionKind.INSPECT_FOOD,
        ActionKind.BUY_FOOD,
        ActionKind.TRAVEL,
        ActionKind.RESPOND_TO_INVITE,
        ActionKind.RECEIVE_RESPONSE,
        ActionKind.TRAIN_TOGETHER,
        null -> ResidentPose.Idle
    }

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
