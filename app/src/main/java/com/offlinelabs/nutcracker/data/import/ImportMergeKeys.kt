package com.offlinelabs.nutcracker.data.import

import java.util.Locale

/**
 * How a backup is applied to the current database.
 * MERGE skips rows that already exist; REPLACE wipes everything first (after an automatic backup).
 */
enum class ImportMode { MERGE, REPLACE }

/**
 * Pure helpers that decide whether a backup row already exists in the database (MERGE mode)
 * and how legacy recipe check-ins are repaired. Kept free of Android/Room types for unit tests.
 *
 * Matching is exact except that text fields are trimmed and a blank brand equals a null brand.
 */
object ImportMergeKeys {

    data class MealKey(
        val name: String,
        val brand: String,
        val calories: Int,
        val servingSizeValue: Double,
        val servingSizeUnit: String,
        val isVisible: Boolean
    )

    /** [gymId] is the id in the target database (null = shared), so the same name at two gyms differs. */
    data class ExerciseKey(val name: String, val category: String, val gymId: Long? = null)
    /** Case-insensitive; blank unit equals no unit. */
    data class GymKey(val name: String, val unit: String)
    data class RecipeKey(val name: String, val createdDate: String)
    data class TagKey(val name: String, val type: String)
    // Child rows are keyed by their parent's natural key (not its id), so identical parents
    // (e.g. two recipe snapshots) stay interchangeable and re-imports are order independent.
    data class MealCheckInKey(val meal: MealKey, val checkInDateTime: String)
    data class ExerciseLogKey(val exercise: ExerciseKey, val logDateTime: String)
    data class PillCheckInKey(val pill: String, val timestamp: String)
    data class MealTagKey(val meal: MealKey, val tag: TagKey)
    data class ExerciseTagKey(val exercise: ExerciseKey, val tag: TagKey)

    /**
     * Hands out existing row ids 1:1 per natural key: the n-th backup row with a key takes the
     * n-th existing row (ascending id) with that key; null once the group is used up (insert then).
     */
    class ExistingIdQueues<K>(existing: List<Pair<K, Long>>) {
        private val queues = HashMap<K, ArrayDeque<Long>>()

        init {
            existing.sortedBy { it.second }.forEach { (key, id) -> queues.getOrPut(key) { ArrayDeque() }.addLast(id) }
        }

        fun take(key: K): Long? = queues[key]?.removeFirstOrNull()
    }

    /** Identifies a legacy recipe check-in independent of the snapshot meal it ends up using. */
    data class LegacyCheckInKey(val checkInDateTime: String, val notes: String)

    fun mealKey(
        name: String,
        brand: String?,
        calories: Int,
        servingSizeValue: Double,
        servingSizeUnit: String,
        isVisible: Boolean
    ) = MealKey(name.trim(), brand?.trim().orEmpty(), calories, servingSizeValue, servingSizeUnit.trim(), isVisible)

    fun exerciseKey(name: String, category: String, gymId: Long? = null) =
        ExerciseKey(name.trim(), category.trim(), gymId)

    fun gymKey(name: String, unit: String?) =
        GymKey(name.trim().lowercase(Locale.ROOT), unit?.trim().orEmpty().lowercase(Locale.ROOT))

    /**
     * Maps a backup exercise's raw gymId column to the new gym id. A missing column, an empty value,
     * a non-numeric value or an id absent from the gym mapping all mean shared (null).
     */
    fun resolveGymId(rawGymId: String?, lookup: (String) -> Long?): Long? {
        val raw = rawGymId?.trim()
        if (raw.isNullOrEmpty() || raw.toLongOrNull() == null) return null
        return lookup(raw)
    }

    fun recipeKey(name: String, createdDate: String) = RecipeKey(name.trim(), createdDate.trim())

    fun tagKey(name: String, type: String) = TagKey(name.trim(), type.trim())

    fun pillKey(name: String) = name.trim()

    fun mealCheckInKey(meal: MealKey, checkInDateTime: String) = MealCheckInKey(meal, checkInDateTime.trim())

    fun exerciseLogKey(exercise: ExerciseKey, logDateTime: String) = ExerciseLogKey(exercise, logDateTime.trim())

    fun pillCheckInKey(pillName: String, timestamp: String) = PillCheckInKey(pillKey(pillName), timestamp.trim())

    fun mealTagKey(meal: MealKey, tag: TagKey) = MealTagKey(meal, tag)

    fun exerciseTagKey(exercise: ExerciseKey, tag: TagKey) = ExerciseTagKey(exercise, tag)

    fun legacyCheckInKey(checkInDateTime: String, notes: String?) =
        LegacyCheckInKey(checkInDateTime.trim(), notes?.trim().orEmpty())

    /**
     * The auto-seeded default pill is only removed when it is provably untouched: it is the only
     * pill, has no check-ins, and the backup brings at least one pill of its own. The default
     * name is localised, so matching by name is not reliable.
     */
    fun shouldRemoveSeededDefaultPill(existingPillCount: Int, existingPillCheckInCount: Int, backupPillCount: Int): Boolean =
        existingPillCount == 1 && existingPillCheckInCount == 0 && backupPillCount >= 1

    /**
     * MERGE applies the backup's goal when there is none yet or the existing one is the untouched
     * default (every field equal, ignoring id); otherwise the user's own goal is kept.
     */
    fun shouldApplyBackupGoal(
        existing: com.offlinelabs.nutcracker.data.model.UserGoal?,
        default: com.offlinelabs.nutcracker.data.model.UserGoal = com.offlinelabs.nutcracker.data.model.UserGoal.default()
    ): Boolean = existing == null || existing.copy(id = default.id) == default

    /** What to do with a legacy recipe check-in (old meal id <= 0, notes "recipe:<id>"). */
    sealed class LegacyCheckInAction {
        /** Create a fresh snapshot from this (already mapped, existing) recipe. */
        data class SnapshotFromRecipe(val recipeId: Long) : LegacyCheckInAction()

        /** Recipe unavailable: clone the imported placeholder meal as a hidden meal. */
        data class CloneMeal(val mealId: Long) : LegacyCheckInAction()

        /** Nothing to build a snapshot from. */
        object Skip : LegacyCheckInAction()
    }

    /**
     * @param mappedRecipeId new id of the recipe if the backup recipe was imported and still exists
     * @param mappedPlaceholderMealId new id of the imported placeholder meal, if any
     */
    fun decideLegacyCheckIn(mappedRecipeId: Long?, mappedPlaceholderMealId: Long?): LegacyCheckInAction = when {
        mappedRecipeId != null -> LegacyCheckInAction.SnapshotFromRecipe(mappedRecipeId)
        mappedPlaceholderMealId != null -> LegacyCheckInAction.CloneMeal(mappedPlaceholderMealId)
        else -> LegacyCheckInAction.Skip
    }

    /** Notes of a legacy check-in rewritten to point at the new recipe id. */
    fun legacyNotes(newRecipeId: Long) = "recipe:$newRecipeId"
}

/** TSV escaping shared by export-compatible parsing; round-trips any string. */
object TsvCodec {
    fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    /** Single-pass so that an escaped backslash followed by 'n' is not read as a newline. */
    fun unescape(value: String): String {
        if (!value.contains('\\')) return value
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    't' -> { sb.append('\t'); i += 2; continue }
                    'n' -> { sb.append('\n'); i += 2; continue }
                    'r' -> { sb.append('\r'); i += 2; continue }
                    '\\' -> { sb.append('\\'); i += 2; continue }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
