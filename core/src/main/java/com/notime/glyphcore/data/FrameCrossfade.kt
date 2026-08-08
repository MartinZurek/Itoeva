package com.notime.glyphcore.data

/**
 * Verwandelt wenige Punkt-Posen ("Keyframes") in eine durchgaengige Sequenz mit weichen
 * Helligkeits-Ueberblendungen zwischen den Posen statt hartem Sprung - bis vor Kurzem nur
 * intern in matrix/ReminderAnimations.kt fuer die 12 fest eingebauten AnimationTypes genutzt
 * (siehe dortiger Kommentar zur Entwurfspruefung). Hierher extrahiert, weil die Technik rein
 * auf Punkt-Mengen arbeitet - keine Kotlin-spezifische Zeichenlogik noetig - und sich damit
 * 1:1 auf die per FrameCodec aus der Datenbank geladenen Bibliotheks-Animationen anwenden
 * laesst (siehe LibraryAnimationRepository.framesFor). Dadurch erreichen Datenbank-Animationen
 * dieselbe Ueberblend-Qualitaet wie die eingebauten, ohne dass framesData selbst Helligkeits-
 * werte speichern muesste - die Punktlisten bleiben reine An/Aus-Posen, das Ueberblenden
 * passiert erst beim Abspielen/in der Vorschau. 1:1 uebernommen aus dem :app-Modul.
 */
object FrameCrossfade {
    private const val MAX_BRIGHTNESS = 4095

    private fun toGrid(width: Int, height: Int, points: Set<Pair<Int, Int>>): IntArray {
        val grid = IntArray(width * height)
        for ((x, y) in points) {
            if (x in 0 until width && y in 0 until height) grid[y * width + x] = MAX_BRIGHTNESS
        }
        return grid
    }

    private fun crossfade(
        width: Int,
        height: Int,
        from: Set<Pair<Int, Int>>,
        to: Set<Pair<Int, Int>>,
        steps: Int
    ): List<IntArray> {
        if (steps <= 0) return emptyList()
        val onlyFrom = from - to
        val onlyTo = to - from
        val common = from intersect to
        return (1..steps).map { step ->
            val f = step.toFloat() / (steps + 1)
            val grid = IntArray(width * height)
            fun set(x: Int, y: Int, brightness: Int) {
                if (x in 0 until width && y in 0 until height) grid[y * width + x] = brightness
            }
            for ((x, y) in common) set(x, y, MAX_BRIGHTNESS)
            for ((x, y) in onlyFrom) set(x, y, (MAX_BRIGHTNESS * (1f - f)).toInt())
            for ((x, y) in onlyTo) set(x, y, (MAX_BRIGHTNESS * f).toInt())
            grid
        }
    }

    /**
     * Verbindet Posen zu einer Sequenz mit Ueberblend-Zwischenframes statt hartem Sprung.
     * [loop] = true haengt auch von der letzten zurueck zur ersten Pose eine Ueberblendung an
     * (fuer durchgaengig wiederholende Animationen); false haelt stattdessen einfach die
     * letzte Pose (fuer Sequenzen, die linear zu einem Ergebnis hinlaufen).
     */
    fun withCrossfades(
        size: Int,
        keyframes: List<List<Pair<Int, Int>>>,
        steps: Int = 2,
        loop: Boolean = true
    ): List<IntArray> = withCrossfades(size, size, keyframes, steps, loop)

    /**
     * Wie oben, aber fuer RECHTECKIGE Raster.
     *
     * Gebraucht seit der Avatar Kopffreiheit hat (siehe AvatarGeometry.HEADROOM): Sein Raster ist
     * hoeher als breit, damit ein Sprung die Ohren nicht mehr an der oberen Kante abschneidet.
     * Punkte ausserhalb des Rasters werden hier - wie zuvor - stillschweigend verworfen; genau
     * dieses Verwerfen war die Ursache, und deshalb muss das Raster gross genug sein.
     */
    fun withCrossfades(
        width: Int,
        height: Int,
        keyframes: List<List<Pair<Int, Int>>>,
        steps: Int = 2,
        loop: Boolean = true
    ): List<IntArray> {
        if (keyframes.isEmpty()) return emptyList()
        val sets = keyframes.map { it.toSet() }
        val frames = mutableListOf<IntArray>()
        val transitions = if (loop) sets.size else sets.size - 1
        for (i in 0 until transitions) {
            frames += toGrid(width, height, sets[i])
            frames += crossfade(width, height, sets[i], sets[(i + 1) % sets.size], steps)
        }
        if (!loop) frames += toGrid(width, height, sets.last())
        return frames
    }
}
