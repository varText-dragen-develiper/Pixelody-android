package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.pixelody.app.core.unskippable.UnskippableMix
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A saved unskippable mix: its fixed tracklist, the recipe it came from, and where you left off. */
data class UnskippableMixRecord(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val recipe: UnskippableMix.Recipe,
    val neighbourIds: List<String> = emptyList(),
    val createdAtMs: Long = 0L,
    val progressTrackId: String? = null,
    val progressPositionMs: Long = 0L,
    val lastFinishedAtMs: Long? = null
)

/** Same fields as the Windows playlist's `unskippable` block, kept apart from ordinary playlists. */
object UnskippableMixCodec {
    private const val VERSION = 1

    private fun strings(array: JSONArray?): List<String> =
        (0 until (array?.length() ?: 0)).mapNotNull { array!!.opt(it) as? String }

    fun encode(records: List<UnskippableMixRecord>): String = JSONObject()
        .put("version", VERSION)
        .put("mixes", JSONArray(records.map { record ->
            JSONObject()
                .put("id", record.id).put("name", record.name)
                .put("trackIds", JSONArray(record.trackIds))
                .put("neighbourIds", JSONArray(record.neighbourIds))
                .put("createdAtMs", record.createdAtMs)
                .put("recipe", JSONObject()
                    .put("version", record.recipe.version).put("style", record.recipe.style)
                    .put("genre", record.recipe.genre ?: JSONObject.NULL).put("genreId", record.recipe.genreId ?: JSONObject.NULL)
                    .put("targetMinutes", record.recipe.targetMinutes).put("seed", record.recipe.seed)
                    .put("includedUntaggedIds", JSONArray(record.recipe.includedUntaggedIds)))
                .put("progress", record.progressTrackId?.let { JSONObject().put("trackId", it).put("positionMs", record.progressPositionMs) } ?: JSONObject.NULL)
                .put("lastFinishedAtMs", record.lastFinishedAtMs ?: JSONObject.NULL)
        }))
        .toString()

    fun decode(raw: String?): List<UnskippableMixRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        if (root.optInt("version") != VERSION) return emptyList()
        val mixes = root.optJSONArray("mixes") ?: return emptyList()
        return (0 until mixes.length()).mapNotNull { index ->
            val item = mixes.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val recipe = item.optJSONObject("recipe") ?: JSONObject()
            val progress = item.optJSONObject("progress")
            UnskippableMixRecord(
                id = id,
                name = item.optString("name").ifBlank { "Unskippable" },
                trackIds = strings(item.optJSONArray("trackIds")),
                recipe = UnskippableMix.normalizeRecipe(UnskippableMix.Recipe(
                    style = recipe.optString("style", "flow"),
                    genre = recipe.opt("genre") as? String,
                    targetMinutes = recipe.optInt("targetMinutes", UnskippableMix.DEFAULT_MINUTES),
                    seed = recipe.optString("seed", "unskippable"),
                    includedUntaggedIds = strings(recipe.optJSONArray("includedUntaggedIds"))
                )),
                neighbourIds = strings(item.optJSONArray("neighbourIds")),
                createdAtMs = item.optLong("createdAtMs", 0L),
                progressTrackId = progress?.optString("trackId")?.takeIf { it.isNotEmpty() },
                progressPositionMs = progress?.optLong("positionMs", 0L) ?: 0L,
                lastFinishedAtMs = if (item.isNull("lastFinishedAtMs")) null else item.optLong("lastFinishedAtMs")
            )
        }
    }
}

class UnskippableMixStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("pixelody_unskippable_mixes", Context.MODE_PRIVATE))

    fun load(): List<UnskippableMixRecord> = UnskippableMixCodec.decode(prefs.getString(KEY, null))

    fun save(records: List<UnskippableMixRecord>) {
        prefs.edit().putString(KEY, UnskippableMixCodec.encode(records)).apply()
    }

    fun create(name: String, preview: UnskippableMix.Preview, nowMs: Long): UnskippableMixRecord {
        val record = UnskippableMixRecord(
            id = "unskippable-${UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifBlank { "Unskippable" },
            trackIds = preview.trackIds,
            recipe = preview.recipe,
            neighbourIds = preview.neighbourIds,
            createdAtMs = nowMs
        )
        save(load() + record)
        return record
    }

    fun update(id: String, change: (UnskippableMixRecord) -> UnskippableMixRecord): UnskippableMixRecord? {
        val current = load()
        val target = current.firstOrNull { it.id == id } ?: return null
        val updated = change(target)
        save(current.map { if (it.id == id) updated else it })
        return updated
    }

    fun delete(id: String) = save(load().filterNot { it.id == id })

    companion object {
        const val KEY = "unskippable_mixes"
    }
}
