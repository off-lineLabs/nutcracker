package com.offlinelabs.nutcracker.ui.components.dialogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeNoteTest {
    @Test fun recipeNoteIsHidden() {
        assertEquals("", visibleCheckInNotes("recipe:42"))
        assertEquals("", visibleCheckInNotes(" recipe:7 "))
    }

    @Test fun userNotesAreShown() {
        assertEquals("tasty", visibleCheckInNotes("tasty"))
        assertEquals("recipe:abc", visibleCheckInNotes("recipe:abc"))
        assertEquals("", visibleCheckInNotes(null))
    }

    @Test fun emptyFieldKeepsRecipeLink() {
        assertEquals("recipe:42", checkInNotesToSave("", "recipe:42"))
        assertEquals("recipe:42", checkInNotesToSave("   ", "recipe:42"))
    }

    @Test fun typedNoteWins() {
        assertEquals("good", checkInNotesToSave("good", "recipe:42"))
    }

    @Test fun plainNotesCanBeCleared() {
        assertNull(checkInNotesToSave("", "old note"))
        assertNull(checkInNotesToSave("", null))
    }
}
