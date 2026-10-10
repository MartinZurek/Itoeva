package com.notime.glyphsim.prototype

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Echte Tiefenpruefung und Normalenbeleuchtung; Kamera bleibt fest orthografisch. */
internal class FennecRenderer(private val context: Context, private val error: (String) -> Unit) : GLSurfaceView.Renderer {
    var yaw = -15f
    var clip = 0
    var paused = false
    private var model: FennecModel? = null
    private var program = 0
    private var position = 0
    private var normal = 0
    private var mvpLocation = 0
    private var worldLocation = 0
    private var colorLocation = 0
    private val vp = FloatArray(16)
    private val view = FloatArray(16)
    private val projection = FloatArray(16)
    private var local = emptyArray<FloatArray>()
    private var world = emptyArray<FloatArray>()
    private val root = FloatArray(16)
    private val mvp = FloatArray(16)
    private var lastNanos = 0L
    private var elapsed = 0f
    private var previousClip = 0
    private val floor = disc(1.1f, .18f, floatArrayOf(.20f, .245f, .205f, 1f))
    private val contact = disc(.34f, .182f, floatArrayOf(.10f, .13f, .10f, 1f))

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(.105f, .15f, .15f, 1f)
        glEnable(GL_DEPTH_TEST)
        // Der offene Blattmantel ist zweiseitiger Stoff, die Koerperteile sind geschlossene Meshes.
        glDisable(GL_CULL_FACE)
        lastNanos = 0L
        try {
            program = glCreateProgram()
            fun shader(type: Int, source: String): Int {
                val s = glCreateShader(type)
                glShaderSource(s, source); glCompileShader(s)
                val ok = IntArray(1); glGetShaderiv(s, GL_COMPILE_STATUS, ok, 0)
                check(ok[0] != 0) { glGetShaderInfoLog(s) }
                return s
            }
            val vertex = shader(GL_VERTEX_SHADER, VERTEX)
            val fragment = shader(GL_FRAGMENT_SHADER, FRAGMENT)
            glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program)
            glDeleteShader(vertex); glDeleteShader(fragment)
            val ok = IntArray(1); glGetProgramiv(program, GL_LINK_STATUS, ok, 0)
            check(ok[0] != 0) { glGetProgramInfoLog(program) }
            position = glGetAttribLocation(program, "aPosition")
            normal = glGetAttribLocation(program, "aNormal")
            mvpLocation = glGetUniformLocation(program, "uMvp")
            worldLocation = glGetUniformLocation(program, "uWorld")
            colorLocation = glGetUniformLocation(program, "uColor")
            model = context.assets.open("models/fennec-prototype.glb").use { FennecModel(it.readBytes()) }
            local = Array(model!!.nodes.size) { FloatArray(16) }
            world = Array(model!!.nodes.size) { FloatArray(16) }
        } catch (e: Exception) {
            model = null
            error(e.message ?: "3D initialization failed")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        val halfHeight = maxOf(1.62f, 1.25f / aspect)
        Matrix.orthoM(projection, 0, -halfHeight * aspect, halfHeight * aspect, -halfHeight, halfHeight, .1f, 30f)
        Matrix.setLookAtM(view, 0, 3.2f, 2.55f, 6f, 0f, 1.35f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        val m = model ?: return
        val now = System.nanoTime()
        val delta = if (lastNanos == 0L) 0f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, .1f)
        lastNanos = now
        if (previousClip != clip) { elapsed = 0f; previousClip = clip }
        if (!paused) elapsed += delta
        m.pose(clip, elapsed)
        Matrix.setIdentityM(root, 0); Matrix.rotateM(root, 0, yaw, 0f, 1f, 0f)
        glUseProgram(program)
        glEnableVertexAttribArray(position); glEnableVertexAttribArray(normal)
        Matrix.setIdentityM(mvp, 0)
        glUniformMatrix4fv(worldLocation, 1, false, mvp, 0)
        glUniformMatrix4fv(mvpLocation, 1, false, vp, 0)
        drawPart(floor); drawPart(contact)
        fun draw(i: Int, parent: FloatArray) {
            val n = m.nodes[i]
            quaternionMatrix(n.rotation, local[i])
            local[i][12] = n.translation[0]; local[i][13] = n.translation[1]; local[i][14] = n.translation[2]
            Matrix.multiplyMM(world[i], 0, parent, 0, local[i], 0)
            Matrix.multiplyMM(mvp, 0, vp, 0, world[i], 0)
            glUniformMatrix4fv(mvpLocation, 1, false, mvp, 0)
            glUniformMatrix4fv(worldLocation, 1, false, world[i], 0)
            for (part in n.parts) {
                drawPart(part)
            }
            for (child in n.children) draw(child, world[i])
        }
        for (r in m.roots) draw(r, root)
        glDisableVertexAttribArray(position); glDisableVertexAttribArray(normal)
    }

    private fun drawPart(part: FennecModel.Part) {
        glUniform4fv(colorLocation, 1, part.color, 0)
        glVertexAttribPointer(position, 3, GL_FLOAT, false, 0, part.positions)
        glVertexAttribPointer(normal, 3, GL_FLOAT, false, 0, part.normals)
        glDrawElements(GL_TRIANGLES, part.count, GL_UNSIGNED_SHORT, part.indices)
    }

    companion object {
        private fun disc(radius: Float, height: Float, color: FloatArray): FennecModel.Part {
            val vertices = FloatArray(3 * 33)
            val normals = FloatArray(3 * 33)
            val indices = ShortArray(32 * 3)
            vertices[1] = height; normals[1] = 1f
            for (i in 0 until 32) {
                val a = i * Math.PI * 2 / 32
                vertices[(i + 1) * 3] = (radius * kotlin.math.cos(a)).toFloat()
                vertices[(i + 1) * 3 + 1] = height
                vertices[(i + 1) * 3 + 2] = (radius * kotlin.math.sin(a)).toFloat()
                normals[(i + 1) * 3 + 1] = 1f
                indices[i * 3] = 0; indices[i * 3 + 1] = ((i + 1) % 32 + 1).toShort()
                indices[i * 3 + 2] = (i + 1).toShort()
            }
            fun floats(a: FloatArray) = ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply { put(a); position(0) }
            val ix = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder())
                .asShortBuffer().apply { put(indices); position(0) }
            return FennecModel.Part(floats(vertices), floats(normals), ix, indices.size, color)
        }
        fun quaternionMatrix(q: FloatArray, out: FloatArray) {
            val (x, y, z, w) = q
            out.fill(0f)
            out[0] = 1 - 2 * (y * y + z * z); out[1] = 2 * (x * y + z * w); out[2] = 2 * (x * z - y * w)
            out[4] = 2 * (x * y - z * w); out[5] = 1 - 2 * (x * x + z * z); out[6] = 2 * (y * z + x * w)
            out[8] = 2 * (x * z + y * w); out[9] = 2 * (y * z - x * w); out[10] = 1 - 2 * (x * x + y * y)
            out[15] = 1f
        }
        private const val VERTEX = """
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            uniform mat4 uMvp;
            uniform mat4 uWorld;
            varying vec3 vNormal;
            void main() {
                vNormal = mat3(uWorld) * aNormal;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """
        private const val FRAGMENT = """
            precision mediump float;
            varying vec3 vNormal;
            uniform vec4 uColor;
            void main() {
                vec3 n = normalize(vNormal);
                if (!gl_FrontFacing) n = -n;
                float diffuse = max(dot(n, normalize(vec3(-0.6, 0.8, 0.7))), 0.0);
                vec3 light = vec3(0.48, 0.47, 0.43) + diffuse * vec3(0.66, 0.60, 0.49);
                gl_FragColor = vec4(uColor.rgb * light, 1.0);
            }
        """
    }
}
