package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.WorldState

/** Eine soziale Funktion in der Welt, keine unveraenderliche Charaktervorschrift. */
enum class ResidentRole {
    SHOPKEEPER,
    PARK_REGULAR,
    ATHLETE
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
    val activeUntilMinute: Int
) {
    fun isActiveAt(minuteOfDay: Int): Boolean =
        minuteOfDay.coerceIn(0, WorldState.MINUTES_PER_DAY - 1) in
            activeFromMinute until activeUntilMinute
}

/**
 * Der erste kleine Einwohnerkatalog.
 *
 * Drei statt einer zufaelligen Form: Der Laden hat eine wiedererkennbare Verkaufskraft, Park
 * und Sportplatz haben je einen Stammgast. Ihre IDs liegen absichtlich ausserhalb der sechs
 * waehlbaren Speziesprofile. Mehr Figuren sind erst sinnvoll, wenn der Renderer mehrere zugleich
 * tragen kann; bis dahin waere ein grosser Katalog nur unsichtbarer Zustand.
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
            activeUntilMinute = 22 * 60
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
                PlayScene.Place.CITY
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
