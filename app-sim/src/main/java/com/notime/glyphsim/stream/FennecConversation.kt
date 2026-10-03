package com.notime.glyphsim.stream

/** Nur direkte Ansprache wird Dialog. Sie bleibt strikt ausserhalb der Spielbefehle. */
internal object FennecConversation {
    data class Address(val viewerId: String, val text: String, val receivedAtMs: Long, val preview: Boolean = false)

    private val opening = Regex("^(?:(?:hey|hi|hello|hallo|huhu|yo)[,! ]+)?@?fennec(?:_itoeva)?\\b", RegexOption.IGNORE_CASE)
    private val closing = Regex("[, ]+fennec[!? .]*$", RegexOption.IGNORE_CASE)
    private val mention = Regex("(?:^|\\s)@fennec(?:_itoeva)?\\b", RegexOption.IGNORE_CASE)

    fun address(viewerId: String, text: String, now: Long): Address? {
        val message = text.trim()
        if (!viewerId.matches(Regex("[a-zA-Z0-9_]{1,25}")) || message.length !in 3..350) return null
        if (message.contains("[Fennec]", ignoreCase = true) || message.startsWith('!')) return null
        if (message.any { it.isISOControl() }) return null
        if (!opening.containsMatchIn(message) && !mention.containsMatchIn(message) &&
            !(message.contains('?') && closing.containsMatchIn(message))) return null
        return Address(viewerId.lowercase(), message, now)
    }

    /** Kein Nachholen alter Nachrichten und keine wartende Antwortschlange im Stream. */
    class Gate {
        private var lastGlobal: Long? = null
        private val viewers = LinkedHashMap<String, Long>()

        fun admit(address: Address, now: Long): Boolean {
            if (now - address.receivedAtMs !in 0..5_000L) return false
            if (lastGlobal?.let { now - it < 10_000L } == true) return false
            if (viewers[address.viewerId]?.let { now - it < 30_000L } == true) return false
            lastGlobal = now
            viewers[address.viewerId] = now
            while (viewers.size > 256) viewers.remove(viewers.keys.first())
            return true
        }
    }
}
