package com.upstandinghackers.syncpass.core

import org.abstractj.kalium.crypto.SecretBox

class Key {
    val box: SecretBox

    constructor(keyData: ByteArray) {
        box = SecretBox(keyData)
    }

    fun encrypt(data: ByteArray): ByteArray {
        TODO()
    }

    fun decrypt(data: ByteArray): ByteArray {
        TODO()
    }
}
