package android.os

/** Attrappe fuer die Offline-Uebersetzung. */
object SystemClock {
    @JvmStatic fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000
    @JvmStatic fun uptimeMillis(): Long = System.nanoTime() / 1_000_000
}
