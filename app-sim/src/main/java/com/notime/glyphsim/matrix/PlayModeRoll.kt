package com.notime.glyphsim.matrix

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.ui.AvatarSpeciesPrefs
import com.notime.glyphsim.ui.PlayModeXp
import com.notime.glyphsim.ui.PresentCompanion
import kotlin.random.Random

/**
 * Wuerfelt fuer die EINE Spiel-Erinnerung eines Avatars bei jeder Neuplanung neu, was als
 * Naechstes kommt - eingehaengt in [com.notime.glyphcore.reminder.ReminderHost] und von
 * [com.notime.glyphcore.reminder.ReminderScheduler.schedule] fuer jede
 * [GlyphReminder.isPlayMode]-Zeile aufgerufen.
 *
 * **Teil-Zufall, kein Gleichverteilungs-Generator.** Was ueberhaupt in Frage kommt und wie
 * wahrscheinlich es ist, gibt der Spielplan des jeweiligen Avatars vor ([PlayGamePlans]) - und der
 * haengt am Charakter der Figur und an ihrem Fortschritt. Gewuerfelt wird innerhalb dieses Rahmens.
 * Dadurch bleibt jedes einzelne Auftauchen ueberraschend, waehrend sich ueber die Zeit trotzdem
 * eine Handschrift ergibt.
 *
 * Das Level entscheidet ueber die Stufe des Plans, deshalb ist [reroll] suspend: Es liest den
 * Spielstand aus der Datenbank.
 */
object PlayModeRoll {

    suspend fun reroll(context: Context, reminder: GlyphReminder): GlyphReminder {
        // **Das Wesen, nicht die Erinnerung.** Vorher stand hier
        // `AvatarSpecies.valueOf(reminder.profileId)` - die Spezies wurde also aus dem
        // Besitzer-Feld einer Routine zurueckgerechnet. Das ging nur gut, solange die Profil-Id
        // der Avatarname IST; sobald die Routinen dem Nutzer gehoeren, faende das `runCatching`
        // keine Spezies mehr und jedes Wesen wuerfelte still nach dem Plan von PUFFLING.
        //
        // Welchen Plan gewuerfelt wird und wie weit er fortgeschritten ist, sind ohnehin Fragen an
        // das anwesende Wesen (siehe PresentCompanion) - so, wie der Spielstand selbst.
        val species = AvatarSpeciesPrefs.get(context)
        val companionId = PresentCompanion.profileId(context)
        val stage = PlayGamePlans.forSpecies(species).stageFor(currentLevel(context, companionId))
        val topic = pickWeighted(stage.topicWeights)
        return reminder.copy(
            label = context.getString(topic.labelRes),
            animationType = topic,
            libraryAnimationId = pickAnimation(context, species)?.id,
            intervalMinutes = stage.intervalMinutes.random(),
            daysOfWeekMask = ALL_DAYS_MASK,
            startMinuteOfDay = 0,
            endMinuteOfDay = END_OF_DAY_MINUTE,
            dailyGoal = NO_GOAL
        )
    }

    /**
     * **Welches BILD die Spiel-Erinnerung zeigt** - `null` heisst: die eingebaute Animation des
     * Themas, wie bisher.
     *
     * Gemeldet als "im Play-Modus kommt fast immer dieselbe Glocke, und die Reaktion ist auch
     * immer dieselbe". Beides stimmte, und beides hatte dieselbe Ursache: Hier stand
     * `libraryAnimationId = null`, also kam ausschliesslich die Typ-Animation - zwoelf Bilder
     * insgesamt, davon eines (GENERAL) am haeufigsten gewuerfelt. Die Bibliothek mit ihren
     * sechsundzwanzig Grundmotiven und dreissig Charakter-Motiven (Rakete, Schnecke, Komet,
     * Blitz, ...) blieb im Spiel unberuehrt, obwohl sie genau dafuer da ist.
     *
     * **Und es haengt mehr daran als das Bild.** Zu jedem der dreissig Charakter-Motive gehoert
     * eine EIGENE Reaktion der Figur ([AvatarSignatureReactions]) - Gloop schwappt der Wolke
     * hinterher, Wyrmling zuckt beim Blitz zusammen. Wer immer nur Typ-Animationen zeigt, bekommt
     * auch immer nur die zwoelf Typ-Reaktionen zu sehen. Ein einziges Feld hat also den groessten
     * Teil der vorhandenen Abwechslung ungenutzt gelassen.
     *
     * Die Gewichtung: meistens ein Motiv aus der Bibliothek, und darunter bevorzugt eines der
     * fuenf, die DIESER Kreatur gehoeren - die Handschrift soll erkennbar bleiben. Die
     * Typ-Animation faellt trotzdem nicht weg: Sie ist die einzige, die zeigt, worum es geht.
     */
    private suspend fun pickAnimation(context: Context, species: AvatarSpecies): LibraryAnimation? =
        chooseMotif(
            available = AppDatabase.getInstance(context).libraryAnimationDao().getAll(),
            ownLabels = AvatarSignatureReactions.labelsFor(species).toSet()
        )

    /**
     * Die Ziehung selbst - ohne Datenbank, damit sie sich pruefen laesst (siehe PlayModeRollTest).
     *
     * Eine leere Bibliothek ist ausdruecklich kein Fehlerfall: Beim allerersten Start ist noch
     * nichts eingespielt (siehe LibraryAnimationRepository.seedOrRefresh), und dann gilt schlicht
     * wieder, was vorher immer galt - es kommt die Animation des Themas.
     */
    internal fun chooseMotif(
        available: List<LibraryAnimation>,
        ownLabels: Set<String>,
        random: Random = Random
    ): LibraryAnimation? {
        if (random.nextInt(100) < TYPE_ANIMATION_PERCENT) return null
        if (available.isEmpty()) return null
        val mine = available.filter { it.label in ownLabels }
        val others = available.filter { it.label !in ownLabels }
        val preferOwn = mine.isNotEmpty() &&
            (others.isEmpty() || random.nextInt(100) < OWN_MOTIF_PERCENT)
        return when {
            preferOwn -> mine.random(random)
            others.isNotEmpty() -> others.random(random)
            else -> null
        }
    }

    /** Wie oft die eingebaute Animation des Themas kommt statt eines Motivs aus der Bibliothek. */
    private const val TYPE_ANIMATION_PERCENT = 30

    /** Und wie oft es dann eines der fuenf eigenen Motive ist statt irgendeines. */
    private const val OWN_MOTIF_PERCENT = 60

    private suspend fun currentLevel(context: Context, companionProfileId: String): Int {
        val xp = AppDatabase.getInstance(context).avatarPlayStateDao()
            .getForProfile(companionProfileId)?.xp ?: 0
        return PlayModeXp.levelFor(xp)
    }

    /**
     * Zieht ein Thema entsprechend seinem Gewicht. Leere oder unsinnige Gewichtungen koennen einen
     * Spielplan nicht zum Absturz bringen - dann kommt schlicht das Allgemein-Thema.
     */
    private fun pickWeighted(weights: Map<AnimationType, Int>): AnimationType {
        val total = weights.values.sum()
        if (total <= 0) return AnimationType.GENERAL
        var roll = Random.nextInt(total)
        for ((topic, weight) in weights) {
            roll -= weight
            if (roll < 0) return topic
        }
        return weights.keys.firstOrNull() ?: AnimationType.GENERAL
    }

    /** Alle sieben Wochentage gesetzt (Bit 0..6), siehe [com.notime.glyphcore.data.DaysOfWeekMask]. */
    private const val ALL_DAYS_MASK = 0b1111111
    private const val END_OF_DAY_MINUTE = 23 * 60 + 59
}
