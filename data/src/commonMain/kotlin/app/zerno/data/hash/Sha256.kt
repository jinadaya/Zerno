package app.zerno.data.hash

/**
 * Streaming SHA-256 (FIPS 180-4), to check downloaded Bible files against the checksum the backend publishes.
 * Kotlin has no common digest API and the files are hashed once per download, so a small pure implementation
 * beats a dependency. Feed it with [update], then read [hex] once.
 */
// The shifts and rotations are the standard's own numbers; naming each would not make them clearer.
@Suppress("MagicNumber")
class Sha256 {
    private val state = Initial.copyOf()
    private val block = ByteArray(BLOCK_SIZE)
    private val words = IntArray(ROUNDS)
    private var blockLength = 0
    private var totalBytes = 0L

    fun update(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        var index = offset
        val end = offset + length
        totalBytes += length
        while (index < end) {
            val count = minOf(BLOCK_SIZE - blockLength, end - index)
            bytes.copyInto(block, blockLength, index, index + count)
            blockLength += count
            index += count
            if (blockLength == BLOCK_SIZE) {
                compress()
                blockLength = 0
            }
        }
    }

    /** The digest as 64 lowercase hex characters, the format of the backend's `sha256`. */
    fun hex(): String {
        val bits = totalBytes * Byte.SIZE_BITS
        block[blockLength++] = PADDING_START
        if (blockLength > BLOCK_SIZE - Long.SIZE_BYTES) {
            block.fill(0, blockLength)
            compress()
            blockLength = 0
        }
        block.fill(0, blockLength)
        for (i in 0 until Long.SIZE_BYTES) {
            block[BLOCK_SIZE - 1 - i] = (bits ushr (i * Byte.SIZE_BITS)).toByte()
        }
        compress()
        return state.joinToString("") { it.toUInt().toString(HEX).padStart(Int.SIZE_BYTES * 2, '0') }
    }

    private fun compress() {
        for (t in 0 until BLOCK_WORDS) {
            val i = t * Int.SIZE_BYTES
            words[t] = (block[i].toInt() and BYTE_MASK shl 24) or (block[i + 1].toInt() and BYTE_MASK shl 16) or
                (block[i + 2].toInt() and BYTE_MASK shl 8) or (block[i + 3].toInt() and BYTE_MASK)
        }
        for (t in BLOCK_WORDS until ROUNDS) {
            val s0 = words[t - 15].rotateRight(7) xor words[t - 15].rotateRight(18) xor (words[t - 15] ushr 3)
            val s1 = words[t - 2].rotateRight(17) xor words[t - 2].rotateRight(19) xor (words[t - 2] ushr 10)
            words[t] = words[t - 16] + s0 + words[t - 7] + s1
        }
        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]
        var e = state[4]
        var f = state[5]
        var g = state[6]
        var h = state[7]
        for (t in 0 until ROUNDS) {
            val t1 = h + (e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)) +
                ((e and f) xor (e.inv() and g)) + K[t] + words[t]
            val t2 = (a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)) +
                ((a and b) xor (a and c) xor (b and c))
            h = g
            g = f
            f = e
            e = d + t1
            d = c
            c = b
            b = a
            a = t1 + t2
        }
        state[0] += a
        state[1] += b
        state[2] += c
        state[3] += d
        state[4] += e
        state[5] += f
        state[6] += g
        state[7] += h
    }

    private companion object {
        const val BLOCK_SIZE = 64
        const val BLOCK_WORDS = 16
        const val ROUNDS = 64
        const val BYTE_MASK = 0xff
        const val HEX = 16
        const val PADDING_START = 0x80.toByte()

        val Initial = ints(
            0x6a09e667,
            0xbb67ae85,
            0x3c6ef372,
            0xa54ff53a,
            0x510e527f,
            0x9b05688c,
            0x1f83d9ab,
            0x5be0cd19,
        )

        val K = ints(
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
        )

        /** The constants are written as in the standard; most do not fit a signed Int literal. */
        fun ints(vararg values: Long) = IntArray(values.size) { values[it].toInt() }
    }
}
