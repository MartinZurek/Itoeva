package com.notime.glyphsim.ui

import android.content.Context

/**
 * Wem die Routinen gehoeren - **getrennt von der Frage, welches Wesen gerade da ist.**
 *
 * ## Die Verwechslung, die es aufloest
 *
 * Im Kern gibt es genau eine Profil-Id ([com.notime.glyphcore.reminder.ActiveProfilePrefs]), und
 * sie beantwortet heute zwei voellig verschiedene Fragen:
 *
 * 1. **Wessen Erinnerungen werden eingeplant?** (Planer, Watchdog, Erinnerungsliste)
 * 2. **Wessen Pflegebuch, Stimmung und Spielstand ist das?** (Fuetter-Ereignisse, AvatarMood,
 *    AvatarPlayState)
 *
 * Solange beide denselben Wert haben, faellt der Unterschied nicht auf - und genau deshalb ist er
 * entstanden. Sichtbar wird er erst an den Folgen: ein Avatarwechsel ist optische
 * Personalisierung, tauscht aber die komplette Routinenliste aus. Das ist Problem 3 der
 * Produktanalyse, und `AvatarProfileCharacterizationTest` misst es nach.
 *
 * ## Was diese Datei jetzt tut - und was noch nicht
 *
 * Sie benennt die erste Frage. Aufrufer, die "wem gehoeren die Routinen" meinen, fragen ab jetzt
 * hier; wer "welches Wesen" meint, fragt weiterhin [AvatarSpeciesPrefs]. Am Ergebnis aendert sich
 * **nichts**: [current] liefert vorerst genau die Avatar-Id, die auch bisher verwendet wurde.
 *
 * Das ist Absicht. Erst wenn die beiden Bedeutungen im Code auseinandergehalten sind, ist der
 * eigentliche Schnitt eine Aenderung an EINER Funktion statt an zwanzig Aufrufstellen - und dann
 * laesst sich auch pruefen, ob wirklich nur das Beabsichtigte kippt.
 *
 * ## Wohin es fuehrt
 *
 * Danach liefert [current] einen festen Wert, und die Routinen gehoeren dem Nutzer statt dem
 * Wesen. Das :app-Modul arbeitet uebrigens schon immer so - es kennt keine Avatare und bleibt beim
 * Standardprofil. Insofern ist der Zielzustand kein neues Konzept, sondern das, was der Kern
 * ohnehin vorsieht.
 */
object RoutineOwner {

    /**
     * Die Profil-Id, unter der die Routinen des Nutzers stehen.
     *
     * **Vorerst identisch mit dem gewaehlten Avatar** - siehe Klassendoku. Wer diesen Wert
     * aendert, aendert damit, welche Erinnerungen eingeplant werden; das ist die ganze Wirkung
     * und der Grund, warum sie an einer Stelle stehen soll.
     */
    fun current(context: Context): String =
        AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(context))
}
