package com.notime.glyphsim.prototype

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt

/** Kleiner glTF-2-Leser fuer unser eigenes, unkomprimiertes GLB; kein allgemeiner Importer. */
internal class FennecModel(bytes: ByteArray) {
    data class Part(val positions: FloatBuffer, val normals: FloatBuffer, val indices: ShortBuffer,
                    val count: Int, val color: FloatArray, val texcoords: FloatBuffer? = null,
                    val textured: Boolean = false)
    data class Node(val name: String, val translation: FloatArray, val children: IntArray,
                    val parts: List<Part>, val rotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f))
    data class Track(val node: Int, val times: FloatArray, val rotations: FloatArray)
    data class Clip(val name: String, val duration: Float, val tracks: List<Track>)
    val nodes: List<Node>
    val roots: IntArray
    val clips: List<Clip>
    val textureBytes: ByteArray

    init {
        val input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(input.int == 0x46546c67 && input.int == 2 && input.int == bytes.size) { "Invalid GLB header" }
        val jsonSize = input.int
        require(input.int == 0x4e4f534a && jsonSize > 0 && jsonSize < input.remaining())
        val jsonBytes = ByteArray(jsonSize).also { input.get(it) }
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))
        val binarySize = input.int
        require(input.int == 0x004e4942 && binarySize <= input.remaining())
        val binary = input.slice().order(ByteOrder.LITTLE_ENDIAN)
        val accessors = json.getJSONArray("accessors")
        val views = json.getJSONArray("bufferViews")
        val imageView = views.getJSONObject(json.getJSONArray("images").getJSONObject(0).getInt("bufferView"))
        textureBytes = ByteArray(imageView.getInt("byteLength"))
        binary.duplicate().apply { position(imageView.optInt("byteOffset")); get(textureBytes) }
        fun floats(index: Int, components: Int): FloatArray {
            val a = accessors.getJSONObject(index)
            require(a.getInt("componentType") == 5126)
            val v = views.getJSONObject(a.getInt("bufferView"))
            val b = binary.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            b.position(v.optInt("byteOffset") + a.optInt("byteOffset"))
            return FloatArray(a.getInt("count") * components) { b.float }
        }
        fun buffer(values: FloatArray): FloatBuffer = ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values); position(0) }
        val materials = json.getJSONArray("materials")
        val meshes = json.getJSONArray("meshes")
        val meshParts = List(meshes.length()) { m ->
            val ps = meshes.getJSONObject(m).getJSONArray("primitives")
            List(ps.length()) { p ->
                val primitive = ps.getJSONObject(p)
                val attrs = primitive.getJSONObject("attributes")
                val a = accessors.getJSONObject(primitive.getInt("indices"))
                require(a.getInt("componentType") == 5123)
                val v = views.getJSONObject(a.getInt("bufferView"))
                val count = a.getInt("count")
                val ib = binary.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                ib.position(v.optInt("byteOffset") + a.optInt("byteOffset"))
                val ix = ByteBuffer.allocateDirect(count * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
                repeat(count) { ix.put(ib.short) }; ix.position(0)
                val mat = materials.getJSONObject(primitive.getInt("material")).getJSONObject("pbrMetallicRoughness")
                val color = mat.getJSONArray("baseColorFactor")
                Part(buffer(floats(attrs.getInt("POSITION"), 3)), buffer(floats(attrs.getInt("NORMAL"), 3)),
                    ix, count, FloatArray(4) { color.getDouble(it).toFloat() },
                    if (attrs.has("TEXCOORD_0")) buffer(floats(attrs.getInt("TEXCOORD_0"), 2)) else null,
                    mat.has("baseColorTexture"))
            }
        }
        val ns = json.getJSONArray("nodes")
        nodes = List(ns.length()) { i ->
            val n = ns.getJSONObject(i)
            val t = n.getJSONArray("translation")
            val cs = n.optJSONArray("children")
            Node(n.getString("name"), FloatArray(3) { t.getDouble(it).toFloat() },
                IntArray(cs?.length() ?: 0) { cs!!.getInt(it) },
                if (n.has("mesh")) meshParts[n.getInt("mesh")] else emptyList())
        }
        val rs = json.getJSONArray("scenes").getJSONObject(json.optInt("scene")).getJSONArray("nodes")
        roots = IntArray(rs.length()) { rs.getInt(it) }
        val animations = json.getJSONArray("animations")
        clips = List(animations.length()) { i ->
            val animation = animations.getJSONObject(i)
            val samplers = animation.getJSONArray("samplers")
            val channels = animation.getJSONArray("channels")
            val tracks = List(channels.length()) { j ->
                val channel = channels.getJSONObject(j)
                val target = channel.getJSONObject("target")
                require(target.getString("path") == "rotation")
                val sampler = samplers.getJSONObject(channel.getInt("sampler"))
                require(sampler.getString("interpolation") == "LINEAR")
                Track(target.getInt("node"), floats(sampler.getInt("input"), 1), floats(sampler.getInt("output"), 4))
            }
            Clip(animation.getString("name"), tracks.maxOf { it.times.last() }, tracks)
        }
    }

    fun pose(clip: Int, seconds: Float) {
        nodes.forEach { it.rotation.fill(0f); it.rotation[3] = 1f }
        val animation = clips[clip.coerceIn(clips.indices)]
        val t = (seconds % animation.duration).coerceAtLeast(0f)
        for (track in animation.tracks) {
            var k = 0
            while (k < track.times.size - 2 && track.times[k + 1] <= t) k++
            val f = (t - track.times[k]) / (track.times[k + 1] - track.times[k])
            PrototypeQuaternion.interpolate(track.rotations, k * 4, (k + 1) * 4, f, nodes[track.node].rotation)
        }
    }
}

/** SLERP behaelt die Rotationslaenge auch zwischen Animationsschluesseln. */
internal object PrototypeQuaternion {
    fun interpolate(q: FloatArray, a: Int, b: Int, f: Float, out: FloatArray) {
        var dot = (0..3).sumOf { (q[a + it] * q[b + it]).toDouble() }.toFloat()
        val sign = if (dot < 0) -1f else 1f
        dot = (dot * sign).coerceIn(-1f, 1f)
        val angle = acos(dot)
        val w1: Float
        val w2: Float
        if (dot > .9995f) { w1 = 1 - f; w2 = f }
        else { w1 = sin((1 - f) * angle) / sin(angle); w2 = sin(f * angle) / sin(angle) }
        var length = 0f
        for (i in 0..3) { out[i] = w1 * q[a + i] + sign * w2 * q[b + i]; length += out[i] * out[i] }
        val inv = 1 / sqrt(length)
        for (i in 0..3) out[i] *= inv
    }
}
