package com.offlinelabs.nutcracker.ui.components.lists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.offlinelabs.nutcracker.data.ListGroup

/**
 * Local, drag-following copy of a grouped list. Items can only move inside their own group (an
 * item may appear in several groups, so every row has its own key). Re-created whenever the source
 * [ListGroup]s change, i.e. after the new order has been saved and re-emitted by the database.
 */
@Stable
class GroupedReorderState<T>(
    initial: List<ListGroup<T>>,
    private val prefix: String,
    private val idOf: (T) -> Long
) {
    var groups by mutableStateOf(initial)
        private set

    private class Row(val groupKey: String, val id: Long)

    private val rows = HashMap<String, Row>()

    /** Unique LazyColumn key of [item] in group [groupKey]. */
    fun itemKey(groupKey: String, item: T): String {
        val id = idOf(item)
        val key = "$prefix|$groupKey|$id"
        rows[key] = Row(groupKey, id)
        return key
    }

    /** Group of the row with [key], or null if the key isn't one of this state's rows. */
    fun groupOf(key: Any?): String? = rows[key]?.groupKey

    /** Moves the row [fromKey] to the position of [toKey]; returns false (no change) across groups. */
    fun move(fromKey: Any?, toKey: Any?): Boolean {
        val from = rows[fromKey] ?: return false
        val to = rows[toKey] ?: return false
        if (from.groupKey != to.groupKey) return false
        groups = groups.map { g ->
            if (g.key != from.groupKey) return@map g
            val items = g.items.toMutableList()
            val fromIndex = items.indexOfFirst { idOf(it) == from.id }
            val toIndex = items.indexOfFirst { idOf(it) == to.id }
            if (fromIndex < 0 || toIndex < 0) return false
            items.add(toIndex, items.removeAt(fromIndex))
            g.copy(items = items)
        }
        return true
    }

    /** Current ids of group [groupKey] in their (possibly dragged) order. */
    fun groupOrder(groupKey: String): List<Long> =
        groups.firstOrNull { it.key == groupKey }?.items?.map(idOf).orEmpty()
}

@Composable
fun <T> rememberGroupedReorderState(
    groups: List<ListGroup<T>>,
    prefix: String,
    idOf: (T) -> Long
): GroupedReorderState<T> = remember(groups, prefix) { GroupedReorderState(groups, prefix, idOf) }
