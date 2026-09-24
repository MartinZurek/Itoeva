package android.os

/** Attrappe fuer die Offline-Uebersetzung. */
object SystemClock {
    @JvmStatic fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000
    @JvmStatic fun uptimeMillis(): Long = System.nanoTime() / 1_000_000
}

/** Attrappe: Offline wird nichts verzoegert ausgefuehrt, nur uebersetzt. */
class Looper private constructor() {
    companion object {
        @JvmStatic fun getMainLooper(): Looper = Looper()
    }
}

class Handler(@Suppress("UNUSED_PARAMETER") looper: Looper) {
    fun postDelayed(r: Runnable, delayMillis: Long): Boolean = true
    fun removeCallbacks(r: Runnable) {}
}
