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
import com.offlinelabs.nutcracker.ui.components.lists.SwipeActionsRow
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
    /** Exercises with a check-in on the day shown (their checkbox is ticked). */
    checkedInIds: Set<Long> = emptySet(),
    /** Checks the exercise in with the row wheel's value (weight, minutes or reps). */
    onQuickCheckIn: (Exercise, Double) -> Unit = { _, _ -> },
    /** Removes the exercise's check-ins of the day shown. */
    onRemoveCheckIns: (Exercise) -> Unit = {},
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
        foodLogRepository = foodLogRepository,
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
        checkedInIds = checkedInIds,
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
    foodLogRepository: FoodLogRepository? = null,
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
    checkedInIds: Set<Long>,
    onQuickCheckIn: (Exercise, Double) -> Unit,
    onRemoveCheckIns: (Exercise) -> Unit,
    onDeleteExercise: (Exercise) -> Unit
) {
    val reorder = rememberGroupedReorderState(groups, prefix = "exercise") { it.id }
    // Only one row's swipe panel is open at a time
    var openRowKey by remember { mutableStateOf<String?>(null) }
    // Wheel values survive rows scrolling out of view (reset when the tab leaves composition)
    val wheelValues = remember { mutableStateMapOf<Long, Double>() }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorder.move(from.key, to.key) // moves across groups are ignored
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header") { header() }
        item(key = "search_database") {
                Button(
                    onClick = onSearchExternal,
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.padding(top = 8.dp)
                    )
            }
        } else {
            item(key = "arrangement_bar") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    arrangementBar()
                    Text(
                        text = stringResource(R.string.swipe_actions_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            if (emptyHint != null) {
                item(key = "empty_hint") {
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            reorder.groups.forEach { group ->
                // An empty routine still shows (with a hint), so a newly created one is visible
                if (group.items.isEmpty() && group.routineId == null) return@forEach
                group.title?.let { title ->
                    item(key = "exercise_header_${group.key}") { ListGroupHeader(groupTitleText(title)) }
                }
                if (group.items.isEmpty()) {
                    item(key = "exercise_empty_${group.key}") {
                        Text(
                            text = stringResource(R.string.routine_group_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
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
                        ExerciseRow(
                            exercise = exercise,
                            foodLogRepository = foodLogRepository,
                            open = openRowKey == rowKey,
                            onOpenChange = { open -> openRowKey = if (open) rowKey else openRowKey.takeIf { it != rowKey } },
                            draggable = draggable,
                            handleModifier = handleModifier,
                            wheelValue = wheelValues[exercise.id] ?: quickWheelStart(exercise),
                            onWheelChange = { wheelValues[exercise.id] = it },
                            checkedIn = exercise.id in checkedInIds,
                            onCheckedChange = { checked ->
                                if (checked) onQuickCheckIn(exercise, wheelValues[exercise.id] ?: quickWheelStart(exercise))
                                else onRemoveCheckIns(exercise)
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

private val SwipeActionWidth = 64.dp

/**
 * One exercise: tap opens its details; swipe left reveals Edit and Delete (plus the drag handle in
 * custom order). The wheel sets the weight (minutes for cardio, reps for bodyweight) and the
 * checkbox checks the exercise in with it, or removes the day's check-ins when unticked.
 */
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    foodLogRepository: FoodLogRepository?,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    draggable: Boolean,
    handleModifier: Modifier,
    wheelValue: Double,
    onWheelChange: (Double) -> Unit,
    checkedIn: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = getContrastingTextColor(cardColor)
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
    val wheelSteps = remember(exercise.id, exercise.defaultWeight, exercise.defaultReps, type) {
        quickWheelValues(quickWheelStart(exercise), if (type == com.offlinelabs.nutcracker.data.model.ExerciseType.STRENGTH) 300 else 200)
    }
    val actionCount = if (draggable) 3 else 2

    SwipeActionsRow(
        open = open,
        onOpenChange = onOpenChange,
        actionsWidth = SwipeActionWidth * actionCount,
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
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                // While open, a tap closes the row instead of opening the details
                .clickable { if (open) onOpenChange(false) else onClick() },
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseImageIcon(exercise = exercise, size = 44.dp, showShadow = true)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    MaxWeightDisplay(exerciseId = exercise.id, foodLogRepository = foodLogRepository, textColor = textColor)
                }
                ValueWheel(
                    values = wheelSteps,
                    value = wheelValue,
                    onValueChange = onWheelChange,
                    unit = unit,
                    contentDescription = stringResource(R.string.quick_wheel_description, exercise.name, formatWheelValue(wheelValue), unit)
                )
                val checkLabel = stringResource(
                    if (checkedIn) R.string.quick_remove_check_in else R.string.quick_check_in,
                    exercise.name
                )
                Checkbox(
                    checked = checkedIn,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = onAccent, uncheckedColor = textColor.copy(alpha = 0.7f)),
                    modifier = Modifier.semantics { contentDescription = checkLabel }
                )
            }
        }
    }
}

/** One button of the swipe panel: an icon on a coloured square. */
@Composable
private fun SwipeAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(SwipeActionWidth)
            .fillMaxHeight()
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = content)
    }
}

@Composable
private fun MaxWeightDisplay(
    exerciseId: Long,
    foodLogRepository: FoodLogRepository? = null,
    textColor: androidx.compose.ui.graphics.Color
) {
    var maxWeight by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(exerciseId, foodLogRepository) {
        foodLogRepository?.getMaxWeightForExercise(exerciseId)?.collect { weight ->
            maxWeight = weight ?: 0.0
        }
    }
    
    Text(
        text = stringResource(R.string.max_weight_display, maxWeight.toString()),
        style = MaterialTheme.typography.bodySmall,
        color = textColor.copy(alpha = 0.7f)
    )
}
