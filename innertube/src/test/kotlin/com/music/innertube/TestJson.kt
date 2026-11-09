/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube

import com.music.innertube.models.response.NextResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class TestJson {
    @Test
    fun testParse() {
        val file = File("../ytmusic_next_response.json")
        assumeTrue("Fixture file not present, skipping", file.exists())

        val json = Json { ignoreUnknownKeys = true }
        val response = json.decodeFromString<NextResponse>(file.readText())

        val mq = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(0)?.tabRenderer?.content?.musicQueueRenderer
        assertNotNull(mq?.subHeaderChipCloud)
    }
}
