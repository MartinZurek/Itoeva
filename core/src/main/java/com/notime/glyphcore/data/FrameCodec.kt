package com.notime.glyphcore.data

/**
 * Kodiert/dekodiert Animations-Frames (je Frame eine Liste (x,y)-Punkte) als simpler
 * Text statt mit einer JSON-Bibliothek - die Werte sind ausschliesslich kleine
 * nichtnegative Ganzzahlen (Rasterkoordinaten 0..12), ein eigenes Mini-Format spart
 * sich damit eine zusaetzliche Abhaengigkeit. Format: Frames getrennt durch "|",
 * Punkte innerhalb eines Frames getrennt durch ";", x/y getrennt durch ",".
 * Beispiel: "6,1;7,2|6,2;7,3"
 */
object FrameCodec {
    fun encode(frames: List<List<Pair<Int, Int>>>): String =
        frames.joinToString("|") { frame -> frame.joinToString(";") { (x, y) -> "$x,$y" } }

    /**
     * Toleriert fehlerhafte Punkte, statt daran zu scheitern.
     *
     * Vorher zerlegte ein `val (x, y) = point.split(",")` jeden Punkt hart: fehlte das Komma
     * oder stand dort keine Zahl, flog eine [IndexOutOfBoundsException] bzw.
     * [NumberFormatException]. Aufgerufen wird das aus dem Alarm-Empfaenger heraus - eine
     * einzige unsaubere Zeile in der Datenbank haette also die App zum Absturz gebracht, und
     * zwar genau dann, wenn die Erinnerung faellig war. Unlesbare Punkte werden jetzt
     * uebersprungen; im schlimmsten Fall fehlt ein Bildpunkt, die Erinnerung erscheint aber.
     */
    fun decode(raw: String): List<List<Pair<Int, Int>>> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { frame ->
            if (frame.isBlank()) {
                emptyList()
            } else {
                frame.split(";").mapNotNull { point ->
                    val parts = point.split(",")
                    if (parts.size != 2) return@mapNotNull null
                    val x = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                    val y = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
                    x to y
                }
            }
        }
    }
}
