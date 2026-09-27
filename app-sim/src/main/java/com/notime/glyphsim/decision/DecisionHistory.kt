package com.notime.glyphsim.decision

/**
 * **Was das Wesen zuletzt gezeigt hat** - die Grundlage fuer Neuheit, Wiederholung und
 * Kontinuitaet der Decision Policy.
 *
 * Bewusst kein Room-Eintrag: Eine neue Spalte hiesse Migrationen in beiden Datenbanken (siehe
 * CLAUDE.md), und dafuer ist ein Verlauf von wenigen Dutzend Zeilen zu wenig. Die App legt ihn
 * als Text in SharedPreferences ab ([encode]/[decode]), wie schon den Bewegungsdrang.
 *
 * Die Zeitachse ist die des Aufrufers - in der App Weltminuten, in der Simulation die Minuten des
 * Kerns. Verglichen wird nur innerhalb derselben Achse.
 */
data class DecisionHistory(val entries: List<Entry> = emptyList()) {

    data class Entry(
        val minute: Long,
        val key: String,
        val family: String,
        val topic: String,
        /** Wer dabei war - fuer "das haben wir schon einmal zusammen gemacht". */
        val partners: Set<String> = emptySet(),
        /** Ob der Ablauf unter freiem Himmel stattfand - fuer "lange nicht draussen gewesen". */
        val outdoor: Boolean = false
    )

    /** Neu vorn, altes faellt an der festen Grenze heraus. */
    fun recorded(entry: Entry): DecisionHistory =
        DecisionHistory((listOf(entry) + entries).take(MAX_ENTRIES))

    fun lastShown(key: String): Long? = entries.firstOrNull { it.key == key }?.minute

    fun recentKeyCount(key: String, window: Int = RECENT_WINDOW): Int =
        entries.take(window).count { it.key == key }

    fun recentFamilyCount(family: String, window: Int = RECENT_WINDOW): Int =
        entries.take(window).count { it.family == family }

    fun recentTopicCount(topic: String, window: Int = RECENT_WINDOW): Int =
        entries.take(window).count { it.topic == topic }

    /** Wann diese Familie zuletzt mit einem dieser Wesen gemacht wurde, oder `null`. */
    fun lastWith(family: String, partners: Set<String>): Long? =
        if (partners.isEmpty()) null
        else entries.firstOrNull { it.family == family && it.partners.any(partners::contains) }?.minute

    /** Wann das Wesen zuletzt draussen war, oder `null`. */
    fun lastOutdoor(): Long? = entries.firstOrNull { it.outdoor }?.minute

    fun encode(): String = entries.joinToString("\n") { e ->
        listOf(
            e.minute.toString(), e.key, e.family, e.topic, e.partners.sorted().joinToString(","),
            if (e.outdoor) "1" else "0"
        ).joinToString("\t")
    }

    companion object {
        const val MAX_ENTRIES = 64
        const val RECENT_WINDOW = 6

        /** Unlesbare Zeilen werden uebersprungen - ein kaputter Verlauf ist ein leerer, kein Absturz. */
        fun decode(text: String?): DecisionHistory {
            if (text.isNullOrBlank()) return DecisionHistory()
            val entries = text.lineSequence().mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 4) return@mapNotNull null
                val minute = parts[0].toLongOrNull() ?: return@mapNotNull null
                Entry(
                    minute = minute,
                    key = parts[1],
                    family = parts[2],
                    topic = parts[3],
                    partners = parts.getOrNull(4).orEmpty().split(',').filter { it.isNotBlank() }.toSet(),
                    outdoor = parts.getOrNull(5) == "1"
                )
            }.take(MAX_ENTRIES).toList()
            return DecisionHistory(entries)
        }
    }
}
