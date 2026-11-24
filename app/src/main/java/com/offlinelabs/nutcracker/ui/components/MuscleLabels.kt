package com.offlinelabs.nutcracker.ui.components

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.components.dialogs.normalizeMuscle

/*
 * Muscles are stored as stable English keys (the exercise database's values, e.g. "lower back"),
 * so data, grouping and backups don't depend on the app language. Only the display is translated:
 * known keys map to string resources; muscles the user typed are shown as typed.
 */

private val MUSCLE_LABELS: Map<String, Int> = mapOf(
    "abdominals" to R.string.muscle_abdominals,
    "abductors" to R.string.muscle_abductors,
    "adductors" to R.string.muscle_adductors,
    "biceps" to R.string.muscle_biceps,
    "calves" to R.string.muscle_calves,
    "chest" to R.string.muscle_chest,
    "forearms" to R.string.muscle_forearms,
    "glutes" to R.string.muscle_glutes,
    "hamstrings" to R.string.muscle_hamstrings,
    "lats" to R.string.muscle_lats,
    "lower back" to R.string.muscle_lower_back,
    "middle back" to R.string.muscle_middle_back,
    "neck" to R.string.muscle_neck,
    "quadriceps" to R.string.muscle_quadriceps,
    "shoulders" to R.string.muscle_shoulders,
    "traps" to R.string.muscle_traps,
    "triceps" to R.string.muscle_triceps
)

/** String resource of a known muscle key, or null for a custom muscle. */
@StringRes
fun muscleLabelRes(key: String): Int? = MUSCLE_LABELS[normalizeMuscle(key)]

/** Display name of a muscle key: translated when known, otherwise the stored text capitalized. */
fun muscleLabel(resources: Resources, key: String): String =
    muscleLabelRes(key)?.let(resources::getString) ?: key.trim().replaceFirstChar { it.uppercase() }

@Composable
@ReadOnlyComposable
fun muscleLabel(key: String): String =
    muscleLabelRes(key)?.let { stringResource(it) } ?: key.trim().replaceFirstChar { it.uppercase() }

/** A label function bound to the current resources (for filtering outside composition). */
@Composable
fun rememberMuscleLabeler(): (String) -> String {
    val resources = LocalContext.current.resources
    return { key -> muscleLabel(resources, key) }
}
