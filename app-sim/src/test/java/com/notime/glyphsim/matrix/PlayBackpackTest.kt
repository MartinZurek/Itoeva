package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayBackpack.Backpack
import com.notime.glyphsim.matrix.PlayEffects.Carried
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayBackpackTest {

    @Test
    fun `hinein, heraus, und nicht mehr als hineinpasst`() {
        var rucksack = Backpack()
        repeat(PlayBackpack.CAPACITY + 3) { rucksack = PlayBackpack.add(rucksack, Carried.FOOD) }
        assertEquals(PlayBackpack.CAPACITY, rucksack.items.size)
        assertTrue(rucksack.isFull)
        val zwei = PlayBackpack.add(PlayBackpack.add(Backpack(), Carried.BOOK), Carried.WOOD)
        assertEquals(listOf(Carried.WOOD), PlayBackpack.removeAt(zwei, 0).items)
        assertEquals(zwei, PlayBackpack.removeAt(zwei, 7))
    }

    @Test
    fun `der Rucksack ueberlebt das Speichern`() {
        val rucksack = Backpack(listOf(Carried.BOOK, Carried.SEEDS, Carried.FOOD))
        assertEquals(rucksack, PlayBackpack.decode(PlayBackpack.encode(rucksack)))
        assertEquals(Backpack(), PlayBackpack.decode(null))
        assertEquals(Backpack(listOf(Carried.CUP)), PlayBackpack.decode("Unsinn,CUP"))
    }

    @Test
    fun `jedes Ding hat ein Bild, und Fundstuecke gibt es an Plaetzen`() {
        for (item in Carried.entries) {
            val bild = PlayBackpack.iconCells(item)
            assertTrue("$item ohne Bild", bild.isNotEmpty())
            assertEquals(0, bild.minOf { it.x })
            assertEquals(0, bild.minOf { it.y })
        }
        assertEquals(Carried.FOOD, PlayBackpack.lootAt(PlayScene.Station.FRIDGE))
        assertEquals(null, PlayBackpack.lootAt(PlayScene.Station.BED))
    }
}
