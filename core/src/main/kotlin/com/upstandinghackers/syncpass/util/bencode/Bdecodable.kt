package com.upstandinghackers.syncpass.util.bencode

interface Bdecodable {
    fun decode(decoder: Bdecode): Bencodable
}