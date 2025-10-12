# Tags System Documentation

## Overview

The tags system allows you to categorize and organize meals and exercises with custom tags. Each tag has a name, color, and type (either MEAL or EXERCISE).

## Database Schema

### Tables Created

1. **`tags`** - Main tags table
   - `id` (Long, Primary Key, Auto-increment)
   - `name` (String) - Tag name
   - `color` (String) - Hex color code (e.g., "#FF5722")
   - `type` (TagType) - Either MEAL or EXERCISE

2. **`meal_tags`** - Junction table for meal-tag relationships
   - `id` (Long, Primary Key, Auto-increment)
   - `mealId` (Long, Foreign Key to meals.id)
   - `tagId` (Long, Foreign Key to tags.id)

3. **`exercise_tags`** - Junction table for exercise-tag relationships
   - `id` (Long, Primary Key, Auto-increment)
   - `exerciseId` (Long, Foreign Key to exercises.id)
   - `tagId` (Long, Foreign Key to tags.id)

## DAOs Available

### TagDao
- `insertTag(tag: Tag): Long`
- `updateTag(tag: Tag)`
- `deleteTag(tag: Tag)`
- `getAllTags(): Flow<List<Tag>>`
- `getTagsByType(type: TagType): Flow<List<Tag>>`
- `getTagById(tagId: Long): Flow<Tag?>`
- `getTagByNameAndType(name: String, type: TagType): Flow<Tag?>`

### MealTagDao
- `insertMealTag(mealTag: MealTag): Long`
- `deleteMealTag(mealTag: MealTag)`
- `getTagsForMeal(mealId: Long): Flow<List<MealTag>>`
- `getMealsForTag(tagId: Long): Flow<List<MealTag>>`
- `deleteTagsForMeal(mealId: Long)`
- `deleteMealsForTag(tagId: Long)`

### ExerciseTagDao
- `insertExerciseTag(exerciseTag: ExerciseTag): Long`
- `deleteExerciseTag(exerciseTag: ExerciseTag)`
- `getTagsForExercise(exerciseId: Long): Flow<List<ExerciseTag>>`
- `getExercisesForTag(tagId: Long): Flow<List<ExerciseTag>>`
- `deleteTagsForExercise(exerciseId: Long)`
- `deleteExercisesForTag(tagId: Long)`

## Migration

The system includes a migration from database version 17 to 18 that:
1. Creates the `tags` table
2. Creates the `meal_tags` junction table
3. Creates the `exercise_tags` junction table
4. Adds appropriate indices for performance

## Usage Examples

### Creating a Tag
```kotlin
val tag = Tag(
    name = "High Protein",
    color = "#FF5722",
    type = TagType.MEAL
)
val tagId = tagDao.insertTag(tag)
```

### Associating a Tag with a Meal
```kotlin
val mealTag = MealTag(
    mealId = mealId,
    tagId = tagId
)
mealTagDao.insertMealTag(mealTag)
```

### Getting All Tags for a Meal
```kotlin
mealTagDao.getTagsForMeal(mealId).collect { mealTags ->
    // Process meal tags
}
```

### Getting All Meals with a Specific Tag
```kotlin
mealTagDao.getMealsForTag(tagId).collect { mealTags ->
    // Process meals with this tag
}
```

## Future UI Integration

The tags system is ready for UI integration. You can:
1. Display tags as colored chips/badges
2. Filter meals/exercises by tags
3. Allow users to add/remove tags from items
4. Create tag management screens

## Database Migration Strategy

The app now uses proper database migrations instead of rebuilding from scratch:
- **Version 17 → 18**: Added tags system
- Future migrations can be added to `DatabaseMigrations.kt`
- The system preserves existing data during migrations
- Fallback to destructive migration is available for major schema changes

## Notes

- Tags are strictly typed (MEAL or EXERCISE) to prevent cross-contamination
- Foreign key constraints ensure data integrity
- Cascade deletes maintain referential integrity
- Indices are created for optimal query performance
- The system is designed to be scalable and maintainable
