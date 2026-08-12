package com.notime.glyphsim.ui

import androidx.annotation.StringRes
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.PlayAmbientActivity
import kotlin.random.Random

/**
 * **Was er beim Tun kurz sagt** - ein einziger Satz ueber dem Kopf, ein paar Sekunden lang.
 *
 * **Warum das ausserhalb des Gespraechs noetig ist.** Alles, was der Begleiter zu sagen hat, stand
 * bisher in einem Feld, das man erst oeffnen muss (siehe [PlayTalkPanel]). Wer nur zusieht,
 * bekommt davon nichts mit - und muss sich aus Kulisse und Bewegung selbst zusammenreimen, was
 * gerade passiert. Bei "geht zum Kuehlschrank und nimmt etwas heraus" gelingt das; bei "steht in
 * der Leseecke und atmet" nicht mehr. Genau dort setzt dieser Satz an: Er beantwortet die Frage,
 * die man sich beim Zuschauen ohnehin stellt, im selben Augenblick, in dem sie aufkommt.
 *
 * **Und er verbindet seinen Tag mit deinem.** Ist das, was er gerade tut, etwas, das du dir selbst
 * vorgenommen und heute noch nicht getan hast, sagt er auch das - ein Halbsatz, keine Ermahnung.
 * Das ist dieselbe Verbindung wie im Gespraech, nur an der Stelle, an der man sie tatsaechlich
 * sieht.
 *
 * **Selten, kurz, und nie nachts.** Drei Bedingungen, die zusammen darueber entscheiden, ob das
 * hier Begleitung oder Geschwaetz ist:
 * - Nur bei etwa jeder dritten Regung. Ein Wesen, das jede Handlung kommentiert, ist anstrengend,
 *   und man liest nach dem fuenften Mal nicht mehr hin.
 * - Ein Satz, keine zwei. Wer laenger reden will, oeffnet das Gespraech.
 * - Nachts gar nicht. Das Dock steht auf einem Nachttisch; ein Text, der um drei Uhr aufleuchtet,
 *   ist genau das, was dieser Modus nicht sein soll.
 */
object PlaySpeech {

    /** Bei wieviel Prozent der Regungen er ueberhaupt etwas sagt. */
    private const val CHANCE_PERCENT = 34

    /**
     * Der Satz zu dieser Taetigkeit - `null` heisst: diesmal sagt er nichts.
     *
     * [openHabit] ist wahr, wenn dasselbe Thema bei DIR heute noch offen ist; dann kommt zum Satz
     * noch der Halbsatz aus [habitHint].
     */
    @StringRes
    fun lineFor(
        topic: AnimationType,
        phase: PlayAmbientActivity.DayPhase,
        random: Random = Random
    ): Int? {
        if (phase == PlayAmbientActivity.DayPhase.NIGHT) return null
        if (random.nextInt(100) >= CHANCE_PERCENT) return null
        return lineOf(topic)
    }

    /** Der Satz zu einer Taetigkeit - ohne Wuerfeln, fuer die Pruefung und fuer Sonderfaelle. */
    @StringRes
    fun lineOf(topic: AnimationType): Int = when (topic) {
        AnimationType.SLEEP -> R.string.speech_sleep
        AnimationType.REST -> R.string.speech_rest
        AnimationType.BOOK -> R.string.speech_book
        AnimationType.MINDFULNESS -> R.string.speech_mindfulness
        AnimationType.LOVE -> R.string.speech_love
        AnimationType.DRINK -> R.string.speech_drink
        AnimationType.MEDICINE -> R.string.speech_medicine
        AnimationType.WORK -> R.string.speech_work
        AnimationType.FOCUS -> R.string.speech_focus
        AnimationType.MOVE -> R.string.speech_move
        AnimationType.CREATIVITY -> R.string.speech_creativity
        AnimationType.GENERAL -> R.string.speech_general
    }

    /**
     * Der Halbsatz, der aus seinem Tun deines macht - nur, wenn dasselbe Thema bei dir heute noch
     * offen ist.
     *
     * Bewusst EIN Text fuer alle Themen statt zwoelf eigener: Was ihn traegt, ist nicht die
     * Formulierung, sondern der Zeitpunkt. Er steht neben einer Figur, die es gerade vormacht.
     */
    @StringRes
    fun habitHint(): Int = R.string.speech_open_habit
}
