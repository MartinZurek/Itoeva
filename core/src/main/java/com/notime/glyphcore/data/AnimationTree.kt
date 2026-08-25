package com.notime.glyphcore.data

import androidx.annotation.StringRes
import com.notime.glyphcore.R

/**
 * **Der Animations-Baum: neun Hauptgruppen, achtzehn Untergruppen, zweiundfuenfzig Blaetter.**
 *
 * Ordnet die vorhandenen Motive - die eingebauten [AnimationType]s, die 26 allgemeinen
 * ([DefaultLibraryAnimations]) und die 30 charakterspezifischen ([AvatarSignatureAnimations]) -
 * zu einer Struktur, in der ein Knoten eine BESTIMMTERE Fassung seines Elternknotens ist.
 *
 * ## Wozu das gut ist
 *
 * **Abgestufte Reaktionen.** Weil jeder Knoten einen Elternknoten hat, muss nicht jeder eine
 * eigene Choreografie mitbringen: Wer keine hat, erbt die seines Elternknotens (der Aufrufer
 * laeuft dafuer [fallbackChain] ab). Neun Choreografien auf Stufe 1 genuegen also, damit ALLE
 * Knoten eine sinnvolle Antwort haben - jede weitere macht die Antwort genauer, schliesst aber
 * keine Luecke. Ohne diese Eigenschaft muesste jeder neue Knoten sofort mit einer eigenen
 * Choreografie kommen, und der Baum waere nicht bezahlbar.
 *
 * **Freischalten.** Ein Baum hat eine Grenze - die Kinder dessen, was schon offen ist. Eine flache
 * Liste hat das nicht, und ohne "was kommt als Naechstes" gibt es nichts freizuschalten.
 *
 * ## Was hier absichtlich NICHT steht
 *
 * **[AnimationType.MEDICINE] ist kein Knoten** (siehe [EXCLUDED_TYPES]). Alles in diesem Baum ist
 * auf Spiel angelegt: gezogen, freigeschaltet, zufaellig vorgesetzt. Eine Medikamenten-Erinnerung
 * darf nie so entstehen. Dieselbe Regel gilt schon im Spielplan des Avatars, wo MEDICINE in
 * keinem Themengewicht vorkommt - hier wird sie nur an der zweiten Stelle wiederholt, an der sie
 * gebrochen werden koennte.
 *
 * **Die Freischaltung gilt nur fuer das Spiel.** Der Baum entscheidet nicht darueber, welche
 * Motive fuer ECHTE Erinnerungen zur Verfuegung stehen - die Bibliothek bleibt vollstaendig
 * offen. Sonst koennte ein Spielfortschritt am Ende eine Erinnerung sperren.
 *
 * **Anzeigenamen nur auf den Stufen 1 und 2** (siehe [AnimationNode.titleRes]).
 *
 * Grundlage und Begruendung der einzelnen Zuordnungen: SKILLBAUM.md im Projektstamm.
 */
object AnimationTree {

    /**
     * Typen, die bewusst ausserhalb des Baums bleiben.
     *
     * Siehe Klassendoku: Was im Baum steht, kann gezogen, freigeschaltet und gewuerfelt werden.
     * Fuer eine Medikamenten-Erinnerung ist all das falsch.
     */
    val EXCLUDED_TYPES: Set<AnimationType> = setOf(AnimationType.MEDICINE)

    // ================= Der Baum =================

    val nodes: List<AnimationNode> = buildList {

        // ---- SPORT & BEWEGUNG: was Kraft kostet und ein Ziel hat ----
        group("sport", "🏃", R.string.node_sport, AnimationType.MOVE)
        sub("sport/ballsport", "⚽", R.string.node_sport_ballsport, "Football")
        leaf("sport/ballsport/basketball", "🏀", "Basketball")
        leaf("sport/ballsport/trophy", "🏆", "Trophy")
        leaf("sport/ballsport/dribbling", "🤾", "Dribble")
        leaf("sport/ballsport/schuss", "🥅", "Shot")
        sub("sport/kraft-ausdauer", "🏋️", R.string.node_sport_kraft, "Fitness")
        leaf("sport/kraft-ausdauer/summit", "🏔️", "Summit")
        leaf("sport/kraft-ausdauer/ladder", "🪜", "Ladder")
        leaf("sport/kraft-ausdauer/flag", "🚩", "Flag")
        leaf("sport/kraft-ausdauer/heben", "💪", "Lift")

        // ---- KOERPER & VERSORGUNG: was dem Koerper zugefuehrt wird ----
        // Der Kopf von "essen" ist noch ungezeichnet: Cake ist nach "feiern" gewandert, weil es
        // dort als Geburtstagsmotiv der haeufigere Anlass ist.
        group("koerper", "💧", R.string.node_koerper, AnimationType.DRINK)
        sub("koerper/trinken", "🌊", R.string.node_koerper_trinken, "Wave")
        leaf("koerper/trinken/drip", "🫠", "Drip")
        leaf("koerper/trinken/puddle", "💦", "Puddle")
        leaf("koerper/trinken/rain", "☂️", "Rain")
        sub("koerper/essen", "🍽️", R.string.node_koerper_essen, "Plate")
        leaf("koerper/essen/plant", "🌱", "Plant")
        leaf("koerper/essen/battery", "🔋", "Battery")

        // ---- RUHE & SCHLAF: die langsamsten Motive, Abschluss immer beruhigend ----
        group("ruhe", "😴", R.string.node_ruhe, AnimationType.SLEEP)
        sub("ruhe/schlafen", "☁️", R.string.node_ruhe_schlafen, "Cloud")
        leaf("ruhe/schlafen/balloon", "🎈", "Balloon")
        leaf("ruhe/schlafen/turtle", "🐢", "Turtle")
        leaf("ruhe/schlafen/snail", "🐌", "Snail")
        subBuiltin("ruhe/pause", "☕", R.string.node_ruhe_pause, AnimationType.REST)
        leaf("ruhe/pause/candle", "🕯️", "Candle")
        leaf("ruhe/pause/lantern", "🏮", "Lantern")
        leaf("ruhe/pause/nest", "🪺", "Nest")

        // ---- ACHTSAMKEIT: nicht schlafen, sondern bewusst aussetzen ----
        // Unterschied zu "ruhe": Der Blick folgt etwas, statt sich zu schliessen.
        group("achtsamkeit", "🧘", R.string.node_achtsamkeit, AnimationType.MINDFULNESS)
        sub("achtsamkeit/atmen", "🌀", R.string.node_achtsamkeit_atmen, "Breathe")
        leaf("achtsamkeit/atmen/feather", "🪶", "Feather")
        leaf("achtsamkeit/atmen/anchor", "⚓", "Anchor")
        sub("achtsamkeit/beobachten", "🦋", R.string.node_achtsamkeit_beobachten, "Butterfly")
        leaf("achtsamkeit/beobachten/bubble", "🫧", "Bubble")
        leaf("achtsamkeit/beobachten/constellation", "🌌", "Constellation")
        leaf("achtsamkeit/beobachten/eye", "👁️", "Eye")

        // ---- ARBEIT & FOKUS: das Geraet, und die Sammlung davor ----
        group("arbeit", "💻", R.string.node_arbeit, AnimationType.WORK)
        sub("arbeit/geraet", "🤖", R.string.node_arbeit_geraet, "Robot")
        leaf("arbeit/geraet/stocks", "📈", "Stocks")
        leaf("arbeit/geraet/tama", "🔤", "TAMA")
        leaf("arbeit/geraet/pause-machen", "⏸️", "Breather")
        subBuiltin("arbeit/erledigen", "🎯", R.string.node_arbeit_erledigen, AnimationType.FOCUS)
        leaf("arbeit/erledigen/check", "✅", "Check")
        leaf("arbeit/erledigen/target", "🎯", "Target")
        leaf("arbeit/erledigen/hourglass", "⏳", "Hourglass")
        leaf("arbeit/erledigen/shield", "🛡️", "Shield")
        leaf("arbeit/erledigen/lighthouse", "🗼", "Lighthouse")

        // ---- LERNEN & ERKENNTNIS: der Moment des Verstehens, keine Handlung ----
        group("lernen", "📖", R.string.node_lernen, AnimationType.BOOK)
        sub("lernen/lesen", "📜", R.string.node_lernen_lesen, "Scroll")
        leaf("lernen/lesen/idea", "💡", "Idea")
        leaf("lernen/lesen/notizen", "📝", "Notes")
        sub("lernen/knobeln", "🧩", R.string.node_lernen_knobeln, "Puzzle")
        leaf("lernen/knobeln/key", "🔑", "Key")
        leaf("lernen/knobeln/compass", "🧭", "Compass")

        // ---- KREATIVITAET: etwas entsteht ----
        group("kreativ", "🎨", R.string.node_kreativ, AnimationType.CREATIVITY)
        sub("kreativ/musik", "🎵", R.string.node_kreativ_musik, "Music")
        leaf("kreativ/musik/drum", "🥁", "Drum")
        leaf("kreativ/musik/bolt", "⚡", "Bolt")
        leaf("kreativ/musik/singen", "🎤", "Sing")
        sub("kreativ/bauen-malen", "⭐", R.string.node_kreativ_bauen, "Star")
        leaf("kreativ/bauen-malen/fire", "🔥", "Fire")
        leaf("kreativ/bauen-malen/kite", "🪁", "Kite")

        // ---- NAEHE & BEZIEHUNG: die einzige Gruppe ohne Charakter-Motiv ----
        // Bei sechs Avataren hat keiner ein Motiv fuer andere Menschen bekommen. Deshalb haengt
        // diese Gruppe ganz an allgemeinen Motiven - und wird als erste gezeichnet (P8).
        group("naehe", "❤️", R.string.node_naehe, AnimationType.LOVE)
        sub("naehe/freunde", "✉️", R.string.node_naehe_freunde, "Mail")
        leaf("naehe/freunde/gift", "🎁", "Gift")
        leaf("naehe/freunde/besuch", "🚪", "Visit")
        leaf("naehe/freunde/anrufen", "📞", "Call")
        sub("naehe/tiere", "🐕", R.string.node_naehe_tiere, "Dog")
        leaf("naehe/tiere/cat", "🐈", "Cat")
        leaf("naehe/tiere/pet", "👾", "Pet")
        leaf("naehe/tiere/paw", "🐾", "Paw")

        // ---- AUFBRUCH & ANLAESSE: der Auffangkorb, und das mit Absicht ----
        // GENERAL ist der Typ fuer alles ohne eigenes Thema; hier liegen die einmaligen Termine.
        group("aufbruch", "🔔", R.string.node_aufbruch, AnimationType.GENERAL)
        sub("aufbruch/reisen", "✈️", R.string.node_aufbruch_reisen, "Airplane")
        leaf("aufbruch/reisen/rocket", "🚀", "Rocket")
        leaf("aufbruch/reisen/comet", "☄️", "Comet")
        leaf("aufbruch/reisen/karte", "🗺️", "Map")
        sub("aufbruch/feiern", "🎂", R.string.node_aufbruch_feiern, "Cake")
        leaf("aufbruch/feiern/konfetti", "🎊", "Confetti")
        leaf("aufbruch/feiern/kerzen", "🕯️", "Candles")
    }

    // ================= Nachschlagen =================

    private val byId: Map<String, AnimationNode> = nodes.associateBy { it.id }

    private val byMotif: Map<AnimationMotif, String> =
        nodes.mapNotNull { node -> node.motif?.let { it to node.id } }.toMap()

    fun node(id: String): AnimationNode? = byId[id]

    /** Die neun Hauptgruppen, in Baumreihenfolge. */
    fun roots(): List<AnimationNode> = nodes.filter { it.depth == 1 }

    fun children(id: String): List<AnimationNode> = nodes.filter { it.parentId == id }

    /**
     * Der Knoten selbst und alle seine Vorfahren, vom Knoten aufwaerts zur Hauptgruppe.
     *
     * **Das ist die Reihenfolge, in der nach einer Choreografie gesucht wird** (siehe
     * Klassendoku): Fuer `sport/ballsport/dribbling` ergibt sich `[dribbling, ballsport, sport]`.
     * Der Aufrufer nimmt den ersten Treffer - hat "Dribbling" noch keine eigene Antwort,
     * antwortet der Avatar so, wie er auf "Ballsport" antworten wuerde.
     *
     * Ein unbekannter Pfad ergibt eine leere Liste. Bewusst nicht die Hauptgruppe als Notnagel:
     * Ein Pfad, den es nicht gibt, ist ein Fehler und soll auffallen, nicht stillschweigend etwas
     * Beliebiges abspielen.
     */
    fun fallbackChain(id: String): List<String> {
        if (id !in byId) return emptyList()
        val chain = mutableListOf<String>()
        var current: String? = id
        while (current != null) {
            chain += current
            current = byId[current]?.parentId
        }
        return chain
    }

    /** Zu welchem Knoten gehoert dieses Bibliotheks-Motiv? `null` bei unbekanntem Label. */
    fun nodeIdFor(label: String): String? = byMotif[AnimationMotif.Library(label)]

    /**
     * Zu welchem Knoten gehoert dieser eingebaute Typ?
     *
     * `null` fuer [AnimationType.MEDICINE] - siehe [EXCLUDED_TYPES]. Aufrufer, die daraus einen
     * Spiel-Knoten machen wollen, bekommen hier also nichts, und das ist die Absicht.
     */
    fun nodeIdFor(type: AnimationType): String? = byMotif[AnimationMotif.Builtin(type)]

    /** Das Motiv eines Knotens, oder `null` wenn es noch nicht gezeichnet ist. */
    fun motifFor(id: String): AnimationMotif? = byId[id]?.motif

    /**
     * Knoten, deren Pixel-Animation noch fehlt - die Arbeitsliste fuer P8.
     *
     * Oeffentlich, weil eine Pruefung sonst nicht festhalten koennte, wie viele es sind: Waechst
     * die Zahl unbemerkt, ist der Baum irgendwann voller Knoten, die beim Ziehen nichts zeigen.
     */
    fun pendingArtwork(): List<AnimationNode> = nodes.filter { it.motif == null }

    // ================= Bauhilfen =================

    private fun MutableList<AnimationNode>.group(
        id: String,
        emoji: String,
        @StringRes titleRes: Int,
        type: AnimationType
    ) = add(
        AnimationNode(id, emoji, AnimationNode.Kind.ACTIVITY, AnimationMotif.Builtin(type), titleRes)
    )

    private fun MutableList<AnimationNode>.sub(
        id: String,
        emoji: String,
        @StringRes titleRes: Int,
        label: String?
    ) = add(
        AnimationNode(
            id, emoji, AnimationNode.Kind.ACTIVITY,
            label?.let { AnimationMotif.Library(it) }, titleRes
        )
    )

    /** Untergruppe, deren Kopf ein eingebauter Typ ist (nur `ruhe/pause` und `arbeit/erledigen`). */
    private fun MutableList<AnimationNode>.subBuiltin(
        id: String,
        emoji: String,
        @StringRes titleRes: Int,
        type: AnimationType
    ) = add(
        AnimationNode(id, emoji, AnimationNode.Kind.ACTIVITY, AnimationMotif.Builtin(type), titleRes)
    )

    private fun MutableList<AnimationNode>.leaf(
        id: String,
        emoji: String,
        label: String?
    ) = add(
        AnimationNode(
            id, emoji, AnimationNode.Kind.FLOURISH,
            label?.let { AnimationMotif.Library(it) }
        )
    )
}
