package com.notime.glyphsim.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Waehlbare Oberflaechensprachen. [ENGLISH] ist die Standardsprache der App. */
enum class AppLanguage(val tag: String, val label: String) {
    // Die Bezeichnungen stehen bewusst in der jeweiligen Sprache selbst und werden NICHT
    // uebersetzt - so findet sich auch jemand zurecht, der die gerade eingestellte Sprache
    // nicht versteht (dieselbe Konvention nutzen die Sprachlisten von Android und iOS).
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch");

    fun toLocale(): Locale = Locale.forLanguageTag(tag)
}

/**
 * Speichert die gewaehlte Oberflaechensprache und wendet sie an.
 *
 * **Warum eigene Verwaltung statt der Systemsprache:** die App soll unabhaengig vom Geraet
 * standardmaessig auf Englisch laufen ([AppLanguage.ENGLISH]) und nur auf ausdrueckliche Wahl
 * auf Deutsch umschalten. Wuerde sie einfach der Systemsprache folgen, bekaeme ein deutsches
 * Geraet automatisch Deutsch - genau das ist hier nicht gewollt.
 *
 * **Warum ueber [wrap] in `attachBaseContext` und nicht ueber die Per-App-Sprache von
 * Android 13:** letztere setzt androidx.appcompat voraus, das dieses Projekt bewusst nicht
 * einbindet (siehe die Icon-Entscheidung in HomeScreen), und griffe ausserdem erst ab API 33 -
 * die App laeuft ab API 26. Das Umhuellen des Basis-Kontexts wirkt auf allen Versionen gleich
 * und kommt ohne zusaetzliche Abhaengigkeit aus.
 */
object LanguagePrefs {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language"

    fun get(context: Context): AppLanguage {
        val tag = prefs(context).getString(KEY_LANGUAGE, null) ?: return AppLanguage.ENGLISH
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.ENGLISH
    }

    fun set(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    /**
     * Liefert einen Kontext, dessen Ressourcen in der gewaehlten Sprache aufgeloest werden.
     * Aus `Activity.attachBaseContext` aufzurufen, also bevor irgendeine Ressource gelesen wird.
     */
    fun wrap(base: Context): Context {
        val locale = get(base).toLocale()
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
