package com.upstandinghackers.syncpass.util.bencode

import com.upstandinghackers.syncpass.util.InvalidStateException
import java.util.*

internal class StateMachine(initialState: State = State.SINGLE) {
    private val depth: Stack<State> = Stack<State>().apply {
        push(initialState)
    }

    val isComplete: Boolean
        get() = depth.size == 1 && depth.peek().complete

    fun step(type: Bdecode.TokenType) {
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

    fun push(state: State) {
        depth.push(state)
    }

    fun replace(state: State) {
        depth.pop()
        depth.push(state)
    }
}