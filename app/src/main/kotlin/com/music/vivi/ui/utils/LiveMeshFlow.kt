/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal const val FLOW_BLOB_COUNT = 4

private const val FLOW_EDGE_MARGIN = 0.05f
private const val FLOW_RADIUS_BASE = 0.7f
private const val FLOW_RADIUS_WOBBLE = 0.12f
private const val FLOW_BEAT_RADIUS = 0.15f
private const val PALETTE_CLUSTERS = 8
private const val PALETTE_KMEANS_ITERATIONS = 10
private const val PALETTE_DISTINCT_DISTANCE = 0.18f
private const val PALETTE_MIN_SHARE = 0.03f

internal val LiveMeshSamplePalette: List<Color> = listOf(
    Color(0xFF7B2FF7),
    Color(0xFFF107A3),
    Color(0xFF2EC4B6),
    Color(0xFFFF9F1C),
)

internal data class FlowBlob(
    val x: Float,
    val y: Float,
    val radius: Float,
    val stretchX: Float,
    val stretchY: Float,
    val angle: Float,
    val paletteIndex: Int,
    val blendIndex: Int,
    val blend: Float,
)

// Clusters the cover's real pixels into more shades than we show, then keeps the most populous ones that differ enough.
// Over-clustering splits a cover into more shades than we show, and close shades are merged back by colour, so the
// dominant background colour ranks first while small accents such as an orange still survive as their own slot.
// Seeding is deterministic, so the same cover always gives the same palette.
internal fun paletteFromPixels(argbPixels: IntArray, width: Int, height: Int): List<Color>? {
    val count = width * height
    if (width < 2 || height < 2 || argbPixels.size < count) return null
    val rgb = FloatArray(count * 3)
    for (pixel in 0 until count) {
        val color = argbPixels[pixel]
        rgb[pixel * 3] = ((color shr 16) and 0xFF) / 255f
        rgb[pixel * 3 + 1] = ((color shr 8) and 0xFF) / 255f
        rgb[pixel * 3 + 2] = (color and 0xFF) / 255f
    }
    val centers = seedCenters(rgb, count)
    val assignment = IntArray(count)
    repeat(PALETTE_KMEANS_ITERATIONS) {
        assignPixels(rgb, count, centers, assignment)
        recomputeCenters(rgb, count, assignment, centers)
    }
    assignPixels(rgb, count, centers, assignment)

    val population = IntArray(PALETTE_CLUSTERS)
    assignment.forEach { population[it]++ }
    val groups = mergeCloseClusters(centers, population)
    // A tiny accent such as a lit window is dropped unless it covers a meaningful share of the cover; the dominant colour always stays.
    val shown = groups.filterIndexed { index, group ->
        index == 0 || group.population >= count * PALETTE_MIN_SHARE
    }
    return List(FLOW_BLOB_COUNT) { slot -> shown[slot % shown.size].color() }
}

// Clusters that describe the same colour are summed before ranking, so one hue split across several seeds
// still outranks a smaller but single cluster of a different colour.
private fun mergeCloseClusters(centers: FloatArray, population: IntArray): List<PaletteGroup> {
    val groups = mutableListOf<PaletteGroup>()
    val populated = (0 until PALETTE_CLUSTERS).filter { population[it] > 0 }.sortedByDescending { population[it] }
    for (cluster in populated) {
        val red = centers[cluster * 3]
        val green = centers[cluster * 3 + 1]
        val blue = centers[cluster * 3 + 2]
        val group = groups.firstOrNull { it.distanceTo(red, green, blue) < PALETTE_DISTINCT_DISTANCE }
        if (group != null) {
            group.add(red, green, blue, population[cluster])
        } else {
            groups += PaletteGroup().apply { add(red, green, blue, population[cluster]) }
        }
    }
    return groups.sortedByDescending { it.population }
}

private class PaletteGroup {
    private var red = 0.0
    private var green = 0.0
    private var blue = 0.0
    var population = 0
        private set

    fun add(red: Float, green: Float, blue: Float, count: Int) {
        this.red += red.toDouble() * count
        this.green += green.toDouble() * count
        this.blue += blue.toDouble() * count
        population += count
    }

    fun distanceTo(red: Float, green: Float, blue: Float): Float {
        val dr = (this.red / population).toFloat() - red
        val dg = (this.green / population).toFloat() - green
        val db = (this.blue / population).toFloat() - blue
        return sqrt(dr * dr + dg * dg + db * db)
    }

    fun color(): Color = Color(
        red = (this.red / population).toFloat(),
        green = (this.green / population).toFloat(),
        blue = (this.blue / population).toFloat(),
    )
}

// Farthest-point seeding spreads the starting centres across the colours the cover actually uses.
private fun seedCenters(rgb: FloatArray, count: Int): FloatArray {
    val centers = FloatArray(PALETTE_CLUSTERS * 3)
    copyPixel(rgb, 0, centers, 0)
    for (cluster in 1 until PALETTE_CLUSTERS) {
        var farthest = 0
        var farthestDistance = -1f
        for (pixel in 0 until count) {
            var nearest = Float.MAX_VALUE
            for (existing in 0 until cluster) {
                nearest = min(nearest, squaredDistance(rgb, pixel, centers, existing))
            }
            if (nearest > farthestDistance) {
                farthestDistance = nearest
                farthest = pixel
            }
        }
        copyPixel(rgb, farthest, centers, cluster)
    }
    return centers
}

private fun assignPixels(rgb: FloatArray, count: Int, centers: FloatArray, assignment: IntArray) {
    for (pixel in 0 until count) {
        var best = 0
        var bestDistance = Float.MAX_VALUE
        for (cluster in 0 until PALETTE_CLUSTERS) {
            val distance = squaredDistance(rgb, pixel, centers, cluster)
            if (distance < bestDistance) {
                bestDistance = distance
                best = cluster
            }
        }
        assignment[pixel] = best
    }
}

// An empty cluster keeps its previous centre instead of collapsing onto zero.
private fun recomputeCenters(rgb: FloatArray, count: Int, assignment: IntArray, centers: FloatArray) {
    val sums = FloatArray(PALETTE_CLUSTERS * 3)
    val counts = IntArray(PALETTE_CLUSTERS)
    for (pixel in 0 until count) {
        val cluster = assignment[pixel]
        counts[cluster]++
        for (channel in 0 until 3) sums[cluster * 3 + channel] += rgb[pixel * 3 + channel]
    }
    for (cluster in 0 until PALETTE_CLUSTERS) {
        if (counts[cluster] == 0) continue
        for (channel in 0 until 3) {
            centers[cluster * 3 + channel] = sums[cluster * 3 + channel] / counts[cluster]
        }
    }
}

private fun squaredDistance(rgb: FloatArray, pixel: Int, centers: FloatArray, cluster: Int): Float {
    var sum = 0f
    for (channel in 0 until 3) {
        val difference = rgb[pixel * 3 + channel] - centers[cluster * 3 + channel]
        sum += difference * difference
    }
    return sum
}

private fun copyPixel(rgb: FloatArray, pixel: Int, centers: FloatArray, cluster: Int) {
    for (channel in 0 until 3) centers[cluster * 3 + channel] = rgb[pixel * 3 + channel]
}

// Integer hash so each blob, cover and session gets its own phases without a shared random source.
internal fun unitHash(seed: Int, index: Int, salt: Int): Float {
    var hash = seed * 0x27d4eb2d + index * 0x165667b1 + salt * 0x9e3779b9.toInt()
    hash = hash xor (hash ushr 15)
    hash *= 0x2c1b3c6d
    hash = hash xor (hash ushr 12)
    hash *= 0x297a2d39
    hash = hash xor (hash ushr 15)
    return (hash ushr 8) / 16777216f
}

// Two sine terms per axis with unrelated rates make the paths wander instead of tracing a circle.
internal fun flowBlobs(time: Float, seed: Int, beat: Float, beatReactivity: Float): List<FlowBlob> =
    List(FLOW_BLOB_COUNT) { index ->
        fun phase(salt: Int) = unitHash(seed, index, salt) * 2f * Math.PI.toFloat()
        fun rate(salt: Int, base: Float, spread: Float) = base + spread * unitHash(seed, index, salt)

        val x = 0.5f +
            0.30f * sin(rate(0, 0.15f, 0.20f) * time + phase(1)) +
            0.14f * sin(rate(2, 0.05f, 0.10f) * time + phase(3))
        val y = 0.5f +
            0.30f * cos(rate(4, 0.12f, 0.18f) * time + phase(5)) +
            0.14f * sin(rate(6, 0.07f, 0.11f) * time + phase(7))
        val radius = FLOW_RADIUS_BASE +
            FLOW_RADIUS_WOBBLE * sin(rate(8, 0.10f, 0.08f) * time + phase(9)) +
            beat.coerceIn(0f, 1f) * FLOW_BEAT_RADIUS * beatReactivity

        val stretchX = 0.7f + 0.3f * sin(rate(10, 0.09f, 0.07f) * time + phase(11))
        val stretchY = 0.7f + 0.3f * sin(rate(12, 0.08f, 0.06f) * time + phase(13))
        val angle = rate(14, 0.05f, 0.08f) * time + phase(15)
        val blend = 0.5f + 0.5f * sin(rate(16, 0.04f, 0.06f) * time + phase(17))

        FlowBlob(
            x = x.coerceIn(FLOW_EDGE_MARGIN, 1f - FLOW_EDGE_MARGIN),
            y = y.coerceIn(FLOW_EDGE_MARGIN, 1f - FLOW_EDGE_MARGIN),
            radius = radius,
            stretchX = stretchX,
            stretchY = stretchY,
            angle = angle,
            paletteIndex = index,
            blendIndex = (index + 1) % FLOW_BLOB_COUNT,
            blend = blend,
        )
    }
