package com.notime.glyphsim.prototype

import org.junit.Assert.*
import org.junit.Test

class PrototypeQuaternionTest {
    @Test fun antipodalRotationsFollowShortPath() {
        val out = FloatArray(4)
        PrototypeQuaternion.interpolate(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f), 0, 4, .5f, out)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), out, .0001f)
    }
    @Test fun quarterTurnInterpolationRemainsNormalized() {
        val q = floatArrayOf(0f, 0f, 0f, 1f, 0f, .70710677f, 0f, .70710677f)
        val out = FloatArray(4)
        PrototypeQuaternion.interpolate(q, 0, 4, .5f, out)
        assertEquals(1.0, out.sumOf { (it * it).toDouble() }, .00001)
        assertEquals(.38268343f, out[1], .0001f)
    }
    @Test fun intervalEndpointsArePreserved() {
        val q = floatArrayOf(0f, 0f, 0f, 1f, .70710677f, 0f, 0f, .70710677f)
        val out = FloatArray(4)
        PrototypeQuaternion.interpolate(q, 0, 4, 0f, out)
        assertArrayEquals(q.copyOfRange(0, 4), out, .0001f)
        PrototypeQuaternion.interpolate(q, 0, 4, 1f, out)
        assertArrayEquals(q.copyOfRange(4, 8), out, .0001f)
    }
}
