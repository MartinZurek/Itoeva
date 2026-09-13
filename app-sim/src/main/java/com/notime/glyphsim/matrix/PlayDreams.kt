package com.notime.glyphsim.matrix

import android.content.Context
import com.notime.glyphcore.data.AnimationType
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

    fun remember(context: Context, profileId: String, topic: AnimationType, dayKey: String = PlayTimeLapse.dayKey()) {
        if (!PlayDreams.isEligibleMemory(topic)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dateKey = key(profileId, KEY_DATE)
        val topicsKey = key(profileId, KEY_TOPICS)
        val storedDay = prefs.getString(dateKey, null)
        val topics = if (storedDay == dayKey) {
            decode(prefs.getString(topicsKey, null))
        } else {
            emptyList()
        }
        // Wiederholungen bleiben nicht als zwoelf identische Erinnerungen liegen. Das Thema wird
        // stattdessen nach hinten geschoben und gilt damit als das juengste Erlebnis dieser Art.
        val next = (topics.filterNot { it == topic } + topic).takeLast(MAX_MEMORIES)
        prefs.edit()
            .putString(dateKey, dayKey)
            .putString(topicsKey, next.joinToString(",") { it.name })
            .apply()
    }

    fun today(context: Context, profileId: String, dayKey: String = PlayTimeLapse.dayKey()): List<AnimationType> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dateKey = key(profileId, KEY_DATE)
        val topicsKey = key(profileId, KEY_TOPICS)
        if (prefs.getString(dateKey, null) != dayKey) return emptyList()
        return decode(prefs.getString(topicsKey, null))
    }

    private fun key(profileId: String, suffix: String): String = "$profileId:$suffix"

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

/** Regeln fuer Tagtraeume und den sicheren Schlafrueckblick. Ohne Android testbar. */
object PlayDreams {
    /** Schlaf selbst und Medizin werden nie als Tageserlebnis zurueckgetraeumt. */
    fun isEligibleMemory(topic: AnimationType): Boolean =
        topic != AnimationType.SLEEP && topic != AnimationType.MEDICINE

    /**
     * **Der Tagtraum** (NT-073) - und der Grund, warum ihn bisher niemand gesehen hat.
     *
     * Traeume gab es ausschliesslich waehrend [RoutineStep.SleepUntilMorning] und dort nur in
     * [PlayAmbientActivity.DayPhase.NIGHT], also ab 23 Uhr. Wer abends zusieht - und das ist die
     * uebliche Zeit -, konnte gar keinen sehen, egal wie lange er zusah. Die Sequenz war nicht
     * selten, sie war unerreichbar.
     *
     * Diese Gelegenheit gilt ausserhalb des Schlafs: beim Ausruhen auf dem Sofa und auf der Bank
     * draussen. Sie bleibt bewusst selten - ein Tagtraum soll ein Aufblitzen sein, kein
     * Dauerzustand. Der Schlafrueckblick darunter folgt einer anderen Regel: Er kommt sicher.
     */
    fun shouldDaydream(random: Random = Random): Boolean = random.nextFloat() < DAYDREAM_CHANCE

    fun choose(memories: List<AnimationType>, random: Random = Random): AnimationType? {
        val eligible = memories.filter(::isEligibleMemory)
        return if (eligible.isEmpty()) null else eligible[random.nextInt(eligible.size)]
    }

    /**
     * Die juengsten wirklichen Tageserlebnisse fuer eine Schlafsequenz.
     *
     * Ein Schlafrueckblick ist kein Zufallsfund: Wer einschlaeft, schaut sicher auf seinen Tag
     * zurueck. Drei Bilder tragen bereits eine kleine Folge, ohne aus dem ruhigen Schlaf einen
     * zweiten Tagesablauf zu machen. Wiederholungen werden vom juengsten Vorkommen her entfernt,
     * damit eine spaete Rueckkehr zu einem Thema auch wirklich am Ende des Rueckblicks steht.
     */
    fun highlights(memories: List<AnimationType>): List<AnimationType> = memories
        .asReversed()
        .filter(::isEligibleMemory)
        .distinct()
        .take(SLEEP_HIGHLIGHT_COUNT)
        .asReversed()

    private const val DAYDREAM_CHANCE = 0.28f
    private const val SLEEP_HIGHLIGHT_COUNT = 3
}
