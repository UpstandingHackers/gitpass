package com.upstandinghackers.syncpass.crypto

import java.security.SecureRandom

object Random {
    val random = SecureRandom()
    fun getBytes(ret: ByteArray) {
        random.nextBytes(ret)

    }
}