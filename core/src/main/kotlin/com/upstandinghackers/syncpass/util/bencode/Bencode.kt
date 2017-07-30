package com.upstandinghackers.syncpass.util.bencode

import com.upstandinghackers.syncpass.util.InvalidStateException
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class Bencode(val outputStream: ByteArrayOutputStream, val charset: Charset = Charsets.UTF_8) {
    private val stateMachine = StateMachine()

    companion object {
        fun encode(charset: Charset = Charsets.UTF_8, closure: Bencode.() -> Unit): ByteArray {
            val os = ByteArrayOutputStream()
            val encoder = Bencode(os, charset)
            closure(encoder)
            if (!encoder.isComplete) {
                throw InvalidStateException("Encoder has not finished encoding an object")
            }
            return os.toByteArray()
        }

        fun encodeMulti(charset: Charset = Charsets.UTF_8, closure: Bencode.() -> Unit): ByteArray {
            return encode {
                stateMachine.replace(State.ANY_TOPLEVEL)
                closure()
            }
        }

        fun dict(closure: Bencode.() -> Unit) = encode { dict(closure) }
        fun list(closure: Bencode.() -> Unit) = encode { list(closure) }
        fun bytes(data: ByteArray) = encode { bytes(data) }
        fun string(string: String) = encode { string(string) }
        fun integer(v: Long) = encode { integer(v) }
        fun obj(o: Any) = encode { obj(o) }
    }

    val isComplete
        get() = stateMachine.isComplete

    fun beginDict() {
        stateMachine.step(Bdecode.TokenType.DICT)
        outputStream.write('d'.toInt())
    }

    fun beginList() {
        stateMachine.step(Bdecode.TokenType.LIST)
        outputStream.write('l'.toInt())
    }

    fun list(vararg elements: Any) {
        list {
            elements.forEach(this::obj)
        }
    }

    fun list(closure: Bencode.() -> Unit) {
        beginList()
        closure.invoke(this)
        end()
    }

    fun dict(closure: Bencode.() -> Unit) {
        beginDict()
        closure.invoke(this)
        end()
    }

    fun end() {
        stateMachine.step(Bdecode.TokenType.END)
        outputStream.write('e'.toInt())
    }

    fun bytes(bytes: ByteArray) {
        stateMachine.step(Bdecode.TokenType.STRING)
        outputStream.write(bytes.size.toString(10).toByteArray())
        outputStream.write(':'.toInt())
        outputStream.write(bytes)
    }

    fun string(s: String): Unit {
        bytes(s.toByteArray(charset))
    }

    fun integer(i: Long) {
        stateMachine.step(Bdecode.TokenType.INT)
        outputStream.write("i${i}e".toByteArray())
    }

    fun pair(k: String, closure: Bencode.() -> Unit) {
        string(k)
        this.closure()
    }

    fun pair(k: String, v: Any) {
        string(k)
        obj(v)
    }

    fun obj(obj: Any?) {
        when (obj) {
            null -> throw IllegalArgumentException("null is unencodable")
            is Bencodable -> obj.encode(this)
            is List<*> -> {
                beginList()
                obj.forEach(this::obj)
                end()
            }
            is Map<*, *> -> {
                beginDict()
                obj.entries.sortedBy { it -> it.key as String }.forEach { elem ->
                    string(elem.key as String)
                    obj(elem.value)
                }
                end()
            }
            is Long -> integer(obj)
            is Int -> integer(obj.toLong())
            is Short -> integer(obj.toLong())
            is Byte -> integer(obj.toLong())
            is Char -> integer(obj.toLong())
            is String -> string(obj)
            is ByteArray -> bytes(obj)
            else -> throw IllegalArgumentException("Invalid type ${obj.javaClass}")
        }
    }

    fun raw(byteArray: ByteArray) {
        outputStream.write(byteArray)
        // If we can write anything, we can write a string
        stateMachine.step(Bdecode.TokenType.STRING)
    }
}


