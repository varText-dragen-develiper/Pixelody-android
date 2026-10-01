package com.pixelody.app.data.model

/**
 * Portable deterministic traversal for standalone Android sessions. It shares
 * the desktop seed hash, Mulberry32 stream, and Fisher-Yates order; local
 * taste signals intentionally remain on their owning device.
 */
object FlowShufflePortable {
    private const val fallbackSeed = 0x6d2b79f5.toInt()

    fun standardOrder(queueIds: List<String>, currentId: String?, seed: String): List<String> {
        val unique = queueIds.filter { it.isNotBlank() }.distinct()
        val current = currentId?.takeIf { unique.contains(it) }
        val tail = unique.filter { it != current }.toMutableList()
        val random = seededRandom(seed)
        for (index in tail.lastIndex downTo 1) {
            val swap = (random() * (index + 1)).toInt()
            val item = tail[index]
            tail[index] = tail[swap]
            tail[swap] = item
        }
        return if (current == null) tail else listOf(current) + tail
    }

    fun next(plan: FlowShufflePlan): String? {
        val forward = plan.order.getOrNull(plan.cursor + 1)
        return forward ?: plan.future.firstOrNull()
    }

    private fun seededRandom(seed: String): () -> Double {
        var value = hashSeed(seed).takeIf { it != 0 } ?: fallbackSeed
        return {
            value += fallbackSeed
            var result = value
            result = (result xor (result ushr 15)) * (result or 1)
            result = result xor (result + ((result xor (result ushr 7)) * (result or 61)))
            ((result xor (result ushr 14)).toLong() and 0xffffffffL).toDouble() / 4294967296.0
        }
    }

    private fun hashSeed(seed: String): Int {
        var hash = 0x811c9dc5.toInt()
        seed.forEach { character ->
            hash = hash xor character.code
            hash *= 0x01000193
        }
        return hash
    }
}
