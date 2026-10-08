package com.notime.glyphsim.matrix

/** Neue Innenraeume aus dem Bildatlas; nur die Game-Welt benutzt sie. */
internal object GameInteriorCatalog {
    val scenes: Map<PlayScene.Place, GameScenes.Scene> = mapOf(
        PlayScene.Place.LIVING to GameScenes.Scene(PlayScene.Place.LIVING, "interiors/living.png",
            145f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.SEAT, GameScenes.Box(170f, 110f, 304f, 164f), 260f, 184f),
                GameScenes.Spot(PlayScene.Station.LAMP, GameScenes.Box(86f, 65f, 124f, 159f), 103f, 180f),
            ), cropTop = .5f),
        PlayScene.Place.BEDROOM to GameScenes.Scene(PlayScene.Place.BEDROOM, "interiors/bedroom.png",
            160f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.BED, GameScenes.Box(220f, 115f, 355f, 227f), 285f, 240f),
            ), cropTop = .5f),
        PlayScene.Place.BATH to GameScenes.Scene(PlayScene.Place.BATH, "interiors/bath.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.TUB, GameScenes.Box(275f, 75f, 355f, 194f), 315f, 222f),
                GameScenes.Spot(PlayScene.Station.BASIN, GameScenes.Box(80f, 75f, 160f, 195f), 120f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.DESK to GameScenes.Scene(PlayScene.Place.DESK, "interiors/desk.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.DESK, GameScenes.Box(275f, 75f, 355f, 195f), 315f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.KITCHEN to GameScenes.Scene(PlayScene.Place.KITCHEN, "interiors/kitchen.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.TABLE, GameScenes.Box(140f, 75f, 220f, 195f), 180f, 222f),
                GameScenes.Spot(PlayScene.Station.FRIDGE, GameScenes.Box(334f, 75f, 414f, 195f), 374f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.NOOK to GameScenes.Scene(PlayScene.Place.NOOK, "interiors/nook.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.BOOKSHELF, GameScenes.Box(275f, 75f, 355f, 195f), 315f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.CRAFT to GameScenes.Scene(PlayScene.Place.CRAFT, "interiors/craft.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.CRAFT, GameScenes.Box(275f, 75f, 355f, 195f), 315f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.SHOP to GameScenes.Scene(PlayScene.Place.SHOP, "interiors/shop.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.CHECKOUT, GameScenes.Box(150f, 75f, 230f, 195f), 190f, 222f),
                GameScenes.Spot(PlayScene.Station.RACK, GameScenes.Box(325f, 75f, 405f, 195f), 365f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.CAFE to GameScenes.Scene(PlayScene.Place.CAFE, "interiors/cafe.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.TABLE, GameScenes.Box(248f, 75f, 328f, 195f), 288f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.WORK to GameScenes.Scene(PlayScene.Place.WORK, "interiors/work.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.WORKPLACE, GameScenes.Box(275f, 75f, 355f, 195f), 315f, 222f),
            ), cropTop = .5f),
        PlayScene.Place.ARCADE to GameScenes.Scene(PlayScene.Place.ARCADE, "interiors/arcade.png",
            193f, 256f, 22f, 458f, 10f, 470f, 40f, 76f, listOf(
                GameScenes.Spot(PlayScene.Station.ARCADE, GameScenes.Box(275f, 75f, 355f, 195f), 315f, 222f),
            ), cropTop = .5f),
    )
}
