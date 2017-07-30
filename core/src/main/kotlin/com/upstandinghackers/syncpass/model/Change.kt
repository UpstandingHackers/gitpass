package com.upstandinghackers.syncpass.model

import com.upstandinghackers.syncpass.util.MalformedMessageException
import com.upstandinghackers.syncpass.util.bencode.Bdecodable
import com.upstandinghackers.syncpass.util.bencode.Bdecode
import com.upstandinghackers.syncpass.util.bencode.Bencodable
import com.upstandinghackers.syncpass.util.bencode.Bencode

sealed class Change: Bencodable {
    companion object: Bdecodable {
        override fun decode(decoder: Bdecode): Bencodable {
            var old: Bencodable? = null
            var new: Bencodable? = null
            var key: String? = null
            decoder.stringDict { k ->
                when (k) {
                    "-" -> old = any()
                    "+" -> new = any()
                    "k" -> key = string()
                }
            }

            return if (key == null) {
                throw MalformedMessageException("Key required")
            } else if (old == null && new == null) {
                throw MalformedMessageException("No change applied to key")
            } else if (old == null && new != null) {
                Add(key!!, new!!)
            } else if (old != null && new == null) {
                Del(key!!, old!!)
            } else {
                Mod(key!!, old!!, new!!)
            }
        }
    }

    data class Add(val key: String, val value: Bencodable): Change() {
        override fun encode(encoder: Bencode) {
            encoder.dict {
                pair("+", value)
                pair("k", key)
            }
        }
    }
    data class Del(val key: String, val value: Bencodable): Change() {
        override fun encode(encoder: Bencode) {
            encoder.dict {
                pair("-", value)
                pair("k", key)
            }
        }
    }
    data class Mod(val key: String, val oldValue: Bencodable, val newValue: Bencodable): Change() {
        override fun encode(encoder: Bencode) {
            encoder.dict {
                pair("+", newValue)
                pair("-", oldValue)
                pair("k", key)
            }
        }
    }
}
