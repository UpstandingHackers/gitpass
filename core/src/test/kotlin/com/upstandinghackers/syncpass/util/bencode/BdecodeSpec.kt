package com.upstandinghackers.syncpass.util.bencode

import com.upstandinghackers.syncpass.util.MalformedMessageException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import java.rmi.activation.ActivationDesc
import kotlin.test.*

object BdecodeSpec: Spek({

    fun givenString(str: String, body: SpecBody.(Bdecode) -> Unit) {
        given("The string \"$str\"") {
            val coder = Bdecode(str.toByteArray())
            afterEachTest { coder.reset() }
            body(coder)
        }
    }
    givenString("i10e") { coder ->
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
            it("should fail") {
                assertFalse(try {
                    val s = coder.bytes()
                    true
                } catch (_: MalformedMessageException) {
                    false
                })
            }
        }
    }

    givenString("d1:c4:test1:nli1ei2eee") { coder ->
        on("Reading an object") {
            val obj = coder.obj({ba -> String(ba)}) as HashMap<String, Any>
            xit("should return {c':'teste', 'n':[1,2]}") {

            }
        }
    }
})