package com.notime.glyphsim.matrix

/** Bekannte Bilder zuerst; der Plan beschreibt nur vorhandene lokale Assets. */
object GameAssetPlan {
    fun paths(place: PlayScene.Place): List<String> {
        val scene=GameWorld.scene(place)!!
        val nearby=GameWorld.neighbors(place).mapNotNull { GameWorld.scene(it)?.asset }
        return (listOf(scene.asset)+nearby+GameWorld.regions.map { it.asset }+
            GameWorld.seams.map { it.asset }+GameInteriorCatalog.scenes.values.map { it.asset }).distinct()
    }
}
