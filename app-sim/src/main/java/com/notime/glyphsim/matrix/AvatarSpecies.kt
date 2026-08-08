package com.notime.glyphsim.matrix

import androidx.annotation.StringRes
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R

/**
 * Waehlbare Avatar-Grundformen (siehe [AvatarBody]/[AvatarBodies] fuer die konkrete Silhouette
 * je Eintrag, [com.notime.glyphsim.ui.AvatarSpeciesPrefs] fuer die Persistenz der Auswahl).
 *
 * Bezeichnung und Beschreibung liegen als Ressourcen-IDs vor, damit sie uebersetzbar sind. Die
 * NAMEN selbst bleiben in beiden Sprachen gleich - es sind Eigennamen der Kreaturen, kein
 * uebersetzbarer Text; uebersetzt wird nur die Beschreibung darunter.
 *
 * Wichtig: der Enum-Name ist zugleich die Profil-ID (siehe AvatarSpeciesPrefs) und steht so in
 * der Datenbank - eine Uebersetzung wirkt sich daher nie auf gespeicherte Daten aus.
 *
 * **Drei getrennte Textebenen, die unabhaengig voneinander stimmen muessen:**
 * - [descriptionRes]: reine Silhouette, fuer die Auswahl in den Einstellungen ("rund mit grossen
 *   Ohren"). Bleibt korrekt, ganz gleich, welcher Charakter dahintersteckt.
 * - [personalityRes]: WER die Figur ist, ein Satz Charakterisierung ("der neugierige
 *   Optimist..."), erzaehlt aus der Erzaehler-Perspektive, nicht aus der Figur selbst.
 * - [taglineRes]: WAS die Figur sagen wuerde - ihr eigener Leitsatz, in ihrer Stimme. Wird schon
 *   im Vorstellungs-Clip verwendet (siehe [com.notime.glyphsim.ui.AvatarClips]).
 *
 * [signatureTopic] ist der thematische Schwerpunkt je Charakter (z.B. Fennec/DRINK: "der
 * verlaessliche Beschuetzer, der auf deine Gewohnheiten aufpasst" passt zu Trinken als
 * Grundgewohnheit schlechthin) - abgeleitet aus [personalityRes], nicht umgekehrt erfunden.
 * PUFFLING bekommt bewusst [AnimationType.GENERAL]: als Einstiegs-Avatar (siehe
 * AvatarSpeciesPrefs, Standardwert) spezialisiert er sich nicht auf eine Gewohnheit, sondern
 * stellt die App selbst vor - GENERAL ist im Kern genau das (siehe ReminderRhythm.FLEXIBLE):
 * keine Festlegung, volle Bandbreite. Deckt sich mit seiner Persoenlichkeit: der neugierige
 * Optimist, der staendig Neues entdeckt, statt sich auf ein Thema zu versteifen.
 *
 * Aktuell reine Metadaten ohne erzwingende Wirkung: eine Erinnerung zu einem anderen Thema laesst
 * sich fuer jeden Avatar weiterhin genauso anlegen. Vorgesehen als Grundlage fuer kuenftige
 * Clips (der Hoehepunkt-Beat zeigt dann die [signatureTopic]-Reaktion statt der generischen
 * Freuden-Reaktion, siehe [AvatarAnimations.reactionFor]) und moeglich fuer spaeter passendere
 * Standard-Vorschlaege je Avatar.
 */
enum class AvatarSpecies(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val personalityRes: Int,
    @StringRes val taglineRes: Int,
    val signatureTopic: AnimationType
) {
    // Der neugierige Optimist - liebt kleine Fortschritte, entdeckt staendig etwas Neues, freut
    // sich ehrlich ueber jede Kleinigkeit. GENERAL statt einer Spezialisierung, siehe Klassendoku:
    // er stellt die App selbst vor, nicht eine einzelne Gewohnheit.
    PUFFLING(
        R.string.avatar_puffling,
        R.string.avatar_puffling_desc,
        R.string.avatar_puffling_personality,
        R.string.avatar_puffling_tagline,
        signatureTopic = AnimationType.GENERAL
    ),
    // Die freundliche Traeumerin - warmherzig, ruhig, hoffnungsvoll, erinnert daran, freundlich
    // zu sich selbst zu sein. MINDFULNESS: Selbstfreundlichkeit ist im Kern Achtsamkeit.
    STARLET(
        R.string.avatar_starlet,
        R.string.avatar_starlet_desc,
        R.string.avatar_starlet_personality,
        R.string.avatar_starlet_tagline,
        signatureTopic = AnimationType.MINDFULNESS
    ),
    // Der kleine Motivator - mutig, positiv, voller Energie, aber nie hektisch. MOVE: "Los geht's,
    // nur ein kleiner Schritt" ist die klassische Sport-Ermutigung.
    WYRMLING(
        R.string.avatar_wyrmling,
        R.string.avatar_wyrmling_desc,
        R.string.avatar_wyrmling_personality,
        R.string.avatar_wyrmling_tagline,
        signatureTopic = AnimationType.MOVE
    ),
    // Der ruhige Beschuetzer - gelassen, aufmerksam, verlaesslich, erinnert ohne je zu nerven.
    // DRINK: "ich passe auf deine Gewohnheiten auf" trifft auf die Grundgewohnheit schlechthin.
    FENNEC(
        R.string.avatar_fennec,
        R.string.avatar_fennec_desc,
        R.string.avatar_fennec_personality,
        R.string.avatar_fennec_tagline,
        signatureTopic = AnimationType.DRINK
    ),
    // Der Entschleuniger - gemuetlich, etwas chaotisch, charmant, erinnert daran, auch mal
    // langsamer zu werden. REST: "alles darf auch langsam gehen" ist woertlich eine Pause.
    GLOOP(
        R.string.avatar_gloop,
        R.string.avatar_gloop_desc,
        R.string.avatar_gloop_personality,
        R.string.avatar_gloop_tagline,
        signatureTopic = AnimationType.REST
    ),
    // Der weise Beobachter - still, geduldig, klug, beobachtet mehr, als er spricht. FOCUS:
    // "Ruhe bringt Klarheit" passt zu konzentriertem, ungestoertem Arbeiten.
    HOOTLET(
        R.string.avatar_hootlet,
        R.string.avatar_hootlet_desc,
        R.string.avatar_hootlet_personality,
        R.string.avatar_hootlet_tagline,
        signatureTopic = AnimationType.FOCUS
    )
}
