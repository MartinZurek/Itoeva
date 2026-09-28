package com.notime.glyphsim.matrix

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * **Der Klang des Ortes** - leise unter der Musik, gerechnet statt aufgenommen.
 *
 * Bis hierher gab es in dieser Welt Musik und sonst nichts: Der Strand hatte Wellen im Bild, aber
 * nicht im Ohr, der Regen fiel lautlos, und die Nacht im Park war so still wie das Badezimmer.
 * Gerade in einem Stream, der rund um die Uhr laeuft, ist das der Unterschied zwischen einer
 * Kulisse und einem Ort: Man hoert, wo man ist, bevor man hinsieht.
 *
 * **Warum gerechnet.** Die Hausregel steht in [com.notime.glyphsim.ui.PlayMusic]: Nur der Score
 * darf eine Datei sein; alles, was die Welt selbst von sich gibt, wird gerechnet - wie schon der
 * Klang des Wesens ([PlayChime]). Damit gibt es auch keine zweite Erzeugungs-Pipeline und keine
 * Rechtefrage. Rauschen, gefiltert und im Takt geformt, ist das klangliche Gegenstueck zu einer
 * Welle aus ein paar Zellen: stilisiert, aber sofort erkennbar.
 *
 * **Was hier steht:** [kindFor] entscheidet, welche Atmo zu einer Lage passt; [render] rechnet
 * eine nahtlose Schleife davon. Beides ohne Android und damit offline pruefbar. Wann sie zu
 * hoeren ist - nur zusammen mit der Musik, unter denselben Sperren -, entscheidet
 * [com.notime.glyphsim.ui.PlayAmbienceSound].
 */
object PlayAmbience {

    /** Die Klangbilder. Wenige, gut unterscheidbare - wie die Motive der Wirkungen. */
    enum class Kind(val loopSeconds: Int, val level: Float) {
        /** Regen draussen: Rauschen und einzelne Tropfen. */
        RAIN(6, 0.55f),

        /** Regen gegen die Scheibe, von drinnen gehoert: dumpfer, leiser. */
        RAIN_WINDOW(6, 0.35f),

        /** Wind ueber Gebirge und Ebene, in zwei Boeen je Durchlauf. */
        WIND(12, 0.45f),

        /** Brandung: eine Welle je Durchlauf, anschwellend und auslaufend. */
        WAVES(10, 0.55f),

        /** Tagsueber im Gruenen: leiser Wind und ein paar Vogelrufe. */
        BIRDS(12, 0.40f),

        /** Nachts draussen: Grillen. */
        CRICKETS(4, 0.30f),

        /** Im Sumpf: Froesche. */
        FROGS(8, 0.40f),

        /** In der Stadt: fernes Brummen und ab und zu ein Auto. */
        CITY(10, 0.35f),

        /** Im Cafe: Stimmengemurmel und ein Loeffel an der Tasse. */
        CAFE(10, 0.35f),

        /** In der Kristallgrotte: ein tiefes Summen und einzelne, hallende Tropfen. */
        CAVE(8, 0.40f)
    }

    /** Dieselbe Abtastrate wie der Klang des Wesens (siehe [PlayChime.SAMPLE_RATE]). */
    const val SAMPLE_RATE = 22_050

    private val GREEN = setOf(
        PlayScene.Place.PARK, PlayScene.Place.FOREST, PlayScene.Place.MEADOW,
        PlayScene.Place.POND, PlayScene.Place.JUNGLE, PlayScene.Place.SPORT
    )

    /**
     * **Welche Atmo zu dieser Lage passt** - `null` heisst Stille unter der Musik.
     *
     * Die Rangfolge: Wetter vor Ort. Wer im Regen am Strand steht, hoert den Regen. Drinnen gibt
     * es nur zwei Klaenge - den Regen an der Scheibe und das Cafe; eine Wohnung klingt nach nichts,
     * und genau das soll sie.
     */
    fun kindFor(
        place: PlayScene.Place,
        dayPhase: PlayAmbientActivity.DayPhase,
        weather: PlayWeather
    ): Kind? {
        // Die Grotte hat eine Decke - Regen und Schnee hoert man dort nicht.
        if (place == PlayScene.Place.GROTTO) return Kind.CAVE
        val outdoors = PlayScene.isOutdoors(place)
        val night = dayPhase == PlayAmbientActivity.DayPhase.NIGHT
        if (weather == PlayWeather.RAIN) return if (outdoors) Kind.RAIN else Kind.RAIN_WINDOW
        // Schnee ist leise - draussen hoert man nur den Wind, drinnen nichts.
        if (weather == PlayWeather.SNOW) return if (outdoors) Kind.WIND else null
        return when {
            place == PlayScene.Place.CAFE -> Kind.CAFE
            !outdoors -> null
            place == PlayScene.Place.BEACH -> Kind.WAVES
            place == PlayScene.Place.MOUNTAINS || place == PlayScene.Place.PLAINS -> Kind.WIND
            place == PlayScene.Place.SWAMP -> Kind.FROGS
            place == PlayScene.Place.CITY || place == PlayScene.Place.STREET -> Kind.CITY
            place in GREEN -> if (night) Kind.CRICKETS else Kind.BIRDS
            else -> null
        }
    }

    /** Laenge der Ueberblendung an der Naht der Schleife. */
    private const val SEAM_SECONDS = 0.25

    /**
     * **Eine nahtlose Schleife** von [Kind.loopSeconds] Sekunden, 16 Bit mono.
     *
     * Nahtlos auf zwei Wegen: Alles Periodische (Boeen, Wellen, Grillenzirpen) hat eine Periode,
     * die die Schleifenlaenge teilt; das Rauschen wird ueber eine Viertelsekunde mit seiner
     * eigenen Fortsetzung ueberblendet, sodass das letzte Stueck nahtlos in das erste uebergeht.
     * Aus demselben [seed] entsteht immer dieselbe Schleife.
     */
    fun render(kind: Kind, seed: Int = 1): ShortArray {
        val n = kind.loopSeconds * SAMPLE_RATE
        val seam = (SEAM_SECONDS * SAMPLE_RATE).toInt()
        val raw = synth(kind, n + seam, n, Random(seed * 31 + kind.ordinal))
        val loop = FloatArray(n) { i ->
            if (i < seam) {
                val w = i.toFloat() / seam
                raw[i] * w + raw[n + i] * (1f - w)
            } else {
                raw[i]
            }
        }
        val peak = loop.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val gain = kind.level * 0.9f / peak
        return ShortArray(n) { i -> (loop[i] * gain * Short.MAX_VALUE).toInt().toShort() }
    }

    // ---- Bausteine ----

    /** Tiefpass erster Ordnung, [a] zwischen 0 (zu) und 1 (offen). */
    private class Lowpass(private val a: Float) {
        private var y = 0f
        fun next(x: Float): Float { y += a * (x - y); return y }
    }

    private fun noise(random: Random) = random.nextFloat() * 2f - 1f

    /** Periodische Huellkurve: [cycles] volle Perioden je Schleife [loop] - teilt die Laenge. */
    private fun periodic(i: Int, loop: Int, cycles: Int, phase: Double = 0.0): Float =
        sin(PI * cycles * i / loop + phase).let { (it * it).toFloat() }

    private fun synth(kind: Kind, total: Int, loop: Int, random: Random): FloatArray = when (kind) {
        Kind.RAIN -> rain(total, random, muffle = false)
        Kind.RAIN_WINDOW -> rain(total, random, muffle = true)
        Kind.WIND -> wind(total, loop, random, gusts = 2, level = 1f)
        Kind.WAVES -> waves(total, loop, random)
        Kind.BIRDS -> add(wind(total, loop, random, gusts = 1, level = 0.35f), birds(total, loop, random))
        Kind.CRICKETS -> add(wind(total, loop, random, gusts = 1, level = 0.12f), crickets(total, loop))
        Kind.FROGS -> add(wind(total, loop, random, gusts = 1, level = 0.15f), frogs(total, loop, random))
        Kind.CITY -> city(total, loop, random)
        Kind.CAFE -> cafe(total, loop, random)
        Kind.CAVE -> cave(total, loop, random)
    }

    private fun add(a: FloatArray, b: FloatArray) = FloatArray(a.size) { a[it] + b[it] }

    /** Ein kurzes Ereignis an [start], das in der Schleife wiederkehrt (auch ueber die Naht). */
    private inline fun stamp(out: FloatArray, loop: Int, start: Int, length: Int, sample: (Int) -> Float) {
        for (k in 0 until length) {
            val v = sample(k)
            var i = start + k
            while (i < out.size) {
                out[i] += v
                i += loop
            }
            // Was ueber das Schleifenende hinausragt, erklingt am Anfang.
            if (start + k >= loop) out[(start + k) % loop] += v
        }
    }

    private fun rain(total: Int, random: Random, muffle: Boolean): FloatArray {
        val hiss = Lowpass(if (muffle) 0.10f else 0.55f)
        // Durch die Scheibe gehoert fehlen die Hoehen deutlich - ein zweiter Tiefpass dahinter
        // macht aus dem Rauschen ein Trommeln.
        val pane = Lowpass(if (muffle) 0.10f else 1f)
        val body = Lowpass(0.06f)
        val out = FloatArray(total) { i ->
            val x = noise(random)
            val h = pane.next(hiss.next(x))
            // Etwas Koerper darunter, damit es nach Wasser klingt und nicht nach Radio.
            h * (if (muffle) 1.4f else 0.7f) + body.next(x) * 1.6f
        }
        // Einzelne Tropfen: kurze, abklingende Knackser an zufaelligen Stellen.
        val drops = if (muffle) 25 else 90
        val decay = SAMPLE_RATE * 0.004
        repeat(drops) {
            val at = random.nextInt(total)
            val amp = 0.25f + random.nextFloat() * (if (muffle) 0.2f else 0.6f)
            val len = (decay * 5).toInt()
            for (k in 0 until len) {
                if (at + k < total) out[at + k] += amp * exp(-k / decay).toFloat() * noise(random)
            }
        }
        return out
    }

    private fun wind(total: Int, loop: Int, random: Random, gusts: Int, level: Float): FloatArray {
        val lo = Lowpass(0.012f)
        val hi = Lowpass(0.06f)
        return FloatArray(total) { i ->
            val x = noise(random)
            // Bandpass: das Rauschen zwischen Brummen und Zischen - der Ton von Luft.
            val band = hi.next(x) - lo.next(x) * 0.6f
            band * level * (0.35f + 0.65f * periodic(i, loop, gusts))
        }
    }

    private fun waves(total: Int, loop: Int, random: Random): FloatArray {
        val body = Lowpass(0.03f)
        val foam = Lowpass(0.4f)
        return FloatArray(total) { i ->
            val x = noise(random)
            // Die Welle schwillt an und laeuft lang aus: steiler Anstieg, flacher Abfall.
            val swell = periodic(i, loop, 1).toDouble().pow(0.6).toFloat()
            // Die Gischt zischt erst, wenn die Welle bricht.
            val crest = periodic(i, loop, 1, phase = -0.35).toDouble().pow(3.0).toFloat()
            body.next(x) * 3f * (0.15f + swell) + foam.next(x) * 0.5f * crest
        }
    }

    private fun birds(total: Int, loop: Int, random: Random): FloatArray {
        val out = FloatArray(total)
        // Drei bis fuenf Rufe je Schleife, jeder aus zwei bis vier kurzen, gleitenden Toenen.
        repeat(3 + random.nextInt(3)) {
            var at = random.nextInt(loop)
            val base = 2600.0 + random.nextDouble() * 2200.0
            repeat(2 + random.nextInt(3)) {
                val len = (SAMPLE_RATE * (0.05 + random.nextDouble() * 0.07)).toInt()
                val sweep = (random.nextDouble() - 0.3) * 900.0
                val amp = 0.14f + random.nextFloat() * 0.1f
                var phase = 0.0
                stamp(out, loop, at, len) { k ->
                    val t = k.toDouble() / len
                    phase += 2 * PI * (base + sweep * t) / SAMPLE_RATE
                    (amp * sin(PI * t) * sin(phase)).toFloat()
                }
                at += len + (SAMPLE_RATE * (0.03 + random.nextDouble() * 0.08)).toInt()
            }
        }
        return out
    }

    private fun crickets(total: Int, loop: Int): FloatArray {
        // Zwei Grillen mit eigenem Ton und eigenem Takt; beide Takte teilen die Schleife.
        data class Cricket(val hz: Double, val chirpsPerLoop: Int, val offset: Double, val amp: Float)
        val crickets = listOf(Cricket(4400.0, 5, 0.0, 0.10f), Cricket(4950.0, 7, 0.37, 0.07f))
        return FloatArray(total) { i ->
            val t = (i % loop).toDouble() / loop
            crickets.sumOf { c ->
                val inChirp = ((t * c.chirpsPerLoop + c.offset) % 1.0)
                // Ein Zirpen ist ein kurzer Pulszug: 30 Pulse je Sekunde, ein Viertel des Takts lang.
                if (inChirp > 0.25) 0.0 else {
                    val pulse = sin(PI * ((i * 30.0 / SAMPLE_RATE) % 1.0)).let { it * it }
                    c.amp * pulse * sin(2 * PI * c.hz * i / SAMPLE_RATE)
                }
            }.toFloat()
        }
    }

    private fun frogs(total: Int, loop: Int, random: Random): FloatArray {
        val out = FloatArray(total)
        repeat(4 + random.nextInt(3)) {
            val at = random.nextInt(loop)
            val hz = 150.0 + random.nextDouble() * 120.0
            val len = (SAMPLE_RATE * (0.18 + random.nextDouble() * 0.15)).toInt()
            val amp = 0.25f + random.nextFloat() * 0.15f
            stamp(out, loop, at, len) { k ->
                val t = k.toDouble() / len
                // Das Quaken: ein tiefer, obertonreicher Ton, 25-mal je Sekunde gepulst.
                val tone = sin(2 * PI * hz * k / SAMPLE_RATE) + 0.5 * sin(4 * PI * hz * k / SAMPLE_RATE)
                val gate = sin(PI * ((k * 25.0 / SAMPLE_RATE) % 1.0))
                (amp * sin(PI * t) * gate * tone).toFloat()
            }
        }
        return out
    }

    private fun city(total: Int, loop: Int, random: Random): FloatArray {
        val rumble = Lowpass(0.008f)
        val swoosh = Lowpass(0.05f)
        // Ein fernes Auto je Schleife: Rauschen, das anschwillt und wieder verebbt.
        val carAt = random.nextInt(loop)
        val carLen = (SAMPLE_RATE * 2.6)
        return FloatArray(total) { i ->
            val x = noise(random)
            val d = Math.floorMod(i - carAt, loop)
            val car = if (d < carLen) sin(PI * d / carLen).pow(2.0).toFloat() else 0f
            rumble.next(x) * 5f + swoosh.next(x) * 0.8f * car
        }
    }

    private fun cave(total: Int, loop: Int, random: Random): FloatArray {
        // Das Summen: sehr tiefes, ruhiges Rauschen - der Raum selbst.
        val hum = Lowpass(0.004f)
        val out = FloatArray(total) { hum.next(noise(random)) * 7f }
        // Tropfen: ein kurzer, heller Ton, der lange nachhallt - drei, vier je Durchlauf.
        repeat(3 + random.nextInt(2)) {
            val at = random.nextInt(loop)
            val hz = 1300.0 + random.nextDouble() * 900.0
            val len = (SAMPLE_RATE * 0.6).toInt()
            stamp(out, loop, at, len) { k ->
                val env = exp(-k / (SAMPLE_RATE * 0.12))
                // Der Ton faellt beim Aufprall ein wenig - daran erkennt man einen Tropfen.
                val glide = hz * (1.0 + 0.25 * exp(-k / (SAMPLE_RATE * 0.01)))
                (0.35 * env * sin(2 * PI * glide * k / SAMPLE_RATE)).toFloat()
            }
        }
        return out
    }

    /** Verstaerkung der Silbenkurve im Cafe - so gewaehlt, dass zwischen Silben Luecken bleiben. */
    private const val SYLLABLE_GAIN = 28f

    private fun cafe(total: Int, loop: Int, random: Random): FloatArray {
        // **Gemurmel ist Rhythmus, nicht Rauschen.** Ein gefiltertes Rauschen allein klingt wie
        // Wind; nach Stimmen klingt es erst im Silbentakt - vier bis sechs Mal je Sekunde lauter
        // und leiser, bei jedem Sprecher anders. Drei Stimmen, jede in ihrem eigenen Tonbereich
        // und mit eigener Silbenkurve.
        // Zwei Tiefpaesse hintereinander (hi, hi2): Stimmen durch einen Raum haben kaum Hoehen -
        // mit einem allein zischte das Gemurmel.
        data class Voice(val lo: Lowpass, val hi: Lowpass, val hi2: Lowpass, val syllable: Lowpass, val gain: Float)
        val voices = listOf(
            Voice(Lowpass(0.03f), Lowpass(0.09f), Lowpass(0.09f), Lowpass(0.0012f), 1.0f),
            Voice(Lowpass(0.05f), Lowpass(0.16f), Lowpass(0.16f), Lowpass(0.0015f), 0.8f),
            Voice(Lowpass(0.02f), Lowpass(0.07f), Lowpass(0.07f), Lowpass(0.0010f), 0.7f)
        )
        val out = FloatArray(total) { i ->
            val room = 0.7f + 0.3f * periodic(i, loop, 3)
            voices.sumOf { v ->
                val x = noise(random)
                val band = v.hi2.next(v.hi.next(x)) - v.lo.next(x)
                // Die Silbenkurve: stark geglaettetes Rauschen, verstaerkt und unten abgeschnitten -
                // dadurch Silben mit Luecken dazwischen, statt eines gleichmaessigen Teppichs.
                val envelope = (SYLLABLE_GAIN * v.syllable.next(noise(random)) + 0.15f).coerceAtLeast(0f)
                (band * envelope * v.gain).toDouble()
            }.toFloat() * 4f * room
        }
        // Zwei, drei Loeffel an der Tasse: ein heller, kurz ausklingender Doppelton.
        repeat(2 + random.nextInt(2)) {
            val at = random.nextInt(loop)
            val hz = 2600.0 + random.nextDouble() * 800.0
            val len = (SAMPLE_RATE * 0.25).toInt()
            stamp(out, loop, at, len) { k ->
                val env = exp(-k / (SAMPLE_RATE * 0.04))
                (0.30 * env * (sin(2 * PI * hz * k / SAMPLE_RATE) + 0.4 * sin(2 * PI * hz * 2.7 * k / SAMPLE_RATE))).toFloat()
            }
        }
        return out
    }
}
