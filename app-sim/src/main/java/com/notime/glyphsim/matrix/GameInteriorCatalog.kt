package com.notime.glyphsim.matrix

/** Gemeinsame Raumprojektion mit freiem Hinter-/Vorderboden und passendem Figurenmassstab.
 * Die Raumkoerper werden separat gezeichnet; die alten Assetpfade bleiben kompatibel. */
internal object GameInteriorCatalog {
    val scenes: Map<PlayScene.Place, GameScenes.Scene> = mapOf(
        PlayScene.Place.LIVING to GameScenes.Scene(PlayScene.Place.LIVING, "interiors/living.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.SEAT, GameScenes.Box(170f, 110f, 304f, 164f), 260f, 184f),
                GameScenes.Spot(PlayScene.Station.LAMP, GameScenes.Box(146f, 96f, 175f, 162f), 154f, 184f),
            ), cropTop = .5f),
        PlayScene.Place.BEDROOM to GameScenes.Scene(PlayScene.Place.BEDROOM, "interiors/bedroom.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.BED, GameScenes.Box(96f, 95f, 280f, 179f), 290f, 190f),
            ), cropTop = .5f),
        PlayScene.Place.BATH to GameScenes.Scene(PlayScene.Place.BATH, "interiors/bath.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.TUB, GameScenes.Box(301f, 132f, 455f, 195f), 360f, 217f),
                GameScenes.Spot(PlayScene.Station.BASIN, GameScenes.Box(99f, 118f, 183f, 191f), 146f, 216f),
            ), cropTop = .5f),
        PlayScene.Place.DESK to GameScenes.Scene(PlayScene.Place.DESK, "interiors/desk.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.DESK, GameScenes.Box(227f, 130f, 414f, 210f), 295f, 226f),
            ), cropTop = .5f),
        PlayScene.Place.KITCHEN to GameScenes.Scene(PlayScene.Place.KITCHEN, "interiors/kitchen.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.TABLE, GameScenes.Box(119f, 146f, 273f, 212f), 197f, 230f),
                GameScenes.Spot(PlayScene.Station.FRIDGE, GameScenes.Box(391f, 57f, 461f, 195f), 416f, 218f),
            ), cropTop = .5f),
        PlayScene.Place.NOOK to GameScenes.Scene(PlayScene.Place.NOOK, "interiors/nook.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.BOOKSHELF, GameScenes.Box(287f, 50f, 401f, 201f), 349f, 220f),
            ), cropTop = .5f),
        PlayScene.Place.CRAFT to GameScenes.Scene(PlayScene.Place.CRAFT, "interiors/craft.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.CRAFT, GameScenes.Box(245f, 133f, 415f, 197f), 323f, 220f),
            ), cropTop = .5f),
        PlayScene.Place.SHOP to GameScenes.Scene(PlayScene.Place.SHOP, "interiors/shop.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.CHECKOUT, GameScenes.Box(98f, 131f, 264f, 201f), 178f, 220f),
                GameScenes.Spot(PlayScene.Station.RACK, GameScenes.Box(404f, 44f, 476f, 226f), 395f, 232f),
            ), cropTop = .5f),
        PlayScene.Place.CAFE to GameScenes.Scene(PlayScene.Place.CAFE, "interiors/cafe.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.TABLE, GameScenes.Box(233f, 141f, 330f, 219f), 285f, 234f),
            ), cropTop = .5f),
        PlayScene.Place.WORK to GameScenes.Scene(PlayScene.Place.WORK, "interiors/work.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.WORKPLACE, GameScenes.Box(215f, 133f, 409f, 208f), 290f, 227f),
            ), cropTop = .5f),
        PlayScene.Place.ARCADE to GameScenes.Scene(PlayScene.Place.ARCADE, "interiors/arcade.png",
            116f, 256f, 48f, 432f, 12f, 468f, 44f, 74f, listOf(
                GameScenes.Spot(PlayScene.Station.ARCADE, GameScenes.Box(282f, 55f, 397f, 207f), 347f, 228f),
            ), cropTop = .5f),
    )
}
