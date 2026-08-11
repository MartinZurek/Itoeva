package com.notime.glyphsim.ui

import android.content.Context

/**
 * Welches Wesen gerade da ist - **die Gegenseite von [RoutineOwner].**
 *
 * ## Die zweite Bedeutung derselben Id
 *
 * [RoutineOwner] beantwortet "wem gehoeren die Routinen". Diese Datei beantwortet die andere
 * Frage, die heute versehentlich dieselbe Antwort bekommt: **wessen Pflegebuch, Stimmung und
 * Spielstand ist das?**
 *
 * Betroffen sind [com.notime.glyphsim.data.AvatarFeedEvent] (jede Ausloesung und jede Reaktion
 * darauf), [AvatarMoodSnapshot], [com.notime.glyphsim.data.AvatarPlayState] und alles, was daraus
 * abgeleitet wird - Statistik, Rhythmus-Vorschlag, was das Wesen im Gespraech ueber den Tag weiss.
 *
 * ## Warum die Trennung nicht kosmetisch ist
 *
 * Sobald die Routinen dem Nutzer gehoeren statt dem Wesen, muss jede dieser Stellen sich
 * entscheiden. Faellt die Entscheidung falsch aus - also zugunsten des Routinen-Besitzers -, dann
 * traegt ein frisch gewaehltes Wesen ab der ersten Sekunde Erinnerungen an Dinge, bei denen es
 * nicht dabei war. Das ist genau die Beziehung, die es aufbauen soll, nur eben erfunden.
 *
 * Die Regel dahinter, in einem Satz: **die Routinen gehoeren dir, die Erinnerungen daran gehoeren
 * dem Wesen, mit dem du sie geteilt hast.** Ein Wechsel bedeutet dann nicht "meine Daten sind
 * weg", sondern "hier faengt jemand Neues an".
 *
 * ## Seit Phase 4b laufen die beiden auseinander
 *
 * Hier stand bis zuletzt "beide liefern die Id des gewaehlten Avatars" - richtig fuer die
 * Zwischenstufe, in der die Unterscheidung erst benannt wurde. Seit [RoutineOwner.current] einen
 * festen Wert liefert, stimmt es nicht mehr: Die Routinen stehen unter dem Standardprofil, das
 * Pflegebuch unter dem Namen des Wesens.
 *
 * Genau dieses Auseinanderlaufen war der Zweck der Uebung. Dass die Unterscheidung vorher im Code
 * stand, hat den eigentlichen Schnitt zu einer Aenderung an EINER Funktion gemacht statt zu einer
 * Suche nach zwanzig Aufrufstellen.
 */
object PresentCompanion {

    /**
     * Die Profil-Id des Wesens, das gerade da ist - der Schluessel seines Pflegebuchs.
     *
     * Es gibt bewusst keine Ueberladung "fuer eine bestimmte Spezies": wer die Spezies schon in
     * der Hand hat (etwa aus dem Bildschirmzustand), nimmt [AvatarSpeciesPrefs.profileId] direkt.
     * Diese Funktion ist fuer die Aufrufer ohne Zustand - Alarme, Hintergrundarbeit, kalt
     * gestartete Prozesse.
     */
    fun profileId(context: Context): String =
        AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(context))
}
