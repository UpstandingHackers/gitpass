package com.upstandinghackers.syncpass.core

import io.atlassian.fugue.Option
import org.eclipse.jgit.lib.Repository

class Repository(val gitRepository: Repository) {
    fun auth(user: String, password: String) : Option<Reference> {
        TODO("Open an existing root reference")
    }

    fun save(user: String, password: String, root: Reference) {
        TODO()
    }

    fun getReference(key: Key) {
        TODO()
    }
}