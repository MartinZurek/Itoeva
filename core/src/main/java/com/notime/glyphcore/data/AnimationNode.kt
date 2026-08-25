package com.notime.glyphcore.data

import androidx.annotation.StringRes

/**
 * Ein Knoten im Animations-Baum (siehe [AnimationTree]).
 *
 * **Warum ueberhaupt ein Baum.** Bisher lagen alle Motive flach nebeneinander: die zwoelf
 * eingebauten [AnimationType]s, 26 allgemeine Bibliotheks-Animationen und 30 charakterspezifische
 * - 68 gleichrangige Eintraege ohne jede Beziehung untereinander. Damit laesst sich weder etwas
 * freischalten (es gibt kein "danach") noch eine Reaktion abstufen (es gibt kein "genauer als").
 *
 * Der Baum hat drei Ebenen, und die Ebene sagt, wie bestimmt der Wunsch ist:
 *
 * - **Stufe 1** - die neun Hauptgruppen. "Beweg dich."
 * - **Stufe 2** - je zwei Untergruppen. "Spiel Ball."
 * - **Stufe 3** - die Blaetter. "Mach einen Dribbling-Trick."
 *
 * [id] ist der Pfad durch diese Ebenen (`sport/ballsport/dribbling`), bewusst als Zeichenkette und
 * nicht als Aufzaehlung: Der Baum waechst inhaltlich weiter (aus je zwei Untergruppen sollen
 * einmal drei bis fuenf werden), und jede neue Untergruppe waere sonst eine Schema-Aenderung.
 * Derselbe Grund, aus dem in der Datenbank der Enum-NAME steht und nicht seine Ordnungszahl.
 *
 * Nur ASCII und Kleinbuchstaben, Ebenen durch `/` getrennt. Der Pfad landet als `nodeId` in der
 * Datenbank und in gespeicherten Ereignissen - er muss stabil bleiben, auch wenn sich die
 * Anzeigenamen aendern oder uebersetzt werden.
 */
data class AnimationNode(
    /** Pfad durch den Baum, z. B. `sport/ballsport/dribbling`. Eindeutig, stabil, nur ASCII. */
    val id: String,

    /** Kurzzeichen fuer Listen und die Zieh-Leiste - sprachneutral, deshalb fest am Knoten. */
    val emoji: String,

    val kind: Kind,

    /**
     * Das Motiv, das auf dem Glyph laeuft, wenn dieser Knoten gezogen wird.
     *
     * `null` heisst: Der Knoten steht im Baum, seine Pixel-Animation ist aber noch nicht
     * gezeichnet (siehe SKILLBAUM.md, Paket P8). Solche Knoten sind bewusst schon hier
     * eingetragen - der Baum ist damit vollstaendig beschrieben, und was fehlt, ist abzaehlbar
     * statt vergessen.
     */
    val motif: AnimationMotif?,

    /**
     * Anzeigename - **nur bei den 27 Knoten der Stufen 1 und 2 gesetzt.**
     *
     * Deren Namen ("Ballsport", "Kraft & Ausdauer") gibt es nirgends sonst; sie entstehen erst mit
     * dem Baum und brauchen deshalb eine eigene, uebersetzbare Zeichenkette. Blaetter tragen
     * dagegen den Namen ihres Motivs, das ohnehin schon einen hat - eine zweite Bezeichnung
     * daneben koennte nur auseinanderlaufen. Blaetter ohne Motiv bekommen ihren Namen zusammen
     * mit ihrer Zeichnung.
     */
    @StringRes val titleRes: Int? = null
) {
    /** `null` nur bei den neun Hauptgruppen. */
    val parentId: String?
        get() = id.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { null }

    /** 1 = Hauptgruppe, 2 = Untergruppe, 3 = Blatt. */
    val depth: Int
        get() = id.count { it == '/' } + 1

    /**
     * Was beim Ziehen auf den Avatar passiert.
     *
     * Der Unterschied ist keine Feinheit der Darstellung, sondern der Grund, warum Stufe 3
     * ueberhaupt eine eigene Ebene ist: "Dribbling" ist keine Beschaeftigung, der man nachgehen
     * kann - es ist etwas, das man TUT, WAEHREND man Ball spielt.
     */
    enum class Kind {
        /**
         * Eine Beschaeftigung: Er geht hin und tut es, und tut es weiter, bis etwas anderes kommt.
         * Stufe 1 und 2.
         */
        ACTIVITY,

        /**
         * Eine Einlage in eine laufende Beschaeftigung. Passt sie zur laufenden, wird sie
         * eingeschoben; laeuft etwas anderes, wechselt er erst zur passenden Beschaeftigung der
         * Stufe 2. Stufe 3.
         */
        FLOURISH
    }
}

/**
 * Woher die Bilder eines Knotens kommen.
 *
 * Beide Quellen liegen laengst nebeneinander und werden auch in der Datenbank getrennt gehalten
 * (`animationType` und `libraryAnimationLabel` in `AvatarFeedEvent`, genau eines von beiden ist
 * gesetzt). Der Baum bildet diese Trennung ab, statt beides in eine Zeichenkette zu ruehren und
 * spaeter an der Schreibweise auseinanderhalten zu muessen.
 */
sealed interface AnimationMotif {
    /** Einer der fest eingebauten Typen - Bilder aus `ReminderAnimations.framesFor`. */
    data class Builtin(val type: AnimationType) : AnimationMotif

    /** Ein Eintrag der Bibliothek, angesprochen ueber [LibraryAnimation.label]. */
    data class Library(val label: String) : AnimationMotif
}
