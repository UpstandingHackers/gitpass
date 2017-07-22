package com.upstandinghackers.syncpass.util.bencode

import com.upstandinghackers.syncpass.util.InvalidStateException

internal enum class State(val endOk: Boolean, val complete: Boolean = false) {
    NONE(false, true), // sink state
    SINGLE(false),
    ANY(true),
    ANY_TOPLEVEL(false, true),
    PAIR_KEY(true),
    PAIR_VALUE(false);

    fun next(): State {
        return when (this) {
            NONE -> throw InvalidStateException("Single element expected")
            SINGLE -> NONE
            ANY -> ANY
            ANY_TOPLEVEL -> ANY_TOPLEVEL
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
            ANY_TOPLEVEL -> true
            SINGLE -> true
            PAIR_VALUE -> true
            PAIR_KEY -> type == Bdecode.TokenType.STRING
        }
    }
}