package com.notime.glyphsim.matrix

import android.content.Context
import android.os.SystemClock
import java.time.LocalDate
import java.time.LocalTime

/**
 * **Testmodus**: laesst den Tag im Zeitraffer ablaufen, damit sich alle Tageszeiten und alle
 * Szenen in wenigen Minuten ansehen lassen, statt einen echten Tag darauf zu warten.
 *
 * **Warum das ueberhaupt eine eigene Sache ist.** Der ganze Tagesablauf haengt an der Uhrzeit
 * (siehe [PlayAmbientActivity.currentDayPhase]): Wer sehen will, wie der Avatar morgens aufsteht,
 * mittags arbeitet und nachts schlaeft, muesste die Geraeteuhr stellen oder vierundzwanzig Stunden
 * zuschauen. Genau daran scheitert sonst jede Beurteilung von "fuehlt sich das richtig an?".
 *
 * **Zwei Dinge werden beschleunigt, und beide sind noetig:**
 * 1. Die *Uhrzeit* selbst ([now]) - ein voller Tag vergeht in [LAPSE_DAY_SECONDS].
 * 2. Das *Tempo der Regungen* ([paceFactor]) - sonst liefe die Uhr schnell, waehrend der Avatar
 *    weiter alle zehn Sekunden EINE Sache taete und man von jeder Tageszeit nur einen Ausschnitt
 *    saehe.
 *
 * **Ausdruecklich nur zum Ausprobieren gedacht.** Im Normalbetrieb ist das aus, und der Avatar
 * folgt der echten Uhr in echtem Tempo - ein Wesen, das im Minutentakt durch seinen Tag hetzt,
 * waere das Gegenteil des ruhigen Begleiters, um den es hier geht.
 */
object PlayTimeLapse {

    /**
     * Wie schnell die Zeit vergeht.
     *
     * Drei Stufen statt eines Schalters, weil die beiden Zwecke verschieden sind: Wer sich den
     * Tagesablauf ansehen will, braucht ihn zuegig, aber noch verfolgbar. Wer dagegen PRUEFEN
     * will, ob alle Orte und Ablaeufe ueberhaupt vorkommen, muss viele Tage in wenigen Minuten
     * durchlaufen lassen - Arbeit und Einkauf haengen am Vorrat und am Geld und tauchen deshalb
     * erst nach mehreren vollstaendigen Kreislaeufen auf.
     */
    enum class Speed(
        /** Wie lange ein voller Tag dauert. */
        val daySeconds: Double,
        /** Faktor auf alle Wartezeiten. */
        val pace: Float
    ) {
        OFF(24 * 60 * 60.0, 1f),
        FAST(240.0, 0.18f),
        TURBO(45.0, 0.04f)
    }

    private const val PREFS = "play_time_lapse"
    private const val KEY_SPEED = "speed"

    /**
     * In-memory gehalten und nicht bei jedem Zugriff aus den Einstellungen gelesen: [now] wird
     * mehrmals je Bild aufgerufen (Kulisse, Themenwahl, Regungen). Ein Dateizugriff an dieser
     * Stelle waere aus Bequemlichkeit erkaufte Last im Zeichenpfad.
     */
    @Volatile
    private var speed = Speed.OFF

    /** Referenzpunkt, ab dem die schnelle Uhr laeuft - beim Umschalten neu gesetzt. */
    @Volatile
    private var startedAtMillis = 0L

    /** Uhrzeit, an der der Zeitraffer beginnt: frueher Morgen, damit ein Durchlauf am Anfang
     *  eines Tages einsetzt statt mitten in der Nacht. */
    private val lapseStart = LocalTime.of(6, 0)

    fun speed(): Speed = speed

    fun isEnabled(): Boolean = speed != Speed.OFF

    /** Beim Start der App einmal aus den Einstellungen uebernehmen. */
    fun restore(context: Context) {
        val stored = prefs(context).getString(KEY_SPEED, Speed.OFF.name)
        setSpeed(context, runCatching { Speed.valueOf(stored!!) }.getOrDefault(Speed.OFF))
    }

    fun setSpeed(context: Context, value: Speed) {
        prefs(context).edit().putString(KEY_SPEED, value.name).apply()
        if (value != speed) startedAtMillis = SystemClock.elapsedRealtime()
        speed = value
    }

    /**
     * Die Uhrzeit, nach der sich der Tagesablauf richtet - im Normalbetrieb die echte, sonst eine
     * beschleunigte.
     *
     * [SystemClock.elapsedRealtime] statt der Wanduhr: Diese Uhr laeuft monoton und wird nicht von
     * Zeitzonen, Sommerzeit oder einem Nutzer, der die Systemzeit stellt, nach hinten gerissen -
     * sonst koennte der Zeitraffer mitten im Durchlauf zurueckspringen.
     */
    fun now(): LocalTime {
        val current = speed
        if (current == Speed.OFF) return LocalTime.now()
        val elapsedSeconds = (SystemClock.elapsedRealtime() - startedAtMillis) / 1000.0
        val dayFraction = (elapsedSeconds / current.daySeconds) % 1.0
        return lapseStart.plusSeconds((dayFraction * 24 * 60 * 60).toLong())
    }

    /**
     * Derselbe Tag wie [now], aber als stabiler Schluessel fuer taggebundene Spielzustande.
     * Im Zeitraffer zaehlt jeder simulierte 24h-Durchlauf als neuer Tag; im Normalbetrieb gilt
     * das echte Kalenderdatum. Ein Wechsel der Zeitraffer-Stufe startet bewusst einen neuen
     * Testtag, genau wie die beschleunigte Uhr dabei wieder am Morgen beginnt.
     */
    fun dayKey(): String {
        val current = speed
        if (current == Speed.OFF) return "real:${LocalDate.now()}"
        val elapsedSeconds = (SystemClock.elapsedRealtime() - startedAtMillis) / 1000.0
        val simulatedDay = (elapsedSeconds / current.daySeconds).toLong()
        return "lapse:${current.name}:$startedAtMillis:$simulatedDay"
    }

    /**
     * Faktor auf alle Wartezeiten - die PAUSEN zwischen den Regungen und das Verweilen innerhalb
     * eines Ablaufs. Die Animationen selbst bleiben unangetastet: Wer sie mitbeschleunigte, saehe
     * nur noch Zucken statt Bewegung.
     */
    fun paceFactor(): Float = speed.pace

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
