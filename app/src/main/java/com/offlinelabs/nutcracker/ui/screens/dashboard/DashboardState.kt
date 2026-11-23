package com.offlinelabs.nutcracker.ui.screens.dashboard

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import com.offlinelabs.nutcracker.FoodLogApplication
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.ExerciseGrouping
import com.offlinelabs.nutcracker.data.GymSelection
import com.offlinelabs.nutcracker.data.ListSort
import com.offlinelabs.nutcracker.data.MealGrouping
import com.offlinelabs.nutcracker.data.SettingsManager
import com.offlinelabs.nutcracker.data.dao.DailyExerciseEntry
import com.offlinelabs.nutcracker.data.dao.DailyNutritionEntry
import com.offlinelabs.nutcracker.data.dao.DailyTotals
import com.offlinelabs.nutcracker.data.exercisesForGym
import com.offlinelabs.nutcracker.data.groupExercises
import com.offlinelabs.nutcracker.data.groupMeals
import com.offlinelabs.nutcracker.data.mapper.FoodInfoToMealMapper
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExternalExercise
import com.offlinelabs.nutcracker.data.model.FoodInfo
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.data.model.MealCategoryMeal
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.data.model.Recipe
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.data.model.toInternalExercise
import com.offlinelabs.nutcracker.data.reorderWithinGroup
import com.offlinelabs.nutcracker.data.resolveGymSelection
import com.offlinelabs.nutcracker.data.service.ImageDownloadService
import com.offlinelabs.nutcracker.data.sortExercises
import com.offlinelabs.nutcracker.data.sortMeals
import com.offlinelabs.nutcracker.ui.components.dialogs.defaultCheckInDateTime
import com.offlinelabs.nutcracker.ui.components.gym.GymDialogsState
import com.offlinelabs.nutcracker.ui.components.gym.rememberGymDialogsState
import com.offlinelabs.nutcracker.ui.components.routine.routinesForGym
import com.offlinelabs.nutcracker.ui.components.tutorial.TutorialState
import com.offlinelabs.nutcracker.util.logger.AppLogger
import com.offlinelabs.nutcracker.util.logger.safeSuspendExecute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

/** editingRoutineId value meaning "create a new routine". */
internal const val NEW_ROUTINE_ID = -1L

/** A delete waiting for the user's confirmation in the shared ConfirmDeleteDialog. */
internal class PendingDelete(val title: String, val message: String, val onConfirm: () -> Unit)

/** The dashboard values that survive rotation (and process death). */
internal class DashboardSavedState(
    val showAddMealDialog: MutableState<Boolean>,
    val editMealDefinitionId: MutableState<Long?>,
    val selectedGymId: MutableState<Long?>,
    val mealSort: MutableState<ListSort>,
    val mealGrouping: MutableState<MealGrouping>,
    val mealDescending: MutableState<Boolean>,
    val exerciseSort: MutableState<ListSort>,
    val exerciseDescending: MutableState<Boolean>,
    val exerciseGrouping: MutableState<ExerciseGrouping>,
    val showMealCategories: MutableState<Boolean>,
    val showRoutines: MutableState<Boolean>,
    val editingRoutineId: MutableState<Long?>
)

/**
 * Everything the dashboard shows and edits: the data loaded from the database, which dialog is
 * open, the list order and grouping, and the shared actions (snackbars, check-in time, list
 * reordering). Kept in one remembered holder so the screen can be split into small composables
 * (the header, each tab, the dialog groups) that read and write it directly.
 */
@Stable
internal class DashboardState(
    val context: Context,
    val settingsManager: SettingsManager?,
    val coroutineScope: CoroutineScope,
    val pagerState: PagerState,
    val gymDialogsState: GymDialogsState,
    /** The Home tab's list (the tutorial scrolls it). */
    val lazyListState: LazyListState,
    saved: DashboardSavedState,
    initialDate: LocalDate
) {
    private val app = context.applicationContext as FoodLogApplication
    val foodLogRepository = app.foodLogRepository
    val externalExerciseService = app.externalExerciseService
    val exerciseImageService = app.exerciseImageService
    val imageDownloadService = ImageDownloadService(context)
    val snackbarHostState = SnackbarHostState()

    // ----- Data from the database -----
    var meals by mutableStateOf(emptyList<Meal>())
    var exercises by mutableStateOf(emptyList<Exercise>())
    var recipes by mutableStateOf(emptyList<Recipe>())
    var gyms by mutableStateOf(emptyList<Gym>())
    var userGoal by mutableStateOf(UserGoal.default())
    var dailyMealCheckIns by mutableStateOf(emptyList<DailyNutritionEntry>())
    var dailyExerciseLogs by mutableStateOf(emptyList<DailyExerciseEntry>())
    /** Heaviest logged weight per exercise: one query for the whole Exercise tab. */
    var exerciseMaxWeights by mutableStateOf<Map<Long, Double>>(emptyMap())
    var refreshCheckIns by mutableIntStateOf(0)
    var dailyTotalsConsumed by mutableStateOf<DailyTotals?>(null)
    var foodCalories by mutableDoubleStateOf(0.0)
    var exerciseCaloriesBurned by mutableDoubleStateOf(0.0)
    var tefCaloriesBurned by mutableDoubleStateOf(0.0)
    var includeExerciseCalories by mutableStateOf(true)
    var includeTEFBonus by mutableStateOf(false)
    var pills by mutableStateOf(emptyList<Pill>())
    var pillCheckInsMap by mutableStateOf<Map<Long, PillCheckIn>>(emptyMap())
    var refreshPillCheckIns by mutableIntStateOf(0)
    var mealLastCheckIns by mutableStateOf<Map<Long, String>>(emptyMap())
    var exerciseLastCheckIns by mutableStateOf<Map<Long, String>>(emptyMap())
    var routines by mutableStateOf(emptyList<Routine>())
    var routineMemberships by mutableStateOf(emptyList<RoutineExercise>())
    var mealCategories by mutableStateOf(emptyList<MealCategory>())
    var mealCategoryLinks by mutableStateOf(emptyList<MealCategoryMeal>())

    // ----- Dialogs -----
    var showSetGoalDialog by mutableStateOf(false)
    // Saveable so the Add Meal form survives rotation and its saved draft is consumed by the same
    // dialog instead of leaking into the next one that opens.
    var showAddMealDialog by saved.showAddMealDialog
    var showBarcodeScanDialog by mutableStateOf(false)
    var showFoodSearchDialog by mutableStateOf(false)
    var showFoodInfoDialog by mutableStateOf<FoodInfo?>(null)
    var showServingSizeDialog by mutableStateOf<FoodInfo?>(null)
    var currentBarcode by mutableStateOf<String?>(null)
    var showCheckInMealDialog by mutableStateOf<Meal?>(null)
    var showAddExerciseDialog by mutableStateOf(false)
    var showCheckInExerciseDialog by mutableStateOf<Exercise?>(null)
    var showUnifiedExerciseDetailDialog by mutableStateOf<Exercise?>(null)
    var showUnifiedMealDetailDialog by mutableStateOf<Meal?>(null)
    var showSelectRecipeDialog by mutableStateOf(false)
    var showCreateRecipeDialog by mutableStateOf(false)
    var editingRecipe by mutableStateOf<Recipe?>(null)
    var selectedExternalExercise by mutableStateOf<ExternalExercise?>(null)
    var selectedExerciseForEdit by mutableStateOf<Exercise?>(null)
    var selectedExerciseForCheckIn by mutableStateOf<Exercise?>(null)
    var lastExerciseLog by mutableStateOf<ExerciseLog?>(null)
    var maxExerciseWeight by mutableStateOf<Double?>(null)
    var showCalendarDialog by mutableStateOf(false)
    var showEditMealDialog by mutableStateOf<DailyNutritionEntry?>(null)
    var showEditExerciseDialog by mutableStateOf<DailyExerciseEntry?>(null)
    var showEditPillDialog by mutableStateOf<Pill?>(null)
    var pendingDelete by mutableStateOf<PendingDelete?>(null)
    var showAddPillDialog by mutableStateOf(false)
    var showMealCategories by saved.showMealCategories
    // Routines dialogs: the list, and the editor (NEW_ROUTINE_ID = creating one)
    var showRoutines by saved.showRoutines
    var editingRoutineId by saved.editingRoutineId

    // Meal whose definition is being edited. Only the id is saved across rotation; the Meal is
    // re-resolved from [meals] (the in-session copy is preferred, as it may be newer than [meals]).
    var editMealDefinitionId by saved.editMealDefinitionId
    private var editMealDefinitionCopy by mutableStateOf<Meal?>(null)
    val showEditMealDefinitionDialog: Meal?
        get() = editMealDefinitionId?.let { id ->
            editMealDefinitionCopy?.takeIf { it.id == id } ?: meals.find { it.id == id }
        }

    fun openEditMealDefinition(meal: Meal?) {
        editMealDefinitionCopy = meal
        editMealDefinitionId = meal?.id
    }

    // ----- Day shown in the header -----
    var selectedDate by mutableStateOf(initialDate)
    /** DB/query key (yyyy-MM-dd); must never depend on translations. */
    val selectedDateString: String by derivedStateOf {
        selectedDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * New check-ins default to the day shown in the header (today: now; other days: that day at the
     * current time), since the Food/Exercise tabs have no date of their own.
     */
    fun newCheckInDateTime(): java.util.Date = defaultCheckInDateTime(selectedDate)

    // ----- Exercise tab gym dropdown -----
    // [selectedGymId] (null = All gyms) is restored from settings and survives rotation;
    // [gymSelection] is the resolved value (stale ids fall back to All gyms).
    var selectedGymId by saved.selectedGymId
    val gymSelection: GymSelection get() = resolveGymSelection(selectedGymId, gyms)
    /** A gym shows its own and the shared exercises (and its routines); All gyms shows everything. */
    val selectedGymForList: Long? get() = (gymSelection as? GymSelection.Specific)?.gymId

    fun selectGym(gymId: Long?) {
        selectedGymId = gymId
        settingsManager?.lastSelectedGymId = gymId
    }

    // ----- Order and grouping of the Food and Exercise tab lists (persisted in settings) -----
    var mealSort by saved.mealSort
    var mealGrouping by saved.mealGrouping
    var mealDescending by saved.mealDescending
    var exerciseSort by saved.exerciseSort
    var exerciseDescending by saved.exerciseDescending
    var exerciseGrouping by saved.exerciseGrouping

    /** Locale-aware, case- and accent-insensitive name order ("Água" next to "Arroz"). */
    private val nameComparator: Comparator<String> = run {
        val collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }
        Comparator { a, b -> collator.compare(a, b) }
    }

    val listedRoutines: List<Routine> by derivedStateOf { routinesForGym(routines, selectedGymForList) }

    /** Group by routine is offered once the listed gym(s) have routines. */
    val exerciseGroupings: List<ExerciseGrouping>
        get() = if (listedRoutines.isEmpty()) ExerciseGrouping.entries - ExerciseGrouping.ROUTINE else ExerciseGrouping.entries

    val effectiveExerciseGrouping: ExerciseGrouping
        get() = if (exerciseGrouping in exerciseGroupings) exerciseGrouping else ExerciseGrouping.GYM

    val listedExercises: List<Exercise> by derivedStateOf { exercisesForGym(exercises, selectedGymForList) }

    val exerciseGroups by derivedStateOf {
        groupExercises(
            sortExercises(listedExercises, exerciseSort, exerciseLastCheckIns, nameComparator, exerciseDescending),
            effectiveExerciseGrouping, listedRoutines, routineMemberships, gyms, selectedGymForList,
            // A routine keeps its own order in custom sort; any other sort applies inside it too
            routineOrder = exerciseSort == ListSort.CUSTOM
        )
    }

    val mealGroups by derivedStateOf {
        groupMeals(
            sortMeals(meals, mealSort, mealLastCheckIns, nameComparator, mealDescending),
            mealGrouping, mealCategories, mealCategoryLinks
        )
    }

    fun changeMealSort(sort: ListSort) {
        mealSort = sort
        mealDescending = sort.defaultDescending
        settingsManager?.mealListSort = sort
        settingsManager?.mealListDescending = sort.defaultDescending
    }

    fun changeMealDescending(descending: Boolean) {
        mealDescending = descending
        settingsManager?.mealListDescending = descending
    }

    fun changeMealGrouping(grouping: MealGrouping) {
        mealGrouping = grouping
        settingsManager?.mealListGrouping = grouping
    }

    fun changeExerciseSort(sort: ListSort) {
        exerciseSort = sort
        exerciseDescending = sort.defaultDescending
        settingsManager?.exerciseListSort = sort
        settingsManager?.exerciseListDescending = sort.defaultDescending
    }

    fun changeExerciseDescending(descending: Boolean) {
        exerciseDescending = descending
        settingsManager?.exerciseListDescending = descending
    }

    fun changeExerciseGrouping(grouping: ExerciseGrouping) {
        exerciseGrouping = grouping
        settingsManager?.exerciseListGrouping = grouping
    }

    /** A drag reorders one group; its items swap into the slots they held in the full custom order. */
    fun saveMealOrder(@Suppress("UNUSED_PARAMETER") groupKey: String, groupOrder: List<Long>) {
        coroutineScope.launch {
            val custom = sortMeals(meals, ListSort.CUSTOM, emptyMap(), nameComparator).map { it.id }
            foodLogRepository.applyMealSortOrder(reorderWithinGroup(custom, groupOrder))
        }
    }

    /** Routine groups keep the routine's own order; any other group edits the custom order. */
    fun saveExerciseOrder(groupKey: String, groupOrder: List<Long>) {
        val routineId = exerciseGroups.firstOrNull { it.key == groupKey }?.routineId
        coroutineScope.launch {
            if (routineId != null) {
                foodLogRepository.applyRoutineExerciseOrder(routineId, groupOrder)
            } else {
                val custom = sortExercises(exercises, ListSort.CUSTOM, emptyMap(), nameComparator).map { it.id }
                foodLogRepository.applyExerciseSortOrder(reorderWithinGroup(custom, groupOrder))
            }
        }
    }

    // ----- Snackbars -----
    fun showSnackbar(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message = message) }
    }

    /**
     * After a successful check-in: on Food/Exercise stay on the tab and offer a way to see it on
     * Home; on Home just show the usual confirmation. Non-blocking, so the code after a check-in
     * doesn't wait for the snackbar to go away.
     */
    fun showCheckInLoggedSnackbar(homeMessage: String) {
        coroutineScope.launch {
            if (pagerState.currentPage == DASHBOARD_PAGE_HOME) {
                snackbarHostState.showSnackbar(message = homeMessage)
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snackbar_logged),
                    actionLabel = context.getString(R.string.snackbar_view),
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    pagerState.animateScrollToPage(DASHBOARD_PAGE_HOME)
                }
            }
        }
    }

    // ----- Home toggles -----
    fun togglePill(pillId: Long) {
        coroutineScope.launch {
            try {
                val checkIn = pillCheckInsMap[pillId]
                if (checkIn == null) {
                    // Today: the current time; another day: 00:00
                    val timestamp = if (selectedDate == LocalDate.now()) {
                        java.time.LocalDateTime.now()
                    } else {
                        java.time.LocalDateTime.of(selectedDate, java.time.LocalTime.of(0, 0))
                    }
                    val newCheckIn = PillCheckIn(pillId = pillId, timestamp = timestamp)
                    foodLogRepository.insertPillCheckIn(newCheckIn)
                    // Optimistic update, then refresh so the Flow catches up
                    pillCheckInsMap = pillCheckInsMap + (pillId to newCheckIn)
                    refreshPillCheckIns++
                    snackbarHostState.showSnackbar(message = context.getString(R.string.daily_supplement_taken))
                } else {
                    foodLogRepository.deletePillCheckInByPillIdAndDate(pillId, selectedDateString)
                    pillCheckInsMap = pillCheckInsMap - pillId
                    refreshPillCheckIns++
                    snackbarHostState.showSnackbar(message = context.getString(R.string.daily_supplement_removed))
                }
            } catch (e: Exception) {
                AppLogger.exception("DashboardScreen", "Failed to update pill status", e, mapOf(
                    "selectedDate" to selectedDateString,
                    "pillId" to pillId
                ))
                snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_update_pill_status))
            }
        }
    }

    fun toggleExerciseCalories() {
        includeExerciseCalories = !includeExerciseCalories
        showSnackbar(context.getString(
            if (includeExerciseCalories) R.string.exercise_bonus_calories_on else R.string.exercise_bonus_calories_off
        ))
    }

    fun toggleTefBonus() {
        includeTEFBonus = !includeTEFBonus
        showSnackbar(context.getString(
            if (includeTEFBonus) R.string.tef_bonus_calories_on else R.string.tef_bonus_calories_off
        ))
    }

    // ----- Saving imported items -----
    /**
     * Inserts [exercise] and, when it comes from the exercise database ([external]), overlays the
     * imported data (instructions, equipment, downloaded images...) with the user's values from
     * the form. The update happens even if the image downloads fail. Returns the new id.
     * @param keepFormMusclesAndVisibility also keep the form's muscles and visibility (the quick
     * save / check-in buttons); otherwise the imported ones are used.
     */
    suspend fun insertExerciseWithExternalData(
        exercise: Exercise,
        external: ExternalExercise?,
        keepFormMusclesAndVisibility: Boolean
    ): Long {
        val exerciseId = foodLogRepository.insertExercise(exercise)
        if (external != null) {
            val imagePaths = if (external.images.isNotEmpty()) {
                val urls = external.images.map { externalExerciseService.getImageUrl(it) }
                exerciseImageService.downloadAndStoreImages(urls, exerciseId.toString()).also { paths ->
                    if (paths.isEmpty()) AppLogger.w("DashboardScreen", "No images downloaded for exercise ID: $exerciseId")
                }
            } else {
                emptyList()
            }
            val imported = external.toInternalExercise(imagePaths)
            foodLogRepository.updateExercise(
                imported.copy(
                    id = exerciseId,
                    gymId = exercise.gymId,
                    primaryMuscles = if (keepFormMusclesAndVisibility) exercise.primaryMuscles else imported.primaryMuscles,
                    kcalBurnedPerUnit = exercise.kcalBurnedPerUnit,
                    defaultWeight = exercise.defaultWeight,
                    defaultReps = exercise.defaultReps,
                    defaultSets = exercise.defaultSets,
                    // The user's personal notes stay separate from the imported instructions
                    notes = exercise.notes,
                    isVisible = if (keepFormMusclesAndVisibility) exercise.isVisible else imported.isVisible
                )
            )
        }
        return exerciseId
    }

    /** Saves a food found by barcode or search as a meal (with its image downloaded). Returns its id. */
    suspend fun insertMealFromFoodInfo(
        foodInfo: FoodInfo,
        servingSizeValue: Double,
        servingSizeUnit: ServingSizeUnit,
        barcode: String?,
        isVisible: Boolean
    ): Long {
        val meal = FoodInfoToMealMapper.mapToMeal(
            foodInfo = foodInfo,
            servingSizeValue = servingSizeValue,
            servingSizeUnit = servingSizeUnit,
            barcode = barcode,
            source = if (barcode != null) "barcode" else "search",
            isVisible = isVisible
        )
        val localImagePath = foodInfo.imageUrl?.let { imageDownloadService.downloadAndSaveImage(it) }
        return foodLogRepository.insertMeal(meal.copy(localImagePath = localImagePath))
    }

    // ----- Tutorial -----
    val tutorialState = TutorialState()
    var elementCoordinates by mutableStateOf<Map<String, Pair<Offset, Size>>>(emptyMap())
    var showHistorySpotlight by mutableStateOf(false)

    /**
     * Records where a tutorial target is. Positions change every frame while the pager is swiped;
     * writing them then would recompose the whole dashboard per frame. The tutorial only runs with
     * the pager at rest on Home, so mid-swipe values are useless.
     */
    val registerElementCoordinates: (String, Offset, Size) -> Unit = { id, offset, size ->
        val entry = offset to size
        if (!pagerState.isScrollInProgress && elementCoordinates[id] != entry) {
            elementCoordinates = elementCoordinates + (id to entry)
        }
    }
}

@Composable
internal fun rememberDashboardState(
    settingsManager: SettingsManager?,
    initialPage: Int,
    initialDate: LocalDate?
): DashboardState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Bottom tabs: 0 = Food, 1 = Home (default), 2 = Exercise. rememberPagerState is saveable.
    val pagerState = rememberPagerState(initialPage = initialPage) { 3 }
    val gymDialogsState = rememberGymDialogsState()
    val lazyListState = rememberLazyListState()
    val saved = DashboardSavedState(
        showAddMealDialog = rememberSaveable { mutableStateOf(false) },
        editMealDefinitionId = rememberSaveable { mutableStateOf<Long?>(null) },
        selectedGymId = rememberSaveable { mutableStateOf(settingsManager?.lastSelectedGymId) },
        mealSort = rememberSaveable { mutableStateOf(settingsManager?.mealListSort ?: ListSort.ALPHABETICAL) },
        mealGrouping = rememberSaveable { mutableStateOf(settingsManager?.mealListGrouping ?: MealGrouping.NONE) },
        mealDescending = rememberSaveable { mutableStateOf(settingsManager?.mealListDescending ?: false) },
        exerciseSort = rememberSaveable { mutableStateOf(settingsManager?.exerciseListSort ?: ListSort.ALPHABETICAL) },
        exerciseDescending = rememberSaveable { mutableStateOf(settingsManager?.exerciseListDescending ?: false) },
        exerciseGrouping = rememberSaveable { mutableStateOf(settingsManager?.exerciseListGrouping ?: ExerciseGrouping.GYM) },
        showMealCategories = rememberSaveable { mutableStateOf(false) },
        showRoutines = rememberSaveable { mutableStateOf(false) },
        editingRoutineId = rememberSaveable { mutableStateOf<Long?>(null) }
    )
    return remember {
        DashboardState(
            context = context,
            settingsManager = settingsManager,
            coroutineScope = coroutineScope,
            pagerState = pagerState,
            gymDialogsState = gymDialogsState,
            lazyListState = lazyListState,
            saved = saved,
            initialDate = initialDate ?: LocalDate.now()
        )
    }
}

/** Keeps the dashboard's data in sync with the database (and the day shown). */
@Composable
internal fun DashboardState.CollectDashboardData(
    onPageChanged: (Int) -> Unit,
    onDateChanged: (LocalDate) -> Unit
) {
    LaunchedEffect(selectedDate) { onDateChanged(selectedDate) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) }
    }

    // Whole-library data
    LaunchedEffect(Unit) {
        launch { foodLogRepository.getAllMeals().collectLatest { meals = it } }
        launch { foodLogRepository.getAllRecipes().collectLatest { recipes = it } }
        launch { foodLogRepository.getAllExercises().collectLatest { exercises = it } }
        launch { foodLogRepository.getMaxWeights().collectLatest { exerciseMaxWeights = it } }
        launch {
            foodLogRepository.getMealLastCheckIns().collectLatest { rows ->
                mealLastCheckIns = rows.mapNotNull { r -> r.lastCheckIn?.let { r.id to it } }.toMap()
            }
        }
        launch {
            foodLogRepository.getExerciseLastCheckIns().collectLatest { rows ->
                exerciseLastCheckIns = rows.mapNotNull { r -> r.lastCheckIn?.let { r.id to it } }.toMap()
            }
        }
        launch { foodLogRepository.getVisibleRoutines().collectLatest { routines = it } }
        launch { foodLogRepository.getVisibleRoutineExercises().collectLatest { routineMemberships = it } }
        launch { foodLogRepository.getVisibleMealCategories().collectLatest { mealCategories = it } }
        launch { foodLogRepository.getVisibleMealCategoryLinks().collectLatest { mealCategoryLinks = it } }
        launch {
            var firstEmission = true
            foodLogRepository.getVisibleGyms().collectLatest { gymList ->
                gyms = gymList
                // Drop a stale persisted selection once, when the real gym list first arrives. Later
                // emissions are left alone so a just-added gym isn't deselected by a late emission.
                if (firstEmission && selectedGymId != null && gymList.none { it.id == selectedGymId }) {
                    selectGym(null)
                }
                firstEmission = false
            }
        }
        launch {
            foodLogRepository.getUserGoal().collectLatest { goalFromDb ->
                userGoal = goalFromDb ?: UserGoal.default()
                if (goalFromDb == null) {
                    coroutineScope.launch {
                        safeSuspendExecute(
                            operation = "Initialize default user goal",
                            block = { foodLogRepository.upsertUserGoal(UserGoal.default()) },
                            onError = { e ->
                                AppLogger.exception("DashboardScreen", "Failed to save default user goal", e)
                                showSnackbar(context.getString(R.string.failed_to_save_goal))
                            }
                        )
                    }
                }
            }
        }
        launch {
            // Pills, creating the default one if there are none
            foodLogRepository.getAllPills().collectLatest { pillList ->
                pills = pillList
                if (pillList.isEmpty()) {
                    coroutineScope.launch {
                        safeSuspendExecute(
                            operation = "Create default pill",
                            block = { foodLogRepository.insertPill(Pill(name = context.getString(R.string.default_pill_name))) },
                            onError = { e ->
                                AppLogger.exception("DashboardScreen", "Failed to create default pill", e)
                                showSnackbar(context.getString(R.string.failed_to_create_default_pill))
                            }
                        )
                    }
                }
            }
        }
    }

    // Data of the day shown
    LaunchedEffect(selectedDateString, refreshCheckIns) {
        foodLogRepository.getCheckInsByDate(selectedDateString).collectLatest { dailyMealCheckIns = it }
    }
    LaunchedEffect(selectedDateString) {
        launch { foodLogRepository.getExerciseLogsByDate(selectedDateString).collectLatest { dailyExerciseLogs = it } }
        launch { foodLogRepository.getDailyExerciseCalories(selectedDateString).collectLatest { exerciseCaloriesBurned = it } }
        launch {
            // Raw food calories (without any booster) and the TEF bonus they earn
            foodLogRepository.getDailyNutrientTotals(selectedDateString).collectLatest { totals ->
                tefCaloriesBurned = totals?.let { com.offlinelabs.nutcracker.utils.TEFCalculator.calculateTEFBonus(it) } ?: 0.0
                foodCalories = totals?.totalCalories ?: 0.0
            }
        }
    }
    LaunchedEffect(selectedDateString, includeExerciseCalories, includeTEFBonus) {
        foodLogRepository.getDailyCombinedTotals(selectedDateString, includeExerciseCalories, includeTEFBonus)
            .collectLatest { dailyTotalsConsumed = it }
    }
    // Check-ins of every pill on the day shown
    LaunchedEffect(selectedDateString, pills, refreshPillCheckIns) {
        val currentPills = pills
        if (currentPills.isEmpty()) {
            pillCheckInsMap = emptyMap()
        } else {
            val flows = currentPills.map { foodLogRepository.getPillCheckInByPillIdAndDate(it.id, selectedDateString) }
            combine(flows) { checkIns ->
                currentPills.indices.mapNotNull { i -> checkIns[i]?.let { currentPills[i].id to it } }.toMap()
            }.collectLatest { pillCheckInsMap = it }
        }
    }
}
