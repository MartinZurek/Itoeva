package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayEffects.Carried
import com.notime.glyphsim.matrix.PlayScene.Station

/**
 * **Der Rucksack von Itoeva 2** - wo die Figur Dinge aufbewahrt.
 *
 * Gewuenscht am 03.10.: "Es soll auch ein Inventar haben, wo er Sachen aufbewahren kann, quasi
 * als Rucksack." Hinein kommt, was man beim Handeln findet ([lootAt]): am Kuehlschrank etwas zu
 * essen, im Regal ein Buch, im Laden Samen ... Herausnehmen heisst: in die Hand nehmen (die
 * Figur traegt es dann sichtbar, siehe [PlayEffects.carriedCells]).
 *
 * Reine Rechnung; abgelegt wird in DockScreen (SharedPreferences, [encode]/[decode]).
 */
object PlayBackpack {

    /** So viele Dinge passen hinein. */
    const val CAPACITY = 8

    /** Mit Ablage-Reihenfolge; das zuletzt gefundene steht hinten. */
    data class Backpack(val items: List<Carried> = emptyList()) {
        val isFull: Boolean get() = items.size >= CAPACITY
    }

    /** Legt [item] hinein - oder laesst alles, wie es ist, wenn kein Platz mehr ist. */
    fun add(backpack: Backpack, item: Carried): Backpack =
        if (backpack.isFull) backpack else backpack.copy(items = backpack.items + item)

    /** Nimmt das Ding an [index] heraus. */
    fun removeAt(backpack: Backpack, index: Int): Backpack =
        if (index !in backpack.items.indices) backpack
        else backpack.copy(items = backpack.items.filterIndexed { i, _ -> i != index })

    /** Was man an diesem Platz findet, wenn man dort handelt - oder `null`. */
    fun lootAt(station: Station): Carried? = when (station) {
        Station.FRIDGE -> Carried.FOOD
        Station.TABLE -> Carried.CUP
        Station.BOOKSHELF -> Carried.BOOK
        Station.RACK -> Carried.SEEDS
        Station.CHECKOUT -> Carried.BASKET
        Station.CRAFT -> Carried.WOOD
        else -> null
    }

    /** Das Bild eines Dings fuer die Rucksack-Ansicht: dieselbe Zeichnung wie in der Hand, ab (0,0). */
    fun iconCells(item: Carried): List<SceneCell> {
        val cells = PlayEffects.carriedCells(item, 0, 0).filter { it.brightness > 0 }
        if (cells.isEmpty()) return cells
        val minX = cells.minOf { it.x }
        val minY = cells.minOf { it.y }
        return cells.map { it.copy(x = it.x - minX, y = it.y - minY) }
    }

    fun encode(backpack: Backpack): String = backpack.items.joinToString(",") { it.name }

    fun decode(text: String?): Backpack = Backpack(
        text.orEmpty().split(',').mapNotNull { name ->
            Carried.entries.firstOrNull { it.name == name }
        }.take(CAPACITY)
    )
}
