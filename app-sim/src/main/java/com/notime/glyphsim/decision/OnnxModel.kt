package com.notime.glyphsim.decision

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * **Ein kleines ONNX-Modell lesen und rechnen - ohne ML-Laufzeit.**
 *
 * ## Warum kein onnxruntime
 *
 * Die Policy ist ein Netz aus drei vollverbundenen Schichten mit wenigen tausend Gewichten. Die
 * Android-Laufzeit von onnxruntime braechte dafuer mehrere Megabyte nativer Bibliotheken je
 * Prozessorarchitektur in die APK, und eine neue Abhaengigkeit ist hier eine Build-Aenderung mit
 * eigener Freigabe (AgentGuide.md). Vor allem aber liefe sie nicht in der Offline-Teststrecke
 * (`tools/reaction-preview/tests.sh`), die ohne Android und ohne Gradle auskommt - die Policy
 * waere dort nicht pruefbar.
 *
 * Deshalb liest diese Datei genau die Teilmenge des Formats, die das Trainingswerkzeug schreibt:
 * das Protobuf-Grundformat, Graph, Knoten, Attribute, Gewichte (als `raw_data` oder
 * `float_data`) und Metadaten. Gerechnet werden `Gemm`, `MatMul`, `Add`, `Relu` und `Identity`.
 * Alles andere wird beim Laden ABGELEHNT, nicht still uebergangen - ein Modell, das hier nicht
 * vollstaendig verstanden wird, faellt auf die bisherige Logik zurueck (siehe [DecisionPolicies]).
 *
 * Dieselbe `.onnx`-Datei ist mit jeder ONNX-Laufzeit lesbar; das Trainingswerkzeug prueft seine
 * Ausgabe mit onnxruntime gegen dieselben Referenzwerte, die `OnnxDecisionPolicyTest` hier gegen
 * diesen Leser prueft.
 */
class OnnxModel private constructor(
    val metadata: Map<String, String>,
    val inputName: String,
    val inputWidth: Int,
    val outputName: String,
    private val nodes: List<Node>,
    private val initializers: Map<String, Tensor>
) {

    class Tensor(val shape: IntArray, val data: FloatArray)

    private class Node(
        val opType: String,
        val inputs: List<String>,
        val outputs: List<String>,
        val attributes: Map<String, Attribute>
    )

    private class Attribute(val f: Float? = null, val i: Long? = null)

    /** Anzahl der Gewichte - fuer Bericht und Groessenpruefung. */
    val parameterCount: Int get() = initializers.values.sumOf { it.data.size }

    /**
     * Rechnet [rows] Zeilen zu je [inputWidth] Merkmalen und liefert die erste Ausgabespalte.
     * [input] ist zeilenweise flach abgelegt.
     */
    fun run(input: FloatArray, rows: Int): FloatArray {
        require(input.size == rows * inputWidth) { "input size ${input.size} != $rows x $inputWidth" }
        val werte = HashMap<String, Tensor>(initializers)
        werte[inputName] = Tensor(intArrayOf(rows, inputWidth), input)
        for (node in nodes) {
            val ergebnis = when (node.opType) {
                "Gemm" -> gemm(node, werte)
                "MatMul" -> matMul(werte.getValue(node.inputs[0]), werte.getValue(node.inputs[1]))
                "Add" -> add(werte.getValue(node.inputs[0]), werte.getValue(node.inputs[1]))
                "Relu" -> werte.getValue(node.inputs[0]).let { t ->
                    Tensor(t.shape, FloatArray(t.data.size) { if (t.data[it] > 0f) t.data[it] else 0f })
                }
                "Identity" -> werte.getValue(node.inputs[0])
                else -> error("unsupported op ${node.opType}")
            }
            werte[node.outputs[0]] = ergebnis
        }
        val out = werte.getValue(outputName)
        val spalten = if (out.shape.size >= 2) out.shape[1] else 1
        return FloatArray(rows) { out.data[it * spalten] }
    }

    private fun gemm(node: Node, werte: Map<String, Tensor>): Tensor {
        val transA = (node.attributes["transA"]?.i ?: 0L) != 0L
        val transB = (node.attributes["transB"]?.i ?: 0L) != 0L
        val alpha = node.attributes["alpha"]?.f ?: 1f
        val beta = node.attributes["beta"]?.f ?: 1f
        val a = werte.getValue(node.inputs[0]).let { if (transA) transpose(it) else it }
        val b = werte.getValue(node.inputs[1]).let { if (transB) transpose(it) else it }
        var y = matMul(a, b)
        if (alpha != 1f) y = Tensor(y.shape, FloatArray(y.data.size) { y.data[it] * alpha })
        val c = node.inputs.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let(werte::getValue)
        if (c != null) {
            val scaled = if (beta != 1f) Tensor(c.shape, FloatArray(c.data.size) { c.data[it] * beta }) else c
            y = add(y, scaled)
        }
        return y
    }

    private fun transpose(t: Tensor): Tensor {
        require(t.shape.size == 2) { "transpose needs 2-d" }
        val (r, c) = t.shape[0] to t.shape[1]
        return Tensor(intArrayOf(c, r), FloatArray(r * c) { i -> t.data[(i % r) * c + i / r] })
    }

    private fun matMul(a: Tensor, b: Tensor): Tensor {
        require(a.shape.size == 2 && b.shape.size == 2 && a.shape[1] == b.shape[0]) {
            "matmul shape ${a.shape.contentToString()} x ${b.shape.contentToString()}"
        }
        val m = a.shape[0]
        val k = a.shape[1]
        val n = b.shape[1]
        val out = FloatArray(m * n)
        for (i in 0 until m) {
            val zeile = i * k
            val ziel = i * n
            for (p in 0 until k) {
                val av = a.data[zeile + p]
                if (av == 0f) continue
                val bZeile = p * n
                for (j in 0 until n) out[ziel + j] += av * b.data[bZeile + j]
            }
        }
        return Tensor(intArrayOf(m, n), out)
    }

    /** Addition mit Zeilen-Broadcast eines Vektors `[N]` oder `[1, N]` auf `[M, N]`. */
    private fun add(a: Tensor, b: Tensor): Tensor {
        if (a.data.size == b.data.size) return Tensor(a.shape, FloatArray(a.data.size) { a.data[it] + b.data[it] })
        val (gross, klein) = if (a.data.size >= b.data.size) a to b else b to a
        val n = klein.data.size
        require(gross.data.size % n == 0) { "cannot broadcast" }
        return Tensor(gross.shape, FloatArray(gross.data.size) { gross.data[it] + klein.data[it % n] })
    }

    companion object {

        val SUPPORTED_OPS = setOf("Gemm", "MatMul", "Add", "Relu", "Identity")

        /** Liest ein Modell oder wirft - der Aufrufer faengt und faellt zurueck. */
        fun parse(bytes: ByteArray): OnnxModel {
            val model = Reader(bytes)
            var graphBytes: ByteArray? = null
            val metadata = LinkedHashMap<String, String>()
            while (model.hasMore()) {
                val (field, wire) = model.tag()
                when {
                    field == 7 && wire == 2 -> graphBytes = model.bytes()
                    field == 14 && wire == 2 -> {
                        val entry = Reader(model.bytes())
                        var key = ""
                        var value = ""
                        while (entry.hasMore()) {
                            val (f, w) = entry.tag()
                            when {
                                f == 1 && w == 2 -> key = entry.string()
                                f == 2 && w == 2 -> value = entry.string()
                                else -> entry.skip(w)
                            }
                        }
                        metadata[key] = value
                    }
                    else -> model.skip(wire)
                }
            }
            val graph = Reader(requireNotNull(graphBytes) { "model has no graph" })
            val nodes = mutableListOf<Node>()
            val initializers = LinkedHashMap<String, Tensor>()
            val inputs = mutableListOf<Pair<String, IntArray>>()
            val outputs = mutableListOf<String>()
            while (graph.hasMore()) {
                val (field, wire) = graph.tag()
                when {
                    field == 1 && wire == 2 -> nodes += parseNode(graph.bytes())
                    field == 5 && wire == 2 -> parseTensor(graph.bytes()).let { (name, t) -> initializers[name] = t }
                    field == 11 && wire == 2 -> inputs += parseValueInfo(graph.bytes())
                    field == 12 && wire == 2 -> outputs += parseValueInfo(graph.bytes()).first
                    else -> graph.skip(wire)
                }
            }
            val unbekannt = nodes.map { it.opType }.filterNot { it in SUPPORTED_OPS }
            require(unbekannt.isEmpty()) { "unsupported ops $unbekannt" }
            val eingang = inputs.firstOrNull { it.first !in initializers }
                ?: error("model has no data input")
            val breite = eingang.second.getOrNull(1)?.takeIf { it > 0 }
                ?: error("input width unknown")
            val ausgang = outputs.firstOrNull() ?: error("model has no output")
            return OnnxModel(metadata, eingang.first, breite, ausgang, nodes, initializers)
        }

        private fun parseNode(bytes: ByteArray): Node {
            val r = Reader(bytes)
            val ins = mutableListOf<String>()
            val outs = mutableListOf<String>()
            var op = ""
            val attrs = LinkedHashMap<String, Attribute>()
            while (r.hasMore()) {
                val (field, wire) = r.tag()
                when {
                    field == 1 && wire == 2 -> ins += r.string()
                    field == 2 && wire == 2 -> outs += r.string()
                    field == 4 && wire == 2 -> op = r.string()
                    field == 5 && wire == 2 -> {
                        val a = Reader(r.bytes())
                        var name = ""
                        var f: Float? = null
                        var i: Long? = null
                        while (a.hasMore()) {
                            val (af, aw) = a.tag()
                            when {
                                af == 1 && aw == 2 -> name = a.string()
                                af == 2 && aw == 5 -> f = a.float32()
                                af == 3 && aw == 0 -> i = a.varint()
                                else -> a.skip(aw)
                            }
                        }
                        attrs[name] = Attribute(f, i)
                    }
                    else -> r.skip(wire)
                }
            }
            return Node(op, ins, outs, attrs)
        }

        private fun parseTensor(bytes: ByteArray): Pair<String, Tensor> {
            val r = Reader(bytes)
            val dims = mutableListOf<Int>()
            var type = 0L
            var name = ""
            var raw: ByteArray? = null
            val floats = mutableListOf<Float>()
            while (r.hasMore()) {
                val (field, wire) = r.tag()
                when {
                    field == 1 && wire == 0 -> dims += r.varint().toInt()
                    field == 1 && wire == 2 -> Reader(r.bytes()).let { p -> while (p.hasMore()) dims += p.varint().toInt() }
                    field == 2 && wire == 0 -> type = r.varint()
                    field == 4 && wire == 2 -> Reader(r.bytes()).let { p -> while (p.hasMore()) floats += p.float32() }
                    field == 4 && wire == 5 -> floats += r.float32()
                    field == 8 && wire == 2 -> name = r.string()
                    field == 9 && wire == 2 -> raw = r.bytes()
                    field == 14 && wire == 0 -> require(r.varint() == 0L) { "external data not supported" }
                    else -> r.skip(wire)
                }
            }
            require(type == FLOAT_TYPE) { "tensor $name is not float32" }
            val data = raw?.let { b ->
                val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
                FloatArray(b.size / 4) { buf.getFloat(it * 4) }
            } ?: floats.toFloatArray()
            val shape = dims.toIntArray()
            require(data.size == shape.fold(1) { acc, d -> acc * d }) { "tensor $name size mismatch" }
            return name to Tensor(shape, data)
        }

        private fun parseValueInfo(bytes: ByteArray): Pair<String, IntArray> {
            val r = Reader(bytes)
            var name = ""
            var dims = IntArray(0)
            while (r.hasMore()) {
                val (field, wire) = r.tag()
                when {
                    field == 1 && wire == 2 -> name = r.string()
                    field == 2 && wire == 2 -> dims = shapeOfType(r.bytes())
                    else -> r.skip(wire)
                }
            }
            return name to dims
        }

        /** TypeProto -> tensor_type -> shape -> dim; unbekannte Dimensionen werden -1. */
        private fun shapeOfType(bytes: ByteArray): IntArray {
            val type = Reader(bytes)
            while (type.hasMore()) {
                val (f, w) = type.tag()
                if (f == 1 && w == 2) {
                    val tensor = Reader(type.bytes())
                    while (tensor.hasMore()) {
                        val (tf, tw) = tensor.tag()
                        if (tf == 2 && tw == 2) {
                            val shape = Reader(tensor.bytes())
                            val dims = mutableListOf<Int>()
                            while (shape.hasMore()) {
                                val (sf, sw) = shape.tag()
                                if (sf == 1 && sw == 2) {
                                    val dim = Reader(shape.bytes())
                                    var value = -1
                                    while (dim.hasMore()) {
                                        val (df, dw) = dim.tag()
                                        if (df == 1 && dw == 0) value = dim.varint().toInt() else dim.skip(dw)
                                    }
                                    dims += value
                                } else {
                                    shape.skip(sw)
                                }
                            }
                            return dims.toIntArray()
                        } else {
                            tensor.skip(tw)
                        }
                    }
                } else {
                    type.skip(w)
                }
            }
            return IntArray(0)
        }

        private const val FLOAT_TYPE = 1L
    }

    /** Das Protobuf-Grundformat: Varints, feste Breiten, laengenpraefixierte Felder. */
    private class Reader(private val buf: ByteArray) {
        private var pos = 0

        fun hasMore(): Boolean = pos < buf.size

        fun tag(): Pair<Int, Int> {
            val key = varint()
            return (key ushr 3).toInt() to (key and 7L).toInt()
        }

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                require(pos < buf.size) { "truncated varint" }
                val b = buf[pos++].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
                require(shift < 64) { "varint too long" }
            }
        }

        fun bytes(): ByteArray {
            val len = varint().toInt()
            require(len >= 0 && pos + len <= buf.size) { "truncated field" }
            return buf.copyOfRange(pos, pos + len).also { pos += len }
        }

        fun string(): String = String(bytes(), Charsets.UTF_8)

        fun float32(): Float {
            require(pos + 4 <= buf.size) { "truncated float" }
            val v = ByteBuffer.wrap(buf, pos, 4).order(ByteOrder.LITTLE_ENDIAN).float
            pos += 4
            return v
        }

        fun skip(wire: Int) {
            when (wire) {
                0 -> varint()
                1 -> pos += 8
                2 -> bytes()
                5 -> pos += 4
                else -> error("unsupported wire type $wire")
            }
            require(pos <= buf.size) { "truncated field" }
        }
    }
}
