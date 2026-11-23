package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExternalExercise
import com.offlinelabs.nutcracker.data.repo.FoodLogRepository
import com.offlinelabs.nutcracker.data.service.ExternalExerciseService
import com.offlinelabs.nutcracker.ui.components.ExerciseImageIcon
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.ui.theme.brandSecondaryShade
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.offlinelabs.nutcracker.ui.components.ValueWheel
import com.offlinelabs.nutcracker.ui.components.formatWheelValue
import com.offlinelabs.nutcracker.ui.components.lists.QuickCheckRow
import com.offlinelabs.nutcracker.ui.components.lists.QuickControls
import com.offlinelabs.nutcracker.ui.components.lists.SwipeAction
import com.offlinelabs.nutcracker.ui.components.lists.listSidePadding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import com.offlinelabs.nutcracker.data.ListGroup
import com.offlinelabs.nutcracker.ui.components.lists.DraggableListRow
import com.offlinelabs.nutcracker.ui.components.lists.GroupedReorderState
import com.offlinelabs.nutcracker.ui.components.lists.ListGroupHeader
import com.offlinelabs.nutcracker.ui.components.lists.groupTitleText
import com.offlinelabs.nutcracker.ui.components.lists.rememberGroupedReorderState
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Inline exercise picker shown on the dashboard's Exercise tab. It fills the space it is given and
 * has no dialog chrome. The external-database search and exercise-details sub-views still open as
 * dialogs over it. [header] is rendered above the action buttons and the list (scrolls with them).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerContent(
    exercises: List<Exercise>,
    externalExerciseService: ExternalExerciseService,
    foodLogRepository: FoodLogRepository? = null,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onImportExternalExercise: (ExternalExercise) -> Unit,
    onEditExercise: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    /** Shown above the list when it has no exercise of the selected gym (null = nothing). */
    emptyHint: String? = null,
    /** [exercises] sorted and grouped for display (default: ungrouped). */
    groups: List<ListGroup<Exercise>> = listOf(ListGroup("all", null, exercises)),
    /** Whether rows of a group can be dragged (e.g. custom order, or a routine's own order). */
    canReorder: (ListGroup<Exercise>) -> Boolean = { false },
    /** New order of a dragged group's exercise ids, called with the group key when a row is dropped. */
    onReorder: (groupKey: String, ids: List<Long>) -> Unit = { _, _ -> },
    /** Sort/group controls shown above the list. */
    arrangementBar: @Composable () -> Unit = {},
    /** Rows put their controls (wheels, check-in box) on the left, away from a scrolling right thumb. */
    controlsFirst: Boolean = false,
    /** Exercises with a check-in on the day shown (ticked outside routine groups). */
    checkedInIds: Set<Long> = emptySet(),
    /**
     * (exercise id, routine id) of the day's check-ins; the routine is null when checked in outside
     * a routine group. A routine group ticks its own check-ins and those without a routine.
     */
    checkedInRoutineKeys: Set<Pair<Long, Long?>> = emptySet(),
    /** Heaviest logged weight per exercise id (shown on the row and where the weight wheel starts). */
    maxWeights: Map<Long, Double> = emptyMap(),
    /** Reps of each exercise's last check-in (where a strength row's reps wheel starts). */
    lastReps: Map<Long, Int> = emptyMap(),
    /**
     * Checks the exercise in with the main wheel's value (weight, minutes or reps), the reps wheel
     * (strength) and the routine of the group it was ticked in (null outside routine groups).
     */
    onQuickCheckIn: (exercise: Exercise, value: Double, reps: Int, routineId: Long?) -> Unit = { _, _, _, _ -> },
    /** Removes the exercise's check-ins of the day shown (only those of [routineId] when given). */
    onRemoveCheckIns: (exercise: Exercise, routineId: Long?) -> Unit = { _, _ -> },
    /** Delete from the row's swipe panel (the caller confirms). */
    onDeleteExercise: (Exercise) -> Unit = {}
) {
    var dialogState by remember { mutableStateOf(DialogState.MAIN) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<ExternalExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedExternalExercise by remember { mutableStateOf<ExternalExercise?>(null) }
    var currentFilterCount by remember { mutableIntStateOf(0) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Smart search with filter count trigger
    LaunchedEffect(dialogState, searchQuery, selectedEquipment, selectedMuscle, selectedCategory) {
        // Only needed while the external search sub-view is open
        if (dialogState == DialogState.MAIN) return@LaunchedEffect
        isLoading = true
        delay(300) // Debounce
        
        // Get all exercises first
        val allExercises = externalExerciseService.getAllExercises()
        
        // Apply filters
        val filteredExercises = allExercises.filter { exercise ->
            val matchesQuery = searchQuery.isEmpty() || 
                exercise.name.contains(searchQuery, ignoreCase = true) ||
                exercise.primaryMuscles.any { it.contains(searchQuery, ignoreCase = true) } ||
                exercise.equipment?.contains(searchQuery, ignoreCase = true) == true ||
                exercise.secondaryMuscles.any { it.contains(searchQuery, ignoreCase = true) }
            
            val matchesEquipment = selectedEquipment.isNullOrEmpty() || 
                exercise.equipment?.equals(selectedEquipment, ignoreCase = true) == true
            
            val matchesMuscle = selectedMuscle.isNullOrEmpty() || 
                exercise.primaryMuscles.any { it.equals(selectedMuscle, ignoreCase = true) }
            
            val matchesCategory = selectedCategory.isNullOrEmpty() || 
                exercise.category.equals(selectedCategory, ignoreCase = true)
            
            matchesQuery && matchesEquipment && matchesMuscle && matchesCategory
        }
        
        // Update filter count
        currentFilterCount = filteredExercises.size
        
        // Show results if we have a reasonable number (15 or fewer)
        // OR if user has typed 3+ characters
        val shouldShowResults = filteredExercises.size <= 15 || searchQuery.length >= 3
        
        searchResults = if (shouldShowResults) filteredExercises else emptyList()
        isLoading = false
    }
    
    MainExerciseSelectionContent(
        exercises = exercises,
        onAddExercise = onAddExercise,
        onSelectExercise = onSelectExercise,
        onEditExercise = onEditExercise,
        onSearchExternal = { dialogState = DialogState.SEARCH },
        modifier = modifier,
        header = header,
        emptyHint = emptyHint,
        groups = groups,
        canReorder = canReorder,
        onReorder = onReorder,
        arrangementBar = arrangementBar,
        controlsFirst = controlsFirst,
        checkedInIds = checkedInIds,
        checkedInRoutineKeys = checkedInRoutineKeys,
        maxWeights = maxWeights,
        lastReps = lastReps,
        onQuickCheckIn = onQuickCheckIn,
        onRemoveCheckIns = onRemoveCheckIns,
        onDeleteExercise = onDeleteExercise
    )

    when (dialogState) {
        DialogState.MAIN -> Unit
        
        DialogState.SEARCH -> {
            SearchExternalExercisesDialog(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedEquipment = selectedEquipment,
                onEquipmentChange = { selectedEquipment = it },
                selectedMuscle = selectedMuscle,
                onMuscleChange = { selectedMuscle = it },
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it },
                searchResults = searchResults,
                isLoading = isLoading,
                currentFilterCount = currentFilterCount,
                onBack = { 
                    dialogState = DialogState.MAIN
                    searchQuery = ""
                    searchResults = emptyList()
                },
                onSelectExternalExercise = { exercise ->
                    selectedExternalExercise = exercise
                    dialogState = DialogState.EXTERNAL_DETAILS
                },
                keyboardController = keyboardController,
                externalExerciseService = externalExerciseService
            )
        }
        
        DialogState.EXTERNAL_DETAILS -> {
            selectedExternalExercise?.let { exercise ->
                ExternalExerciseDetailsDialog(
                    exercise = exercise,
                    externalExerciseService = externalExerciseService,
                    onBack = { dialogState = DialogState.SEARCH },
                    onImport = { 
                        dialogState = DialogState.MAIN
                        searchQuery = ""
                        searchResults = emptyList()
                        onImportExternalExercise(exercise)
                    }
                )
            }
        }
    }
}

enum class DialogState {
    MAIN, SEARCH, EXTERNAL_DETAILS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainExerciseSelectionContent(
    exercises: List<Exercise>,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onEditExercise: (Exercise) -> Unit,
    onSearchExternal: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    emptyHint: String? = null,
    groups: List<ListGroup<Exercise>>,
    canReorder: (ListGroup<Exercise>) -> Boolean,
    onReorder: (groupKey: String, ids: List<Long>) -> Unit,
    arrangementBar: @Composable () -> Unit,
    controlsFirst: Boolean,
    checkedInIds: Set<Long>,
    checkedInRoutineKeys: Set<Pair<Long, Long?>>,
    maxWeights: Map<Long, Double>,
    lastReps: Map<Long, Int>,
    onQuickCheckIn: (Exercise, Double, Int, Long?) -> Unit,
    onRemoveCheckIns: (Exercise, Long?) -> Unit,
    onDeleteExercise: (Exercise) -> Unit
) {
    val reorder = rememberGroupedReorderState(groups, prefix = "exercise") { it.id }
    // Only one row's swipe panel is open at a time
    var openRowKey by remember { mutableStateOf<String?>(null) }
    // Wheel values survive rows scrolling out of view (reset when the tab leaves composition)
    val wheelValues = remember { mutableStateMapOf<Long, Double>() }
    val repsValues = remember { mutableStateMapOf<Long, Int>() }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorder.move(from.key, to.key) // moves across groups are ignored
    }

    // Rows span the full width; the other items keep the screen's side padding
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item(key = "header") { Box(Modifier.listSidePadding()) { header() } }
        item(key = "search_database") {
                Button(
                    onClick = onSearchExternal,
                    modifier = Modifier.fillMaxWidth().listSidePadding().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandSecondaryShade(0)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.search_exercise_database))
                }
        }
        item(key = "add_exercise") {
                Button(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth().listSidePadding().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandSecondaryShade(2)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ballot),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.type_information))
                }
        }

        // Existing exercises list
        if (exercises.isEmpty()) {
            item(key = "no_exercises") {
                    Text(
                        text = emptyHint ?: stringResource(R.string.no_exercises_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.listSidePadding().padding(top = 16.dp)
                    )
            }
        } else {
            item(key = "arrangement_bar") {
                Column(modifier = Modifier.listSidePadding().padding(top = 16.dp, bottom = 8.dp)) {
                    arrangementBar()
                }
            }
            if (emptyHint != null) {
                item(key = "empty_hint") {
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.listSidePadding()
                    )
                }
            }
            reorder.groups.forEach { group ->
                // An empty routine still shows (with a hint), so a newly created one is visible
                if (group.items.isEmpty() && group.routineId == null) return@forEach
                group.title?.let { title ->
                    item(key = "exercise_header_${group.key}") {
                        ListGroupHeader(groupTitleText(title), Modifier.listSidePadding().padding(top = 12.dp, bottom = 6.dp))
                    }
                }
                if (group.items.isEmpty()) {
                    item(key = "exercise_empty_${group.key}") {
                        Text(
                            text = stringResource(R.string.routine_group_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.listSidePadding().padding(start = 4.dp)
                        )
                    }
                }
                val draggable = canReorder(group)
                items(group.items, key = { reorder.itemKey(group.key, it) }) { exercise ->
                    val rowKey = reorder.itemKey(group.key, exercise)
                    DraggableListRow(
                        state = reorderableState,
                        key = rowKey,
                        enabled = draggable,
                        handleDescription = stringResource(R.string.reorder_handle_description, exercise.name),
                        onDragStopped = { onReorder(group.key, reorder.groupOrder(group.key)) },
                        // The handle lives in the swipe panel instead of beside the card
                        showHandle = false
                    ) { handleModifier ->
                        val maxWeight = maxWeights[exercise.id]
                        val repsStart = quickRepsStart(exercise, lastReps[exercise.id])
                        // A routine group counts the check-ins made under it and those without a routine
                        val routineId = group.routineId
                        ExerciseRow(
                            exercise = exercise,
                            maxWeight = maxWeight,
                            controlsFirst = controlsFirst,
                            open = openRowKey == rowKey,
                            onOpenChange = { open -> openRowKey = if (open) rowKey else openRowKey.takeIf { it != rowKey } },
                            draggable = draggable,
                            handleModifier = handleModifier,
                            wheelValue = wheelValues[exercise.id] ?: quickWheelStart(exercise, maxWeight),
                            onWheelChange = { wheelValues[exercise.id] = it },
                            repsStart = repsStart,
                            repsValue = repsValues[exercise.id] ?: repsStart,
                            onRepsChange = { repsValues[exercise.id] = it },
                            checkedIn = if (routineId != null) {
                                (exercise.id to routineId) in checkedInRoutineKeys || (exercise.id to null) in checkedInRoutineKeys
                            } else {
                                exercise.id in checkedInIds
                            },
                            onCheckedChange = { checked ->
                                if (checked) {
                                    onQuickCheckIn(
                                        exercise,
                                        wheelValues[exercise.id] ?: quickWheelStart(exercise, maxWeight),
                                        repsValues[exercise.id] ?: repsStart,
                                        routineId
                                    )
                                } else {
                                    onRemoveCheckIns(exercise, routineId)
                                }
                            },
                            onClick = { onSelectExercise(exercise) },
                            onEdit = { openRowKey = null; onEditExercise(exercise) },
                            onDelete = { openRowKey = null; onDeleteExercise(exercise) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One exercise as a full-width row: tap opens its details; swipe left reveals Edit and Delete
 * (plus the drag handle in custom order). Strength rows have a weight and a reps wheel, cardio a
 * minutes wheel and bodyweight a reps wheel; the box checks the exercise in with them, or removes
 * the day's check-ins when unticked. [controlsFirst] puts the wheels and box on the left.
 */
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    maxWeight: Double?,
    controlsFirst: Boolean,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    draggable: Boolean,
    handleModifier: Modifier,
    wheelValue: Double,
    onWheelChange: (Double) -> Unit,
    repsStart: Int,
    repsValue: Int,
    onRepsChange: (Int) -> Unit,
    checkedIn: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val accent = com.offlinelabs.nutcracker.ui.components.form.FormDefaults.colors().accent
    val onAccent = com.offlinelabs.nutcracker.ui.components.form.FormDefaults.colors().onAccent
    val type = remember(exercise.category) { com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper.getExerciseType(exercise.category) }
    val unit = stringResource(
        when (type) {
            com.offlinelabs.nutcracker.data.model.ExerciseType.STRENGTH -> R.string.kg_unit
            com.offlinelabs.nutcracker.data.model.ExerciseType.CARDIO -> R.string.min_unit
            com.offlinelabs.nutcracker.data.model.ExerciseType.BODYWEIGHT -> R.string.reps_unit
        }
    )
    val wheelStart = quickWheelStart(exercise, maxWeight)
    // The steps keep the start's fraction (42.5 kg gives …41.5, 42.5, 43.5…)
    val wheelSteps = remember(wheelStart, type) {
        quickWheelValues(wheelStart, if (type == com.offlinelabs.nutcracker.data.model.ExerciseType.STRENGTH) 300 else 200)
    }
    val isStrength = type == com.offlinelabs.nutcracker.data.model.ExerciseType.STRENGTH
    val repsUnit = stringResource(R.string.reps_unit)
    val repsSteps = remember { (1..MAX_QUICK_REPS).map { it.toDouble() } }

    QuickCheckRow(
        controlsFirst = controlsFirst,
        open = open,
        onOpenChange = onOpenChange,
        actionCount = if (draggable) 3 else 2,
        actions = {
            if (draggable) {
                SwipeAction(
                    icon = Icons.Filled.DragIndicator,
                    label = stringResource(R.string.reorder_handle_description, exercise.name),
                    background = MaterialTheme.colorScheme.outline,
                    content = MaterialTheme.colorScheme.surface,
                    modifier = handleModifier,
                    onClick = {}
                )
            }
            SwipeAction(Icons.Filled.Edit, stringResource(R.string.edit), accent, onAccent, onClick = onEdit)
            SwipeAction(Icons.Filled.Delete, stringResource(R.string.delete), MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError, onClick = onDelete)
        },
        onClick = onClick,
        controls = {
            QuickControls(
                controlsFirst = controlsFirst,
                checked = checkedIn,
                onCheckedChange = onCheckedChange,
                checkLabel = stringResource(if (checkedIn) R.string.quick_remove_check_in else R.string.quick_check_in, exercise.name)
            ) {
                // Re-created when the start moves (the max weight loads or grows), so it scrolls there
                key(wheelSteps) {
                    ValueWheel(
                        values = wheelSteps,
                        value = wheelValue,
                        onValueChange = onWheelChange,
                        unit = unit,
                        contentDescription = stringResource(R.string.quick_wheel_description, exercise.name, formatWheelValue(wheelValue), unit),
                        width = 56.dp
                    )
                }
                if (isStrength) {
                    key(repsStart) {
                        ValueWheel(
                            values = repsSteps,
                            value = repsValue.toDouble(),
                            onValueChange = { onRepsChange(it.toInt()) },
                            unit = repsUnit,
                            contentDescription = stringResource(R.string.quick_wheel_description, exercise.name, repsValue.toString(), repsUnit),
                            width = 48.dp
                        )
                    }
                }
            }
        }
    ) {
        // The name takes the full width above the picture and the max weight
        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                ExerciseImageIcon(exercise = exercise, size = 40.dp, showShadow = true)
                Text(
                    text = stringResource(R.string.max_weight_display, (maxWeight ?: 0.0).toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

