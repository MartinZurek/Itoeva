package com.notime.glyphsim.ui

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.notime.glyphsim.matrix.AvatarSpecies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein [ContextWrapper] rein im Speicher, ohne Robolectric oder ein Geraet - fuer die einzige
 * Stelle in dieser Datei, die tatsaechlich eine Ablage braucht ([lastToldPiece]). Ueberschrieben
 * wird ausschliesslich [getSharedPreferences]; alles andere an `ContextWrapper` bliebe unbenutzt
 * und wuerde als ungemockte Android-Methode ohnehin scheitern.
 */
private class InMemoryPrefsContext : ContextWrapper(null) {
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
 * Prueft den Aufbau der Geschichten (siehe [PlayLore]) - nicht die Texte selbst, sondern das, was
 * an ihnen strukturell stimmen muss.
 *
 * **Warum das ueberhaupt geprueft gehoert.** Eine vergessene Zeile in einer Liste aus zweiundvierzig
 * Textverweisen faellt niemandem auf: Das Wesen erzaehlt dann eben ein Stueck weniger, und wer
 * soll wissen, dass da eines fehlte? Genau solche Luecken schleichen sich beim Ergaenzen ein.
 */
class PlayLoreTest {

    @Test
    fun `jedes Wesen hat gleich viel zu erzaehlen`() {
        // Gleich viel, damit keines vorzeitig verstummt - wer sich fuer eine Kreatur entscheidet,
        // soll nicht die kuerzere Geschichte erwischt haben.
        for (species in AvatarSpecies.entries) {
            assertEquals(
                "$species hat nicht ${PlayLore.PIECES} Stuecke",
                PlayLore.PIECES,
                PlayLore.story(species).size
            )
        }
    }

    @Test
    fun `kein Stueck kommt zweimal vor`() {
        // Zwei gleiche Verweise waeren ein Tippfehler, der sich als Wiederholung tarnt: Das Wesen
        // erzaehlt dieselbe Sache zweimal, und die andere gar nicht.
        val all = AvatarSpecies.entries.flatMap { PlayLore.story(it) }
        assertEquals(
            "Ein Text wird von zwei Stellen verwendet",
            all.size,
            all.toSet().size
        )
        assertTrue("Ein Stueck verweist ins Leere", all.none { it == 0 })
    }

    // ---- Der Kalender ----

    private val day = java.time.LocalDate.of(2026, 3, 2)

    @Test
    fun `das erste Stueck ist sofort da`() {
        // Auf den Anfang wartet niemand. Wer die App installiert und fragt, bekommt eine Antwort -
        // "komm morgen wieder" als allererstes waere die schlechteste Begruessung, die es gibt.
        assertEquals(1, PlayLore.unlockedBy(firstDay = null, today = day))
        assertEquals(1, PlayLore.unlockedBy(firstDay = day, today = day))
    }

    @Test
    fun `je Kalendertag kommt eines dazu`() {
        assertEquals(2, PlayLore.unlockedBy(day, day.plusDays(1)))
        assertEquals(3, PlayLore.unlockedBy(day, day.plusDays(2)))
    }

    @Test
    fun `wer tagelang nicht fragt, bekommt alles Angesammelte`() {
        // **Der Unterschied zwischen einem Rhythmus und einem Belohnungsplan.** Nichts verfaellt,
        // nichts muss taeglich abgeholt werden: Wer eine Woche weg war, hat eine Woche gut.
        assertEquals(5, PlayLore.unlockedBy(day, day.plusDays(4)))
        assertEquals(
            "Nach einer Woche muss die Geschichte vollstaendig sein",
            PlayLore.PIECES,
            PlayLore.unlockedBy(day, day.plusDays(PlayLore.PIECES.toLong()))
        )
    }

    @Test
    fun `mehr als die Geschichte gibt es nicht`() {
        assertEquals(PlayLore.PIECES, PlayLore.unlockedBy(day, day.plusDays(400)))
    }

    @Test
    fun `ein zurueckgestelltes Geraet nimmt nichts weg`() {
        // Wer die Uhr zurueckstellt (oder ueber eine Zeitzone reist), soll nicht ploetzlich
        // weniger wissen als gestern. Freigeschaltet bleibt freigeschaltet.
        assertEquals(1, PlayLore.unlockedBy(day, day.minusDays(3)))
    }

    @Test
    fun `jedes Wesen hat eine eigene Geschichte`() {
        // Der Zweck des Ganzen: Die sechs sollen sich nicht bloss anders bewegen, sondern anders
        // KLINGEN. Geteilte Texte waeren der schnellste Weg, das wieder einzuebnen.
        val perSpecies = AvatarSpecies.entries.associateWith { PlayLore.story(it).toSet() }
        for (a in AvatarSpecies.entries) {
            for (b in AvatarSpecies.entries) {
                if (a.ordinal >= b.ordinal) continue
                val shared = perSpecies.getValue(a).count { it in perSpecies.getValue(b) }
                assertEquals("$a und $b teilen sich Text", 0, shared)
            }
        }
    }

    // ---- Das zuletzt erzaehlte Stueck ----

    @Test
    fun `vor dem ersten Erzaehlen gibt es kein zuletzt erzaehltes Stueck`() {
        val context = InMemoryPrefsContext()
        assertNull(PlayLore.lastToldPiece(context, AvatarSpecies.PUFFLING))
    }

    @Test
    fun `nach dem Erzaehlen liefert lastToldPiece genau das zuletzt gehoerte Stueck`() {
        val context = InMemoryPrefsContext()
        val species = AvatarSpecies.PUFFLING

        PlayLore.remember(context, species, today = day)

        assertEquals(
            PlayLore.story(species)[PlayLore.heard(context, species) - 1],
            PlayLore.lastToldPiece(context, species)
        )
    }
}
