package com.upstandinghackers.syncpass.model

import com.upstandinghackers.syncpass.core.Repository
import com.upstandinghackers.syncpass.util.bencode.BMap
import com.upstandinghackers.syncpass.util.bencode.Bencodable
import com.upstandinghackers.syncpass.util.bencode.Bencode
import java.util.*

open class Document(protected val repo: Repository, val uuid: UUID = UUID.randomUUID()) {
    private val properties: MutableMap<String, Bencodable>
    private var savedProperties: Map<String, Bencodable> = mapOf()
    private var head: ByteArray?

    init {
        properties = TreeMap<String, Bencodable>()
        savedProperties = mapOf()
        head = null
    }

    fun applyChange(change: Change) {
        when (change) {
            is Change.Mod -> {
                if (properties[change.key] == change.oldValue) {
                    properties[change.key] = change.newValue
                } else {
                    throw InvalidChangeException()
                }
            }
            is Change.Add -> {
                if (change.key in properties && properties[change.key] != change.value) {
                    throw InvalidChangeException("Key ${change.key} added twice")
                } else {
                    properties[change.key] = change.value
                }
            }
            is Change.Del -> {
                if (change.key !in properties) {
                    throw InvalidChangeException("Unable to remove missing key  ${change.key}")
                }
            }
        }
    }

    fun snapshot(): Bencodable = BMap(properties)

    fun snapshot(encoder: Bencode) {
        encoder.obj(properties)
    }
}

