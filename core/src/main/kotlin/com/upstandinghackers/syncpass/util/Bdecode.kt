package com.upstandinghackers.syncpass.util

import io.atlassian.fugue.Option

class Bdecode(val data: ByteArray) {
    var pos = 0

    enum class TokenType(signifier: Char) {
        INT('i'),
        STRING('s'),
        LIST('l'),
        DICT('d'),
        END('e'),
    }

    fun peekTokenType(): Option<TokenType> {
        if (pos >= data.size) {
            return Option.none()
        }
        val type = when (data[pos].toChar()) {
            'i' -> TokenType.INT
            'l' -> TokenType.LIST
            'd' -> TokenType.DICT
            'e' -> TokenType.END
            in '0'..'9' -> TokenType.STRING
            else -> throw MalformedMessageException()
        }
        return Option.some(type)
    }

    private fun peekByte(): Byte = data[pos]
    private fun nextByte(): Byte = data[pos++]
    private fun getByteString(len: Int): ByteArray {
        val ret = data.copyOfRange(pos, pos+len)
        pos += len
        return ret
    }

    fun nextInt(): Long {
        if (nextByte().toChar() != 'i') {
            throw MalformedMessageException("Expected int")
        }
        return internalNextInt('e', true)
    }

    fun nextString(): ByteArray {
        var lengthLong = internalNextInt(':', false)
        val length = when (lengthLong) {
            in 0..Int.MAX_VALUE.toLong() -> lengthLong.toInt()
            else -> throw MalformedMessageException("Byte string length too long")
        }
        return getByteString(length)
    }

    private fun internalNextInt(term: Char = 'e', allowNegative: Boolean = true): Long {
        val termByte = term.toByte()
        var charsRead = 0
        var ret: Long = 0
        var negative: Long = 1
        if (peekByte().toChar() == '-') {
            if (!allowNegative) {
                throw MalformedMessageException("Negative integer not allowed here")
            }
            negative = -1
            pos++
        }

        // parse the number...
        while (true) {
            val nc = nextByte().toChar()
            if (nc.isDigit()) {
                if (nc == '0' && charsRead == 0) {
                    if (negative < 0) {
                        throw MalformedMessageException("Negative zero is not allowed")
                    }
                    if (nextByte() != termByte) {
                        throw MalformedMessageException("Initial zero is not allowed")
                    } else {
                        return 0
                    }
                }
                charsRead++
                ret = Math.addExact(
                        Math.multiplyExact(ret, 10),
                        negative * (nc - '0').toLong())
            } else if (nc == term) {
                if (charsRead == 0) {
                    throw MalformedMessageException("Missing integer literal")
                } else {
                    return ret
                }
            } else {
                throw MalformedMessageException("Unexpected character in integer")
            }
        }
    }


}

