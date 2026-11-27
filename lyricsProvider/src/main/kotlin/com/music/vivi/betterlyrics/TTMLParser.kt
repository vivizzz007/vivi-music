/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.betterlyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

object TTMLParser {
    
    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
        val agent: String? = null,
        val isBackground: Boolean = false,
        val backgroundLines: List<ParsedLine> = emptyList()
    )
    
    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double
    )
    
    private data class SpanInfo(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean
    )
    
    // Helper function to get attribute by local name (handles namespace prefixes)
    private fun Element.getAttributeByLocalName(localName: String): String {
        // First try namespace-aware lookup
        val nsValue = getAttributeNS("http://www.w3.org/ns/ttml#metadata", localName)
        if (nsValue.isNotEmpty()) return nsValue
        
        // Then try with common prefixes
        val prefixedValue = getAttribute("ttm:$localName")
        if (prefixedValue.isNotEmpty()) return prefixedValue
        
        // Finally, search through all attributes
        val attrs = attributes
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            val attrName = attr.nodeName ?: continue
            if (attrName == localName || attrName.endsWith(":$localName")) {
                return attr.nodeValue ?: ""
            }
        }
        return ""
    }
    
    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            
            val pElements = doc.getElementsByTagNameNS("*", "p")
            
            for (i in 0 until pElements.length) {
                val pElement = pElements.item(i) as? Element ?: continue
                
                val begin = pElement.getAttribute("begin")
                if (begin.isNullOrEmpty()) continue
                
                val startTime = parseTime(begin)
                val pEnd = pElement.getAttribute("end")
                val lineEndTime = if (pEnd.isNotEmpty()) parseTime(pEnd) else startTime + 3.0
                val spanInfos = mutableListOf<SpanInfo>()
                val backgroundLines = mutableListOf<ParsedLine>()
                
                // Get agent/vocalist info (ttm:agent attribute)
                val agent = pElement.getAttributeByLocalName("agent").ifEmpty { null }
                
                collectSpans(pElement, lineEndTime, spanInfos, backgroundLines, startTime)
                
                // Merge consecutive spans without whitespace between them into single words
                val words = mergeSpansIntoWords(spanInfos)
                val lineText = words.joinToString(" ") { it.text }
                
                // If no spans found, use text content directly (excluding background text)
                val finalText = if (lineText.isEmpty()) {
                    getDirectTextContent(pElement).trim()
                } else {
                    lineText
                }
                
                if (finalText.isNotEmpty()) {
                    lines.add(
                        ParsedLine(
                            text = finalText,
                            startTime = startTime,
                            words = words,
                            agent = agent,
                            isBackground = false,
                            backgroundLines = backgroundLines
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        
        return lines
    }
    
    private fun collectSpans(
        parent: Element,
        fallbackEndTime: Double,
        spanInfos: MutableList<SpanInfo>,
        backgroundLines: MutableList<ParsedLine>,
        lineStartTime: Double,
    ) {
        val childNodes = parent.childNodes
        for (j in 0 until childNodes.length) {
            val node = childNodes.item(j)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val span = node as? Element ?: continue
            if (span.localName?.lowercase() != "span") continue
            when (span.getAttributeByLocalName("role")) {
                "x-bg" -> parseBackgroundSpan(span, lineStartTime)?.let { backgroundLines.add(it) }
                "x-translation", "x-roman" -> Unit
                else -> {
                    val wordBegin = span.getAttribute("begin")
                    val wordEnd = span.getAttribute("end")
                    val hasNestedSpan = (0 until span.childNodes.length).any { idx ->
                        val c = span.childNodes.item(idx)
                        c.nodeType == Node.ELEMENT_NODE && (c as? Element)?.localName?.lowercase() == "span"
                    }
                    if (hasNestedSpan) {
                        val nestedEnd = if (wordEnd.isNotEmpty()) parseTime(wordEnd) else fallbackEndTime
                        collectSpans(span, nestedEnd, spanInfos, backgroundLines, lineStartTime)
                    } else {
                        val wordText = span.textContent?.trim() ?: ""
                        if (wordText.isNotEmpty() && wordBegin.isNotEmpty()) {
                            val start = parseTime(wordBegin)
                            val end = if (wordEnd.isNotEmpty()) parseTime(wordEnd) else maxOf(start + 0.1, fallbackEndTime)
                            val nextSibling = node.nextSibling
                            val hasTrailingSpace = nextSibling?.nodeType == Node.TEXT_NODE &&
                                nextSibling.textContent?.contains(Regex("\\s")) == true
                            spanInfos.add(SpanInfo(wordText, start, end, hasTrailingSpace))
                        }
                    }
                }
            }
        }
    }

    private fun parseBackgroundSpan(span: Element, parentStartTime: Double): ParsedLine? {
        val bgBegin = span.getAttribute("begin")
        val bgEnd = span.getAttribute("end")
        val bgStartTime = if (bgBegin.isNotEmpty()) parseTime(bgBegin) else parentStartTime
        
        val spanInfos = mutableListOf<SpanInfo>()
        val childNodes = span.childNodes
        
        for (j in 0 until childNodes.length) {
            val node = childNodes.item(j)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val innerSpan = node as? Element
                if (innerSpan?.localName?.lowercase() == "span") {
                    val role = innerSpan.getAttributeByLocalName("role")
                    
                    // Skip translation and romanization spans
                    if (role == "x-translation" || role == "x-roman") continue
                    
                    val wordBegin = innerSpan.getAttribute("begin")
                    val wordEnd = innerSpan.getAttribute("end")
                    val wordText = innerSpan.textContent?.trim() ?: ""
                    
                    if (wordText.isNotEmpty() && wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                        val nextSibling = node.nextSibling
                        val hasTrailingSpace = nextSibling?.nodeType == Node.TEXT_NODE && 
                            nextSibling.textContent?.contains(Regex("\\s")) == true
                        
                        spanInfos.add(
                            SpanInfo(
                                text = wordText,
                                startTime = parseTime(wordBegin),
                                endTime = parseTime(wordEnd),
                                hasTrailingSpace = hasTrailingSpace
                            )
                        )
                    }
                }
            }
        }
        
        val words = mergeSpansIntoWords(spanInfos)
        val lineText = words.joinToString(" ") { it.text }
        
        val finalText = if (lineText.isEmpty()) {
            getDirectTextContent(span).trim()
        } else {
            lineText
        }
        
        return if (finalText.isNotEmpty()) {
            ParsedLine(
                text = finalText,
                startTime = bgStartTime,
                words = words,
                agent = null,
                isBackground = true,
                backgroundLines = emptyList()
            )
        } else null
    }
    
    private fun getDirectTextContent(element: Element): String {
        val sb = StringBuilder()
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.TEXT_NODE) {
                sb.append(node.textContent)
            } else if (node.nodeType == Node.ELEMENT_NODE) {
                val el = node as? Element
                val role = el?.getAttributeByLocalName("role") ?: ""
                // Skip background, translation, and romanization spans
                if (role != "x-bg" && role != "x-translation" && role != "x-roman") {
                    if (el?.localName?.lowercase() == "span") {
                        sb.append(el.textContent ?: "")
                    }
                }
            }
        }
        return sb.toString()
    }
    
    private fun mergeSpansIntoWords(spanInfos: List<SpanInfo>): List<ParsedWord> {
        if (spanInfos.isEmpty()) return emptyList()
        
        val words = mutableListOf<ParsedWord>()
        var currentText = StringBuilder()
        var currentStartTime = spanInfos[0].startTime
        var currentEndTime = spanInfos[0].endTime
        
        for ((index, span) in spanInfos.withIndex()) {
            if (index == 0) {
                currentText.append(span.text)
                currentStartTime = span.startTime
                currentEndTime = span.endTime
            } else {
                // Check if previous span had trailing space (word boundary)
                val prevSpan = spanInfos[index - 1]
                if (prevSpan.hasTrailingSpace) {
                    // Save current word and start new one
                    if (currentText.isNotEmpty()) {
                        words.add(
                            ParsedWord(
                                text = currentText.toString().trim(),
                                startTime = currentStartTime,
                                endTime = currentEndTime
                            )
                        )
                    }
                    currentText = StringBuilder(span.text)
                    currentStartTime = span.startTime
                    currentEndTime = span.endTime
                } else {
                    // No space between spans - merge into same word (syllables)
                    currentText.append(span.text)
                    currentEndTime = span.endTime
                }
            }
        }
        
        // Add the last word
        if (currentText.isNotEmpty()) {
            words.add(
                ParsedWord(
                    text = currentText.toString().trim(),
                    startTime = currentStartTime,
                    endTime = currentEndTime
                )
            )
        }
        
        return words
    }
    
    fun toLRC(lines: List<ParsedLine>): String {
        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10
                
                // Add agent info if present
                val agentPrefix = if (!line.agent.isNullOrEmpty()) "{agent:${line.agent}}" else ""
                
                appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, agentPrefix, line.text))
                
                if (line.words.isNotEmpty()) {
                    val wordsData = line.words.joinToString("|") { word ->
                        val safeText = word.text.replace("|", " ").replace(":", " ")
                        "$safeText:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordsData>")
                }
                
                // Add background vocals as separate lines
                line.backgroundLines.forEach { bgLine ->
                    val bgTimeMs = (bgLine.startTime * 1000).toLong()
                    val bgMinutes = bgTimeMs / 60000
                    val bgSeconds = (bgTimeMs % 60000) / 1000
                    val bgCentiseconds = (bgTimeMs % 1000) / 10
                    
                    appendLine(String.format(Locale.US, "[%02d:%02d.%02d]{bg}%s", bgMinutes, bgSeconds, bgCentiseconds, bgLine.text))
                    
                    if (bgLine.words.isNotEmpty()) {
                        val bgWordsData = bgLine.words.joinToString("|") { word ->
                            val safeText = word.text.replace("|", " ").replace(":", " ")
                            "$safeText:${word.startTime}:${word.endTime}"
                        }
                        appendLine("<$bgWordsData>")
                    }
                }
            }
        }
    }
    
    private fun parseTime(timeStr: String): Double {
        return try {
            val raw = timeStr.trim()
            val isMs = raw.endsWith("ms", ignoreCase = true)
            val clean = raw.replace(Regex("[a-zA-Z]+$"), "")
            val seconds = when {
                clean.contains(":") -> {
                    val parts = clean.split(":")
                    when (parts.size) {
                        2 -> parts[0].toDouble() * 60 + parts[1].toDouble()
                        3 -> parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()
                        else -> clean.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> clean.toDoubleOrNull() ?: 0.0
            }
            if (isMs) seconds / 1000.0 else seconds
        } catch (e: Exception) {
            0.0
        }
    }
}
