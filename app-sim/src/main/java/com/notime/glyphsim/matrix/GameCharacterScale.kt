package com.notime.glyphsim.matrix

/** Ein Weltmass fuer Spieler und Bewohner; der Bildrahmen ist keine Koerperhoehe. */
object GameCharacterScale {
    /** Die groessere Game-Figur erreicht auch die 59-/68-Pixel-Tischplatten. */
    const val MAX_STEP_HEIGHT = 78f
    /** Standreferenz aus Rolle 0, nicht aus dem wechselnden Lauf-/Sitzbild. */
    data class Reference(val top: Int, val left: Int, val right: Int, val relativeHeight: Float)

    fun reference(species: AvatarSpecies): Reference = when (species) {
        AvatarSpecies.FENNEC -> Reference(17, 7, 113, 1f)
        AvatarSpecies.GLOOP -> Reference(56, 23, 107, .91f)
        AvatarSpecies.PUFFLING -> Reference(61, 25, 105, .93f)
        AvatarSpecies.WYRMLING -> Reference(32, 9, 119, 1.19f)
        AvatarSpecies.STARLET -> Reference(44, 23, 105, .90f)
        AvatarSpecies.HOOTLET -> Reference(61, 28, 101, .86f)
    }

    // Fennecs Scheitel liegt bei 43, seine Ohrspitzen bei 17, die Pfoten bei 126.
    // Der Bodenmassstab gilt fuer den Koerper; Ohren erhalten ihre eigene Bildhoehe.
    private const val BODY_SPAN = 83f
    private const val FENNEC_SPAN = 109f

    fun visibleHeight(scene: GameScenes.Scene, pos: PlayControl.Pos, species: AvatarSpecies): Float =
        GameScenes.avatarHeight(scene, pos) * FENNEC_SPAN / BODY_SPAN * reference(species).relativeHeight

    /** Breite des bestehenden AvatarSpriteView, einschliesslich seines alten Sprite-Faktors. */
    fun layoutWidth(scene: GameScenes.Scene, pos: PlayControl.Pos, species: AvatarSpecies): Float =
        visibleHeight(scene, pos, species) * CreatureSprites.Rich.FRAME /
            (CreatureSprites.Rich.FEET - reference(species).top) / CreatureSprites.Rich.scaleFor(species)

    /** Im Game darf der Layout-Rand ueber dem Bildschirm liegen; der Fussanker bleibt fest. */
    fun layoutTop(feetY: Float, width: Float, species: AvatarSpecies): Float =
        feetY - localFeetY(width, species)

    fun localFeetY(width: Float, species: AvatarSpecies): Float =
        (AvatarBodies.forSpecies(species).groundRow() + 1) * width / AvatarGeometry.SIZE

    fun hitBox(scene: GameScenes.Scene, pos: PlayControl.Pos, species: AvatarSpecies): GameScenes.Box {
        val (x, y) = GameScenes.feet(scene, pos)
        val ref = reference(species)
        val drawn = layoutWidth(scene, pos, species) * CreatureSprites.Rich.scaleFor(species)
        return GameScenes.Box(x + (ref.left / 128f - .5f) * drawn - 3f,
            y - visibleHeight(scene, pos, species) - 3f,
            x + (ref.right / 128f - .5f) * drawn + 3f, y + 4f)
    }

}
