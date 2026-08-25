package com.notime.glyphsim.ui

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.AvatarFeedEvent
import com.notime.glyphsim.data.AvatarFeedingRepository
import com.notime.glyphsim.data.FeedCommitResult
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.ReactionTrigger
import com.notime.glyphsim.widget.GlyphClockWidgetProvider

/**
 * Gemeinsame Fuetter-Mechanik fuer [DockScreen] und [HomeScreen]: die Uhr auf den Avatar
 * schieben beendet die laufende Erinnerung sofort und loest die typ-spezifische Reaktion aus
 * (siehe [AvatarAnimations.reactionFramesFor]).
 *
 * Bewusst hier gebuendelt statt in beiden Screens dupliziert - beide brauchen exakt dieselbe
 * Ueberlappungs-Regel und dieselbe Reaktions-Wiedergabe inklusive des Sonderfalls "Rocket",
 * der den Avatar zusaetzlich ueber den ganzen Bildschirm fliegen laesst (siehe
 * [AvatarAnimations.rocketFlightOffsets]).
 */
object AvatarFeeding {

    /**
     * Beide Boxen werden vor dem Vergleich etwas eingezogen, damit ein blosses Beruehren der
     * aeussersten Ecken noch nicht als Treffer zaehlt.
     */
    private const val OVERLAP_INSET = 0.15f

    fun overlaps(a: Rect, b: Rect): Boolean {
        if (a.width <= 0f || a.height <= 0f || b.width <= 0f || b.height <= 0f) return false
        val aInset = a.deflate(minOf(a.width, a.height) * OVERLAP_INSET)
        val bInset = b.deflate(minOf(b.width, b.height) * OVERLAP_INSET)
        return aInset.left < bInset.right && aInset.right > bInset.left &&
            aInset.top < bInset.bottom && aInset.bottom > bInset.top
    }

    /**
     * Vermerkt, dass auf diese eine Ausloesung reagiert wurde.
     *
     * Die Zeile existiert bereits - sie wurde beim Ausloesen angelegt (siehe
     * reminder/ReminderTrigger), damit sich auch Uebergangenes zaehlen laesst. Frueher entstand
     * sie erst hier, wodurch ausschliesslich die Erfolge in der Datenbank standen.
     * Muss aus einer IO-Coroutine aufgerufen werden.
     *
     * Beendet zugleich eine eventuell noch laufende Animation im Home-Screen-Widget: Wer die
     * Erinnerung dort sah, die App oeffnete und hier fuettert, hat sie beantwortet - sie darf
     * dann nicht weiterlaufen, als waere sie noch offen.
     */
    suspend fun logFeedEvent(context: Context, occurrenceId: Long): FeedCommitResult {
        val result = AvatarFeedingRepository(AppDatabase.getInstance(context)).feed(
            occurrenceId = occurrenceId,
            fedAtMillis = System.currentTimeMillis(),
            xpPerFeed = PlayModeXp.XP_PER_FEED
        )
        // Widget/Ton/Reaktion koennen nicht Teil der SQLite-Transaktion sein. Ein erster Feed und
        // ein Retry nach bereits erfolgtem Commit sind UI-seitig beide Erfolg; eine unbekannte ID
        // dagegen nicht.
        if (result.isUiSuccess()) {
            GlyphClockWidgetProvider.stopReminderAnimation(context)
        }
        return result
    }

    /**
     * Spielt die Reaktion einmal komplett durch. [onOffset] bekommt bei der Rocket-Reaktion die
     * Verschiebung der Sprite-Box relativ zur Startposition (in Pixeln, berechnet aus
     * [screenWidthPx]/[screenHeightPx]) - dadurch fliegt der Avatar ueber den ganzen Bildschirm
     * statt nur innerhalb seines eigenen 16x16-Rasters; bei allen anderen Reaktionen bleibt die
     * Verschiebung durchgehend [Offset.Zero].
     */
    suspend fun playReaction(
        species: AvatarSpecies,
        trigger: ReactionTrigger,
        screenWidthPx: Float,
        screenHeightPx: Float,
        onFrame: (IntArray) -> Unit,
        onOffset: (Offset) -> Unit
    ) {
        // Abbruch der Coroutine (Screen verlassen, Spezies gewechselt) ist KEIN Fehler und muss
        // durchgereicht werden - wird CancellationException mitgefangen, laeuft die Wiedergabe
        // im Hintergrund weiter, obwohl niemand mehr zusieht.
        try {
            playReactionInternal(
                species, trigger,
                screenWidthPx, screenHeightPx, onFrame, onOffset
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            // Eine fehlerhafte Animation darf hoechstens ihre eigene Reaktion kosten, nicht die
            // ganze App. Der Avatar verschwindet dann einfach, statt dass ein Absturzdialog
            // erscheint - der Aufrufer raeumt in seinem finally ohnehin auf.
            Log.e(TAG, "Reaktion fehlgeschlagen (species=$species, trigger=$trigger)", e)
        }
    }

    private suspend fun playReactionInternal(
        species: AvatarSpecies,
        trigger: ReactionTrigger,
        screenWidthPx: Float,
        screenHeightPx: Float,
        onFrame: (IntArray) -> Unit,
        onOffset: (Offset) -> Unit
    ) {
        // playTimed statt eines festen Takts: jede Reaktion bringt ihren eigenen Rhythmus mit
        // (schneller Einsatz, langsames Ausklingen, Nachklang am Ende) - siehe AvatarAnimations.
        val reaction = AvatarAnimations.reactionFor(species, trigger)
        if (reaction.frames.isEmpty()) {
            Log.w(TAG, "Reaktion ohne Frames (species=$species, trigger=$trigger)")
            return
        }
        val flightOffsets = AvatarAnimations.flightOffsetsFor(trigger)
        if (flightOffsets.isEmpty()) {
            MatrixAnimator.playTimed(reaction.frames, reaction.holdsMs, onFrame)
            return
        }
        var frameIndex = 0
        MatrixAnimator.playTimed(reaction.frames, reaction.holdsMs) { f ->
            val (dxFraction, dyFraction) = flightOffsets.getOrElse(frameIndex) { flightOffsets.last() }
            frameIndex++
            onFrame(f)
            onOffset(Offset(dxFraction * screenWidthPx, dyFraction * screenHeightPx))
        }
    }

    private const val TAG = "AvatarFeeding"
}

/** Reine Entscheidung als gemeinsame, direkt testbare Grenze fuer Home und Dock. */
internal fun FeedCommitResult.isUiSuccess(): Boolean =
    this == FeedCommitResult.FIRST_FEED || this == FeedCommitResult.ALREADY_FED
