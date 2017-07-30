package com.upstandinghackers.syncpass.core

import com.upstandinghackers.syncpass.util.bencode.Bdecode
import io.atlassian.fugue.Option
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.util.sha1.SHA1
import java.util.*

class Repository(val gitRepository: Repository) {
    fun auth(user: String, password: String) : Option<Reference> {
        TODO("Open an existing root reference")
    }

    fun save(user: String, password: String, root: Reference) {
        TODO()
    }

    fun loadObject(uuid: UUID, key: Key) {
        TODO()
    }

    fun loadFragment(hash: ObjectId): Bdecode {
        val loader = gitRepository.open(hash, Constants.OBJ_BLOB)
        return Bdecode(loader.cachedBytes)
    }

    fun gitHash(content: ByteArray, type: String): ObjectId {
        val hasher = SHA1.newInstance()
        hasher.update("blob ${content.size}\u0000".toByteArray())
        hasher.update(content)
        return hasher.toObjectId()
    }
}