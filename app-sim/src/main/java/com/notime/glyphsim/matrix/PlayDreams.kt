package com.notime.glyphsim.matrix

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import java.time.LocalDate
import kotlin.random.Random

/**
 * Kleine semantische Erinnerungen fuer die Traum-Schicht.
 *
 * Bewusst werden keine Screenshots oder Videos gespeichert: Der Traum merkt sich nur, WAS am Tag
 * passiert ist. Beim Schlafen kann daraus dieselbe vorhandene Reaktionsbibliothek wieder eine
 * kurze Szene machen. So wachsen die Traeume automatisch mit dem normalen Animationsrepertoire.
 */
object PlayDreamMemory {
    private const val PREFS = "play_dream_memory"
    private const val KEY_DATE = "date"
    private const val KEY_TOPICS = "topics"
    private const val MAX_MEMORIES = 12

    fun remember(context: Context, topic: AnimationType, date: LocalDate = LocalDate.now()) {
        if (!PlayDreams.isEligibleMemory(topic)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = date.toString()
        val storedDay = prefs.getString(KEY_DATE, null)
        val topics = if (storedDay == day) {
            decode(prefs.getString(KEY_TOPICS, null))
        } else {
            emptyList()
        }
        // Wiederholungen bleiben nicht als zwoelf identische Erinnerungen liegen. Das Thema wird
        // stattdessen nach hinten geschoben und gilt damit als das juengste Erlebnis dieser Art.
        val next = (topics.filterNot { it == topic } + topic).takeLast(MAX_MEMORIES)
        prefs.edit()
            .putString(KEY_DATE, day)
            .putString(KEY_TOPICS, next.joinToString(",") { it.name })
            .apply()
    }

    fun today(context: Context, date: LocalDate = LocalDate.now()): List<AnimationType> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DATE, null) != date.toString()) return emptyList()
        return decode(prefs.getString(KEY_TOPICS, null))
    }

    private fun decode(raw: String?): List<AnimationType> =
        raw.orEmpty()
            .split(',')
            .mapNotNull { name ->
                name.takeIf { it.isNotBlank() }?.let {
                    runCatching { AnimationType.valueOf(it) }.getOrNull()
                }
            }
            .filter(PlayDreams::isEligibleMemory)
}

/** Regeln fuer seltene Traeume. Getrennt vom Speicher, damit die Auswahl ohne Android testbar ist. */
object PlayDreams {
    /** Schlaf selbst und Medizin werden nie als Tageserlebnis zurueckgetraeumt. */
    fun isEligibleMemory(topic: AnimationType): Boolean =
        topic != AnimationType.SLEEP && topic != AnimationType.MEDICINE

    /**
     * Nicht jede Gelegenheit wird ein Traum. Zusammen mit dem langen Abstand bleibt ein Traum ein
     * Fundstueck und kein zweiter Bildschirmschoner.
     */
    fun shouldDream(random: Random = Random): Boolean = random.nextFloat() < DREAM_CHANCE

    fun choose(memories: List<AnimationType>, random: Random = Random): AnimationType? {
        val eligible = memories.filter(::isEligibleMemory)
        return if (eligible.isEmpty()) null else eligible[random.nextInt(eligible.size)]
    }

    /** Zwischen zwei Traum-Gelegenheiten liegen im Normalbetrieb sechs bis fuenfzehn Minuten. */
    fun nextPauseMillis(random: Random = Random): Long =
        random.nextLong(DREAM_PAUSE_MIN_MS, DREAM_PAUSE_MAX_MS + 1)

    private const val DREAM_CHANCE = 0.40f
    private const val DREAM_PAUSE_MIN_MS = 6 * 60_000L
    private const val DREAM_PAUSE_MAX_MS = 15 * 60_000L
}
