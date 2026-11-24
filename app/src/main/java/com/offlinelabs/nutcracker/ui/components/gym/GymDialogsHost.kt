package com.offlinelabs.nutcracker.ui.components.gym

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.repo.FoodLogRepository
import kotlinx.coroutines.launch

/**
 * Which gym dialog is open. Survives rotation; the edit/delete dialogs keep only the gym id and
 * look the gym up again in the current gyms list.
 */
@Stable
class GymDialogsState(
    showAdd: Boolean = false,
    showManage: Boolean = false,
    editGymId: Long? = null,
    deleteGymId: Long? = null
) {
    var showAdd by mutableStateOf(showAdd)
    var showManage by mutableStateOf(showManage)
    var editGymId by mutableStateOf(editGymId)
    var deleteGymId by mutableStateOf(deleteGymId)

    companion object {
        val Saver = listSaver<GymDialogsState, Any?>(
            save = { listOf(it.showAdd, it.showManage, it.editGymId, it.deleteGymId) },
            restore = {
                GymDialogsState(
                    showAdd = it[0] as Boolean,
                    showManage = it[1] as Boolean,
                    editGymId = it[2] as Long?,
                    deleteGymId = it[3] as Long?
                )
            }
        )
    }
}

@Composable
fun rememberGymDialogsState(): GymDialogsState = rememberSaveable(saver = GymDialogsState.Saver) { GymDialogsState() }

/**
 * Hosts the add / edit / manage / delete gym dialogs and performs the repository calls.
 *
 * @param selectedGymId the current dropdown selection (null = All gyms).
 * @param onSelectedGymIdChange called to select a newly added gym, or to fall back to All gyms
 *   (null) when the selected gym was deleted.
 */
@Composable
fun GymDialogsHost(
    state: GymDialogsState,
    gyms: List<Gym>,
    selectedGymId: Long?,
    onSelectedGymIdChange: (Long?) -> Unit,
    repository: FoodLogRepository
) {
    val scope = rememberCoroutineScope()

    if (state.showAdd) {
        GymFormDialog(
            gym = null,
            onDismiss = { state.showAdd = false },
            onSave = { gym ->
                state.showAdd = false
                scope.launch {
                    val id = repository.upsertGym(gym)
                    onSelectedGymIdChange(id)
                }
            }
        )
    }

    if (state.showManage) {
        ManageGymsDialog(
            gyms = gyms,
            onEdit = { state.editGymId = it.id },
            onDelete = { state.deleteGymId = it.id },
            onDismiss = { state.showManage = false }
        )
    }

    val editGym = state.editGymId?.let { id -> gyms.firstOrNull { it.id == id } }
    if (editGym != null) {
        GymFormDialog(
            gym = editGym,
            onDismiss = { state.editGymId = null },
            onSave = { gym ->
                state.editGymId = null
                scope.launch { repository.upsertGym(gym) }
            }
        )
    }

    val deleteGym = state.deleteGymId?.let { id -> gyms.firstOrNull { it.id == id } }
    if (deleteGym != null) {
        val deleteGymAndClose = { removeExercises: Boolean ->
            state.deleteGymId = null
            scope.launch {
                repository.deleteGym(deleteGym.id, removeExercises)
                if (selectedGymId == deleteGym.id) onSelectedGymIdChange(null)
            }
            Unit
        }
        DeleteGymDialog(
            gym = deleteGym,
            onDeleteGymOnly = { deleteGymAndClose(false) },
            onDeleteWithExercises = { deleteGymAndClose(true) },
            onDismiss = { state.deleteGymId = null }
        )
    }
}
