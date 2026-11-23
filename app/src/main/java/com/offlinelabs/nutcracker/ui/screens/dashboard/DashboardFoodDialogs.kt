package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.offlinelabs.nutcracker.FoodLogApplication
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.mapper.FoodInfoMapper
import com.offlinelabs.nutcracker.data.model.FoodInfo
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.OpenFoodFactsResponse
import com.offlinelabs.nutcracker.data.model.RecipeIngredient
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.ui.components.dialogs.AddMealDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.CheckInMealDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.CreateRecipeDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.EditMealDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.FoodInfoDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.FoodSearchDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.GoogleCodeScannerDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.SelectRecipeDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.ServingSizeDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.UnifiedMealDetailsDialog
import com.offlinelabs.nutcracker.ui.components.mealcategory.MealCategoriesDialog
import com.offlinelabs.nutcracker.util.logger.AppLogger
import kotlinx.coroutines.launch

/** Dialogs opened from the Food tab: adding, scanning and searching foods, recipes and meal types. */
@Composable
internal fun DashboardState.FoodDialogs() {
    if (showSelectRecipeDialog) SelectRecipeDialogHost()
    if (showCreateRecipeDialog) CreateRecipeDialogHost()
    if (showAddMealDialog) AddMealDialogHost()
    showCheckInMealDialog?.let { CheckInMealDialogHost(it) }
    if (showMealCategories) MealCategoriesDialogHost()
    if (showFoodSearchDialog) FoodSearchDialogHost()
    if (showBarcodeScanDialog) BarcodeScanDialogHost()
    // The food info dialog steps aside while its serving size dialog is open
    showFoodInfoDialog?.let { if (showServingSizeDialog == null) FoodInfoDialogHost(it) }
    showServingSizeDialog?.let { ServingSizeDialogHost(it) }
    showUnifiedMealDetailDialog?.let { UnifiedMealDetailsDialogHost(it) }
    showEditMealDefinitionDialog?.let { EditMealDefinitionDialogHost(it) }
}

@Composable
private fun DashboardState.SelectRecipeDialogHost() {
    SelectRecipeDialog(
        recipes = recipes,
        onDismiss = { showSelectRecipeDialog = false },
        onCreateNew = {
            showSelectRecipeDialog = false
            editingRecipe = null
            showCreateRecipeDialog = true
        },
        onSelectRecipe = { recipe ->
            showSelectRecipeDialog = false
            coroutineScope.launch {
                try {
                    // Each log gets its own hidden nutrition snapshot meal
                    val snapshotMealId = foodLogRepository.createRecipeSnapshotMeal(recipe.id)
                    if (snapshotMealId != null) {
                        val mealCheckIn = MealCheckIn.create(
                            mealId = snapshotMealId,
                            servingSize = 1.0,
                            notes = "recipe:${recipe.id}",
                            customDateTime = newCheckInDateTime()
                        )
                        foodLogRepository.insertMealCheckIn(mealCheckIn)
                        showCheckInLoggedSnackbar(context.getString(R.string.check_in_completed_success))
                    } else {
                        // Recipe nutrition could not be calculated (e.g., no ingredients)
                        snackbarHostState.showSnackbar(message = context.getString(R.string.check_in_completed_error))
                    }
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to log recipe", e)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.check_in_completed_error))
                }
            }
        },
        onEditRecipe = { recipe ->
            showSelectRecipeDialog = false
            editingRecipe = recipe
            showCreateRecipeDialog = true
        },
        onDeleteRecipe = { recipe ->
            coroutineScope.launch {
                try {
                    foodLogRepository.deleteRecipe(recipe)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.recipe_deleted))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to delete recipe", e)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.failed_delete_recipe))
                }
            }
        }
    )
}

@Composable
private fun DashboardState.CreateRecipeDialogHost() {
    var loadedIngredients by remember { mutableStateOf<List<Pair<RecipeIngredient, Meal?>>>(emptyList()) }
    // Load the existing ingredients when editing
    LaunchedEffect(editingRecipe) {
        loadedIngredients = editingRecipe?.let { recipe ->
            foodLogRepository.getIngredientsByRecipeIdSync(recipe.id).map { ingredient ->
                ingredient to meals.find { it.id == ingredient.mealId }
            }
        } ?: emptyList()
    }
    CreateRecipeDialog(
        recipe = editingRecipe,
        meals = meals,
        initialIngredients = loadedIngredients,
        onDismiss = {
            showCreateRecipeDialog = false
            editingRecipe = null
        },
        onSave = { recipe, ingredients ->
            coroutineScope.launch {
                try {
                    val recipeId = if (recipe.id > 0) {
                        foodLogRepository.updateRecipe(recipe)
                        // Replace the old ingredients
                        foodLogRepository.deleteIngredientsByRecipeId(recipe.id)
                        recipe.id
                    } else {
                        foodLogRepository.insertRecipe(recipe)
                    }
                    foodLogRepository.insertRecipeIngredients(ingredients.map { it.copy(recipeId = recipeId) })
                    snackbarHostState.showSnackbar(
                        message = context.getString(if (recipe.id > 0) R.string.recipe_updated else R.string.recipe_created)
                    )
                    showCreateRecipeDialog = false
                    editingRecipe = null
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to save recipe", e)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.failed_save_recipe))
                }
            }
        }
    )
}

@Composable
private fun DashboardState.AddMealDialogHost() {
    AddMealDialog(
        categories = mealCategories,
        onManageCategories = { showMealCategories = true },
        onDismiss = { showAddMealDialog = false },
        onAddMeal = { newMeal, categoryIds ->
            coroutineScope.launch {
                try {
                    val newId = foodLogRepository.insertMeal(newMeal)
                    foodLogRepository.setMealCategories(newId, categoryIds)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.meal_added_success))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to add meal", e, mapOf("mealName" to newMeal.name))
                    snackbarHostState.showSnackbar(message = context.getString(R.string.meal_add_error))
                }
            }
            showAddMealDialog = false
        }
    )
}

@Composable
private fun DashboardState.CheckInMealDialogHost(mealToCheckIn: Meal) {
    CheckInMealDialog(
        meal = mealToCheckIn,
        defaultDateTime = remember(mealToCheckIn) { newCheckInDateTime() },
        onDismiss = { showCheckInMealDialog = null },
        onCheckIn = { mealCheckIn ->
            coroutineScope.launch {
                try {
                    foodLogRepository.insertMealCheckIn(mealCheckIn)
                    showCheckInLoggedSnackbar(context.getString(R.string.check_in_completed_success))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to complete meal check-in", e, mapOf(
                        "mealId" to mealCheckIn.mealId,
                        "date" to mealCheckIn.checkInDate
                    ))
                    snackbarHostState.showSnackbar(message = context.getString(R.string.check_in_completed_error))
                }
            }
            showCheckInMealDialog = null
        }
    )
}

@Composable
private fun DashboardState.MealCategoriesDialogHost() {
    MealCategoriesDialog(
        categories = mealCategories,
        onDismiss = { showMealCategories = false },
        onAdd = { name -> coroutineScope.launch { foodLogRepository.upsertMealCategory(MealCategory(name = name)) } },
        // A renamed built-in category keeps the typed text from then on
        onRename = { category, name ->
            coroutineScope.launch { foodLogRepository.upsertMealCategory(category.copy(name = name, defaultKey = null)) }
        },
        onDelete = { category -> coroutineScope.launch { foodLogRepository.deleteMealCategory(category.id) } },
        onReorder = { ids -> coroutineScope.launch { foodLogRepository.applyMealCategoryOrder(ids) } }
    )
}

@Composable
private fun DashboardState.FoodSearchDialogHost() {
    val app = context.applicationContext as FoodLogApplication
    FoodSearchDialog(
        onDismiss = { showFoodSearchDialog = false },
        onProductSelected = { product ->
            val foodInfo = FoodInfoMapper.mapToFoodInfo(
                OpenFoodFactsResponse(status = 1, statusVerbose = "found", product = product),
                currentLanguage = app.settingsManager.currentAppLanguage
            )
            if (foodInfo != null) {
                showFoodInfoDialog = foodInfo
            } else {
                showSnackbar(context.getString(R.string.unable_to_process_food_info))
            }
        },
        openFoodFactsService = app.openFoodFactsService,
        currentLanguage = app.settingsManager.currentAppLanguage
    )
}

@Composable
private fun DashboardState.BarcodeScanDialogHost() {
    GoogleCodeScannerDialog(
        onDismiss = { showBarcodeScanDialog = false },
        onBarcodeScanned = { barcode ->
            // Kept for the meal that the serving size dialog saves
            currentBarcode = barcode
            val app = context.applicationContext as FoodLogApplication
            coroutineScope.launch {
                try {
                    app.openFoodFactsService.getProductByBarcode(barcode).fold(
                        onSuccess = { apiResponse ->
                            val foodInfo = FoodInfoMapper.mapToFoodInfo(
                                apiResponse,
                                currentLanguage = app.settingsManager.currentAppLanguage
                            )
                            if (foodInfo != null) {
                                showFoodInfoDialog = foodInfo
                            } else {
                                snackbarHostState.showSnackbar(message = context.getString(R.string.food_info_not_found_for_barcode))
                            }
                        },
                        onFailure = { error ->
                            AppLogger.exception("DashboardScreen", "Failed to fetch food info", error, mapOf("barcode" to barcode))
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.failed_to_fetch_food_info_with_reason, error.message.orEmpty())
                            )
                        }
                    )
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Unexpected error fetching food info", e, mapOf("barcode" to barcode))
                    snackbarHostState.showSnackbar(message = context.getString(R.string.error_fetching_food_info))
                }
            }
            showBarcodeScanDialog = false
        }
    )
}

@Composable
private fun DashboardState.FoodInfoDialogHost(foodInfo: FoodInfo) {
    FoodInfoDialog(
        foodInfo = foodInfo,
        // currentBarcode is kept: it belongs to the whole scan flow
        onBack = { showFoodInfoDialog = null },
        onSelect = {
            showFoodInfoDialog = null
            showServingSizeDialog = foodInfo
        }
    )
}

@Composable
private fun DashboardState.ServingSizeDialogHost(foodInfo: FoodInfo) {
    /**
     * Saves the food as a meal ([isVisible] false: only for this check-in) and optionally checks
     * it in on the day shown. The dialog state is cleared first so it can't show again meanwhile.
     */
    fun save(servingSizeValue: Double, servingSizeUnit: ServingSizeUnit, isVisible: Boolean, checkIn: Boolean, successMessage: String) {
        val barcode = currentBarcode
        showServingSizeDialog = null
        showFoodInfoDialog = null
        currentBarcode = null
        coroutineScope.launch {
            try {
                val mealId = insertMealFromFoodInfo(foodInfo, servingSizeValue, servingSizeUnit, barcode, isVisible)
                if (checkIn) {
                    foodLogRepository.insertMealCheckIn(
                        MealCheckIn.create(mealId = mealId, servingSize = 1.0, notes = null, customDateTime = newCheckInDateTime())
                    )
                    showCheckInLoggedSnackbar(successMessage)
                    refreshCheckIns++
                } else {
                    snackbarHostState.showSnackbar(message = successMessage)
                }
            } catch (e: Exception) {
                AppLogger.exception("DashboardScreen", "Failed to save scanned food", e, mapOf("foodName" to foodInfo.name))
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.failed_to_complete_action_with_reason, e.message.orEmpty())
                )
            }
        }
    }

    ServingSizeDialog(
        foodInfo = foodInfo,
        barcode = currentBarcode,
        onDismiss = {
            showServingSizeDialog = null
            showFoodInfoDialog = null
            currentBarcode = null
        },
        onSaveAndCheckIn = { value, unit ->
            save(value, unit, isVisible = true, checkIn = true, context.getString(R.string.meal_saved_and_checked_in))
        },
        onJustSave = { value, unit ->
            save(value, unit, isVisible = true, checkIn = false, context.getString(R.string.meal_saved_success))
        },
        onJustCheckIn = { value, unit ->
            // Not listed in the Food tab, only logged
            save(value, unit, isVisible = false, checkIn = true, context.getString(R.string.check_in_completed_success))
        }
    )
}

@Composable
private fun DashboardState.UnifiedMealDetailsDialogHost(meal: Meal) {
    UnifiedMealDetailsDialog(
        meal = meal,
        onBack = { showUnifiedMealDetailDialog = null },
        onEdit = { mealToEdit ->
            showUnifiedMealDetailDialog = null
            openEditMealDefinition(mealToEdit)
        },
        onCheckIn = {
            showUnifiedMealDetailDialog = null
            showCheckInMealDialog = meal
        }
    )
}

@Composable
private fun DashboardState.EditMealDefinitionDialogHost(meal: Meal) {
    val mealCategoryIds by produceState(emptyList<Long>(), meal.id) {
        value = foodLogRepository.getMealCategoryIds(meal.id)
    }
    EditMealDialog(
        meal = meal,
        categories = mealCategories,
        initialCategoryIds = mealCategoryIds,
        onManageCategories = { showMealCategories = true },
        onDismiss = { openEditMealDefinition(null) },
        onUpdateMeal = { updatedMeal, categoryIds ->
            // Close the edit dialog immediately and update in the background
            openEditMealDefinition(null)
            showUnifiedMealDetailDialog = updatedMeal
            coroutineScope.launch {
                try {
                    foodLogRepository.updateMeal(updatedMeal)
                    foodLogRepository.setMealCategories(updatedMeal.id, categoryIds)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.meal_updated_success))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to update meal", e, mapOf(
                        "mealId" to updatedMeal.id.toString(),
                        "mealName" to updatedMeal.name
                    ))
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.failed_to_update_meal_with_reason, e.message.orEmpty())
                    )
                }
            }
        },
        onDelete = { confirmDeleteMeal(meal) { openEditMealDefinition(null) } }
    )
}
