package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Place

/** Gemalte Durchgaenge statt frei schwebender Tuerrechtecke. Nicht im Spielstand gespeichert. */
object GameDoors {
    /** Bodensitzen ist kein gesperrter Weg; ein laufender Sprung/Rollimpuls muss erst enden. */
    fun canEnter(state: GameMovement.State): Boolean = state.height <= 0f &&
        state.action !in setOf(GameMovement.Action.JUMP, GameMovement.Action.ROLL, GameMovement.Action.RISE)

    const val OPEN_MS = 420L
    const val CLOSE_MS = 320L
    data class State(val passage: GameWorld.Passage, val started: Long, val arriving: Boolean = false)
    fun opening(state: State?, passage: GameWorld.Passage, clock: Long): Float {
        if(state?.passage != passage) return 0f
        val t=((clock-state.started).toFloat()/if(state.arriving) CLOSE_MS else OPEN_MS).coerceIn(0f,1f)
        val smooth=t*t*(3f-2f*t)
        return if(state.arriving) 1f-smooth else smooth
    }
    /** Direkter Zugriff auf Roh-Innenbilder vermeidet scene/links-Initialisierungsrekursion. */
    fun aperture(from: Place, to: Place): GameScenes.Box {
        if (GameWorld.contains(from)) return when (from) {
            Place.STREET -> if(to==Place.LIVING) GameScenes.Box(15f,354f,59f,486f)
                else GameScenes.Box(157f,387f,195f,486f)
            Place.CITY -> when(to) {
                Place.WORK -> GameScenes.Box(18f,370f,52f,489f)
                Place.CAFE -> GameScenes.Box(165f,402f,191f,488f)
                else -> GameScenes.Box(302f,415f,330f,489f)
            }
            else -> error("Kein Aussen-Tuereingang: $from")
        }
        return when(from) {
            Place.LIVING -> when(to) {
                Place.BEDROOM -> GameScenes.Box(201f,62f,232f,151f)
                Place.NOOK -> GameScenes.Box(275f,69f,305f,153f)
                Place.KITCHEN -> GameScenes.Box(446f,58f,477f,183f)
                else -> GameScenes.Box(5f,39f,30f,183f)
            }
            Place.BEDROOM -> when(to) {
                Place.BATH -> GameScenes.Box(337f,59f,372f,163f)
                Place.LIVING -> GameScenes.Box(437f,38f,470f,173f)
                else -> GameScenes.Box(7f,42f,40f,175f)
            }
            Place.KITCHEN -> GameScenes.Box(8f,43f,43f,198f)
            else -> GameScenes.Box(10f,49f,46f,198f)
        }
    }
    fun choices(passage: GameWorld.Passage) = GameWorld.passages(passage.from).filter {
        it.door && aperture(it.from,it.to)==aperture(passage.from,passage.to)
    }
    /** Die bewegte Tuer nimmt dieselben Pixel wie die sichtbare Kulisse, auch an Weltnaehten. */
    fun sample(passage: GameWorld.Passage): GameAtmosphere.Sample? {
        val box=aperture(passage.from,passage.to)
        return GameAtmosphere.sample(GameWorld.scene(passage.from)!!,
            GameAtmosphere.Patch(box.x0,box.y0,box.x1-box.x0,box.y1-box.y0,0))
    }
    fun anchor(from: Place,to: Place): PlayControl.Pos {
        val box=aperture(from,to)
        val scene=if(GameWorld.contains(from)) null else GameInteriorCatalog.scenes.getValue(from)
        val x=(box.x0+box.x1)/2f; val y=box.y1+8f
        val pos=if(scene!=null) GameScenes.posAt(scene,x,y)
            else PlayControl.Pos(x/480f,(y-450f)/175f)
        return pos.copy(x=pos.x.coerceIn(.06f,.94f),depth=pos.depth.coerceIn(.12f,.88f))
    }
}
