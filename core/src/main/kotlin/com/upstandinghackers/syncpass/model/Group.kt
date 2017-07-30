package com.upstandinghackers.syncpass.model;

import com.upstandinghackers.syncpass.util.MalformedMessageException
import com.upstandinghackers.syncpass.util.bencode.*
import java.util.HashMap;
import java.util.UUID;

data class GroupMember(val name: String, val key: ByteArray, val extra: HashMap<String, Bencodable>): Bencodable {

    companion object : Bdecodable {
        override fun decode(decoder: Bdecode): GroupMember {
            var key: ByteArray? = null
            var name: String? = null
            val extra: HashMap<String, Bencodable> = HashMap()
            decoder.stringDict { k ->
                when(k) {
                    "k" -> key = bytes()
                    "n" -> name = string()
                    else -> extra.put(k, any())
                }
            }

            return GroupMember(
                    name = name ?: throw MalformedMessageException("Missing name"),
                    key = key ?: throw MalformedMessageException("Missing key"),
                    extra = extra
            )
        }
    }

    override fun encode(encoder: Bencode) {
        encoder.obj(HashMap(extra).apply {
            put("k", BBytes(key))
            put("n", BString(toString()))
        })
    }

    override fun equals(other: Any?): Boolean {
        if (other !is GroupMember) {
            return false
        }
        return other.name == name && other.key.contentEquals(key) && other.extra == extra
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

class Group: Document(null) {
    companion object Schema : DocumentSchema() {
        override fun decodeProperty(key: String, decoder: Bdecode): Bencodable {
            val uuid = try {
                UUID.fromString(key)
            } catch (ex: IllegalArgumentException) {
                throw MalformedMessageException("Invalid UUID", ex)
            }
            return GroupMember.decode(decoder)
        }

    }
    var name: String = ""
    val members: HashMap<UUID, GroupMember> = HashMap()



}
