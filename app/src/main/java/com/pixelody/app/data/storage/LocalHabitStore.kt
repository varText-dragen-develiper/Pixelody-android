package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Circadian phases for time-of-day habituation.
 */
enum class CircadianPhase(val displayName: String, val startHour: Int, val endHour: Int) {
    Morning("Morning Awakening", 5, 11),
    Afternoon("Daylight Focus", 11, 17),
    Evening("Sunset Unwind", 17, 22),
    Night("Late Night Deep Dive", 22, 5);

    companion object {
        fun current(calendar: Calendar = Calendar.getInstance()): CircadianPhase {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            return when {
                hour in 5..10 -> Morning
                hour in 11..16 -> Afternoon
                hour in 17..21 -> Evening
                else -> Night
            }
        }
    }
}

/**
 * Contextual habit interaction types.
 */
enum class HabitActionType {
    PlaySomething,
    ResumeTrack,
    OpenFavorites,
    OpenQueue,
    SmartFlowShuffle,
    HiResFilter,
    HeavyRotation,
    OpenTurntable,
    OpenMastering,
    OpenEqualizer,
    OpenStems,
    SelectLocalPhoneSource,
    SelectDesktopHostSource,
    SelectJamMeshSource,
    OpenSonicCapsule,
    AddMusic
}

/**
 * On-device zero-telemetry habit store for recording usage frequency
 * and predicting habitual muscle-memory pathways.
 */
class LocalHabitStore(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun recordAction(action: HabitActionType, phase: CircadianPhase = CircadianPhase.current()) {
        val totalKey = "total_${action.name}"
        val phaseKey = "phase_${phase.name}_${action.name}"
        val lastUsedKey = "last_${action.name}"

        val totalCount = prefs.getInt(totalKey, 0) + 1
        val phaseCount = prefs.getInt(phaseKey, 0) + 1

        prefs.edit()
            .putInt(totalKey, totalCount)
            .putInt(phaseKey, phaseCount)
            .putLong(lastUsedKey, System.currentTimeMillis())
            .apply()
    }

    fun getActionWeight(action: HabitActionType, phase: CircadianPhase = CircadianPhase.current()): Float {
        val totalKey = "total_${action.name}"
        val phaseKey = "phase_${phase.name}_${action.name}"
        val lastUsedKey = "last_${action.name}"

        val total = prefs.getInt(totalKey, 0)
        val phaseScore = prefs.getInt(phaseKey, 0)
        val lastUsed = prefs.getLong(lastUsedKey, 0L)

        if (total == 0) return 0.1f

        // Recency boost (within 24 hours)
        val hoursSinceLast = (System.currentTimeMillis() - lastUsed) / (1000f * 60f * 60f)
        val recencyFactor = when {
            hoursSinceLast < 6f -> 1.5f
            hoursSinceLast < 24f -> 1.2f
            hoursSinceLast < 72f -> 1.0f
            else -> 0.7f
        }

        // Phase alignment boost
        val phaseAlignment = if (total > 0) (phaseScore.toFloat() / total.toFloat()) else 0.5f

        val baseScore = (total.coerceAtMost(50) / 50f) * 0.4f + phaseAlignment * 0.4f
        return (baseScore * recencyFactor).coerceIn(0.05f, 1.0f)
    }

    fun topHabitActions(phase: CircadianPhase = CircadianPhase.current(), limit: Int = 3): List<HabitActionType> {
        return HabitActionType.values()
            .sortedByDescending { getActionWeight(it, phase) }
            .take(limit)
    }

    companion object {
        const val PREFERENCES_NAME = "pixelody_local_habits"
    }
}
