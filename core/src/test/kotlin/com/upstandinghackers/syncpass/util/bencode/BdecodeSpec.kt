package com.upstandinghackers.syncpass.util.bencode

import com.upstandinghackers.syncpass.util.MalformedMessageException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.*

object BdecodeSpec: Spek({
    given("The string \"i10e\"") {
        val coder = Bdecode("i10e".toByteArray())

        afterEachTest { coder.reset() }

        on("reading an integer") {
            val i = coder.integer()
            it("should return 10") {
                assertEquals(i, 10)
            }
        }

        on("reading an object") {
            val i = coder.obj { key -> key}
            it("should be a long") {
                assert(i is Long)
            }
            it("should have value 10") {
                assertEquals(i as Long, 10)
            }
        }

        on("reading a string") {
            val succeeded = try {
                val s = coder.bytes()
                true
            } catch (_: MalformedMessageException) {
                false
            }

            it("should fail") {
                assertFalse(succeeded)
            }
        }
    }

})