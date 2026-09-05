package android.content

/** Attrappe fuer die Offline-Uebersetzung - siehe tools/reaction-preview/README.md. */
class Context {
    companion object {
        const val MODE_PRIVATE = 0
        const val AUDIO_SERVICE = "audio"
    }

    val applicationContext: Context get() = this
    val packageName: String get() = "com.notime.glyphsim"
    val resources: Resources get() = Resources()

    fun getSharedPreferences(name: String, mode: Int): SharedPreferences = SharedPreferences()

    /**
     * Gibt immer `null` zurueck, und das ist fuer die geprueften Faelle richtig: Die
     * Musikentscheidung liegt in `PlayMusic.decide` und bekommt ihre Eingaben als Parameter.
     * Was `getSystemService` liefert, ist nur der Weg dorthin und wird nicht mitgeprueft.
     */
    fun getSystemService(name: String): Any? = null
}

/**
 * Nur `getIdentifier`, und immer 0 - also "dieser Track ist nicht ausgeliefert".
 *
 * Damit uebersetzt die Namenssuche aus `PlayMusic`, ohne dass es ein echtes Paket braucht. Welche
 * Rollen tatsaechlich vorhanden sind, geben die Tests ohnehin selbst vor.
 */
class Resources {
    fun getIdentifier(name: String, defType: String, defPackage: String): Int = 0
}

class SharedPreferences {
    fun getLong(key: String, default: Long): Long = default
    fun getFloat(key: String, default: Float): Float = default
    fun getInt(key: String, default: Int): Int = default
    fun getBoolean(key: String, default: Boolean): Boolean = default
    fun getString(key: String, default: String?): String? = default
    fun edit(): Editor = Editor()
    class Editor {
        fun putLong(key: String, value: Long): Editor = this
        fun putFloat(key: String, value: Float): Editor = this
        fun putInt(key: String, value: Int): Editor = this
        fun putBoolean(key: String, value: Boolean): Editor = this
        fun putString(key: String, value: String?): Editor = this
        fun remove(key: String): Editor = this
        fun apply() {}
    }
}
