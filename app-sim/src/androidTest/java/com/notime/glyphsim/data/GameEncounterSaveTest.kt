package com.notime.glyphsim.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.matrix.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Die reale Preference-Grenze bewahrt Besitz und beide Erinnerungen auch beim Wiederladen. */
@RunWith(AndroidJUnit4::class)
class GameEncounterSaveTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.getSharedPreferences("itoeva2", Context.MODE_PRIVATE)
    @Before fun clearBefore() { assertTrue(prefs.edit().clear().commit()) }
    @After fun clearAfter() { assertTrue(prefs.edit().clear().commit()) }

    @Test fun transferSurvivesNewStoreWithBothMemories() {
        val id = LivingResidents.all.first { it.anchorPlace == PlayScene.Place.PARK }.profileId
        val resident = LivingPopulation.initial(480).getValue(id)
        val actor = GameResidents.Actor(LivingPopulation.snapshot(mapOf(id to resident)).single()
            .copy(place = PlayScene.Place.PARK, publiclyPresent = true), PlayControl.Pos(.5f, .72f))
        val before = GameAdventure.State(place = PlayScene.Place.PARK, pos = PlayControl.Pos(.45f, .72f),
            backpack = PlayBackpack.Backpack(listOf(PlayEffects.Carried.BOOK)))
        val result = GameEncounters.give(before, AvatarSpecies.FENNEC, resident, actor, 0)
        assertEquals(GameEncounters.Outcome.CHANGED, result.outcome)
        assertTrue(GameSaveStore(context).save(result.state))
        val restored = GameSaveStore(context).load()
        assertEquals(result.state, restored)
        assertTrue(restored.backpack.items.isEmpty())
        assertEquals(listOf(PlayEffects.Carried.BOOK), GameEncounters.received(restored, id))
        assertTrue(GameEncounters.knows(restored, AvatarSpecies.FENNEC, id))
        assertTrue(GameEncounters.restore(restored, resident).agent.relationships.containsKey(GameEncounters.host(AvatarSpecies.FENNEC)))
    }

    @Test fun failedCommitLeavesOriginalSnapshotAndLegacyBackpackTogether() {
        val before = GameAdventure.State(backpack = PlayBackpack.Backpack(listOf(PlayEffects.Carried.WOOD)))
        assertTrue(GameSaveStore(context).save(before))
        val encoded = prefs.getString("world_snapshot", null)
        val legacy = prefs.getString("backpack", null)
        val failing = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val real = super.getSharedPreferences(name, mode)
                return object : SharedPreferences by real {
                    override fun edit(): SharedPreferences.Editor {
                        val editor = real.edit()
                        return object : SharedPreferences.Editor by editor {
                            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                                editor.putString(key, value)
                                return this
                            }
                            override fun commit() = false
                        }
                    }
                }
            }
        }
        assertFalse(GameSaveStore(failing).save(before.copy(backpack = PlayBackpack.Backpack())))
        assertEquals(encoded, prefs.getString("world_snapshot", null))
        assertEquals(legacy, prefs.getString("backpack", null))
        assertEquals(before, GameSaveStore(context).load())
    }

    @Test fun oldKnowledgeMigratesAndFutureSaveIsNeverOverwritten() {
        val id = LivingResidents.all.first().profileId
        val before = GameAdventure.State(met = setOf(id), stowed = listOf(PlayEffects.Carried.SEEDS))
        val v1 = GameAdventure.encode(before).split('\n').take(11).joinToString("\n").replace("ITOEVA2:2", "ITOEVA2:1")
        assertTrue(prefs.edit().putString("world_snapshot", v1).commit())
        val loaded = GameSaveStore(context).load()
        assertEquals(before, loaded.copy(social = emptyMap()))
        assertTrue(GameEncounters.knows(loaded, AvatarSpecies.FENNEC, id))
        val future = GameAdventure.encode(loaded).replace("ITOEVA2:2", "ITOEVA2:3")
        assertTrue(prefs.edit().putString("world_snapshot", future).commit())
        val protected = GameSaveStore(context)
        protected.load()
        assertNotNull(protected.error)
        assertFalse(protected.save(GameAdventure.State()))
        assertEquals(future, prefs.getString("world_snapshot", null))
    }
}
