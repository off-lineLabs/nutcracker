package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.repair.LegacyRecipeRepair

/**
 * Recipe check-ins store an internal `recipe:<id>` link in their notes. It is used by repair and
 * import, so it must survive editing but never be shown to the user.
 */

/** Notes text to show in the edit field: empty when the stored notes are the internal recipe link. */
fun visibleCheckInNotes(stored: String?): String =
    if (LegacyRecipeRepair.parseRecipeId(stored) != null) "" else stored.orEmpty()

/**
 * Notes to persist: the user's text if any, otherwise the original internal recipe link (kept),
 * otherwise null.
 */
fun checkInNotesToSave(typed: String, original: String?): String? =
    typed.takeIf { it.isNotBlank() }
        ?: original?.takeIf { LegacyRecipeRepair.parseRecipeId(it) != null }
