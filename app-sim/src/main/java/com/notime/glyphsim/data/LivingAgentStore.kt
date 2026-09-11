package com.notime.glyphsim.data

import android.content.Context
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.Episode
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingEvent
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.RelationshipState
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.SymbolicIntent
import com.notime.glyphsim.living.WorldState
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Ein gespeicherter Agent mit den Ressourcen seiner eigenen Welt.
 *
 * [savedAtMinute] ist Simulationszeit und keine Wanduhr. Der Runtime-Adapter darf spaeter echte
 * Zeit oder den Zeitraffer hineinreichen, ohne dass diese Schicht gegen eine zweite Uhr rechnet.
 */
data class LivingAgentSnapshot(
    val agent: AgentState,
    val world: WorldState,
    val savedAtMinute: Int
)

/** Ergebnis eines Ladens, einschliesslich eines sichtbar vollzogenen Versionssprungs. */
data class RestoredLivingAgent(
    val agent: AgentState,
    val world: WorldState,
    val migratedFromVersion: Int? = null
)

/**
 * Kleinste austauschbare Ablagegrenze. Der Codec und seine Tests bleiben dadurch reines Kotlin;
 * nur [SharedPreferencesLivingAgentStorage] kennt Android.
 */
interface LivingAgentStorage {
    fun read(profileId: String): String?
    fun write(profileId: String, payload: String)
}

/**
 * Android-Ablage: genau ein versionierter Text je Profil.
 *
 * Ein Blob statt vieler Preferences-Schluessel macht den Snapshot atomar: Ein Prozessabbruch
 * kann nicht neue Beduerfnisse mit alten Beziehungen hinterlassen.
 */
class SharedPreferencesLivingAgentStorage(context: Context) : LivingAgentStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE
    )

    override fun read(profileId: String): String? = preferences.getString(key(profileId), null)

    override fun write(profileId: String, payload: String) {
        preferences.edit().putString(key(profileId), payload).apply()
    }

    private fun key(profileId: String): String =
        "agent_" + LivingAgentSnapshotCodec.encodeText(profileId)

    private companion object {
        const val PREFERENCES = "living_agent_state"
    }
}

/**
 * Profilbezogener, versionierter Zugang zum Agentenzustand.
 *
 * Das Ziel ueberlebt das Laden, der alte Plan nicht: Verfuegbarkeit und anwesende Wesen koennen
 * sich waehrend der Pause geaendert haben. Der naechste Simulationsschritt plant deshalb gegen
 * die aktuelle Welt neu.
 */
class LivingAgentStore(private val storage: LivingAgentStorage) {

    fun save(
        agent: AgentState,
        world: WorldState,
        simulationMinute: Int = world.absoluteMinute
    ) {
        require(agent.profileId.isNotBlank()) { "profileId darf nicht leer sein" }
        storage.write(
            agent.profileId,
            LivingAgentSnapshotCodec.encode(LivingAgentSnapshot(agent, world, simulationMinute))
        )
    }

    fun restore(
        profileId: String,
        currentSimulationMinute: Int,
        currentOpenSites: Set<LivingSite>,
        currentNearbyProfiles: Set<String> = emptySet()
    ): RestoredLivingAgent? {
        val raw = storage.read(profileId) ?: return null
        val decoded = LivingAgentSnapshotCodec.decode(raw) ?: return null
        if (decoded.snapshot.agent.profileId != profileId) return null

        val elapsed = (currentSimulationMinute - decoded.snapshot.savedAtMinute).coerceAtLeast(0)
        val restoredAgent = decoded.snapshot.agent.copy(
            needs = decoded.snapshot.agent.needs.advanced(
                elapsed,
                decoded.snapshot.agent.personality
            ),
            plan = null
        )
        val restoredWorld = decoded.snapshot.world.advanced(elapsed).copy(
            openSites = currentOpenSites,
            nearbyProfiles = currentNearbyProfiles
        )
        val migratedFrom = decoded.sourceVersion.takeIf {
            it < LivingAgentSnapshotCodec.CURRENT_VERSION
        }

        if (migratedFrom != null) {
            storage.write(
                profileId,
                LivingAgentSnapshotCodec.encode(
                    LivingAgentSnapshot(restoredAgent, restoredWorld, currentSimulationMinute)
                )
            )
        }
        return RestoredLivingAgent(restoredAgent, restoredWorld, migratedFrom)
    }
}

/**
 * Deterministischer Textcodec mit expliziter Version.
 *
 * Version 1 kannte den Kernzustand, aber noch keine Episoden, Beziehungen und gelernten
 * Praeferenzen. Version 2 liest diese Felder hinzu. Fehlende V1-Felder werden leer migriert;
 * eine unbekannte Zukunftsversion oder ein beschaedigter Pflichtwert wird nicht geraten.
 */
object LivingAgentSnapshotCodec {
    const val CURRENT_VERSION = 2

    data class Decoded(val snapshot: LivingAgentSnapshot, val sourceVersion: Int)

    fun encode(snapshot: LivingAgentSnapshot): String = buildList {
        add("version=$CURRENT_VERSION")
        add("profile=${encodeText(snapshot.agent.profileId)}")
        add("savedAt=${snapshot.savedAtMinute}")
        add("worldDay=${snapshot.world.day}")
        add("worldMinute=${snapshot.world.minuteOfDay}")
        add("site=${snapshot.world.site.name}")
        add("coins=${snapshot.world.coins}")
        add("portions=${snapshot.world.portions}")
        add("needs=${encodeNeedMap(snapshot.agent.needs)}")
        add("bias=${encodeEnumMap(snapshot.agent.personality.goalBias)}")
        add("growth=${encodeEnumMap(snapshot.agent.personality.needGrowthPerHour)}")
        add("goal=${snapshot.agent.goal?.name ?: NONE}")
        add("preferences=${encodeEnumMap(snapshot.agent.learnedPreferences)}")
        add("episodes=${encodeEpisodes(snapshot.agent.episodes)}")
        add("relationships=${encodeRelationships(snapshot.agent.relationships)}")
        add("lastEvent=${snapshot.agent.lastEvent?.let(::encodeEventToken) ?: NONE}")
    }.joinToString("\n")

    fun decode(raw: String): Decoded? = runCatching {
        val fields = raw.lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0)
                line.substring(0, separator) to line.substring(separator + 1)
            }
        val version = fields.required("version").toInt()
        require(version in 1..CURRENT_VERSION)

        val profileId = decodeText(fields.required("profile"))
        val world = WorldState(
            day = fields.required("worldDay").toInt(),
            minuteOfDay = fields.required("worldMinute").toInt(),
            site = LivingSite.valueOf(fields.required("site")),
            coins = fields.required("coins").toInt(),
            portions = fields.required("portions").toInt(),
            openSites = emptySet()
        )
        val personality = Personality(
            goalBias = decodeEnumMap(fields["bias"].orEmpty(), GoalKind::valueOf),
            needGrowthPerHour = decodeEnumMap(fields["growth"].orEmpty(), NeedKind::valueOf)
        )
        val agent = AgentState(
            profileId = profileId,
            personality = personality,
            needs = Needs(decodeEnumMap(fields.required("needs"), NeedKind::valueOf)),
            goal = fields["goal"].enumOrNull(GoalKind::valueOf),
            plan = null,
            learnedPreferences = if (version >= 2) {
                decodeEnumMap(fields["preferences"].orEmpty(), GoalKind::valueOf)
            } else emptyMap(),
            episodes = if (version >= 2) {
                decodeEpisodes(fields["episodes"].orEmpty()).takeLast(AgentState.MAX_EPISODES)
            } else emptyList(),
            relationships = if (version >= 2) {
                decodeRelationships(fields["relationships"].orEmpty())
            } else emptyMap(),
            lastEvent = fields["lastEvent"]?.takeUnless { it == NONE }?.let(::decodeEventToken)
        )
        Decoded(
            LivingAgentSnapshot(agent, world, fields.required("savedAt").toInt()),
            version
        )
    }.getOrNull()

    internal fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            value.toByteArray(StandardCharsets.UTF_8)
        )

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private fun encodeNeedMap(needs: Needs): String =
        NeedKind.entries.joinToString(";") { "${it.name}:${needs.pressure(it)}" }

    private fun <E : Enum<E>> encodeEnumMap(values: Map<E, Double>): String =
        values.entries.sortedBy { it.key.ordinal }
            .joinToString(";") { "${it.key.name}:${it.value}" }

    private fun <E : Enum<E>> decodeEnumMap(
        raw: String,
        enumValue: (String) -> E
    ): Map<E, Double> =
        raw.split(';').mapNotNull { entry ->
            val separator = entry.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            runCatching {
                enumValue(entry.substring(0, separator)) to
                    entry.substring(separator + 1).toDouble()
            }.getOrNull()
        }.toMap()

    private fun encodeEpisodes(episodes: List<Episode>): String =
        episodes.takeLast(AgentState.MAX_EPISODES).joinToString(";") {
            "${encodeEventToken(it.event)},${it.valence}"
        }

    private fun decodeEpisodes(raw: String): List<Episode> =
        raw.split(';').mapNotNull { entry ->
            val separator = entry.lastIndexOf(',')
            if (separator <= 0) return@mapNotNull null
            runCatching {
                Episode(
                    decodeEventToken(entry.substring(0, separator)),
                    entry.substring(separator + 1).toInt()
                )
            }.getOrNull()
        }

    private fun encodeRelationships(values: Map<String, RelationshipState>): String =
        values.entries.sortedBy { it.key }.joinToString(";") { (profileId, state) ->
            listOf(
                encodeText(profileId),
                state.trust,
                state.closeness,
                state.interactions,
                state.lastInteraction?.let(::encodeEventToken) ?: NONE
            ).joinToString(",")
        }

    private fun decodeRelationships(raw: String): Map<String, RelationshipState> =
        raw.split(';').mapNotNull { entry ->
            val parts = entry.split(',')
            if (parts.size != 5) return@mapNotNull null
            runCatching {
                decodeText(parts[0]) to RelationshipState(
                    trust = parts[1].toDouble(),
                    closeness = parts[2].toDouble(),
                    interactions = parts[3].toInt(),
                    lastInteraction = parts[4].takeUnless { it == NONE }?.let(::decodeEventToken)
                )
            }.getOrNull()
        }.toMap()

    private fun encodeEventToken(event: LivingEvent): String =
        encodeText(
            listOf(
                event.kind.name,
                event.atMinute,
                event.goal?.name ?: NONE,
                event.action?.name ?: NONE,
                encodeRequirement(event.blockedBy),
                event.counterpartProfileId?.let(::encodeText) ?: NONE,
                event.intents.sortedBy { it.ordinal }.joinToString(",") { it.name }
            ).joinToString("|")
        )

    private fun decodeEventToken(token: String): LivingEvent {
        val parts = decodeText(token).split('|')
        require(parts.size == 7)
        return LivingEvent(
            kind = LivingEventKind.valueOf(parts[0]),
            atMinute = parts[1].toInt(),
            goal = parts[2].enumOrNull(GoalKind::valueOf),
            action = parts[3].enumOrNull(ActionKind::valueOf),
            blockedBy = decodeRequirement(parts[4]),
            counterpartProfileId = parts[5].takeUnless { it == NONE }?.let(::decodeText),
            intents = parts[6].split(',').mapNotNull {
                it.takeIf(String::isNotBlank)?.let { name ->
                    runCatching { SymbolicIntent.valueOf(name) }.getOrNull()
                }
            }.toSet()
        )
    }

    private fun encodeRequirement(requirement: Requirement?): String = when (requirement) {
        null -> NONE
        is Requirement.Coins -> "C:${requirement.amount}"
        is Requirement.Portions -> "P:${requirement.amount}"
        is Requirement.At -> "A:${requirement.site.name}"
        is Requirement.SiteOpen -> "O:${requirement.site.name}"
        is Requirement.Near -> "N:${encodeText(requirement.profileId)}"
    }

    private fun decodeRequirement(raw: String): Requirement? {
        if (raw == NONE) return null
        val type = raw.substringBefore(':')
        val value = raw.substringAfter(':', "")
        return when (type) {
            "C" -> Requirement.Coins(value.toInt())
            "P" -> Requirement.Portions(value.toInt())
            "A" -> Requirement.At(LivingSite.valueOf(value))
            "O" -> Requirement.SiteOpen(LivingSite.valueOf(value))
            "N" -> Requirement.Near(decodeText(value))
            else -> error("Unbekannte Voraussetzung")
        }
    }

    private fun <E : Enum<E>> String?.enumOrNull(enumValue: (String) -> E): E? =
        this?.takeUnless { it == NONE || it.isBlank() }?.let(enumValue)

    private fun Map<String, String>.required(key: String): String =
        get(key) ?: error("Pflichtfeld fehlt: $key")

    private const val NONE = "-"
}
