package com.music.vivi

import com.music.vivi.betterlyrics.TTMLParser
import org.junit.Test

class TtmlTest {
    @Test
    fun testUnisonTtml() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:ttp="http://www.w3.org/ns/ttml#parameter" xmlns:composer="https://composer.betterlyrics.org/ttml" ttp:timeBase="media" xml:lang="en" composer:timing="Word"><head><metadata><ttm:title>Avenoir - AFTER HOURS</ttm:title><ttm:agent xml:id="v1" type="person"><ttm:name>Voice 1</ttm:name></ttm:agent><ttm:agent xml:id="v1000" type="group"><ttm:name>Harmony</ttm:name></ttm:agent><ttm:agent xml:id="v2000" type="other"><ttm:name>Chorus</ttm:name></ttm:agent></metadata></head><body dur="2:53.026"><div><p begin="0:24.695" end="0:27.129" ttm:agent="v1"><span begin="0:24.695" end="0:24.774">we</span> <span begin="0:24.774" end="0:25.009">could</span> <span begin="0:25.009" end="0:25.341">meet</span> <span begin="0:25.341" end="0:25.503">o</span><span begin="0:25.503" end="0:25.695">ver</span> <span begin="0:25.695" end="0:25.998">wine</span> <span begin="0:25.998" end="0:26.170">it's</span> <span begin="0:26.170" end="0:26.324">a</span> <span begin="0:26.324" end="0:26.687">ha</span><span begin="0:26.687" end="0:27.129">bit</span></p><p begin="0:27.347" end="0:30.039" ttm:agent="v1"><span begin="0:27.347" end="0:27.634">me</span> <span begin="0:27.634" end="0:27.745">and</span> <span begin="0:27.745" end="0:28.012">you</span> <span begin="0:28.012" end="0:28.135">o</span><span begin="0:28.135" end="0:28.324">ver</span> <span begin="0:28.324" end="0:28.675">wine</span> <span begin="0:28.675" end="0:29.059">that's</span> <span begin="0:29.059" end="0:29.405">hap</span><span begin="0:29.405" end="0:29.585">pen</span><span begin="0:29.585" end="0:30.039">in'</span></p></div></body></tt>"""
        
        val parsed = TTMLParser.parseTTML(ttml)
        println("PARSED LINES COUNT: ${parsed.size}")
        if (parsed.isEmpty()) {
            println("FAILED TO PARSE")
            return
        }
        val lrc = TTMLParser.toLRC(parsed)
        println("LRC OUTPUT:")
        println(lrc)
    }
}
