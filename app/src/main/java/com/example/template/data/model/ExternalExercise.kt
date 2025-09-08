package com.example.template.data.model

data class ExternalExercise(
    val id: String,
    val name: String,
    val force: String?,
    val level: String,
    val mechanic: String?,
    val equipment: String?,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val category: String,
    val images: List<String>
)

// Extension function to convert external exercise to internal
fun ExternalExercise.toInternalExercise(): Exercise {
    return Exercise(
        name = this.name,
        category = mapCategory(this.category),
        equipment = this.equipment,
        primaryMuscles = this.primaryMuscles,
        secondaryMuscles = this.secondaryMuscles,
        force = this.force,
        notes = this.instructions.joinToString("\n\n"),
        defaultWeight = getDefaultWeightForExercise(this),
        defaultReps = getDefaultRepsForExercise(this),
        defaultSets = getDefaultSetsForExercise(this)
    )
}

private fun mapCategory(externalCategory: String): String {
    return when (externalCategory.lowercase()) {
        "strength" -> "strength"
        "stretching" -> "stretching"
        "strongman" -> "strongman"
        "plyometrics" -> "plyometrics"
        "cardio" -> "cardio"
        "olympic weightlifting" -> "olympic weightlifting"
        else -> "strength"
    }
}

private fun getDefaultWeightForExercise(external: ExternalExercise): Double {
    return when (external.equipment?.lowercase()) {
        "barbell", "dumbbell", "kettlebells" -> 20.0
        "body only" -> 0.0
        else -> 0.0
    }
}

private fun getDefaultRepsForExercise(external: ExternalExercise): Int {
    return when (external.category.lowercase()) {
        "strength", "strongman", "olympic weightlifting" -> 8
        "stretching" -> 1
        "cardio", "plyometrics" -> 10
        else -> 8
    }
}

private fun getDefaultSetsForExercise(external: ExternalExercise): Int {
    return when (external.category.lowercase()) {
        "strength", "strongman", "olympic weightlifting" -> 3
        "stretching" -> 1
        "cardio", "plyometrics" -> 1
        else -> 3
    }
}
