package com.pixelody.app.core.genre

import java.util.Locale
import kotlin.math.min

/**
 * High-level sonic clusters for genre affinity and harmonic flow cohesion.
 */
enum class GenreCluster(val displayName: String) {
    Electronic("Electronic & Dance"),
    Rock("Rock & Alternative"),
    Metal("Metal & Heavy"),
    HipHop("Hip-Hop & Urban"),
    PopRnB("Pop, Soul & R&B"),
    JazzBlues("Jazz & Blues"),
    ClassicalCinematic("Classical & Cinematic"),
    FolkAcoustic("Folk & Acoustic"),
    Unknown("Eclectic");

    companion object {
        fun areAdjacent(a: GenreCluster, b: GenreCluster): Boolean = when {
            a == b -> true
            a == Unknown || b == Unknown -> false
            // Natural sonic adjacencies / bridges
            (a == Electronic && (b == HipHop || b == PopRnB)) ||
            (b == Electronic && (a == HipHop || a == PopRnB)) -> true

            (a == Rock && (b == Metal || b == FolkAcoustic || b == PopRnB)) ||
            (b == Rock && (a == Metal || a == FolkAcoustic || a == PopRnB)) -> true

            (a == HipHop && (b == PopRnB || b == JazzBlues)) ||
            (b == HipHop && (a == PopRnB || a == JazzBlues)) -> true

            (a == JazzBlues && (b == PopRnB || b == ClassicalCinematic)) ||
            (b == JazzBlues && (a == PopRnB || a == ClassicalCinematic)) -> true

            (a == ClassicalCinematic && (b == Electronic || b == FolkAcoustic)) ||
            (b == ClassicalCinematic && (a == Electronic || a == FolkAcoustic)) -> true

            else -> false
        }

        fun areDissonant(a: GenreCluster, b: GenreCluster): Boolean = when {
            a == Unknown || b == Unknown -> false
            (a == ClassicalCinematic && b == Metal) || (b == ClassicalCinematic && a == Metal) -> true
            (a == FolkAcoustic && b == Metal) || (b == FolkAcoustic && a == Metal) -> true
            (a == JazzBlues && b == Metal) || (b == JazzBlues && a == Metal) -> true
            (a == Electronic && b == Metal) || (b == Electronic && a == Metal) -> true
            else -> false
        }
    }
}

/**
 * Canonical Genre Taxonomy and Fuzzy Normalization Engine.
 *
 * Normalizes freeform and informal user tags, resolves typos via Levenshtein distance,
 * provides autocomplete suggestions, and calculates flow affinity scores.
 */
object GenreTaxonomyEngine {

    val CANONICAL_GENRES: List<Pair<String, GenreCluster>> = listOf(
        // Electronic & Dance
        "Electronic" to GenreCluster.Electronic,
        "House" to GenreCluster.Electronic,
        "Techno" to GenreCluster.Electronic,
        "Ambient" to GenreCluster.Electronic,
        "Synthwave" to GenreCluster.Electronic,
        "Downtempo" to GenreCluster.Electronic,
        "Drum & Bass" to GenreCluster.Electronic,
        "Dubstep" to GenreCluster.Electronic,
        "Trance" to GenreCluster.Electronic,
        "IDM" to GenreCluster.Electronic,
        "Chillout" to GenreCluster.Electronic,

        // Rock & Alternative
        "Rock" to GenreCluster.Rock,
        "Alternative" to GenreCluster.Rock,
        "Indie Rock" to GenreCluster.Rock,
        "Hard Rock" to GenreCluster.Rock,
        "Punk" to GenreCluster.Rock,
        "Grunge" to GenreCluster.Rock,
        "Post-Rock" to GenreCluster.Rock,
        "Classic Rock" to GenreCluster.Rock,
        "Psychedelic Rock" to GenreCluster.Rock,
        "Progressive Rock" to GenreCluster.Rock,

        // Metal & Heavy
        "Metal" to GenreCluster.Metal,
        "Heavy Metal" to GenreCluster.Metal,
        "Thrash Metal" to GenreCluster.Metal,
        "Death Metal" to GenreCluster.Metal,
        "Black Metal" to GenreCluster.Metal,
        "Doom Metal" to GenreCluster.Metal,
        "Progressive Metal" to GenreCluster.Metal,
        "Industrial Metal" to GenreCluster.Metal,
        "Nu-Metal" to GenreCluster.Metal,

        // Hip-Hop & Urban
        "Hip-Hop" to GenreCluster.HipHop,
        "Rap" to GenreCluster.HipHop,
        "Trap" to GenreCluster.HipHop,
        "Boom Bap" to GenreCluster.HipHop,
        "Lo-Fi Hip Hop" to GenreCluster.HipHop,
        "Cloud Rap" to GenreCluster.HipHop,
        "Drill" to GenreCluster.HipHop,

        // Pop, Soul & R&B
        "Pop" to GenreCluster.PopRnB,
        "Synthpop" to GenreCluster.PopRnB,
        "Indie Pop" to GenreCluster.PopRnB,
        "R&B" to GenreCluster.PopRnB,
        "Soul" to GenreCluster.PopRnB,
        "Neo-Soul" to GenreCluster.PopRnB,
        "Funk" to GenreCluster.PopRnB,
        "Disco" to GenreCluster.PopRnB,

        // Jazz & Blues
        "Jazz" to GenreCluster.JazzBlues,
        "Fusion" to GenreCluster.JazzBlues,
        "Bebop" to GenreCluster.JazzBlues,
        "Smooth Jazz" to GenreCluster.JazzBlues,
        "Blues" to GenreCluster.JazzBlues,
        "Soul Blues" to GenreCluster.JazzBlues,

        // Classical & Cinematic
        "Classical" to GenreCluster.ClassicalCinematic,
        "Orchestral" to GenreCluster.ClassicalCinematic,
        "Baroque" to GenreCluster.ClassicalCinematic,
        "Chamber" to GenreCluster.ClassicalCinematic,
        "Contemporary Classical" to GenreCluster.ClassicalCinematic,
        "Soundtrack" to GenreCluster.ClassicalCinematic,
        "Cinematic" to GenreCluster.ClassicalCinematic,

        // Folk & Acoustic
        "Folk" to GenreCluster.FolkAcoustic,
        "Indie Folk" to GenreCluster.FolkAcoustic,
        "Country" to GenreCluster.FolkAcoustic,
        "Americana" to GenreCluster.FolkAcoustic,
        "Acoustic" to GenreCluster.FolkAcoustic,
        "World" to GenreCluster.FolkAcoustic,
        "Reggae" to GenreCluster.FolkAcoustic,
        "Latin" to GenreCluster.FolkAcoustic
    )

    private val GENRE_TO_CLUSTER: Map<String, GenreCluster> =
        CANONICAL_GENRES.associate { (name, cluster) -> name.lowercase(Locale.US) to cluster }

    private val ALIAS_MAP: Map<String, String> = mapOf(
        "hip hop" to "Hip-Hop",
        "hiphop" to "Hip-Hop",
        "rap" to "Hip-Hop",
        "r&b" to "R&B",
        "rnb" to "R&B",
        "r and b" to "R&B",
        "dnb" to "Drum & Bass",
        "d&b" to "Drum & Bass",
        "drum and bass" to "Drum & Bass",
        "drum & bass" to "Drum & Bass",
        "lo-fi" to "Lo-Fi Hip Hop",
        "lofi" to "Lo-Fi Hip Hop",
        "lo fi" to "Lo-Fi Hip Hop",
        "lo-fi hip hop" to "Lo-Fi Hip Hop",
        "lo fi hip hop" to "Lo-Fi Hip Hop",
        "alt rock" to "Alternative",
        "alt-rock" to "Alternative",
        "alternative rock" to "Alternative",
        "synth wave" to "Synthwave",
        "retrowave" to "Synthwave",
        "synthwave" to "Synthwave",
        "soundtrack" to "Soundtrack",
        "sound track" to "Soundtrack",
        "ost" to "Soundtrack",
        "film score" to "Soundtrack",
        "game soundtrack" to "Soundtrack",
        "heavy metal" to "Heavy Metal",
        "heavymetal" to "Heavy Metal",
        "indie" to "Indie Rock",
        "indie-rock" to "Indie Rock",
        "edm" to "Electronic",
        "electro" to "Electronic",
        "electronic dance music" to "Electronic",
        "prog rock" to "Progressive Rock",
        "progressive rock" to "Progressive Rock",
        "prog metal" to "Progressive Metal",
        "industrial" to "Industrial Metal",
        "industrial metal" to "Industrial Metal",
        "industrial rock" to "Industrial Metal",
        "post rock" to "Post-Rock",
        "post-rock" to "Post-Rock",
        "classic rock" to "Classic Rock",
        "psychedelic" to "Psychedelic Rock",
        "psych rock" to "Psychedelic Rock"
    )

    /**
     * Cleans and normalizes freeform genre input into its canonical taxonomy form.
     */
    fun canonicalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        val lower = trimmed.lowercase(Locale.US)

        // 1. Direct match on canonical names (case-insensitive)
        val directMatch = CANONICAL_GENRES.firstOrNull { it.first.equals(lower, ignoreCase = true) }
        if (directMatch != null) return directMatch.first

        // 2. Direct alias lookup
        val alias = ALIAS_MAP[lower]
        if (alias != null) return alias

        // 3. Handle delimiter composites (e.g. "Rock / Metal", "Pop, Dance", "Indie | Folk")
        val primarySegment = lower.split(Regex("[/,;|]")).firstOrNull()?.trim()
        if (!primarySegment.isNullOrBlank() && primarySegment != lower) {
            val segmentCanonical = canonicalize(primarySegment)
            if (segmentCanonical.isNotBlank()) return segmentCanonical
        }

        // 4. Fuzzy distance matching against canonical names (Levenshtein distance <= 2 for short, <= 3 for long)
        var bestCandidate: String? = null
        var minDistance = Int.MAX_VALUE
        for ((canonical, _) in CANONICAL_GENRES) {
            val dist = levenshteinDistance(lower, canonical.lowercase(Locale.US))
            val threshold = if (canonical.length <= 5) 2 else 3
            if (dist <= threshold && dist < minDistance) {
                minDistance = dist
                bestCandidate = canonical
            }
        }
        if (bestCandidate != null) return bestCandidate

        // 5. Fallback: nicely Title-Case the raw string
        return trimmed.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.US).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    /**
     * Resolves the high-level sonic cluster for a given genre.
     */
    fun clusterOf(genre: String?): GenreCluster {
        val canonical = canonicalize(genre)
        if (canonical.isBlank()) return GenreCluster.Unknown
        return GENRE_TO_CLUSTER[canonical.lowercase(Locale.US)] ?: GenreCluster.Unknown
    }

    /**
     * Calculates compatibility score between two genres for Flow Shuffle transitions.
     * Higher score = smoother transition.
     */
    fun genreAffinityScore(genreA: String?, genreB: String?): Int {
        val c1 = canonicalize(genreA)
        val c2 = canonicalize(genreB)
        if (c1.isBlank() || c2.isBlank()) return 0

        if (c1.equals(c2, ignoreCase = true)) {
            return 6 // Exact genre match
        }

        val cluster1 = clusterOf(c1)
        val cluster2 = clusterOf(c2)

        return when {
            cluster1 == cluster2 && cluster1 != GenreCluster.Unknown -> 4 // Same sonic cluster
            GenreCluster.areAdjacent(cluster1, cluster2) -> 1 // Harmonious adjacent cluster
            GenreCluster.areDissonant(cluster1, cluster2) -> -4 // Dissonant clash
            else -> 0
        }
    }

    /**
     * Autocomplete suggestions for user editing and search chips.
     */
    fun suggestGenres(query: String?, limit: Int = 6): List<String> {
        val q = query?.trim()?.lowercase(Locale.US).orEmpty()
        if (q.isBlank()) {
            return listOf("Rock", "Electronic", "Hip-Hop", "Ambient", "Alternative", "Pop").take(limit)
        }

        val results = mutableListOf<String>()

        // 1. Starts with
        CANONICAL_GENRES.map { it.first }.forEach { name ->
            if (name.lowercase(Locale.US).startsWith(q) && name !in results) {
                results.add(name)
            }
        }

        // 2. Contains
        if (results.size < limit) {
            CANONICAL_GENRES.map { it.first }.forEach { name ->
                if (name.lowercase(Locale.US).contains(q) && name !in results) {
                    results.add(name)
                }
            }
        }

        // 3. Aliases
        if (results.size < limit) {
            ALIAS_MAP.forEach { (alias, target) ->
                if (alias.contains(q) && target !in results) {
                    results.add(target)
                }
            }
        }

        return results.take(limit)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[m][n]
    }
}
