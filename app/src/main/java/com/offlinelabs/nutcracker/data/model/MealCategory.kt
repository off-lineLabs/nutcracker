package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A meal of the day that foods can belong to (Breakfast, Lunch, Dinner, Snack, or the user's own).
 *
 * The built-in ones have a [defaultKey] (see [MealCategoryDefaults]) and are shown with a translated
 * name; [name] then only holds the English fallback. Renaming one clears [defaultKey], so the
 * user's text is shown as typed from then on.
 */
@Entity(tableName = "meal_categories")
data class MealCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val defaultKey: String? = null,
    /** Position among categories (ascending). */
    val sortOrder: Int = 0,
    /** Soft delete: false hides the category. */
    val isVisible: Boolean = true
)

/** A food ([mealId]) belonging to a [MealCategory]; a food can be in several categories. */
@Entity(
    tableName = "meal_category_meals",
    primaryKeys = ["categoryId", "mealId"],
    foreignKeys = [
        ForeignKey(
            entity = MealCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId")]
)
data class MealCategoryMeal(
    val categoryId: Long,
    val mealId: Long
)

/** The categories every install starts with. Keys are stored in the database; names are the English fallback. */
object MealCategoryDefaults {
    const val BREAKFAST = "breakfast"
    const val LUNCH = "lunch"
    const val DINNER = "dinner"
    const val SNACK = "snack"

    val all: List<MealCategory> = listOf(
        MealCategory(name = "Breakfast", defaultKey = BREAKFAST, sortOrder = 0),
        MealCategory(name = "Lunch", defaultKey = LUNCH, sortOrder = 1),
        MealCategory(name = "Dinner", defaultKey = DINNER, sortOrder = 2),
        MealCategory(name = "Snack", defaultKey = SNACK, sortOrder = 3)
    )

    /** SQL that seeds [all] (used by the migration and on database creation). */
    fun insertStatements(): List<String> = all.map { c ->
        "INSERT INTO meal_categories (name, defaultKey, sortOrder, isVisible) " +
            "VALUES ('${c.name}', '${c.defaultKey}', ${c.sortOrder}, 1)"
    }
}
