package com.notime.glyphsim.stream

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import com.notime.glyphsim.matrix.PlayMap
import com.notime.glyphsim.matrix.PlayScene.Place

/** Der PC verwaltet KI und Twitch-Anmeldung; in der APK steht kein Kontogeheimnis. */
internal object FennecReplyClient {
    data class Reply(val text: String, val sent: Boolean, val presentation: FennecWorld.Presentation?, val german: Boolean)

    suspend fun reply(address: FennecConversation.Address, place: String, activity: String, hour: Int): Reply? =
        withContext(Dispatchers.IO) {
            val connection = URL("http://127.0.0.1:18766/reply").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 2_000
                connection.readTimeout = 20_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val body = JSONObject().put("viewer", address.viewerId).put("message", address.text)
                    .put("place", place).put("activity", activity).put("hour", hour).put("preview", address.preview)
                    .put("world", JSONObject().put("version", FennecWorld.VERSION).put("nodes", JSONArray(
                        Place.entries.map { node -> JSONObject().put("id", node.name)
                            .put("region", PlayMap.regionOf(node).name)
                            .put("neighbors", JSONArray(PlayMap.neighbors(node).map { it.name })) }
                    )))
                val now = java.time.Instant.now()
                body.put("clock", JSONObject().put("at", now.toString()).put("times", JSONArray(
                    StreamTime.readings(now).map { time -> JSONObject().put("id", time.id)
                        .put("label", time.label).put("time", time.time).put("date", time.date).put("offset", time.offset) }
                )))
                body.put("followup", address.followup)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode != 200) return@withContext null
                val result = connection.inputStream.bufferedReader().use { it.readText() }
                if (result.length > 2_048) return@withContext null
                val json = JSONObject(result)
                val text = json.optString("text").trim()
                if (text.isBlank() || text.length > 320 || text.any { it.isISOControl() }) null
                else Reply(text, json.optBoolean("sent"),
                    FennecWorld.presentation(json.optString("action"), json.optString("target")),
                    json.optString("language") == "de")
            } catch (_: java.io.IOException) {
                null
            } catch (_: org.json.JSONException) {
                null
            } finally {
                connection.disconnect()
            }
        }
}
