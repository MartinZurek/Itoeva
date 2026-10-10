package com.notime.glyphsim.matrix

/** Kurze, lokale Verschnaufpause nach echtem Rennen, ohne gespeicherte Strafe oder Agentenbeduerfnis. */
object GameBreath {
    const val PAUSE_MS=1200L
    data class State(val reserve: Float=1f,val pauseLeft: Long=0L,val recovering: Boolean=false)
    fun input(state: State,input: PlayControl.Stick): PlayControl.Stick {
        if(state.pauseLeft>0L) return PlayControl.Stick()
        val cap=if(state.recovering) .65f else 1f
        val scale=if(input.strength>cap) cap/input.strength else 1f
        return input.copy(x=input.x*scale,y=input.y*scale)
    }
    fun tick(state: State,running: Boolean,moving: Boolean,dt: Long): State {
        val step=dt.coerceIn(0L,50L)
        if(step==0L) return state
        if(state.pauseLeft>0L) return state.copy(pauseLeft=(state.pauseLeft-step).coerceAtLeast(0L),
            reserve=(state.reserve+step/3000f).coerceAtMost(1f),recovering=true)
        val reserve=(state.reserve+if(running && !state.recovering) -step/12000f
            else step/if(moving) 6000f else 3000f).coerceIn(0f,1f)
        if(reserve<=.00001f && running) return State(0f,PAUSE_MS,true)
        return State(reserve,0L,state.recovering && reserve<.45f)
    }
    fun cue(state: State): CreatureSprites.MotionCue? = if(state.pauseLeft>0L)
        CreatureSprites.MotionCue(CreatureSprites.Motion.STRETCH,1f-state.pauseLeft.toFloat()/PAUSE_MS) else null
}
