package com.upstandinghackers.syncpass.model

import com.upstandinghackers.syncpass.util.MalformedMessageException
import com.upstandinghackers.syncpass.util.bencode.Bdecode
import com.upstandinghackers.syncpass.util.bencode.Bencodable
import io.atlassian.fugue.Either

interface PropertySpec<T: Bencodable> {
    fun decode(decoder: Bdecode): T
}

abstract class DocumentSchema {
    abstract fun decodeProperty(key: String, decoder: Bdecode): Bencodable
}