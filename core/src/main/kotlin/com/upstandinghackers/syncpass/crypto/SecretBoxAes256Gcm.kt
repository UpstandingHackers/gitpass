package com.upstandinghackers.syncpass.crypto

import com.upstandinghackers.syncpass.util.MalformedMessageException
import com.upstandinghackers.syncpass.util.bencode.Bdecode
import com.upstandinghackers.syncpass.util.bencode.Bencode
import io.atlassian.fugue.Option
import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter

object SecretBoxAes256Gcm : SecretBox {
    const val NONCE_LEN = 12
    const val KEY_LEN = 32
    const val TAG_LEN = 128

    private fun genNonce(): ByteArray {
        val nonce = ByteArray(NONCE_LEN)
        Random.getBytes(nonce)
        return nonce
    }

    override fun encrypt(key: ByteArray, msg: ByteArray, ad: ByteArray): ByteArray {
        assert(key.size == KEY_LEN)
        val cipher = GCMBlockCipher(AESEngine())
        val nonce = genNonce()
        cipher.init(true, AEADParameters(KeyParameter(key), TAG_LEN, nonce, ad))
        val ciphertext = ByteArray(cipher.getOutputSize(msg.size))
        var off = cipher.processBytes(msg, 0, msg.size, ciphertext, 0)
        off += cipher.doFinal(ciphertext, off)
        if (off != ciphertext.size) {
            throw Exception("Ciphertext size misestimation")
        }
        return Bencode.dict {
            pair("n", nonce)
            pair("c", ciphertext)
        }
    }

    override fun decrypt(key: ByteArray, ciphertext: ByteArray, ad: ByteArray): Option<ByteArray> {
        var cp: ByteArray? = null
        var np: ByteArray? = null
        Bdecode(ciphertext).stringDict { k ->
            when (k) {
                "c" -> cp = bytes()
                "n" -> np = bytes()
                else -> reportError("Unexpected key $k")
            }
        }

        val c = cp ?: throw MalformedMessageException("Missing ciphertext")
        val n = np ?: throw MalformedMessageException("Missing nonce")


        val cipher = GCMBlockCipher(AESEngine())
        cipher.init(false, AEADParameters(KeyParameter(key), TAG_LEN, n, ad))
        try {
            val plaintext = ByteArray(cipher.getOutputSize(c.size))
            var off = cipher.processBytes(c, 0, c.size, plaintext, 0)
            off += cipher.doFinal(plaintext, off)
            if (off != plaintext.size) {
                throw Exception("Plaintext size misestimation")
            }
            return Option.some(plaintext)
        } catch (ex: CryptoException) {
            return Option.none()
        }
    }
}