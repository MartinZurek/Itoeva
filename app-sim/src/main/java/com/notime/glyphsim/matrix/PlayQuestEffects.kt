package com.notime.glyphsim.matrix

/**
 * **Was eine Quest im Bild zeigt** - ohne ein Wort (siehe [PlayQuests]).
 *
 * Jede Stufe einer Quest muss sich von selbst erklaeren: Eine Karte flattert aus einem Buch, es
 * wird gegraben, eine Truhe steigt aus dem Sand, ein Zauber misslingt erst und gelingt dann, ein
 * Ei bekommt Risse. Dieselbe Sprache wie die Motive der Wirkungen ([PlayEffects]): Gegenstaende
 * in derselben Tonwertordnung wie die Kulisse, freigestellt, mit einem einzigen Glanzpunkt - und
 * Bewegung ueber [age], die Takte seit Beginn des Bildes.
 *
 * Gezeichnet wird neben der Figur, auf der Seite, auf der Platz ist - wie bei den Wirkungen.
 */
object PlayQuestEffects {

    /** Die Bilder der Queststufen. */
    enum class Effect {
        /** Aus dem Buch flattert eine Karte herab. */
        MAP_FOUND,

        /** Die Karte, offen vor der Figur: ein gepunkteter Weg zu einem blinkenden Kreuz. */
        MAP_STUDY,

        /** Graben: Erdbrocken fliegen im Bogen, daneben waechst ein Haufen. */
        DIG,

        /** Die Truhe steigt aus dem Boden, der Deckel springt auf, Funken steigen. */
        CHEST_FOUND,

        /** Ein Zauber misslingt: zwei, drei matte Funken, dann ein Woelkchen Rauch. */
        SPELL_FIZZLE,

        /** Ein Zauber gelingt halb: Funken kreisen um die Figur. */
        SPELL_SPARKS,

        /** Der Zauber gelingt: ein Sternenregen ueber der Figur. */
        SPELL_STARBURST,

        /** Fern am Horizont blitzt etwas auf - dort muss etwas sein. */
        CRYSTAL_GLINT,

        /** Die Entdeckung: Licht bricht aus dem Felsen. */
        DISCOVERY,

        /** Ein schimmerndes Ei am Boden. */
        EGG_FOUND,

        /** Waerme: kleine warme Punkte steigen auf. */
        EGG_WARM,

        /** Das Ei bricht auf, Schalenstuecke fliegen, ein kleiner Drache ist da. */
        EGG_HATCH
    }

    /** So viele Takte laeuft ein Bild, bis es sich wiederholt oder stehen bleibt. */
    const val CYCLE = 24

    fun cells(
        effect: Effect,
        avatarCellX: Int,
        avatarCellY: Int,
        age: Int,
        widthCells: Int
    ): List<SceneCell> {
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
        val floorY = groundY + 1
        val t = age.coerceAtLeast(0)
        val useRight = avatarCellX + AvatarGeometry.SIZE + 2 + MOTIF_WIDTH <= widthCells
        val direction = if (useRight) 1 else -1
        val ox = if (useRight) avatarCellX + AvatarGeometry.SIZE + 1 else avatarCellX - 2

        fun sketch(height: Int, lift: Int = 0) =
            PlayInk.Sketch(ox, groundY - height + 1 - lift, direction, widthCells, floorY)

        return when (effect) {
            Effect.MAP_FOUND -> {
                // Die Karte segelt in acht Takten von Kopfhoehe herab und bleibt liegen.
                val fall = (t.coerceAtMost(8) * 10) / 8
                val map = sketch(height = 3, lift = (10 - fall).coerceAtLeast(0))
                map.art(0, 0, "#####", "#+#+#", "#####")
                if (t < 8) map.spark(if (t % 2 == 0) 0 else 4, 0) else map.spark(2, 1)
                map.render(grounded = t >= 8)
            }

            Effect.MAP_STUDY -> {
                // Offen in Kopfhoehe: Rand, Weg aus Punkten, am Ende das Kreuz, das blinkt.
                val map = sketch(height = 6, lift = 8)
                map.box(0, 0, 8, 5, PlayInk.BODY)
                map.fill(1, 1, 7, 4, PlayInk.SHADOW)
                for ((i, p) in listOf(1 to 4, 2 to 3, 3 to 3, 4 to 2).withIndex()) {
                    if ((t / 2 + i) % 4 != 0) map.dot(p.first, p.second, PlayInk.DETAIL)
                }
                // Das Kreuz: Mitte hell, die vier Ecken - drei von vier Takten sichtbar.
                if (t % 4 < 3) {
                    map.dot(6, 2, PlayInk.SPARK)
                    listOf(5 to 1, 7 to 1, 5 to 3, 7 to 3).forEach { (x, y) -> map.dot(x, y, PlayInk.EDGE) }
                }
                map.render(grounded = false)
            }

            Effect.DIG -> {
                // Der Haufen waechst mit der Zeit, Brocken fliegen im Takt nach aussen.
                val pile = sketch(height = 3)
                val size = (1 + t / 6).coerceAtMost(3)
                when (size) {
                    1 -> pile.art(1, 2, "###")
                    2 -> pile.art(0, 1, " ### ", "#####")
                    else -> pile.art(0, 0, "  #  ", " ### ", "#####")
                }
                val air = sketch(height = 8, lift = 2)
                val k = t % 4
                // Zwei Brocken auf einer Wurfparabel, versetzt.
                listOf(k, (k + 2) % 4).forEach { s ->
                    val x = 1 + s * 2
                    val y = 7 - listOf(2, 5, 6, 4)[s]
                    air.dot(x, y, PlayInk.DETAIL)
                }
                pile.render(grounded = true) + air.render(carve = false)
            }

            Effect.CHEST_FOUND -> {
                // In acht Takten steigt die Truhe; danach springt der Deckel auf und es funkelt.
                val rise = t.coerceAtMost(8)
                val chest = sketch(height = 5, lift = 0)
                val open = t >= 10
                val rows = if (open) {
                    listOf("#     ", "##****", "######", "#++++#", "######")
                } else {
                    listOf("      ", "######", "#++*+#", "#++++#", "######")
                }
                // Nur so viel zeigen, wie schon aus dem Boden ragt.
                val visible = (rise * 5) / 8
                rows.takeLast(visible.coerceAtLeast(1)).let { shown ->
                    chest.art(0, 5 - shown.size, *shown.toTypedArray())
                }
                val sparks = sketch(height = 10, lift = 5)
                if (open) {
                    for (i in 0..2) {
                        val y = 9 - ((t + i * 3) % 9)
                        sparks.dot(1 + i * 2, y, if ((t + i) % 3 == 0) PlayInk.SPARK else PlayInk.EDGE)
                    }
                }
                chest.render(grounded = true) + sparks.render(carve = false)
            }

            Effect.SPELL_FIZZLE -> {
                // Zwei matte Funken, dann Rauch, der aufsteigt und vergeht.
                val s = sketch(height = 10, lift = 6)
                if (t % 8 < 3) {
                    s.dot(0, 8, PlayInk.EDGE)
                    s.dot(2, 7, PlayInk.DETAIL)
                } else {
                    val puff = (t % 8) - 3
                    s.art(0, 6 - puff, "+#+", "###")
                }
                s.render(carve = false)
            }

            Effect.SPELL_SPARKS -> {
                // Acht Funken auf einem Kreis um die Figur, drei davon hell, der Kreis dreht sich.
                val cx = avatarCellX + AvatarGeometry.SIZE / 2
                val cy = avatarCellY + AvatarGeometry.HEIGHT / 2
                val r = AvatarGeometry.SIZE / 2 + 2
                val ring = RING.map { (dx, dy) -> dx * r / 10 to dy * r / 10 }
                ring.mapIndexedNotNull { i, (dx, dy) ->
                    val lit = (i + t) % 8
                    if (lit > 3) null
                    else SceneCell(
                        cx + dx, cy + dy,
                        if (lit == 0) PlayScene.HIGHLIGHT else PlayScene.GLOW - lit * 400,
                        isLight = true
                    )
                }.filter { it.x in 0 until widthCells && it.y in 0 until floorY }
            }

            Effect.SPELL_STARBURST -> {
                // Ueber dem Kopf: ein Stern, dessen Strahlen wachsen, und Sterne, die herabregnen.
                val cx = avatarCellX + AvatarGeometry.SIZE / 2
                val top = avatarCellY - 2
                val grow = (t % 12).coerceAtMost(6)
                val cells = mutableListOf<SceneCell>()
                cells += SceneCell(cx, top - 3, PlayScene.HIGHLIGHT, isLight = true)
                for (s in 1..grow) {
                    val level = (PlayScene.GLOW - s * 250).coerceAtLeast(900)
                    listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0).forEach { (dx, dy) ->
                        cells += SceneCell(cx + dx * s, top - 3 + dy * s, level, isLight = true)
                    }
                    if (s <= grow / 2) {
                        listOf(1 to 1, -1 to 1, 1 to -1, -1 to -1).forEach { (dx, dy) ->
                            cells += SceneCell(cx + dx * s, top - 3 + dy * s, level - 400, isLight = true)
                        }
                    }
                }
                for (i in 0..5) {
                    val x = cx - 8 + i * 3
                    val y = top - 6 + ((t + i * 5) % 14)
                    if (y < groundY) cells += SceneCell(x, y, if ((t + i) % 2 == 0) PlayScene.GLOW else PlayScene.GLOW - 900, isLight = true)
                }
                cells.filter { it.x in 0 until widthCells && it.y in 0 until floorY }
            }

            Effect.CRYSTAL_GLINT -> {
                // Weit rechts, knapp ueber dem Boden: ein kurzes Aufblitzen im Takt.
                val x = (widthCells * 0.88f).toInt()
                val y = groundY - 9
                val on = t % 6 < 2
                if (!on) emptyList() else listOf(
                    SceneCell(x, y, PlayScene.HIGHLIGHT, isLight = true),
                    SceneCell(x - 1, y, PlayScene.GLOW - 800, isLight = true),
                    SceneCell(x + 1, y, PlayScene.GLOW - 800, isLight = true),
                    SceneCell(x, y - 1, PlayScene.GLOW - 800, isLight = true),
                    SceneCell(x, y + 1, PlayScene.GLOW - 800, isLight = true)
                ).filter { it.x in 0 until widthCells && it.y >= 0 }
            }

            Effect.DISCOVERY -> {
                // Strahlen, die von der Figur weg nach rechts oben ausstrahlen, in Wellen.
                val cells = mutableListOf<SceneCell>()
                val sx = ox + direction * 2
                val sy = groundY - 6
                val wave = t % 8
                for (ray in 0..4) {
                    val dy = ray - 2
                    for (step in 1..(wave + 2)) {
                        val level = (PlayScene.GLOW - step * 180).coerceAtLeast(700)
                        cells += SceneCell(sx + direction * step, sy + dy * step / 2, level, isLight = true)
                    }
                }
                cells += SceneCell(sx, sy, PlayScene.HIGHLIGHT, isLight = true)
                cells.filter { it.x in 0 until widthCells && it.y in 0 until floorY }
            }

            Effect.EGG_FOUND -> {
                val egg = sketch(height = 6)
                egg.art(0, 0, " ## ", "#+##", "####", "##+#", "####", " ## ")
                egg.spark(1, 1)
                // Ein Schimmer ringsum, im Takt: links, rechts, darueber.
                val glow = sketch(height = 9, lift = 0)
                if (t % 4 < 2) {
                    glow.dot(-1, 5, PlayInk.EDGE)
                    glow.dot(4, 5, PlayInk.EDGE)
                    glow.dot(1, 1, PlayInk.EDGE)
                }
                egg.render(grounded = true) + glow.render(carve = false)
            }

            Effect.EGG_WARM -> {
                // Waerme steigt zwischen Figur und Nest auf: drei Punkte, versetzt.
                val s = sketch(height = 12, lift = 2)
                for (i in 0..2) {
                    val y = 10 - ((t + i * 4) % 11)
                    s.dot(i % 2, y, if (i == 1) PlayInk.EDGE else PlayInk.DETAIL)
                }
                s.render(carve = false)
            }

            Effect.EGG_HATCH -> {
                // Acht Takte Risse, dann fliegt die Schale, und das Junge sitzt da und funkelt.
                val s = sketch(height = 7)
                if (t < 8) {
                    s.art(0, 0, "  ##  ", " #### ", "######", "######", " #### ", "  ##  ")
                    val cracks = listOf(2 to 1, 3 to 2, 2 to 3, 3 to 4)
                    cracks.take(1 + t / 2).forEach { (x, y) -> s.dot(x, y, PlayInk.SHADOW) }
                    if (t % 2 == 0) s.dot(2 + t % 3, 0, PlayInk.SPARK)
                    s.render(grounded = true)
                } else {
                    // Das Junge: Kopf, Fluegelchen, Schwanz; daneben die beiden Schalenhaelften.
                    s.art(0, 1, " ##  ", "###+#", "#### ", " ## #", "#  # ")
                    s.spark(1, 1)
                    val shell = sketch(height = 3)
                    val fly = ((t - 8) * 2).coerceAtMost(6)
                    shell.art(-2 - fly / 2, 3 - (if (fly < 6) fly / 2 else 0) + (fly / 3), "#.#")
                    shell.art(6 + fly / 2, 3 - (if (fly < 6) fly / 2 else 0) + (fly / 3), "#.#")
                    s.render(grounded = true) + shell.render(carve = false)
                }
            }
        }
    }

    /** Breitestes Motiv - entscheidet wie bei den Wirkungen, ob rechts Platz ist. */
    private const val MOTIF_WIDTH = 9

    /** Acht Punkte auf einem Kreis mit Radius 10 (auf Zehntel gerechnet). */
    private val RING = listOf(0 to -10, 7 to -7, 10 to 0, 7 to 7, 0 to 10, -7 to 7, -10 to 0, -7 to -7)
}
