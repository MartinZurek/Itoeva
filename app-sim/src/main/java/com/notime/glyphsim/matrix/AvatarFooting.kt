package com.notime.glyphsim.matrix

/**
 * **Wo eine Figur stehen muss, damit sie auf dem Boden steht.**
 *
 * Klingt banal und war die Ursache eines Fehlers, den man erst sieht, wenn man ihn ausloest: Die
 * Position des Avatars stand in Pixeln fest, einmal aus der damaligen Bodenhoehe gerechnet. Boden
 * und Zellgroesse haengen aber an der Uhrgroesse und an den Bildschirmmassen - zieht man die Uhr
 * groesser oder dreht das Geraet, wandert der Boden, und die Figur bleibt in der Luft haengen.
 *
 * **Warum das hier als eigene Regel steht.** Die Rechnung wurde an drei Stellen gebraucht - beim
 * Hinstellen, beim Hinsetzen auf eine Requisite und beim Umstellen nach einer Groessenaenderung -
 * und an jeder von ihnen von Hand wiederholt. Drei Kopien einer Formel, die zusammenpassen
 * MUESSEN, sind eine Verabredung ohne Vertrag: Faellt eine aus dem Takt, steht die Figur je nach
 * Weg unterschiedlich hoch, und niemand sieht der einzelnen Zeile an, dass sie zu den anderen
 * gehoert.
 *
 * Ohne Compose-Typen, damit sich die Regel ohne Geraet und ohne Oberflaeche pruefen laesst - und
 * genau das ist der Punkt: Ein Fehler, der nur beim Drehen eines echten Telefons auftritt, wird
 * sonst nur zufaellig gefunden.
 */
object AvatarFooting {

    /**
     * Der obere Rand der Figur, damit ihre Fussreihe auf [floorPx] aufsetzt.
     *
     * [groundRow] ist die unterste Zeile ihres Koerpers (siehe [AvatarBodies.groundRow]) - je nach
     * Kreatur verschieden, weil ein Schleim tiefer sitzt als eine Eule auf zwei Beinen.
     */
    fun topFor(floorPx: Float, avatarPx: Float, groundRow: Int): Float {
        val cell = avatarPx / AvatarGeometry.SIZE
        return (floorPx - (groundRow + 1) * cell).coerceAtLeast(0f)
    }

    /** Der linke Rand fuer eine Verankerung als Bruchteil der freien Breite. */
    fun leftFor(fraction: Float, avatarPx: Float, widthPx: Float): Float {
        val bound = (widthPx - avatarPx).coerceAtLeast(0f)
        return bound * fraction.coerceIn(0f, 1f)
    }

    /** Der linke Rand, damit die Figur mittig ueber [centerCellX] steht. */
    fun leftForCenter(centerCellX: Int, avatarPx: Float, widthPx: Float): Float {
        val cell = avatarPx / AvatarGeometry.SIZE
        return (centerCellX * cell - avatarPx / 2f)
            .coerceIn(0f, (widthPx - avatarPx).coerceAtLeast(0f))
    }

    /**
     * Aus welchem Bruchteil der Breite eine bestehende Position stammt.
     *
     * Gebraucht beim Drehen: Die alte Pixelposition muss mit den ALTEN Massen zurueckgerechnet
     * werden, sonst springt die Figur quer durchs Bild - mit den neuen gerechnet bedeutet
     * derselbe Pixelwert einen ganz anderen Ort.
     */
    fun fractionOf(leftPx: Float, avatarPx: Float, widthPx: Float): Float {
        val bound = (widthPx - avatarPx).coerceAtLeast(1f)
        return (leftPx / bound).coerceIn(0f, 1f)
    }
}
