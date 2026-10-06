package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.matrix.GameAdventure.State
import com.notime.glyphsim.matrix.GameAdventure.ObjectId
import com.notime.glyphsim.matrix.GameAdventure.Event
import com.notime.glyphsim.matrix.GameAdventure.Outcome
import com.notime.glyphsim.matrix.PlayEffects.Carried
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test

class GameAdventureTest {
    @Test fun `Fund Verwendung und Neustart behalten genau dieselbe Welt`() {
        val found = GameAdventure.act(State(place = Place.FOREST), ObjectId.SEEDS).state
        val planted = GameAdventure.act(found.copy(place = Place.PARK), ObjectId.GARDEN).state
        assertTrue(planted.backpack.items.isEmpty())
        val restored = GameAdventure.decode(GameAdventure.encode(planted))
        assertEquals(planted, restored)
        assertEquals(Outcome.ALREADY, GameAdventure.act(restored, ObjectId.GARDEN).outcome)
        assertEquals(Outcome.ALREADY, GameAdventure.act(restored.copy(place = Place.FOREST), ObjectId.SEEDS).outcome)
    }
    @Test fun `voller Rucksack verliert den Fund nicht`() {
        val full = State(place = Place.FOREST, backpack = PlayBackpack.Backpack(List(8) { Carried.BOOK }))
        assertEquals(full, GameAdventure.act(full, ObjectId.SEEDS).state)
        assertEquals(Outcome.FULL, GameAdventure.act(full, ObjectId.SEEDS).outcome)
        assertEquals(Outcome.CHANGED, GameAdventure.act(full.copy(backpack = PlayBackpack.Backpack()), ObjectId.SEEDS).outcome)
    }
    @Test fun `fehlendes Material veraendert weder Welt noch Inventar`() {
        val state = State(place = Place.PARK)
        assertEquals(Outcome.NEED_SEEDS, GameAdventure.act(state, ObjectId.GARDEN).outcome)
        assertEquals(state, GameAdventure.act(state, ObjectId.GARDEN).state)
        assertEquals(Outcome.NEED_WOOD, GameAdventure.act(state, ObjectId.BENCH).outcome)
    }
    @Test fun `Holzentscheidung verbraucht eine Ressource fuer eine sichtbare Folge`() {
        val found = GameAdventure.act(State(place = Place.FOREST), ObjectId.WOOD).state
        val park = GameAdventure.act(found.copy(place = Place.PARK), ObjectId.BENCH).state
        assertTrue(Event.BENCH_REPAIRED in park.events)
        assertEquals(Outcome.NEED_WOOD, GameAdventure.act(park.copy(place = Place.CAMP), ObjectId.CAMP).outcome)
        assertEquals(park, GameAdventure.act(park, ObjectId.BENCH).state)
    }
    @Test fun `Stationsfunde werden nicht dupliziert und sind pro Ort getrennt`() {
        val state = State(place = Place.CRAFT)
        val found = GameAdventure.collect(state, PlayScene.Station.CRAFT)
        assertEquals(Outcome.CHANGED, found.outcome)
        assertEquals(Outcome.ALREADY, GameAdventure.collect(found.state, PlayScene.Station.CRAFT).outcome)
        assertEquals(1, found.state.backpack.items.size)
    }
    @Test fun `Version Position und duplizierte Gegenstaende werden verlustfrei gelesen`() {
        val source = State(backpack = PlayBackpack.Backpack(listOf(Carried.WOOD, Carried.WOOD, Carried.SEEDS)),
            events = listOf(Event.OLD_MARKER), met = setOf(LivingResidents.all.first().profileId), elapsed = 123456,
            pos = PlayControl.Pos(.32f, .71f), facing = PlayControl.Dir.LEFT, lampOn = false, tvOn = true)
        assertEquals(source, GameAdventure.decode(GameAdventure.encode(source)))
    }
    @Test fun `fremde oder beschaedigte Versionen werden abgewiesen`() {
        for (text in listOf("", "ITOEVA2:99", GameAdventure.encode(State()).replace("0.5,0.65", "NaN,0.65"))) {
            try { GameAdventure.decode(text); fail(text) } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun `alle Weltanker und gespeicherte Positionen sind frei von Moebelkoerpern`() {
        for (id in ObjectId.entries) {
            val scene = GameScenes.of(id.place)!!
            val pos = GameAdventure.safePosition(id.place, id.pos)
            assertTrue(GameSurfaces.painted(scene).none { it.contains(pos) && it.heightAt(pos) > 1 })
        }
        for (place in GameScenes.painted) {
            val scene = GameScenes.of(place)!!
            for (surface in GameSurfaces.painted(scene)) {
                val pos = GameAdventure.safePosition(place, PlayControl.Pos((surface.x0 + surface.x1) / 2, (surface.d0 + surface.d1) / 2))
                assertTrue(GameSurfaces.painted(scene).none { it.contains(pos) && it.heightAt(pos) > 1 })
            }
        }
    }
    @Test fun `Menue und Abwesenheit treiben die Spielzeit nicht voran`() {
        val state = State(elapsed = 5000)
        assertEquals(state, GameAdventure.tick(state, 60_000, false))
        assertEquals(5050L, GameAdventure.tick(state, 50, true).elapsed)
        assertEquals(482, GameAdventure.tick(state, 50, true).absoluteMinute)
    }
    @Test fun `Fund am falschen Ort hat keine Wirkung`() {
        val state = State(place = Place.PARK)
        assertEquals(state, GameAdventure.act(state, ObjectId.SEEDS).state)
    }
    @Test fun `Chronik und Bekanntschaften wachsen nur aus gueltigen Ereignissen`() {
        val state = State(place = Place.FOREST)
        val marker = GameAdventure.act(state, ObjectId.MARKER).state
        assertTrue(Event.OLD_MARKER in marker.events)
        assertEquals(marker, GameAdventure.meet(marker, "unknown"))
        val id = LivingResidents.all.first().profileId
        assertEquals(1, GameAdventure.meet(GameAdventure.meet(marker, id), id).met.size)
    }
    @Test fun `voller Legacy Rucksack kann reversibel Platz fuer Pflanzen schaffen`() {
        val full = State(place = Place.FOREST, backpack = PlayBackpack.decode(List(8) { "BOOK" }.joinToString(",")))
        val cleared = GameAdventure.stow(full, 0)
        val found = GameAdventure.act(cleared, ObjectId.SEEDS).state
        val planted = GameAdventure.act(found.copy(place = Place.PARK), ObjectId.GARDEN).state
        val restored = GameAdventure.decode(GameAdventure.encode(planted))
        assertTrue(Event.PLANTED in restored.events)
        assertEquals(listOf(Carried.BOOK), restored.stowed)
        val retrieved = GameAdventure.retrieve(restored, 0).state
        assertEquals(List(8) { Carried.BOOK }, retrieved.backpack.items)
        assertTrue(retrieved.stowed.isEmpty())
    }
    @Test fun `Chronik bewahrt die tatsaechliche Entdeckungsreihenfolge`() {
        val marker = GameAdventure.act(State(place = Place.FOREST), ObjectId.MARKER).state
        val seeds = GameAdventure.act(marker, ObjectId.SEEDS).state
        assertEquals(listOf(Event.OLD_MARKER, Event.FOREST_SEEDS), GameAdventure.decode(GameAdventure.encode(seeds)).events)
    }

    private fun resident(index: Int) = LivingResidents.all[index].let { profile ->
        ResidentSnapshot(profileId = profile.profileId, role = profile.role, species = profile.species,
            place = Place.PARK, site = LivingSite.OUTSIDE, goal = null, nextAction = ActionKind.READ,
            blockedBy = null, coins = 0, portions = 1, minuteOfDay = 480, publiclyPresent = true,
            nextSpecialActivity = null, currentAction = ActionKind.READ)
    }
    @Test fun `zwei lesende Bewohner erreichen verschiedene freie Bankplaetze`() {
        val scene = GameScenes.of(Place.PARK)!!
        val snapshots = listOf(resident(0), resident(1))
        var actors = emptyMap<String, GameResidents.Actor>()
        repeat(2000) { actors = GameResidents.tick(scene, actors, snapshots, 50, 0, null) }
        assertEquals(2, actors.size)
        assertEquals(2, actors.values.map { it.pos }.distinct().size)
        assertTrue(actors.values.none { it.moving })
        for (actor in actors.values) assertTrue(GameSurfaces.painted(scene).none { it.contains(actor.pos) && it.heightAt(actor.pos) > 1f })
    }
    @Test fun `ruhender Bewohner behaelt die Blickrichtung beim Gespraech`() {
        val snapshot = resident(0)
        val scene = GameScenes.of(Place.PARK)!!
        val old = GameResidents.Actor(snapshot, GameAdventure.safePosition(Place.PARK, PlayControl.Pos(.5f, .6f)), facing = PlayControl.Dir.RIGHT)
        val actors = GameResidents.tick(scene, mapOf(snapshot.profileId to old), listOf(snapshot), 50, 0, snapshot.profileId)
        assertEquals(PlayControl.Dir.RIGHT, actors.getValue(snapshot.profileId).facing)
    }
    @Test fun `alle Objektanker sind ueber die vorhandene Wegsuche erreichbar`() {
        for (id in ObjectId.entries) {
            val scene = GameScenes.of(id.place)!!
            val target = GameAdventure.position(scene, id)
            for (start in listOf(PlayControl.Pos(.05f, .5f), PlayControl.Pos(.95f, .5f))) {
                var pos = GameAdventure.safePosition(id.place, start)
                repeat(1500) { pos = GameSurfaces.approach(scene, pos, target, 50) }
                assertEquals("$id from $start", target, pos)
            }
        }
    }

    @Test fun `aufgenommene Funde verschwinden aber bleibende Objekte bleiben sichtbar`() {
        val found = GameAdventure.act(State(place = Place.FOREST), ObjectId.SEEDS).state
        assertFalse(GameAdventure.visible(found, ObjectId.SEEDS))
        assertTrue(GameAdventure.visible(found, ObjectId.MARKER))
        assertTrue(GameAdventure.visible(found, ObjectId.GARDEN))
    }
    @Test fun `Legacy Tischposition wird beim Wiedereinstieg auf freien Boden korrigiert`() {
        val surfaces = GameSurfaces.tables(Place.KITCHEN, AvatarSpecies.FENNEC, 40, 22, 1000f, 140f, 25f, 90f)
        for (table in surfaces) {
            val requested = PlayControl.Pos((table.x0 + table.x1) / 2, (table.d0 + table.d1) / 2)
            val safe = GameAdventure.safePosition(Place.KITCHEN, requested, surfaces)
            assertTrue(surfaces.none { it.contains(safe) && it.heightAt(safe) > 1 })
        }
        assertTrue(surfaces.isNotEmpty())
    }

}
