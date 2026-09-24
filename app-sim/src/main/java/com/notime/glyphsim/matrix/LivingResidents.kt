package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.WorldState

/** Eine soziale Funktion in der Welt, keine unveraenderliche Charaktervorschrift. */
enum class ResidentRole {
    SHOPKEEPER,
    PARK_REGULAR,
    ATHLETE,
    /** Kein fester Beruf - laeuft einfach in der Stadt herum, wie Fennec dort auch. */
    NEIGHBOR
}

/**
 * Ein nicht waehlbarer Bewohner mit stabiler Identitaet und einem oeffentlichen Ankerort.
 *
 * Die Rolle gibt nur einen kleinen Startbias und ein Anwesenheitsfenster. Beduerfnisse,
 * Erinnerungen und Beziehungen liegen danach wie bei jedem Wesen im [AgentState] und koennen
 * sich auseinanderentwickeln.
 */
data class LivingResident(
    val profileId: String,
    val species: AvatarSpecies,
    val role: ResidentRole,
    val anchorPlace: PlayScene.Place,
    val visitPlaces: Set<PlayScene.Place>,
    val activeFromMinute: Int,
    val activeUntilMinute: Int,
    /**
     * Welche Domaenenorte der Ankerort fuer DIESEN Einwohner vertritt.
     *
     * **Der Grund steht im Laden.** Die Verkaufskraft arbeitet dort, aber die Domaene kennt fuer
     * Arbeit nur [LivingSite.WORKPLACE], und `siteFor(SHOP)` ist [LivingSite.MARKET]. Ohne diese
     * Angabe fiel sie beim Arbeiten auf die Kulisse WORK durch und stand im eigenen Laden nur
     * dann, wenn sie dort gerade EINKAUFTE - gemessen an neun von 240 Schnappschuessen.
     *
     * Das ist eine Tatsache ueber die Figur, kein Ablaufskript: Sie sagt, wo ihre Arbeit
     * stattfindet, und nicht, dass sie arbeiten muss. Wird sie hungrig genug, geht sie trotzdem.
     *
     * Leer gelassen heisst "nur der Domaenenort des Ankerorts" - siehe [anchorSites].
     */
    private val extraAnchorSites: Set<LivingSite> = emptySet()
) {
    /** Die Domaenenorte, an denen dieser Einwohner an seinem Ankerort zu sehen ist. */
    val anchorSites: Set<LivingSite>
        get() = extraAnchorSites + LivingRuntimeAdapter.siteFor(anchorPlace)

    fun isActiveAt(minuteOfDay: Int): Boolean =
        minuteOfDay.coerceIn(0, WorldState.MINUTES_PER_DAY - 1) in
            activeFromMinute until activeUntilMinute
}

/**
 * Der Einwohnerkatalog - eine feste Form statt einer zufaelligen, ihre IDs liegen absichtlich
 * ausserhalb der sechs waehlbaren Speziesprofile.
 *
 * Sechs statt drei: Seit der Renderer mehrere Besucher gleichzeitig tragen kann (siehe `visitors`
 * in [com.notime.glyphsim.ui.DockScreen] und [LivingPopulationLayout.visitorCapFor]), waere ein
 * kleinerer Katalog an offenen Orten wie dem Park nie ausgeschoepft - dort waeren trotz Platz
 * fuer vier gleichzeitige Gaeste nie mehr als zwei wirklich anwesend gewesen. Jede der sechs
 * waehlbaren Spezies traegt jetzt genau einen Bewohner: Fennec verkauft, Puffling und Starlet
 * halten sich oft im Park auf, Wyrmling und Hootlet trainieren (Hootlet zusaetzlich oft im
 * Park), Gloop laeuft ohne festen Beruf durch die Stadt.
 */
object LivingResidents {
    val all: List<LivingResident> = listOf(
        LivingResident(
            profileId = "resident:shop:fennec",
            species = AvatarSpecies.FENNEC,
            role = ResidentRole.SHOPKEEPER,
            anchorPlace = PlayScene.Place.SHOP,
            // Verkauf ist ihre feste Rolle; WORK bleibt erreichbar, weil die vorhandene Welt
            // dort ausdruecklich Besuche erlaubt und berufliche Begegnungen sonst verschwinden.
            visitPlaces = setOf(
                PlayScene.Place.SHOP,
                PlayScene.Place.WORK,
                PlayScene.Place.CITY
            ),
            activeFromMinute = 7 * 60,
            activeUntilMinute = 22 * 60,
            // Ihre Arbeit findet im Laden statt und nicht in einem Buero.
            extraAnchorSites = setOf(LivingSite.WORKPLACE)
        ),
        LivingResident(
            profileId = "resident:park:puffling",
            species = AvatarSpecies.PUFFLING,
            role = ResidentRole.PARK_REGULAR,
            anchorPlace = PlayScene.Place.PARK,
            visitPlaces = setOf(
                PlayScene.Place.PARK,
                PlayScene.Place.STREET,
                PlayScene.Place.LIVING
            ),
            activeFromMinute = 8 * 60,
            activeUntilMinute = 21 * 60
        ),
        LivingResident(
            profileId = "resident:sport:wyrmling",
            species = AvatarSpecies.WYRMLING,
            role = ResidentRole.ATHLETE,
            anchorPlace = PlayScene.Place.SPORT,
            visitPlaces = setOf(
                PlayScene.Place.SPORT,
                PlayScene.Place.PARK,
                PlayScene.Place.CITY,
                // Wer sich gern bewegt, misst sich auch gern - am Automaten in der Spielhalle.
                PlayScene.Place.ARCADE
            ),
            activeFromMinute = 6 * 60,
            activeUntilMinute = 22 * 60
        ),
        LivingResident(
            profileId = "resident:park:starlet",
            species = AvatarSpecies.STARLET,
            role = ResidentRole.PARK_REGULAR,
            anchorPlace = PlayScene.Place.PARK,
            visitPlaces = setOf(
                PlayScene.Place.PARK,
                PlayScene.Place.STREET,
                PlayScene.Place.CITY
            ),
            activeFromMinute = 7 * 60,
            activeUntilMinute = 22 * 60
        ),
        // **Reihenfolge ist hier keine Nebensache.** Die Interessens-Rotation in
        // LivingPopulation.interestFor waehlt ueber `(world.day + Listenindex) mod 3` aus einer
        // drei Eintraege langen, rollenspezifischen Themenliste - zwei Bewohner DERSELBEN Rolle
        // duerfen sich deshalb nicht um ein Vielfaches von drei in ihrem Listenindex
        // unterscheiden, sonst waehlen sie an jedem Tag dasselbe Thema und entwickeln identische
        // gelernte Vorlieben (siehe LivingPopulationTest, "die drei Einwohner entwickeln
        // verschiedene Historien"). Hootlet (ATHLETE, Index 4) und Wyrmling (ATHLETE, Index 2)
        // liegen deshalb bewusst nur zwei statt drei Plaetze auseinander; Gloop (NEIGHBOR) traegt
        // als einziger seiner Rolle ohnehin kein Kollisionsrisiko und steht deshalb zuletzt.
        LivingResident(
            profileId = "resident:sport:hootlet",
            species = AvatarSpecies.HOOTLET,
            role = ResidentRole.ATHLETE,
            anchorPlace = PlayScene.Place.SPORT,
            visitPlaces = setOf(
                PlayScene.Place.SPORT,
                PlayScene.Place.PARK,
                PlayScene.Place.STREET
            ),
            activeFromMinute = 7 * 60,
            activeUntilMinute = 19 * 60
        ),
        LivingResident(
            profileId = "resident:city:gloop",
            species = AvatarSpecies.GLOOP,
            role = ResidentRole.NEIGHBOR,
            anchorPlace = PlayScene.Place.CITY,
            visitPlaces = setOf(
                PlayScene.Place.CITY,
                PlayScene.Place.STREET,
                PlayScene.Place.SHOP,
                // Der Nachbar aus der Stadt schaut in der Spielhalle vorbei - sie liegt um die Ecke.
                PlayScene.Place.ARCADE
            ),
            activeFromMinute = 6 * 60,
            activeUntilMinute = 22 * 60
        )
    )

    init {
        require(all.map { it.profileId }.distinct().size == all.size)
        require(all.none { resident -> AvatarSpecies.entries.any { it.name == resident.profileId } })
    }

    /**
     * Liefert den naechsten passenden Bewohner in fester Rotation.
     *
     * Kein Zufall: Derselbe Ort, dieselbe Minute und derselbe Vorgaenger ergeben denselben Gast.
     * Das macht spaeter auch einen Mehrtageslauf der Bevoelkerung reproduzierbar.
     */
    fun nextVisitor(
        place: PlayScene.Place,
        minuteOfDay: Int,
        previousProfileId: String? = null
    ): LivingResident? {
        val candidates = all.filter { place in it.visitPlaces && it.isActiveAt(minuteOfDay) }
        if (candidates.isEmpty()) return null
        val previousIndex = candidates.indexOfFirst { it.profileId == previousProfileId }
        return candidates[(previousIndex + 1).mod(candidates.size)]
    }

    /** Rolle als kleiner Anfangsunterschied, nie als Zwang gegen ein dringendes Beduerfnis. */
    fun initialAgent(resident: LivingResident): AgentState {
        val base = LivingRuntimeAdapter.initialAgent(resident.profileId, resident.species)
        val roleBias = when (resident.role) {
            ResidentRole.SHOPKEEPER -> mapOf(
                GoalKind.EARN_MONEY to 0.08,
                GoalKind.CONNECT_WITH to 0.05
            )
            ResidentRole.PARK_REGULAR -> mapOf(
                GoalKind.EXPLORE to 0.08,
                GoalKind.CONNECT_WITH to 0.04
            )
            ResidentRole.ATHLETE -> mapOf(
                GoalKind.HAVE_FUN to 0.08,
                GoalKind.DEVELOP to 0.05
            )
            // REST wird hier bewusst auf 0 uebersteuert (`+` auf zwei Maps ersetzt einen
            // vorhandenen Schluessel, statt ihn aufzuaddieren - siehe Personality.of): Ohne diese
            // Zeile behielte Gloop seinen speziesseitigen REST-Bias von 0,08 (staerker als der
            // jeder anderen Spezies), und ein Nachbar, der lieber zuhause ausruht, waere oeffentlich
            // kaum zu sehen - genau das, was der urspruengliche Auftrag beheben sollte.
            ResidentRole.NEIGHBOR -> mapOf(
                GoalKind.EXPLORE to 0.08,
                GoalKind.CONNECT_WITH to 0.05,
                GoalKind.REST to 0.0
            )
        }
        return base.copy(
            personality = base.personality.copy(
                goalBias = base.personality.goalBias + roleBias
            )
        )
    }

    /** Eigene Ressourcen am Ankerort; kein Zugriff auf Geld oder Vorrat des Hauptavatars. */
    fun initialWorld(resident: LivingResident, absoluteMinute: Int): WorldState =
        LivingRuntimeAdapter.initialWorld(
            absoluteMinute = absoluteMinute,
            place = resident.anchorPlace,
            coins = 2,
            portions = 3
        )
}
