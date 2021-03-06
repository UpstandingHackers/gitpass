package com.upstandinghackers.syncpass.util.bencode

import com.upstandinghackers.syncpass.util.MalformedMessageException
import com.upstandinghackers.syncpass.util.decode
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction

class Bdecode(val data: ByteArray, charset: Charset = Charsets.UTF_8) {
    private var pos = 0
    private val decoder: CharsetDecoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

    enum class TokenType(val signifier: Char, val singleByte: Boolean) {
        INT('i', false),
        STRING('s', false),
        LIST('l', true),
        DICT('d', true),
        END('e', true),
    }

    val isAtEof: Boolean get() = pos == data.size

    fun peekTokenType(): TokenType {
        if (pos >= data.size) {
            reportError("Unexpected EOF")
        }
        return when (data[pos].toChar()) {
            'i' -> TokenType.INT
            'l' -> TokenType.LIST
            'd' -> TokenType.DICT
            'e' -> TokenType.END
            in '0'..'9' -> TokenType.STRING
            else -> reportError("Invalid token ${data[pos].toChar()}")
        }
    }

    private fun peekByte(): Byte = data[pos]
    private fun nextByte(): Byte = data[pos++]
    private fun getByteString(len: Int): ByteArray {
        val ret = data.copyOfRange(pos, pos + len)
        pos += len
        return ret
    }

    fun reportError(msg: String): Nothing {
        throw MalformedMessageException("$msg at position $pos")
    }

    fun integer(): Long {
        if (nextByte().toChar() != 'i') {
            reportError("Expected int")
        }
        return internalNextInt('e', true)
    }

    fun bytes(): ByteArray {
        if (peekByte().toChar() !in '0'..'9') {
            reportError("Expected bytestring")
        }
        var lengthLong = internalNextInt(':', false)
        val length = when (lengthLong) {
            in 0..Int.MAX_VALUE.toLong() -> lengthLong.toInt()
            else -> reportError("Byte string length too long")
        }
        return getByteString(length)
    }

    fun decodeString(byteArray: ByteArray): String {
        try {
            return decoder.decode(byteArray)
        } catch (ex: CharacterCodingException) {
            throw MalformedMessageException("Invalid character", ex)
        }
    }


    /**
     * Decode a string in this decoder's charset
     */
    fun string(): String {
        return decodeString(bytes())
    }

    private fun internalNextInt(term: Char = 'e', allowNegative: Boolean = true): Long {
        val termByte = term.toByte()
        var charsRead = 0
        var ret: Long = 0
        var negative: Long = 1
        if (peekByte().toChar() == '-') {
            if (!allowNegative) {
                reportError("Negative integer not allowed here")
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
                        reportError("Negative zero is not allowed")
                    }
                    if (nextByte() != termByte) {
                        reportError("Initial zero is not allowed")
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
                    reportError("Missing integer literal")
                } else {
                    return ret
                }
            } else {
                reportError("Unexpected character in integer")
            }
        }
    }

    /**
     * Parse a dict from the stream, calling closure with each key.
     * Closure must consume exactly one object: the value
     */
    fun dict(closure: Bdecode.(ByteArray) -> Unit) {
        consumeToken(TokenType.DICT)
        while (peekTokenType() != TokenType.END) {
            if (peekTokenType() != TokenType.STRING) {
                reportError("dict keys must be strings")
            }
            val nextKey = bytes()
            if (peekTokenType() == TokenType.END) {
                reportError("Missing value")
            }
            closure(nextKey)
        }
        consumeToken(TokenType.END)
    }

    fun stringDict(closure: Bdecode.(String) -> Unit) {
        dict { k ->
            closure(decodeString(k))
        }
    }

    /**
     * Parse a list from the stream. Closure is called for each object
     */
    fun list(closure: Bdecode.() -> Unit) {
        consumeToken(TokenType.LIST)
        while (peekTokenType() != TokenType.END) {
            closure()
        }
        consumeToken(TokenType.END)
    }

    private fun consumeToken(token: TokenType) {
        if (!token.singleByte) {
            throw IllegalArgumentException("Can only consume single byte tokens")
        }
        if (peekTokenType() != token) {
            throw MalformedMessageException("Expected $token")
        }
        nextByte()
    }

    fun consumeAny(): Unit = dispatch(
            integer = {},
            bytes = {},
            dictStart = {},
            dict = { _, _ -> consumeAny() },
            listStart = {},
            list = { _ -> consumeAny() },
            eof = {}
    )

    fun any(): Bencodable = dispatch(
            integer = { l -> BInt(l) },
            bytes = { b -> BBytes(b) },
            dictStart = { BMap() },
            dict = { d, k -> d.put(decodeString(k), any()); d },
            listStart = { BList() },
            list = { l -> l.add(any()); l }
    )

    fun <R, L : R, D : R> dispatch(integer: (Long) -> R = { v -> reportError("Unexpected integer $v") },
                                   bytes: (ByteArray) -> R = { b -> reportError("Unexpected bytes") },
                                   dictStart: () -> D = { this.reportError("Unpexpected dict") },
                                   dict: Bdecode.(D, ByteArray) -> D = { d, k -> d },
                                   listStart: () -> L = { this.reportError("Unexpected list") },
                                   list: Bdecode.(L) -> L = { l -> l },
                                   eof: Bdecode.() -> R = { this.reportError("Unexpected EOF") }): R {
        return if (isAtEof) {
            eof()
        } else when (peekTokenType()) {
            TokenType.END -> reportError("Unexpected end")
            TokenType.INT -> integer(integer())
            TokenType.STRING -> bytes(bytes())
            TokenType.LIST -> {
                var v = listStart()
                list { v = list(v) }
                v
            }
            TokenType.DICT -> {
                var d = dictStart()
                dict { k -> d = dict(d, k) }
                d
            }
        }
    }

    fun raw(closure: Bdecode.() -> Unit): ByteArray {
        val p = pos
        closure()
        return data.copyOfRange(p, pos)
    }

    fun raw(): ByteArray = raw { consumeAny() }

    internal fun reset() {
        pos = 0
    }
}

annotation class Deserializer