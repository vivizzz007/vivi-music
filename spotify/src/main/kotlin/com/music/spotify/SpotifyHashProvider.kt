package com.music.spotify

import java.util.concurrent.ConcurrentHashMap

object SpotifyHashProvider {

    enum class HashSource { HARDCODED, CACHED, REMOTE }

    data class GqlHashEntry(
        val hash: String,
        val previousHash: String? = null,
        val source: HashSource = HashSource.HARDCODED,
    )

    private val hashes = ConcurrentHashMap<String, GqlHashEntry>()

    init {
        loadHardcodedDefaults()
    }

    private fun loadHardcodedDefaults() {
        val defaults = mapOf(
            "profileAttributes" to "53bcb064f6cd18c23f752bc324a791194d20df612d8e1239c735144ab0399ced",
            "libraryV3" to "973e511ca44261fda7eebac8b653155e7caee3675abb4fb110cc1b8c78b091c3",
            "fetchPlaylist" to "346811f856fb0b7e4f6c59f8ebea78dd081c6e2fb01b77c954b26259d5fc6763",
            "fetchLibraryTracks" to "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240"
        )
        defaults.forEach { (op, hash) ->
            hashes[op] = GqlHashEntry(hash = hash, source = HashSource.HARDCODED)
        }
    }

    fun getHash(operationName: String): String =
        hashes[operationName]?.hash
            ?: error("No hash registered for GQL operation: $operationName")

    fun getPreviousHash(operationName: String): String? =
        hashes[operationName]?.previousHash

    fun updateHashes(
        remoteHashes: Map<String, RemoteHashEntry>,
        source: HashSource,
    ): UpdateResult {
        var updated = 0
        var unchanged = 0
        remoteHashes.forEach { (op, remote) ->
            val current = hashes[op]
            if (current != null) {
                val hashChanged = current.hash != remote.hash
                if (hashChanged) updated++ else unchanged++
                hashes[op] = GqlHashEntry(
                    hash = remote.hash,
                    previousHash = remote.previousHash,
                    source = source,
                )
            }
        }
        return UpdateResult(updated = updated, unchanged = unchanged)
    }

    data class UpdateResult(val updated: Int, val unchanged: Int)

    fun getAll(): Map<String, GqlHashEntry> = hashes.toMap()

    data class RemoteHashEntry(
        val hash: String,
        val previousHash: String? = null,
    )
}
