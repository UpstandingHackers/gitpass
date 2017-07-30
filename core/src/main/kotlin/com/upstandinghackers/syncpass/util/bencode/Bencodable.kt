package com.upstandinghackers.syncpass.util.bencode

interface Bencodable {
    fun encode(encoder: Bencode)
}

data class BInt(val v: Long) : Bencodable {
    override fun encode(encoder: Bencode) {
        encoder.integer(v)
    }
}

data class BList(val v: MutableList<Bencodable>): Bencodable, MutableList<Bencodable> by v {
    constructor(): this(ArrayList())

    override fun encode(encoder: Bencode) {
        encoder.list(v)
    }
}

data class BMap(val v: MutableMap<String, Bencodable>): Bencodable, MutableMap<String, Bencodable> by v {
    constructor(): this(HashMap())
    override fun encode(encoder: Bencode) {
        encoder.dict {
            v.entries.sortedBy { entry -> entry.key }.forEach { entry ->
                pair(entry.key, entry.value)
            }
        }
    }
}

data class BBytes(val array: ByteArray): Bencodable {
    override fun encode(encoder: Bencode) {
        encoder.bytes(array)
    }

    override fun equals(other: Any?)
            = other is BBytes && other.array.contentEquals(array)

    override fun hashCode(): Int
            = array.contentHashCode()

    fun get(index: Int): Byte = array[index]
    fun set(index: Int, v: Byte) {
        array[index] = v
    }
}

data class BString(val s: String): Bencodable {
    override fun encode(encoder: Bencode) {
        encoder.string(s)
    }
}