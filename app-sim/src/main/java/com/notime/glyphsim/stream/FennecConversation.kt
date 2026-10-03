package com.notime.glyphsim.stream

/** Ansprache und kurze passende Folgefragen werden Dialog, strikt ausserhalb der Spielbefehle. */
internal object FennecConversation {
    data class Address(val viewerId: String, val text: String, val receivedAtMs: Long, val preview: Boolean = false,
                       val followup: Boolean = false)
    const val GLOBAL_GAP_MS = 4_000L
    const val VIEWER_GAP_MS = 8_000L
    const val SESSION_MS = 45_000L

    private val opening = Regex("^(?:(?:hey|hej|hi|hello|hallo|huhu|yo)[,;:!?.\\s]+)?@?fennec(?:_itoeva)?\\b", RegexOption.IGNORE_CASE)
    private val closing = Regex("[, ]+fennec[!? .]*$", RegexOption.IGNORE_CASE)
    private val mention = Regex("(?:^|\\s)@fennec(?:_itoeva)?\\b", RegexOption.IGNORE_CASE)

    private val questionStart = Regex("^(?:and|und|why|warum|what|was|where|wo|how|wie|can|could|kannst|show|zeig|zeige|tell|erzähl|erzähle|thanks|thank you|danke)\\b", RegexOption.IGNORE_CASE)

    private fun candidate(viewerId: String, text: String, now: Long): Address? {
        val message = text.trim()
        if (!viewerId.matches(Regex("[a-zA-Z0-9_]{1,25}")) || message.length !in 3..350) return null
        if (message.contains("[Fennec]", ignoreCase = true) || message.startsWith('!')) return null
        if (message.any { it.isISOControl() }) return null
        return Address(viewerId.lowercase(), message, now)
    }

    fun address(viewerId: String, text: String, now: Long): Address? = candidate(viewerId, text, now)?.takeIf {
        opening.containsMatchIn(it.text) || mention.containsMatchIn(it.text) ||
            ((it.text.contains('?') || questionStart.containsMatchIn(it.text)) && closing.containsMatchIn(it.text))
    }

    /** Nur nach einer angenommenen Ansprache, kurz und je Zuschauer getrennt. Kein Chat-Verlauf. */
    class Session {
        private val viewers = LinkedHashMap<String, Long>()

        @Synchronized fun activate(viewerId: String, now: Long) {
            viewers.entries.removeAll { now - it.value !in 0..SESSION_MS }
            viewers[viewerId.lowercase()] = now
            while (viewers.size > 32) viewers.remove(viewers.keys.first())
        }

        @Synchronized fun read(viewerId: String, text: String, now: Long): Address? {
            address(viewerId, text, now)?.let { return it }
            val candidate = candidate(viewerId, text, now) ?: return null
            val at = viewers[candidate.viewerId] ?: return null
            if (now - at !in 0..SESSION_MS || candidate.text.contains('@') ||
                !(candidate.text.contains('?') || questionStart.containsMatchIn(candidate.text))) return null
            return candidate.copy(followup = true)
        }
    }

    /** Kein Nachholen alter Nachrichten und keine wartende Antwortschlange im Stream. */
    class Gate {
        private var lastGlobal: Long? = null
        private val viewers = LinkedHashMap<String, Long>()

        fun retryAfterMs(address: Address, now: Long): Long? {
            if (now - address.receivedAtMs !in 0..5_000L) return null
            return maxOf(0L, lastGlobal?.let { GLOBAL_GAP_MS - (now - it) } ?: 0L,
                viewers[address.viewerId]?.let { VIEWER_GAP_MS - (now - it) } ?: 0L)
        }

        fun admit(address: Address, now: Long): Boolean {
            if (retryAfterMs(address, now) != 0L) return false
            lastGlobal = now
            viewers[address.viewerId] = now
            while (viewers.size > 256) viewers.remove(viewers.keys.first())
            return true
        }
    }
}
