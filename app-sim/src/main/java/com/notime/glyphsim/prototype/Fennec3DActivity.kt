package com.notime.glyphsim.prototype

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.notime.glyphsim.R

/** Getrennter Techniktest: Erst beim Oeffnen werden Modell und GL-Kontext geladen. */
class Fennec3DActivity : Activity() {
    private lateinit var surface: GLSurfaceView
    private lateinit var renderer: FennecRenderer
    private var active = false
    private var lastFrame = 0L
    private var selectedClip = 0
    private var angle = 165
    private var frozen = false
    private val ticker = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!active) return
            // 30 Bilder/s reichen zur Modellabnahme und vermeiden unnoetige GPU-Arbeit.
            if (frameTimeNanos - lastFrame >= 32_000_000L) {
                surface.requestRender(); lastFrame = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        angle = savedInstanceState?.getInt("angle", 165) ?: 165
        selectedClip = savedInstanceState?.getInt("clip", 0) ?: 0
        frozen = savedInstanceState?.getBoolean("paused", false) ?: false
        val density = resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(27, 38, 38))
            setPadding(dp(12), dp(4), dp(12), dp(8))
        }
        val caption = TextView(this).apply {
            setText(R.string.fennec_3d_caption); setTextColor(Color.rgb(244, 218, 175)); textSize = 15f
        }
        column.addView(caption)
        renderer = FennecRenderer(this) { message -> runOnUiThread {
            caption.text = getString(R.string.fennec_3d_error, message)
        } }.apply { yaw = angle - 180f; clip = selectedClip; paused = frozen }
        surface = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 0, 16, 0)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        column.addView(surface, LinearLayout.LayoutParams(-1, 0, 1f))
        val slider = SeekBar(this).apply { max = 360; progress = angle
            contentDescription = getString(R.string.fennec_3d_rotate)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    angle = value; surface.queueEvent { renderer.yaw = value - 180f }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        column.addView(slider)
        val buttons = LinearLayout(this)
        fun button(label: Int, action: (Button) -> Unit) {
            val b = Button(this).apply { setText(label); textSize = 12f; setOnClickListener { action(this) } }
            buttons.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        button(R.string.fennec_3d_idle) { selectedClip = 0; surface.queueEvent { renderer.clip = 0 } }
        button(R.string.fennec_3d_walk) { selectedClip = 1; surface.queueEvent { renderer.clip = 1 } }
        button(R.string.fennec_3d_run) { selectedClip = 2; surface.queueEvent { renderer.clip = 2 } }
        button(if (frozen) R.string.fennec_3d_resume else R.string.fennec_3d_pause) { b ->
            frozen = !frozen; b.setText(if (frozen) R.string.fennec_3d_resume else R.string.fennec_3d_pause)
            surface.queueEvent { renderer.paused = frozen }
        }
        button(R.string.fennec_3d_back) { finish() }
        column.addView(buttons); setContentView(column)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("angle", angle); outState.putInt("clip", selectedClip); outState.putBoolean("paused", frozen)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        super.onResume(); surface.onResume(); active = true; lastFrame = 0L
        Choreographer.getInstance().postFrameCallback(ticker)
    }
    override fun onPause() {
        active = false; Choreographer.getInstance().removeFrameCallback(ticker)
        surface.onPause(); super.onPause()
    }
}
