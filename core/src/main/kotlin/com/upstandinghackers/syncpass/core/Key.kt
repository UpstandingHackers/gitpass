package com.upstandinghackers.syncpass.core

import com.upstandinghackers.syncpass.crypto.SecretBox
import com.upstandinghackers.syncpass.crypto.SecretBoxAes256Gcm
import java.security.SecureRandom

class Key(private val keyData: ByteArray) {
    companion object {
    }

    fun encrypt(data: ByteArray): ByteArray {
        return SecretBoxAes256Gcm.encrypt(keyData, data, byteArrayOf())
    }

    fun decrypt(data: ByteArray): ByteArray {
        TODO()
    }
}
