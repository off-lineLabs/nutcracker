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
fun ExternalExercise.toInternalExercise(imagePaths: List<String> = emptyList()): Exercise {
    return Exercise(
        name = this.name,
        category = mapCategory(this.category),
        equipment = this.equipment,
        primaryMuscles = this.primaryMuscles,
        secondaryMuscles = this.secondaryMuscles,
        force = this.force,
        level = this.level,
        mechanic = this.mechanic,
        instructions = this.instructions,
        notes = null, // User can add their own notes separately
        defaultWeight = getDefaultWeightForExercise(this),
        defaultReps = getDefaultRepsForExercise(this),
        defaultSets = getDefaultSetsForExercise(this),
        kcalBurnedPerUnit = getDefaultKcalPerUnit(this),
        imagePaths = imagePaths
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

private fun getDefaultKcalPerUnit(external: ExternalExercise): Double {
    return when (external.category.lowercase()) {
        "strength", "strongman", "olympic weightlifting" -> 5.0 // kcal per set
        "cardio", "plyometrics" -> 8.0 // kcal per minute
        "stretching" -> 0.5 // kcal per rep
        else -> 5.0 // Default to strength
    }
}
