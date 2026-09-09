package com.notime.glyphsim.ui

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.notime.glyphsim.matrix.AvatarSpecies
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein [ContextWrapper] rein im Speicher, ohne Robolectric oder ein Geraet.
 *
 * **Eigener Name mit Absicht.** Im selben Paket steht in `PlayLoreTest` bereits eine Klasse
 * gleichen Zwecks; zwei Top-Level-Klassen gleichen Namens kollidieren in Kotlin unabhaengig von
 * ihrer Sichtbarkeit, und `private` schuetzt davor nicht. Die vorhandene zu teilen haette
 * bedeutet, sie in einer fremden Testdatei oeffentlich zu machen - fuer zwanzig Zeilen der
 * schlechtere Tausch.
 */
private class CharacterThemePrefsContext : ContextWrapper(null) {
    private val values = mutableMapOf<String, Any>()

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        object : SharedPreferences {
            override fun getAll(): MutableMap<String, *> = values
            override fun getString(key: String?, defValue: String?) =
                values[key] as? String ?: defValue

            override fun getStringSet(key: String?, defValues: MutableSet<String>?) = defValues
            override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
            override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
            override fun getFloat(key: String?, defValue: Float) =
                values[key] as? Float ?: defValue

            override fun getBoolean(key: String?, defValue: Boolean) =
                values[key] as? Boolean ?: defValue

            override fun contains(key: String?) = values.containsKey(key)
            override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
                override fun putString(key: String?, value: String?) = apply {
                    if (value == null) values.remove(key) else values[key!!] = value
                }

                override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {}
                override fun putInt(key: String?, value: Int) = apply { values[key!!] = value }
                override fun putLong(key: String?, value: Long) = apply { values[key!!] = value }
                override fun putFloat(key: String?, value: Float) = apply { values[key!!] = value }
                override fun putBoolean(key: String?, value: Boolean) = apply {
                    values[key!!] = value
                }

                override fun remove(key: String?) = apply { values.remove(key) }
                override fun clear() = apply { values.clear() }
                override fun commit() = true
                override fun apply() {}
            }

            override fun registerOnSharedPreferenceChangeListener(
                listener: SharedPreferences.OnSharedPreferenceChangeListener?
            ) {
            }

            override fun unregisterOnSharedPreferenceChangeListener(
                listener: SharedPreferences.OnSharedPreferenceChangeListener?
            ) {
            }
        }
}

/**
 * Prueft die Ablage hinter dem Charakterstueck - dass der Anlass tatsaechlich einmal am Tag
 * eintritt und je Wesen getrennt gezaehlt wird.
 *
 * Die Regel selbst steht in `PlayCharacterThemeTest`; hier geht es um das, was zwischen Regel
 * und SharedPreferences schiefgehen kann: ein gemeinsamer Schluessel fuer alle sechs Wesen etwa
 * faellt in der reinen Rechnung nicht auf.
 */
class PlayCharacterThemeLogTest {

    private val heute = LocalDate.of(2026, 9, 9)

    @Test
    fun `beim ersten Mal ist die Begruessung faellig`() {
        val context = CharacterThemePrefsContext()
        assertTrue(PlayCharacterThemeLog.isDue(context, AvatarSpecies.PUFFLING, heute))
    }

    @Test
    fun `nach der Begruessung ist heute Schluss und morgen wieder`() {
        val context = CharacterThemePrefsContext()
        PlayCharacterThemeLog.greeted(context, AvatarSpecies.PUFFLING, heute)
        assertFalse(PlayCharacterThemeLog.isDue(context, AvatarSpecies.PUFFLING, heute))
        assertTrue(PlayCharacterThemeLog.isDue(context, AvatarSpecies.PUFFLING, heute.plusDays(1)))
    }

    /**
     * **Je Wesen und nicht je Tag.** Wer den Avatar wechselt, hoert am selben Tag auch das
     * zweite Thema - sonst haette das neue Wesen seinen eigenen Gruss an das vorige verloren.
     */
    @Test
    fun `jedes Wesen begruesst fuer sich`() {
        val context = CharacterThemePrefsContext()
        PlayCharacterThemeLog.greeted(context, AvatarSpecies.PUFFLING, heute)
        for (species in AvatarSpecies.entries - AvatarSpecies.PUFFLING) {
            assertTrue("$species", PlayCharacterThemeLog.isDue(context, species, heute))
        }
    }

    @Test
    fun `zweimal festhalten aendert nichts`() {
        val context = CharacterThemePrefsContext()
        PlayCharacterThemeLog.greeted(context, AvatarSpecies.GLOOP, heute)
        PlayCharacterThemeLog.greeted(context, AvatarSpecies.GLOOP, heute)
        assertFalse(PlayCharacterThemeLog.isDue(context, AvatarSpecies.GLOOP, heute))
    }
}
