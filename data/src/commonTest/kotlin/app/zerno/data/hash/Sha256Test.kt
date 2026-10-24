package app.zerno.data.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256Test {
    private fun sha256(text: String) = Sha256().apply { update(text.encodeToByteArray()) }.hex()

    @Test
    fun knownDigests() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256(""))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc"))
        // Two blocks: the padding does not fit after 56 bytes.
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            sha256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }

    @Test
    fun chunksGiveTheSameDigestAsOnePiece() {
        val bytes = ByteArray(1000) { (it * 7).toByte() }
        val whole = Sha256().apply { update(bytes) }.hex()
        val chunked = Sha256().apply {
            var offset = 0
            for (size in listOf(1, 63, 64, 65, 300, 507)) {
                update(bytes, offset, size)
                offset += size
            }
        }.hex()
        assertEquals(whole, chunked)
    }
}
