package com.notime.glyphsim.prototype

import org.junit.Assert.*
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class FennecModelTest {
    // Debug-Tests benutzen dieselbe generierte Datei im Test-APK; der normale App-Bau
    // verpackt das Modell ausschliesslich in der Game-Variante.
    private fun bytes(): ByteArray = InstrumentationRegistry.getInstrumentation().context
        .assets.open("models/fennec-prototype.glb").use { it.readBytes() }
    @Test fun compactAssetHasThreeCompleteClips() {
        val data = bytes(); assertTrue(data.size < 2_000_000)
        val m = FennecModel(data)
        assertEquals(listOf("idle", "walk", "run"), m.clips.map { it.name })
        assertTrue(m.nodes.any { it.name == "head" && it.parts.isNotEmpty() })
        assertTrue(m.nodes.any { it.name == "tail" && it.parts.isNotEmpty() })
        m.clips.forEach { clip ->
            assertTrue(clip.duration > 0)
            clip.tracks.forEach { track ->
                assertEquals(track.times.size * 4, track.rotations.size)
                assertEquals(0f, track.times.first(), 0f)
                assertTrue(track.times.asList().zipWithNext().all { (a, b) -> b > a })
                assertTrue(track.node in m.nodes.indices)
            }
        }
    }
    @Test fun paintedTextureHasCoherentUvCoordinates() {
        val m = FennecModel(bytes())
        assertTrue(m.textureBytes.size in 100_000..1_000_000)
        assertEquals(255, m.textureBytes[0].toInt() and 255)
        assertEquals(216, m.textureBytes[1].toInt() and 255)
        val textured = m.nodes.flatMap { it.parts }.filter { it.textured }
        assertTrue(textured.size > 10)
        textured.forEach { p ->
            val uv = requireNotNull(p.texcoords)
            assertEquals(p.positions.capacity() / 3 * 2, uv.capacity())
            repeat(uv.capacity()) { assertTrue(uv.get(it).isFinite() && uv.get(it) in 0f..1f) }
            for (axis in 0..1) {
                val values = (0 until uv.capacity() / 2).map { uv.get(it * 2 + axis) }
                assertTrue(values.max() - values.min() < .5f)
            }
        }
    }
    @Test fun trianglesHaveDepthFiniteNormalsAndValidIndices() {
        val m = FennecModel(bytes()); var triangles = 0
        m.nodes.flatMap { it.parts }.forEach { p ->
            assertEquals(p.positions.capacity(), p.normals.capacity())
            val vertices = p.positions.capacity() / 3
            repeat(p.indices.capacity()) { assertTrue((p.indices.get(it).toInt() and 65535) < vertices) }
            repeat(p.positions.capacity()) { assertTrue(p.positions.get(it).isFinite()) }
            repeat(vertices) { i ->
                val length = sqrt((0..2).sumOf { val n = p.normals.get(i * 3 + it); (n * n).toDouble() })
                assertEquals(1.0, length, .015)
            }
            triangles += p.count / 3
        }
        assertTrue(triangles in 10_000..50_000)
        val head = m.nodes.first { it.name == "head" }.parts.flatMap { p ->
            (0 until p.positions.capacity() / 3).map { p.positions.get(it * 3 + 2) }
        }
        assertTrue(head.max() - head.min() > .5f)
    }
    @Test fun hierarchyVisitsEveryNodeExactlyOnce() {
        val m = FennecModel(bytes()); val seen = mutableSetOf<Int>()
        fun visit(i: Int) { assertTrue(seen.add(i)); m.nodes[i].children.forEach { visit(it) } }
        m.roots.forEach { visit(it) }; assertEquals(m.nodes.size, seen.size)
    }
    @Test fun walkMovesLegsInOppositeDirectionsWithoutChangingBoneLength() {
        val m = FennecModel(bytes()); val translations = m.nodes.map { it.translation.copyOf() }
        m.pose(1, m.clips[1].duration / 4)
        val left = m.nodes.first { it.name == "hip-1" }.rotation
        val right = m.nodes.first { it.name == "hip1" }.rotation
        assertTrue(abs(left[0]) > .15f); assertEquals(-left[0], right[0], .001f)
        m.nodes.forEachIndexed { i, node -> assertArrayEquals(translations[i], node.translation, 0f) }
    }
    @Test fun everyAnimationKeepsNormalizedRotationsAndLoops() {
        val m = FennecModel(bytes())
        m.clips.forEachIndexed { i, clip ->
            repeat(101) { step ->
                m.pose(i, step * clip.duration / 100)
                m.nodes.forEach { n -> assertEquals(1.0, n.rotation.sumOf { (it * it).toDouble() }, .001) }
            }
            m.pose(i, .123f); val before = m.nodes.map { it.rotation.copyOf() }
            m.pose(i, .123f + clip.duration)
            m.nodes.forEachIndexed { n, node -> assertArrayEquals(before[n], node.rotation, .0001f) }
        }
    }
    @Test fun quaternionInterpolationUsesShortPath() {
        val q = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f); val out = FloatArray(4)
        PrototypeQuaternion.interpolate(q, 0, 4, .5f, out)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), out, .00001f)
    }
    @Test fun quaternionMatrixPreservesLengths() {
        val matrix = FloatArray(16)
        FennecRenderer.quaternionMatrix(floatArrayOf(0f, .70710677f, 0f, .70710677f), matrix)
        assertEquals(1f, matrix[8], .0001f)
        assertEquals(-1f, matrix[2], .0001f)
        assertEquals(1.0, (0..2).sumOf { (matrix[it] * matrix[it]).toDouble() }, .0001)
    }
    @Test fun brokenHeaderIsRejected() {
        val data = bytes(); data[0] = 0
        assertThrows(IllegalArgumentException::class.java) { FennecModel(data) }
    }
}
