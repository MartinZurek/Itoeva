package com.notime.glyphsim.matrix

import kotlin.math.abs

/** Die Malerei hat Boden, eine hintere Grenze und vorne Felskanten, keinen begehbaren Himmel.
 * Die Projektion bleibt gleich; alle Wege und Speicherpunkte lesen dieselbe Bodenbegrenzung. */
object GameTerrain {
    fun band(scene: GameScenes.Scene, x: Float): Pair<Float,Float> {
        val projected = GameScenes.floorBand(scene,x)
        if (!GameWorld.isWorld(scene)) {
            if (!GameRoomSpace.enabled(scene)) return projected
            var back = when(scene.place) {
                PlayScene.Place.LIVING -> 153f
                PlayScene.Place.BEDROOM -> 155f
                else -> 165f
            }
            for (door in GameWorld.passages(scene.place).filter { it.door }) {
                val anchor=GameScenes.feet(scene,door.pos)
                if(abs(x-door.pos.x)<.08f) back=minOf(back,anchor.second)
            }
            return back to scene.nearY
        }
        if (scene.place == PlayScene.Place.COAST_PATH) {
            val t = ((minOf(x,1f-x))/.12f).coerceIn(0f,1f)
            // Der gemalte Brueckenbogen ist schon vermessen, nur die Randwege sind schmaler.
            return (495f*(1-t)+projected.first*t) to (562f*(1-t)+projected.second*t)
        }
        val measured=GameWalkingMap.band(scene,x)
        var back=measured?.first ?: 495f; var front=measured?.second ?: 562f
        if (scene.asset == "world/coast.png") { back=450f; front=615f }
        // Die letzte Wasserstrecke wird vor der trockenen Anschlussnaht zum Ufer.
        val wx=GameWorld.origin(scene.place)+x*GameWorld.region(scene.place)!!.section
        if (scene.asset == "world/coast.png") {
            val t=((wx-1800f)/120f).coerceIn(0f,1f)
            back += (495f-back)*t; front += (562f-front)*t
        }
        for (door in GameWorld.passages(scene.place).filter { it.door }) {
            val p=GameScenes.feet(scene,door.pos)
            val distance=abs(x-door.pos.x)
            if (distance < .10f) back=minOf(back,p.second+maxOf(0f,distance-.035f)*350f)
        }
        return back.coerceIn(projected.first,projected.second-1f) to front.coerceIn(projected.first+1f,projected.second)
    }
    fun clamp(scene: GameScenes.Scene, requested: PlayControl.Pos): PlayControl.Pos {
        val x=requested.x.coerceIn(0f,1f)
        val full=GameScenes.floorBand(scene,x); val legal=band(scene,x)
        val bounded=requested.copy(x=x, depth=requested.depth.coerceIn(
            ((legal.first-full.first)/(full.second-full.first)).coerceIn(0f,1f),
            ((legal.second-full.first)/(full.second-full.first)).coerceIn(0f,1f)))
        if (!GameWalkingMap.blocked(scene,bounded)) return bounded
        // Beim Laden einmal auf die naechste freie Stelle setzen, nie beim laufenden Schritt teleportieren.
        return (1..40).flatMap { radius -> listOf(
            bounded.copy(x=(bounded.x-radius*.0025f).coerceIn(0f,1f)),
            bounded.copy(x=(bounded.x+radius*.0025f).coerceIn(0f,1f)),
            bounded.copy(depth=(bounded.depth-radius*.01f).coerceIn(0f,1f)),
            bounded.copy(depth=(bounded.depth+radius*.01f).coerceIn(0f,1f))) }
            .firstOrNull { !GameWalkingMap.blocked(scene,it) && floorValid(scene,it) } ?: bounded
    }
    fun valid(scene: GameScenes.Scene, p: PlayControl.Pos): Boolean {
        return floorValid(scene,p) && !GameWalkingMap.blocked(scene,p)
    }
    private fun floorValid(scene: GameScenes.Scene,p: PlayControl.Pos): Boolean {
        if (!p.x.isFinite() || !p.depth.isFinite() || p.x !in 0f..1f || p.depth !in 0f..1f) return false
        val feet=GameScenes.feet(scene,p);val bounds=band(scene,p.x)
        return feet.second >= bounds.first-.001f && feet.second <= bounds.second+.001f
    }
    fun resolve(scene: GameScenes.Scene,from: PlayControl.Pos,to: PlayControl.Pos): PlayControl.Pos {
        val safe=clamp(scene,to)
        // Die Profilkante darf gleiten; Sperrinseln muessen auf dem gesamten Weg respektiert werden.
        fun clear(p: PlayControl.Pos) = GameWalkingMap.clear(scene,from,p)
        if (clear(safe)) return safe
        val horizontal=clamp(scene,to.copy(depth=from.depth,pushMs=0L))
        if (clear(horizontal)) return horizontal
        val vertical=clamp(scene,to.copy(x=from.x,pushMs=0L))
        if (clear(vertical)) return vertical
        return from.copy(pushMs=0L)
    }
    fun command(scene: GameScenes.Scene,state: GameMovement.State,command: GameMovement.Command,
        input: PlayControl.Stick,surfaces: List<GameMovement.Surface>): GameMovement.State {
        // Im tiefen Wasser ruht man durch Wassertreten; Rollen und Bodensitzen haben dort keinen Halt.
        if(state.height<=0f && GameWorld.wetness(scene,state.pos)>=.20f && command!=GameMovement.Command.JUMP)
            return state.copy(action=null,elapsed=0L,velocity=PlayControl.Stick())
        return GameMovement.command(state,command,input,surfaces,GameCharacterScale.MAX_STEP_HEIGHT)
    }
    /** Game-Adapter des bestehenden Motors, keine zweite Bewegungssimulation. */
    fun tick(scene: GameScenes.Scene, species: AvatarSpecies, state: GameMovement.State,
        input: PlayControl.Stick, dt: Long, surfaces: List<GameMovement.Surface>,
        request: Pair<GameMovement.Command, PlayControl.Stick>? = null): GameMovement.Result {
        // Der UI-Befehl wird erst nach Orts-/Positionsabgleich im selben Motorbild verbraucht.
        val controlled = request?.let { command(scene, state, it.first, it.second, surfaces) } ?: state
        val drag=if(controlled.height>0f) 1f else GameWater.drag(scene,controlled.pos)
        val result=GameMovement.tick(controlled,input.copy(x=input.x*drag,y=input.y*drag),dt,surfaces,
            immediateExits=GameWorld.immediateExits(scene.place), horizontalScale=GameWorld.horizontalScale(scene.place),
            maxStepHeight=GameCharacterScale.MAX_STEP_HEIGHT,sweptJumpCollision=true)
        var next=result.state
        if (next.support == null) {
            val p=resolve(scene,clamp(scene,state.pos),next.pos)
            next=next.copy(pos=p, arc=next.arc?.let { it.copy(to=clamp(scene,it.to)) })
        }
        if (!GameWorld.isWorld(scene)) {
            val ceiling=(GameScenes.feet(scene,next.pos).second-GameCharacterScale.visibleHeight(scene,next.pos,species)-20f).coerceAtLeast(0f)
            next=next.copy(height=minOf(next.height,ceiling))
        }
        if(next.action==null && abs(next.pos.x-state.pos.x)<.000001f &&
            abs(next.pos.depth-state.pos.depth)<.000001f && result.exit==null) {
            next=next.copy(velocity=PlayControl.Stick(),gaitMs=state.gaitMs,running=false,
                runBlend=(state.runBlend-dt.coerceIn(0L,50L)/160f).coerceAtLeast(0f))
        }
        return result.copy(state=next,exit=result.exit.takeIf { next.pos.x == result.state.pos.x })
    }
}
