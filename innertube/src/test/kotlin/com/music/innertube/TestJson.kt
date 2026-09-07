package com.music.innertube

import com.music.innertube.models.NextResponse
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull

class TestJson {
    @Test
    fun testParse() {
        val json = Json { ignoreUnknownKeys = true }
        val data = File("../ytmusic_next_response.json").readText()
        val response = json.decodeFromString<NextResponse>(data)
        
        val mq = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(0)?.tabRenderer?.content?.musicQueueRenderer
        println("MusicQueueRenderer is null: ${mq == null}")
        println("subHeaderChipCloud is null: ${mq?.subHeaderChipCloud == null}")
        println("Chips count: ${mq?.subHeaderChipCloud?.chipCloudRenderer?.chips?.size}")
        assertNotNull(mq?.subHeaderChipCloud)
    }
}
