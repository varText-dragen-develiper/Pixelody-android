package com.pixelody.app.core.playback

import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.Track
import java.util.PriorityQueue
import kotlin.math.abs

/**
 * Step detail along a multi-track Harmonic GPS journey.
 */
data class HarmonicGpsStep(
    val stepIndex: Int,
    val fromTrack: Track,
    val toTrack: Track,
    val fromKey: CamelotKey,
    val toKey: CamelotKey,
    val relation: HarmonicRelation,
    val modulationType: String,
    val bpmDelta: Float,
    val recommendedCurve: DjTransitionCurve,
    val transitionAdvice: String
)

/**
 * Complete multi-track Harmonic GPS route plotted between two distant songs.
 */
data class HarmonicGpsRoute(
    val originTrack: Track,
    val destinationTrack: Track,
    val waypoints: List<Track>,
    val steps: List<HarmonicGpsStep>,
    val totalHops: Int,
    val averageHarmonicCohesionScore: Float,
    val tempoProgressionStability: Float,
    val estimatedJourneyDurationSeconds: Int
)

/**
 * HarmonicGpsEngine: Dijkstra & A* graph pathfinder across Camelot Wheel transitions.
 * Calculates optimal multi-track stepping-stone journeys between harmonically distant songs.
 */
object HarmonicGpsEngine {

    // Keep this recursive Comparable type denotable for Kotlin UAST/lint.
    private data class NodeState(
        val trackId: String,
        val gCost: Float,
        val fCost: Float,
        val hopCount: Int,
        val path: List<String>
    ) : Comparable<NodeState> {
        override fun compareTo(other: NodeState): Int = fCost.compareTo(other.fCost)
    }

    /**
     * Finds the most harmonic multi-track path from [origin] to [destination] using candidate [pool].
     */
    fun findHarmonicJourney(
        origin: Track,
        destination: Track,
        pool: List<Track>,
        maxHops: Int = 5
    ): HarmonicGpsRoute? {
        if (origin.id == destination.id) {
            val originTel = HarmonicKeyEngine.estimateTrackTelemetry(origin)
            return HarmonicGpsRoute(
                originTrack = origin,
                destinationTrack = destination,
                waypoints = listOf(origin),
                steps = emptyList(),
                totalHops = 0,
                averageHarmonicCohesionScore = 1.0f,
                tempoProgressionStability = 1.0f,
                estimatedJourneyDurationSeconds = origin.durationSeconds
            )
        }

        // Deduplicate pool and ensure origin and destination are present
        val nodeMap = mutableMapOf<String, Track>()
        nodeMap[origin.id] = origin
        nodeMap[destination.id] = destination
        pool.filterNot { it.missing || it.streamUrl.isBlank() }.forEach { nodeMap[it.id] = it }

        val allNodes = nodeMap.values.toList()
        val telemetryCache = allNodes.associate { it.id to HarmonicKeyEngine.estimateTrackTelemetry(it) }

        val destTelemetry = telemetryCache[destination.id] ?: HarmonicKeyEngine.estimateTrackTelemetry(destination)

        // A* graph search
        val pq = PriorityQueue<NodeState>()
        val minCostToNode = mutableMapOf<String, Float>()

        val initialHeuristic = calculateKeyHeuristic(telemetryCache[origin.id]?.key ?: destTelemetry.key, destTelemetry.key)
        pq.add(NodeState(trackId = origin.id, gCost = 0f, fCost = initialHeuristic, hopCount = 0, path = listOf(origin.id)))
        minCostToNode[origin.id] = 0f

        var bestFoundPath: List<String>? = null

        while (pq.isNotEmpty()) {
            val current = pq.poll() ?: break

            if (current.trackId == destination.id) {
                bestFoundPath = current.path
                break
            }

            if (current.hopCount >= maxHops) continue
            if (current.gCost > (minCostToNode[current.trackId] ?: Float.MAX_VALUE)) continue

            val currentTrack = nodeMap[current.trackId] ?: continue
            val currentTel = telemetryCache[current.trackId] ?: continue

            for (neighbor in allNodes) {
                if (neighbor.id in current.path) continue // Avoid cycles

                val neighborTel = telemetryCache[neighbor.id] ?: continue
                val edgeCost = calculateEdgeCost(currentTrack, neighbor, currentTel.key, neighborTel.key, currentTel.bpm, neighborTel.bpm)
                val newGCost = current.gCost + edgeCost

                // A* heuristic: key distance to destination
                val hCost = calculateKeyHeuristic(neighborTel.key, destTelemetry.key)
                val newFCost = newGCost + hCost

                val prevBest = minCostToNode[neighbor.id] ?: Float.MAX_VALUE
                if (newGCost < prevBest) {
                    minCostToNode[neighbor.id] = newGCost
                    pq.add(NodeState(
                        trackId = neighbor.id,
                        gCost = newGCost,
                        fCost = newFCost,
                        hopCount = current.hopCount + 1,
                        path = current.path + neighbor.id
                    ))
                }
            }
        }

        val finalTrackPath = if (bestFoundPath != null) {
            bestFoundPath.mapNotNull { nodeMap[it] }
        } else {
            // Fallback greedy pathbuilder
            buildGreedyBridgePath(origin, destination, allNodes, maxHops)
        }

        if (finalTrackPath.size < 2) return null

        return buildRouteFromWaypoints(finalTrackPath)
    }

    /**
     * Calculates the edge cost for graph traversal between two tracks.
     */
    private fun calculateEdgeCost(
        trackA: Track,
        trackB: Track,
        keyA: CamelotKey,
        keyB: CamelotKey,
        bpmA: Float,
        bpmB: Float
    ): Float {
        val relation = HarmonicKeyEngine.calculateHarmonicRelation(keyA, keyB)
        var cost = (1.0f - relation.score) * 100f

        if (relation == HarmonicRelation.DissonantClash) {
            cost += 160f
        }

        // BPM difference penalty
        val bpmDiff = abs(bpmA - bpmB)
        cost += (bpmDiff * 0.75f).coerceAtMost(35f)

        // Artist anti-clump penalty
        if (trackA.artist.isNotBlank() && trackA.artist.equals(trackB.artist, ignoreCase = true)) {
            cost += 35f
        }

        // Base hop cost to favor fewer transitions when equal
        cost += 12f

        return cost
    }

    private fun calculateKeyHeuristic(currentKey: CamelotKey, destKey: CamelotKey): Float {
        if (currentKey == destKey) return 0f
        val numDiff = abs(currentKey.number - destKey.number)
        val circularDiff = minOf(numDiff, 12 - numDiff)
        val modeDiff = if (currentKey.mode != destKey.mode) 1 else 0
        return (circularDiff * 15f) + (modeDiff * 10f)
    }

    private fun buildGreedyBridgePath(
        origin: Track,
        destination: Track,
        pool: List<Track>,
        maxHops: Int
    ): List<Track> {
        val result = mutableListOf(origin)
        val available = pool.filterNot { it.id == origin.id || it.id == destination.id }.toMutableList()
        val destTel = HarmonicKeyEngine.estimateTrackTelemetry(destination)

        var current = origin
        while (result.size < maxHops && available.isNotEmpty()) {
            val currentTel = HarmonicKeyEngine.estimateTrackTelemetry(current)
            val directRel = HarmonicKeyEngine.calculateHarmonicRelation(currentTel.key, destTel.key)
            if (directRel.isHarmonic) {
                break
            }

            val nextBest = available.minByOrNull { cand ->
                val candTel = HarmonicKeyEngine.estimateTrackTelemetry(cand)
                calculateEdgeCost(current, cand, currentTel.key, candTel.key, currentTel.bpm, candTel.bpm) +
                        calculateKeyHeuristic(candTel.key, destTel.key)
            } ?: break

            result.add(nextBest)
            available.remove(nextBest)
            current = nextBest
        }

        result.add(destination)
        return result
    }

    private fun buildRouteFromWaypoints(waypoints: List<Track>): HarmonicGpsRoute {
        val telemetries = waypoints.map { HarmonicKeyEngine.estimateTrackTelemetry(it) }
        val steps = mutableListOf<HarmonicGpsStep>()

        var totalCohesion = 0f
        var totalBpmDelta = 0f

        for (i in 0 until waypoints.size - 1) {
            val fromTrack = waypoints[i]
            val toTrack = waypoints[i + 1]
            val fromTel = telemetries[i]
            val toTel = telemetries[i + 1]

            val analysis = HarmonicKeyEngine.analyzeTransition(fromTel, toTel)
            val modulation = determineModulationLabel(fromTel.key, toTel.key, analysis.relation)

            val step = HarmonicGpsStep(
                stepIndex = i,
                fromTrack = fromTrack,
                toTrack = toTrack,
                fromKey = fromTel.key,
                toKey = toTel.key,
                relation = analysis.relation,
                modulationType = modulation,
                bpmDelta = analysis.bpmDelta,
                recommendedCurve = analysis.recommendedCurve,
                transitionAdvice = analysis.relation.advice
            )
            steps.add(step)
            totalCohesion += analysis.overallCompatibilityScore
            totalBpmDelta += abs(analysis.bpmDelta)
        }

        val stepCount = steps.size.coerceAtLeast(1)
        val avgCohesion = totalCohesion / stepCount.toFloat()
        val avgBpmDelta = totalBpmDelta / stepCount.toFloat()
        val tempoStability = (1.0f - (avgBpmDelta / 20f)).coerceIn(0.2f, 1.0f)
        val totalDuration = waypoints.sumOf { it.durationSeconds }

        return HarmonicGpsRoute(
            originTrack = waypoints.first(),
            destinationTrack = waypoints.last(),
            waypoints = waypoints,
            steps = steps,
            totalHops = steps.size,
            averageHarmonicCohesionScore = avgCohesion,
            tempoProgressionStability = tempoStability,
            estimatedJourneyDurationSeconds = totalDuration
        )
    }

    private fun determineModulationLabel(keyA: CamelotKey, keyB: CamelotKey, relation: HarmonicRelation): String {
        return when (relation) {
            HarmonicRelation.ExactMatch -> "Direct Harmonic Match (Same Key)"
            HarmonicRelation.RelativeMajorMinor -> if (keyB.mode == CamelotMode.Major) "Relative Major Shift" else "Relative Minor Shift"
            HarmonicRelation.AdjacentStep -> {
                val diff = (keyB.number - keyA.number + 12) % 12
                if (diff == 1) "Dominant 5th (+1)" else "Subdominant 4th (-1)"
            }
            HarmonicRelation.EnergyBoost -> "Energy Lift Modulation (+2)"
            HarmonicRelation.EnergyDrop -> "Sunset Downshift (-2)"
            HarmonicRelation.DiagonalStep -> "Diagonal Modal Pivot"
            HarmonicRelation.DissonantClash -> "Chromatic Bridge Crossing"
        }
    }
}
