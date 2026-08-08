package com.notime.glyphsim.ui

import androidx.annotation.StringRes
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarSpecies

/**
 * **Wie die sechs Kreaturen dasselbe sagen.**
 *
 * Die Auskuenfte im Gespraech ([PlayTalk]) sind Zahlen und Listen und fuer alle gleich - vier von
 * sechs Tagesziele sind vier von sechs, egal wer es sagt. Was sich unterscheidet, ist der Rahmen
 * drumherum: die Begruessung, der Trost bei einem leeren Tag, die Freude ueber einen vollen.
 *
 * **Genau da und sonst nirgends.** Der erste Entwurf haette jede einzelne Zeile sechsmal
 * gebraucht - rund neunzig Texte je Sprache, von denen fuenf Sechstel nie jemand zu sehen bekommt,
 * weil man ohnehin nur einen Avatar hat. Und schlimmer: Sechs Fassungen von "4/6 erreicht" zu
 * pflegen heisst, dass irgendwann fuenf davon veralten.
 *
 * Charakter sitzt ohnehin nicht in der Auskunft, sondern im Ton, in dem sie gerahmt wird. Wyrmling
 * sagt "Weiter so!", Gloop sagt "Kein Stress." Beide nennen dieselbe Zahl - und trotzdem sind es
 * erkennbar zwei verschiedene Wesen. Das ist dasselbe Prinzip, mit dem die Arbeitsplaetze aus
 * geteilten Moebeln entstehen: Der Unterschied liegt in der Zusammenstellung, nicht in der Menge.
 *
 * **Abgeleitet aus dem, was die Figuren ohnehin sind** (siehe [AvatarSpecies] und die
 * Charakterbeschreibungen in den Ressourcen) - hier wird nichts neu erfunden, sondern nur ein
 * bereits beschriebener Charakter zu Ende gesprochen.
 */
data class PlayVoice(
    /** Womit er das Gespraech eroeffnet. */
    @StringRes val greeting: Int,
    /** Wenn heute noch nichts geschehen ist - der Ton, in dem das gesagt wird, entscheidet alles. */
    @StringRes val emptyDay: Int,
    /** Wenn alles erledigt ist. */
    @StringRes val allDone: Int,
    /** Wie er einen Vorschlag anbietet. */
    @StringRes val offering: Int,
    /** Womit er sich verabschiedet. */
    @StringRes val farewell: Int
) {
    companion object {
        fun forSpecies(species: AvatarSpecies): PlayVoice = when (species) {
            // Der neugierige Optimist: freut sich ueber jede Kleinigkeit.
            AvatarSpecies.PUFFLING -> PlayVoice(
                greeting = R.string.voice_puffling_greeting,
                emptyDay = R.string.voice_puffling_empty,
                allDone = R.string.voice_puffling_done,
                offering = R.string.voice_puffling_offer,
                farewell = R.string.voice_puffling_bye
            )
            // Die freundliche Traeumerin: warm, ruhig, niemals fordernd.
            AvatarSpecies.STARLET -> PlayVoice(
                greeting = R.string.voice_starlet_greeting,
                emptyDay = R.string.voice_starlet_empty,
                allDone = R.string.voice_starlet_done,
                offering = R.string.voice_starlet_offer,
                farewell = R.string.voice_starlet_bye
            )
            // Der kleine Motivator: knapp, energisch, aber nie hektisch.
            AvatarSpecies.WYRMLING -> PlayVoice(
                greeting = R.string.voice_wyrmling_greeting,
                emptyDay = R.string.voice_wyrmling_empty,
                allDone = R.string.voice_wyrmling_done,
                offering = R.string.voice_wyrmling_offer,
                farewell = R.string.voice_wyrmling_bye
            )
            // Der ruhige Beschuetzer: sachlich, verlaesslich, ohne Druck.
            AvatarSpecies.FENNEC -> PlayVoice(
                greeting = R.string.voice_fennec_greeting,
                emptyDay = R.string.voice_fennec_empty,
                allDone = R.string.voice_fennec_done,
                offering = R.string.voice_fennec_offer,
                farewell = R.string.voice_fennec_bye
            )
            // Der Entschleuniger: gemuetlich, ein bisschen chaotisch.
            AvatarSpecies.GLOOP -> PlayVoice(
                greeting = R.string.voice_gloop_greeting,
                emptyDay = R.string.voice_gloop_empty,
                allDone = R.string.voice_gloop_done,
                offering = R.string.voice_gloop_offer,
                farewell = R.string.voice_gloop_bye
            )
            // Der weise Beobachter: wenige Worte, jedes davon ueberlegt.
            AvatarSpecies.HOOTLET -> PlayVoice(
                greeting = R.string.voice_hootlet_greeting,
                emptyDay = R.string.voice_hootlet_empty,
                allDone = R.string.voice_hootlet_done,
                offering = R.string.voice_hootlet_offer,
                farewell = R.string.voice_hootlet_bye
            )
        }
    }
}
