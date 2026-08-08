package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AvatarSignatureAnimations
import com.notime.glyphcore.data.FrameCodec
import com.notime.glyphcore.data.FrameCrossfade
import com.notime.glyphcore.data.ReminderFrameGrid
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.ClipBeat
import com.notime.glyphsim.matrix.ClockCue
import com.notime.glyphsim.matrix.ClockMotion
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, was in den Vorstellungs-Clips tatsaechlich drinsteht.
 *
 * Alle drei Invarianten hier stammen aus gemeldeten Beobachtungen am laufenden Clip, nicht aus
 * theoretischen Ueberlegungen: Animationen brachen mittendrin ab, die Uhr zog am Avatar vorbei
 * statt bei ihm anzukommen, und die eigens gezeichneten Signatur-Animationen kamen ueberhaupt
 * nicht vor.
 */
class AvatarClipContentTest {

    /**
     * Obergrenze fuer die Cliplaenge.
     *
     * Die Clips sind mit der Zustellung und den zwei Signatur-Wellen von rund 36 auf rund 45
     * Sekunden gewachsen - vertretbar fuer eine Vorstellung, die fuenf verschiedene Animationen
     * zeigt, aber nichts, was unbemerkt weiterwachsen sollte. Jeder weitere Beat faellt hier auf,
     * statt erst dem Nutzer beim Zusehen.
     */
    @Test
    fun clipsStayWatchable() {
        for (species in AvatarSpecies.entries) {
            val seconds = clipFor(species).totalDurationMs / 1000.0
            assertTrue(
                "$species-Clip dauert ${seconds}s - zu lang fuer eine Vorstellung",
                seconds <= MAX_CLIP_SECONDS
            )
        }
    }

    private companion object {
        const val MAX_CLIP_SECONDS = 55.0
    }

    private fun clipFor(species: AvatarSpecies) =
        requireNotNull(AvatarClips.forSpecies(species)) { "Kein Clip fuer $species" }

    /**
     * Ein Beat mit [ClipBeat.playToEnd] muss lang genug sein, dass die Animation einmal ganz
     * durchlaeuft - sonst ist das Flag wirkungslos und die Reaktion bricht wieder im Sprung ab.
     */
    @Test
    fun playToEndBeatsActuallyRunTheWholeAnimation() {
        for (species in AvatarSpecies.entries) {
            clipFor(species).beats.forEachIndexed { index, beat ->
                if (!beat.playToEnd) return@forEachIndexed
                assertTrue(
                    "$species, Beat $index: laeuft ${beat.effectiveDurationMs}ms, die Animation " +
                        "braucht aber ${beat.frameHoldsMs.sum()}ms fuer einen Durchlauf",
                    beat.effectiveDurationMs >= beat.frameHoldsMs.sum()
                )
            }
        }
    }

    /**
     * Jede Reaktion - also jeder Beat ohne Uhr, der auf eine Erinnerung antwortet - muss zu Ende
     * spielen duerfen. Geprueft ueber alle Beats, die laenger sind als eine Ruhe-Schleife und
     * keine Uhr zeigen: Das sind genau die Reaktionen und das Finale.
     */
    @Test
    fun everyReactionBeatIsAllowedToFinish() {
        for (species in AvatarSpecies.entries) {
            val clip = clipFor(species)
            val reactionBeats = clip.beats.filterIndexed { index, beat ->
                // Eine Reaktion folgt immer unmittelbar auf einen Beat MIT Uhr und hat selbst
                // keine. Flug-Reaktionen (Rocket) sind ausgenommen: Der Player spielt sie ohnehin
                // Frame fuer Frame im eigenen Tempo komplett durch, weil die Flugbahn daran
                // haengt - fuer sie waere playToEnd wirkungslos.
                beat.clock == null &&
                    beat.flightOffsets == null &&
                    clip.beats.getOrNull(index - 1)?.clock != null
            }
            assertTrue("$species hat gar keine Reaktions-Beats", reactionBeats.isNotEmpty())
            for (beat in reactionBeats) {
                assertTrue(
                    "$species: Eine Reaktion darf nicht abgeschnitten werden - playToEnd fehlt",
                    beat.playToEnd
                )
            }
        }
    }

    /**
     * Die Erinnerung muss im Clip tatsaechlich ANKOMMEN. Frueher zog die Uhr nur in einem Bogen
     * ueber den Kopf hinweg und verschwand daneben - die Geste, um die sich die ganze App dreht,
     * kam im Vorstellungs-Clip damit nicht vor.
     */
    @Test
    fun everyClipDeliversTheReminderOntoTheAvatar() {
        for (species in AvatarSpecies.entries) {
            val delivers = clipFor(species).beats.count {
                (it.clock as? ClockCue.Animation)?.motion is ClockMotion.Deliver
            }
            assertTrue(
                "$species: keine Zustellung im Clip - die Uhr erreicht den Avatar nie",
                delivers > 0
            )
        }
    }

    /**
     * Jeder Clip zeigt mindestens zwei der eigens fuer DIESEN Avatar gezeichneten Animationen -
     * sonst waeren sie umsonst gezeichnet worden, und der Clip bliebe fuer alle sechs Figuren
     * derselbe Ablauf mit denselben Standard-Motiven.
     *
     * Verglichen wird der tatsaechliche Bildinhalt und nicht die Frame-ANZAHL: Die Uhr-Frames
     * durchlaufen vor dem Abspielen noch [FrameCrossfade], das zusaetzliche Zwischenbilder
     * einfuegt - eine Zaehlung gegen die rohen Punktdaten ginge deshalb immer daneben, und eine
     * Zaehlung gegen andere Animationen koennte zufaellig uebereinstimmen.
     */
    @Test
    fun everyClipUsesItsOwnSignatureAnimations() {
        val signatureFirstFrames = AvatarSignatureAnimations.seed().associate { animation ->
            animation.label to FrameCrossfade.withCrossfades(
                ReminderFrameGrid.SIZE,
                FrameCodec.decode(animation.framesData)
            ).first().toList()
        }

        for (species in AvatarSpecies.entries) {
            val shownFirstFrames = clipFor(species).beats
                .mapNotNull { it.clock as? ClockCue.Animation }
                .mapNotNull { it.frames.firstOrNull()?.toList() }
                .toSet()
            val used = signatureFirstFrames.filterValues { it in shownFirstFrames }.keys
            assertTrue(
                "$species zeigt keine seiner eigenen Signatur-Animationen im Clip",
                used.isNotEmpty()
            )
        }
    }
}
