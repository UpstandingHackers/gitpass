package com.upstandinghackers.syncpass.util

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.*

class Bencode(val outputStream: ByteArrayOutputStream, val charset: Charset = Charsets.UTF_8) {
    companion object {
        fun encode(charset: Charset = Charsets.UTF_8, closure: Bencode.() -> Unit): ByteArray {
            val os = ByteArrayOutputStream()
            val encoder = Bencode(os, charset)
            closure.invoke(encoder)
            return os.toByteArray()
        }

        fun bytes(data: ByteArray) = encode { bytes(data) }
    }

    private enum class State(val endOk: Boolean) {
        NONE(false), // sink state
        SINGLE(false),
        ANY(true),
        PAIR_KEY(true),
        PAIR_VALUE(false);

        fun next(): State {
            return when (this) {
                NONE -> throw InvalidStateException("Single element expected")
                SINGLE -> NONE
                ANY -> ANY
                PAIR_KEY -> PAIR_VALUE
                PAIR_VALUE -> PAIR_KEY
            }
        }

        fun validateType(type: Bdecode.TokenType): Boolean {
            return if (type == Bdecode.TokenType.END) {
                endOk
            } else when (this) {
                NONE -> false
                ANY -> true
                SINGLE -> true
                PAIR_VALUE -> true
                PAIR_KEY -> type == Bdecode.TokenType.STRING
            }
        }
    }

    private val depth: Stack<State> = Stack()

    private fun stepStateMachine(type: Bdecode.TokenType) {
        val curState = depth.peek()
        if (curState == null || curState == State.NONE) {
            throw InvalidStateException("No more items expected")
        }
        if (!curState.validateType(type)) {
            throw InvalidStateException("Unexpected $type")
        } else if (type == Bdecode.TokenType.END) {
            depth.pop()
            if (depth.empty()) {
                throw InvalidStateException("Popped too much; this should be unreachable")
            }
        }
        depth.push(depth.pop().next())
    }

    fun beginDict() {
        stepStateMachine(Bdecode.TokenType.DICT)
        outputStream.write('d'.toInt())
    }

    fun beginList() {
        stepStateMachine(Bdecode.TokenType.LIST)
        outputStream.write('l'.toInt())
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
        stepStateMachine(Bdecode.TokenType.END)
        outputStream.write('e'.toInt())
    }

    fun bytes(bytes: ByteArray) {
        stepStateMachine(Bdecode.TokenType.STRING)
        outputStream.write(bytes.size.toString(10).toByteArray())
        outputStream.write(':'.toInt())
        outputStream.write(bytes)
    }

    fun string(s: String): Unit {
        bytes(s.toByteArray(charset))
    }

    fun integer(i: Long) {
        stepStateMachine(Bdecode.TokenType.INT)
        outputStream.write("i${i}e".toByteArray())
    }

    fun obj(obj: Any?) {
        when (obj) {
            null -> throw IllegalArgumentException("null is unencodable")
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
            is String -> string(obj)
            is ByteArray -> bytes(obj)
            else -> throw IllegalArgumentException("Invalid type ${obj.javaClass}")
        }
    }
}


