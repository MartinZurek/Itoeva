package com.notime.glyphsim.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.LibraryAnimation

/**
 * Ein Log-Eintrag: der User hat die Uhr waehrend einer laufenden Erinnerungs-Animation auf den
 * Tamagotchi-Avatar geschoben ("gefuettert") - im Dock-Modus (ui/DockScreen.kt) oder auf dem
 * Startbildschirm (ui/HomeScreen.kt).
 *
 * [animationType] ist null, wenn die Erinnerung eine Custom-Bibliotheksanimation
 * ([LibraryAnimation]) statt eines festen [AnimationType] verwendet hat - dann traegt
 * [libraryAnimationLabel] deren Namen. Genau eines von beiden ist gesetzt; zusammen ergeben sie
 * die Antwort auf "womit wurde gefuettert", die die Statistik aufschluesselt.
 *
 * [profileId] haelt fest, WELCHER Avatar gefuettert wurde (der Avatar-Name ist die Profil-ID,
 * siehe ui/AvatarSpeciesPrefs). Ohne dieses Feld liessen sich die Fuetterungen nicht je Avatar
 * auswerten - alle Avatare teilten sich eine gemeinsame Zahl, obwohl jeder seinen eigenen Satz
 * Erinnerungen hat.
 */
@Entity(tableName = "avatar_feed_events")
data class AvatarFeedEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val animationType: AnimationType?,
    /** Zeitpunkt, zu dem die Erinnerung ausgeloest hat. */
    val epochMillis: Long,
    val profileId: String,
    val libraryAnimationLabel: String? = null,
    /**
     * Zeitpunkt der Reaktion, oder null wenn niemand reagiert hat.
     *
     * **Warum eine Zeile je AUSLOESUNG und nicht je Fuetterung:** vorher entstand ein Eintrag
     * ausschliesslich beim Fuettern - gezaehlt wurden also nur die Erfolge. Damit liess sich die
     * eigentlich interessante Frage nicht beantworten: worauf reagiere ich regelmaessig, und was
     * ignoriere ich? "5 Mahlzeiten" ist eine Zahl, "Trinken uebergehst du zu 80 %" ist eine
     * Erkenntnis. Seit jede Ausloesung eine Zeile anlegt (siehe reminder/ReminderAlarmReceiver),
     * ergibt sich beides aus derselben Tabelle: alle Zeilen = ausgeloest, Zeilen mit
     * [fedAtMillis] = beantwortet, der Rest = verpasst.
     */
    val fedAtMillis: Long? = null,
    /**
     * Uebernommen von [com.notime.glyphcore.data.GlyphReminder.isPlayMode] zum Zeitpunkt der
     * Ausloesung (siehe reminder/ReminderTrigger.show) - damit erkennt [AvatarFeeding] beim
     * Fuettern ohne Zusatz-Abfrage, ob XP faellig sind (siehe ui/PlayModeXp), und die normale
     * Fuetter-Statistik (FeedStatsDialog) kann Play-Mode-Ausloesungen bei Bedarf getrennt halten.
     */
    val isPlayMode: Boolean = false,

    /**
     * Der Knoten im Animations-Baum, auf den diese Ausloesung faellt (siehe
     * [com.notime.glyphcore.data.AnimationTree]) - etwa `sport/ballsport/basketball`.
     *
     * **Redundant zu [animationType]/[libraryAnimationLabel], und trotzdem eine eigene Spalte.**
     * Beide liessen sich zur Laufzeit in einen Knoten uebersetzen; die Neigungsrechnung des
     * Skillbaums (SKILLBAUM.md, Paket P4) muesste das dann aber fuer jede einzelne Zeile tun,
     * statt einfach nach Zweig gruppieren zu koennen - aus einem `GROUP BY` wuerde ein
     * vollstaendiger Durchlauf durch die gesamte Historie.
     *
     * **Gefuellt fuer BEIDE Quellen**, eingebaute Typen wie Bibliotheks-Animationen. Genau das ist
     * der Sinn: Fuer die Frage "welchen Zweig bediene ich am ehesten" ist es gleichgueltig, ob die
     * Erinnerung an einem eingebauten Typ hing oder an einer Bibliotheks-Animation - beide sagen
     * etwas ueber dasselbe Interesse aus.
     *
     * `null` bei den beiden Faellen, die zu keinem Knoten gehoeren: einer selbstgezeichneten
     * Animation, und [com.notime.glyphcore.data.AnimationType.MEDICINE], das bewusst ausserhalb
     * des Baums steht.
     */
    val nodeId: String? = null
)
