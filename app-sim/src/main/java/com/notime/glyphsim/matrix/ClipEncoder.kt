package com.notime.glyphsim.matrix

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Schreibt eine Folge von Bildern als MP4-Datei.
 *
 * **Warum ueber [MediaCodec.getInputImage] und nicht ueber einen selbst gepackten Farbpuffer.**
 * Der uebliche Stolperstein beim Videoschreiben auf Android ist die Farbanordnung: Manche Geraete
 * erwarten die Farbanteile getrennt hintereinander, andere verschraenkt, mit unterschiedlichen
 * Zeilen- und Punktabstaenden. Wer den Puffer selbst packt, muss das erraten - und liegt auf
 * irgendeinem Geraet daneben, was zu einem Film fuehrt, der sich zwar schreiben laesst, aber in
 * Falschfarben oder verschoben abspielt.
 *
 * [MediaCodec.getInputImage] liefert stattdessen die einzelnen Ebenen MIT ihren Abstaenden, sodass
 * sich jeder Bildpunkt an die Stelle schreiben laesst, die dieses Geraet tatsaechlich erwartet.
 * Damit entfaellt das Raten vollstaendig.
 *
 * **Bewusst ohne Ton.** Musik unterzulegen hiesse entweder, welche mitzuliefern (mit allem, was an
 * Rechten daran haengt), oder fremde Dateien einzumischen - beides ist ungleich aufwendiger als
 * das Bild. Und die ueblichen Videoportale ersetzen den Ton ohnehin durch eigene Musik; ein
 * stummer Film ist dort genau das, was gebraucht wird.
 */
object ClipEncoder {

    private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val FRAME_RATE = 15
    private const val KEYFRAME_INTERVAL_SECONDS = 1
    private const val BIT_RATE = 6_000_000
    private const val DEQUEUE_TIMEOUT_US = 10_000L

    /**
     * Kodiert [frames] als MP4 nach [target].
     *
     * Gibt bei Erfolg die Datei zurueck und `null`, wenn das Geraet keinen passenden Kodierer hat
     * oder das Schreiben scheitert. Bewusst kein Ausloesen einer Ausnahme: Ein misslungener Film
     * ist aergerlich, aber kein Grund, die App abstuerzen zu lassen - der Aufrufer sagt dem Nutzer
     * schlicht Bescheid.
     */
    fun encode(
        frameCount: Int,
        width: Int,
        height: Int,
        target: File,
        /**
         * Zeichnet Bild [index] in die uebergebene Bitmap. Bewusst eine Funktion statt einer
         * fertigen Liste: So existiert immer nur EIN Bild im Speicher (siehe
         * PlayClipRenderer.renderInto) - eine Liste aus neunzig Bildern in voller Aufloesung
         * spraengte den Speicher.
         */
        produce: (index: Int, into: Bitmap) -> Unit
    ): File? {
        if (frameCount <= 0) return null
        // Videokodierer arbeiten in Bloecken; ungerade Kantenlaengen lehnen viele ab.
        if (width % 2 != 0 || height % 2 != 0) return null

        val scratch = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        return try {
            val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEYFRAME_INTERVAL_SECONDS)
            }
            codec = MediaCodec.createEncoderByType(MIME)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val info = MediaCodec.BufferInfo()
            var frameIndex = 0

            while (true) {
                val done = frameIndex >= frameCount
                // Naechstes Bild einspeisen.
                val inIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                if (inIndex >= 0) {
                    if (done) {
                        codec.queueInputBuffer(
                            inIndex, 0, 0, presentationTimeUs(frameIndex),
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    } else {
                        val image = codec.getInputImage(inIndex)
                        if (image == null) return null.also { target.delete() }
                        produce(frameIndex, scratch)
                        writeBitmapAsYuv(scratch, image)
                        codec.queueInputBuffer(
                            inIndex, 0, imageSize(width, height),
                            presentationTimeUs(frameIndex), 0
                        )
                        frameIndex++
                    }
                }

                // Fertiges Material abholen.
                var outIndex = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                while (outIndex >= 0 || outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Das endgueltige Format steht erst JETZT fest - erst danach darf der
                        // Muxer gestartet werden, sonst schreibt er einen unbrauchbaren Kopf.
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else {
                        val buffer: ByteBuffer = codec.getOutputBuffer(outIndex)
                            ?: return null.also { target.delete() }
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 &&
                            info.size > 0 && muxerStarted
                        ) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(trackIndex, buffer, info)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            return if (muxerStarted) target else null.also { target.delete() }
                        }
                    }
                    outIndex = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                }
            }
            @Suppress("UNREACHABLE_CODE")
            target
        } catch (t: Throwable) {
            target.delete()
            null
        } finally {
            scratch.recycle()
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { if (muxerStarted) muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    private fun presentationTimeUs(frameIndex: Int): Long =
        frameIndex * 1_000_000L / FRAME_RATE

    private fun imageSize(width: Int, height: Int): Int = width * height * 3 / 2

    /**
     * Schreibt eine Bitmap in die drei Ebenen des Kodierer-Puffers.
     *
     * Die Abstaende ([android.media.Image.Plane.getRowStride]/`getPixelStride`) werden bewusst
     * ausgelesen statt angenommen - genau darin unterscheiden sich die Geraete (siehe
     * Klassendoku). Die Umrechnung selbst ist die uebliche Fernseh-Norm; da unsere Bilder
     * praktisch farblos sind (warmes Weiss auf Schwarz), traegt ohnehin fast alles die
     * Helligkeitsebene.
     */
    internal fun writeBitmapAsYuv(bitmap: Bitmap, image: android.media.Image) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = pixels[y * width + x]
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF

                val luma = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuffer.put(y * yPlane.rowStride + x * yPlane.pixelStride, luma.coerceIn(0, 255).toByte())

                // Farbebenen sind halb so fein aufgeloest - nur jeder zweite Punkt.
                if (y % 2 == 0 && x % 2 == 0) {
                    val cb = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val cr = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    val offset = (y / 2) * uPlane.rowStride + (x / 2) * uPlane.pixelStride
                    uBuffer.put(offset, cb.coerceIn(0, 255).toByte())
                    vBuffer.put((y / 2) * vPlane.rowStride + (x / 2) * vPlane.pixelStride, cr.coerceIn(0, 255).toByte())
                }
            }
        }
    }
}
