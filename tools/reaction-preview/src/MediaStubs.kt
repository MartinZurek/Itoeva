package android.media

/**
 * Attrappen fuer die Audio-Wiedergabe - siehe tools/reaction-preview/README.md.
 *
 * Geprueft wird ausdruecklich NICHT, ob etwas erklingt, sondern die reine Auswahl und die
 * Lautstaerke-Kurve der Ueberblendung. Deshalb genuegt hier, dass die Android-Anbindung
 * uebersetzt; den echten Zeitverlauf prueft die CI beziehungsweise der Geraetetest.
 */
class AudioAttributes private constructor() {
    class Builder {
        fun setUsage(usage: Int): Builder = this
        fun setContentType(type: Int): Builder = this
        fun build(): AudioAttributes = AudioAttributes()
    }

    companion object {
        const val USAGE_MEDIA = 1
        const val CONTENT_TYPE_MUSIC = 2
    }
}

class AudioManager {
    val isMusicActive: Boolean = false
    val ringerMode: Int = 2

    companion object {
        const val RINGER_MODE_SILENT = 0
        const val RINGER_MODE_VIBRATE = 1
    }
}

class MediaPlayer {
    var isLooping: Boolean = false
    val isPlaying: Boolean = false
    fun setAudioAttributes(attributes: AudioAttributes) {}
    fun setVolume(left: Float, right: Float) {}
    fun start() {}
    fun stop() {}
    fun release() {}

    companion object {
        @JvmStatic
        fun create(context: android.content.Context, resId: Int): MediaPlayer? = null
    }
}
