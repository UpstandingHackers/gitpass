package com.upstandinghackers.syncpass.crypto

import io.atlassian.fugue.Option

interface SecretBox {

    fun encrypt(key: ByteArray, msg: ByteArray, ad: ByteArray): ByteArray
    fun decrypt(key: ByteArray, ciphertext: ByteArray, ad: ByteArray): Option<ByteArray>
}