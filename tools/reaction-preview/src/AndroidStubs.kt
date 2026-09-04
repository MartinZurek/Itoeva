package android.content

/** Attrappe fuer die Offline-Uebersetzung - siehe tools/reaction-preview/README.md. */
class Context {
    companion object { const val MODE_PRIVATE = 0 }
    fun getSharedPreferences(name: String, mode: Int): SharedPreferences = SharedPreferences()
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
