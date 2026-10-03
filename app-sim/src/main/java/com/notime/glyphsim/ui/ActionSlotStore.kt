package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.settings.SettingsCatalog
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dauerhafter, profilgetrennter Speicher fuer die vier kleinen Aktions-Snapshots.
 *
 * Bewusst keine Room-Tabelle: Die Eintraege sind reiner UI-Zwischenzustand, hoechstens vier
 * 16x16-Vorschauen gross und besitzen mit `occurrenceId` bereits eine stabile Referenz auf das
 * eigentliche Ereignis in Room. Eine Schema-Migration nur fuer diese kleine Anzeige wuerde das
 * Risiko fuer die nicht wiederherstellbaren Nutzerdaten unnoetig erhoehen.
 */
internal object ActionSlotStore {
    fun read(context: Context, profileId: String): List<SavedAction?> =
        List(ACTION_SLOT_COUNT) { index ->
            val raw = prefs(context).getString(key(profileId, index), null) ?: return@List null
            runCatching { decode(raw) }.getOrNull()
        }

    fun write(context: Context, profileId: String, index: Int, action: SavedAction?) {
        require(index in 0 until ACTION_SLOT_COUNT)
        prefs(context).edit().apply {
            if (action == null) remove(key(profileId, index))
            else putString(key(profileId, index), encode(action))
        }.apply()
    }

    /** Stream-only waiting stock: full snapshots are retained, never marked completed. */
    internal fun deferForStream(context: Context, profileId: String, action: SavedAction) {
        val stock = deferredForStream(context, profileId)
        if (stock.any { it.occurrenceId == action.occurrenceId }) return
        writeDeferred(context, profileId, stock + action)
    }

    internal fun deferredForStream(context: Context, profileId: String): List<SavedAction> =
        runCatching {
            val raw = JSONArray(prefs(context).getString("stream_waiting_$profileId", "[]"))
            List(raw.length()) { decode(raw.getString(it)) }
        }.getOrDefault(emptyList())

    internal fun removeDeferred(context: Context, profileId: String, occurrenceId: Long) {
        val stock = deferredForStream(context, profileId)
        if (stock.none { it.occurrenceId == occurrenceId }) return
        writeDeferred(context, profileId, stock.filterNot { it.occurrenceId == occurrenceId })
    }

    private fun writeDeferred(context: Context, profileId: String, stock: List<SavedAction>) {
        val raw = JSONArray().apply { stock.forEach { put(encode(it)) } }
        // Persist the waiting stock before removing a visible slot, including across process death.
        check(prefs(context).edit().putString("stream_waiting_$profileId", raw.toString()).commit())
    }

    /**
     * Leert alle Speicherplaetze eines Wesens - fuer den "Pflegebuch zuruecksetzen"-Pfad in
     * [FeedStatsDialog]: der loescht dort saemtliche `avatar_feed_events`-Zeilen des Wesens, auf
     * die ein belegter Platz per `occurrenceId` verweist. Ohne diesen Aufruf bliebe der Platz
     * sichtbar belegt, liesse sich aber nicht mehr fuettern - `AvatarFeeding.logFeedEvent` faende
     * das Ereignis nicht mehr und der Platz verschwaende beim Versuch stillschweigend, ohne
     * Reaktion.
     */
    fun clear(context: Context, profileId: String) {
        prefs(context).edit().apply {
            repeat(ACTION_SLOT_COUNT) { index -> remove(key(profileId, index)) }
            remove("stream_waiting_$profileId")
        }.apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            SettingsCatalog.DYNAMIC_ACTION_SLOT_FILE,
            Context.MODE_PRIVATE
        )

    private fun key(profileId: String, index: Int) = "${profileId}_$index"

    private fun encode(action: SavedAction): String = JSONObject().apply {
        put("reminderId", action.reminderId)
        put("occurrenceId", action.occurrenceId)
        put("animationType", action.animationType?.name)
        put("libraryAnimationLabel", action.libraryAnimationLabel)
        put("frames", JSONArray().apply {
            action.frames.forEach { frame ->
                put(JSONArray().apply { frame.forEach { put(it) } })
            }
        })
    }.toString()

    private fun decode(raw: String): SavedAction {
        val json = JSONObject(raw)
        val type = json.optString("animationType").takeIf { it.isNotBlank() }
            ?.let(AnimationType::valueOf)
        val framesJson = json.getJSONArray("frames")
        val frames = List(framesJson.length()) { frameIndex ->
            val frame = framesJson.getJSONArray(frameIndex)
            IntArray(frame.length()) { pointIndex -> frame.getInt(pointIndex) }
        }
        require(frames.isNotEmpty())
        return SavedAction(
            reminderId = json.getLong("reminderId"),
            occurrenceId = json.getLong("occurrenceId"),
            animationType = type,
            libraryAnimationLabel = json.optString("libraryAnimationLabel")
                .takeIf { it.isNotBlank() && it != "null" },
            frames = frames
        )
    }
}
