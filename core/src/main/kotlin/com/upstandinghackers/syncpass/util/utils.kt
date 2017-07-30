package com.upstandinghackers.syncpass.util

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction

fun CharsetDecoder.decode(byteArray: ByteArray): String
        = decode(ByteBuffer.wrap(byteArray)).toString()

fun Charset.decode(byteArray: ByteArray): String
        = newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(byteArray)