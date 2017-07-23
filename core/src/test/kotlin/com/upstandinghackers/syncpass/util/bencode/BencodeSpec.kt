package com.upstandinghackers.syncpass.util.bencode

import io.kotlintest.matchers.Matcher
import io.kotlintest.matchers.Result
import io.kotlintest.matchers.should
import io.kotlintest.specs.FunSpec
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

class BencodeSpec: FunSpec({
    fun encodeTo(expected: String): Matcher<ByteArray> = object : Matcher<ByteArray> {
        fun encode(ba: ByteArray): String {
            val o = StringBuilder()
            for (byte in ba) {
                when (byte.toChar()) {
                    '%' -> o.append("%25")
                    in ' '..'~' -> o.append(byte.toChar())
                    else -> o.append(String.format("%%%02x", byte))
                }
            }
            return o.toString()
        }

        override fun test(value: ByteArray): Result {
            val encodedResult = encode(value)
            return Result(
                    encodedResult == expected,
                    "Result string $encodedResult should be $expected"
            )
        }
    }

    fun testEncode(description: String, expected: String, body: (ByteArrayOutputStream) -> Unit) {
        test(description) {
            val os = ByteArrayOutputStream(expected.length)
            body(os)
            os.toByteArray() should encodeTo("foo")
        }
    }


    test("10 encodes to i10e") {
        Bencode.integer(10) should encodeTo("i10e")
        Bencode.obj(10) should encodeTo("i10e")
    }

    test("{c':'test', 'n':[1,2]} using dsl") {
        Bencode.dict {
            pair("c", "test")
            pair("n") {
                list {
                    integer(1)
                    integer(2)
                }
            }
        } should encodeTo("d1:c4:test1:nli1ei2eee")
    }

    test("{c':'test', 'n':[1,2]} using dsl with shortcuts") {
        Bencode.dict {
            pair("c") { string("test") }
            pair("n") {
                list(1, 2)
            }
        } should encodeTo("d1:c4:test1:nli1ei2eee")
    }

    test("{c':'test', 'n':[1,2]} using low-level calls") {
        Bencode.encode {
            beginDict()
            string("c")
            string("test")
            string("n")
            beginList()
            integer(1)
            integer(2)
            end()
            end()
        } should encodeTo("d1:c4:test1:nli1ei2eee")
    }

    test("{c':'test', 'n':[1,2]} using object") {
        Bencode.obj(mapOf(
                "c" to "test",
                "n" to listOf(1,2)
        )) should encodeTo("d1:c4:test1:nli1ei2eee")
    }

})