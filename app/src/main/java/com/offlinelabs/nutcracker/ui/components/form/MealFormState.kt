package com.offlinelabs.nutcracker.ui.components.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The editable fields of [MealForm], in display order.
 *
 * [Salt] is entered in grams but stored in [Meal.sodium_mg], which (despite its name) holds salt in
 * milligrams; [MealFormState] converts between the two.
 */
enum class MealField { Name, Quantity, Calories, Carbs, Protein, Fat, Fiber, Salt }

/**
 * Parsed, validated output of a [MealFormState]. Units: calories in kcal, [sodiumMg] is salt in mg
 * (stored as [Meal.sodium_mg]), others in g.
 */
data class MealFormResult(
    val name: String,
    val calories: Int,
    val carbohydratesG: Double,
    val proteinG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sodiumMg: Double,
    val servingSizeValue: Double,
    val servingSizeUnit: ServingSizeUnit
) {
    /** A new manual meal with these values (same defaults the add dialog always used). */
    fun toNewMeal(): Meal = Meal(
        name = name,
        calories = calories,
        carbohydrates_g = carbohydratesG,
        protein_g = proteinG,
        fat_g = fatG,
        fiber_g = fiberG,
        sodium_mg = sodiumMg,
        servingSize_value = servingSizeValue,
        servingSize_unit = servingSizeUnit
    )

    /** [meal] with these values applied; every other field (id, source, OFF data...) is kept. */
    fun applyTo(meal: Meal): Meal = meal.copy(
        name = name,
        calories = calories,
        carbohydrates_g = carbohydratesG,
        protein_g = proteinG,
        fat_g = fatG,
        fiber_g = fiberG,
        sodium_mg = sodiumMg,
        servingSize_value = servingSizeValue,
        servingSize_unit = servingSizeUnit
    )
}

/**
 * State and validation for the add/edit meal form.
 *
 * - Numbers accept `,` or `.` ([parseDecimal]) and are pre-filled with the [locale]'s separator.
 * - Rules: name required; quantity required and > 0; calories required, >= 0, rounded to whole
 *   kcal; other nutrients optional (blank = 0) and >= 0.
 * - "Required" errors are only reported by [visibleError] after the user edited that field, so a
 *   fresh form isn't covered in red; [isValid] (and the save button) always considers them.
 * - When editing ([original] != null) the link toggle rescales all nutrients proportionally from
 *   the original values whenever the quantity changes ([toggleLink]).
 * - Salt is shown and typed in grams (up to three decimals) but stored in mg: pre-fill shows
 *   `sodium_mg / 1000`, saving stores grams * 1000 in [MealFormResult.sodiumMg].
 * - Untouched pre-filled fields keep the original exact value, so a value shown rounded to two
 *   (salt: three) decimals isn't silently truncated on save.
 *
 * Create it with [rememberMealFormState] so input survives configuration changes.
 */
@Stable
class MealFormState(
    private val original: Meal?,
    private val locale: Locale = Locale.getDefault()
) {
    private val texts = mutableStateMapOf<MealField, String>()
    private val prefilledTexts: Map<MealField, String>
    /** Original values in storage units (salt in mg). */
    private val prefilledValues: Map<MealField, Double>
    private val dirty = mutableStateMapOf<MealField, Boolean>()

    var unit: ServingSizeUnit by mutableStateOf(original?.servingSize_unit ?: ServingSizeUnit.GRAMS)
        private set

    /** Whether nutrients follow the quantity (edit only). */
    var isLinked: Boolean by mutableStateOf(false)
        private set

    /** The link/rescale toggle is only meaningful when editing an existing meal. */
    val canRescale: Boolean get() = original != null

    init {
        if (original == null) {
            texts[MealField.Quantity] = DEFAULT_QUANTITY
            prefilledTexts = emptyMap()
            prefilledValues = emptyMap()
        } else {
            prefilledValues = mapOf(
                MealField.Quantity to original.servingSize_value,
                MealField.Calories to original.calories.toDouble(),
                MealField.Carbs to original.carbohydrates_g,
                MealField.Protein to original.protein_g,
                MealField.Fat to original.fat_g,
                MealField.Fiber to original.fiber_g,
                MealField.Salt to original.sodium_mg
            )
            prefilledTexts = buildMap {
                put(MealField.Name, original.name)
                put(MealField.Calories, original.calories.toString())
                for ((field, value) in prefilledValues) {
                    if (field != MealField.Calories) put(field, formatField(field, value))
                }
            }
            texts.putAll(prefilledTexts)
        }
    }

    fun text(field: MealField): String = texts[field].orEmpty()

    fun onValueChange(field: MealField, value: String) {
        texts[field] = value
        dirty[field] = true
        if (field == MealField.Quantity && isLinked) rescaleFromOriginal()
    }

    fun onUnitChange(newUnit: ServingSizeUnit) {
        unit = newUnit
    }

    /** Turns linking on/off; turning it on immediately rescales to the current quantity. */
    fun toggleLink() {
        if (!canRescale) return
        isLinked = !isLinked
        if (isLinked) rescaleFromOriginal()
    }

    /** The problem with [field]'s current text, or null if it's acceptable. */
    fun error(field: MealField): FieldError? {
        val text = text(field)
        return when (field) {
            MealField.Name -> validateRequiredText(text)
            MealField.Quantity -> validateDecimal(text, required = true, allowZero = false)
            MealField.Calories -> validateDecimal(text, required = true)
            MealField.Carbs, MealField.Protein, MealField.Fat,
            MealField.Fiber, MealField.Salt -> validateDecimal(text, required = false)
        }
    }

    /** [error], except "Required" is hidden until the user has edited the field. */
    fun visibleError(field: MealField): FieldError? =
        error(field)?.takeIf { it != FieldError.Required || dirty[field] == true }

    val isValid: Boolean get() = MealField.entries.all { error(it) == null }

    /** The parsed values, or null while any field is invalid. */
    fun toResult(): MealFormResult? {
        if (!isValid) return null
        return MealFormResult(
            name = text(MealField.Name).trim(),
            calories = number(MealField.Calories).roundToInt(),
            carbohydratesG = number(MealField.Carbs),
            proteinG = number(MealField.Protein),
            fatG = number(MealField.Fat),
            fiberG = number(MealField.Fiber),
            sodiumMg = number(MealField.Salt),
            servingSizeValue = number(MealField.Quantity),
            servingSizeUnit = unit
        )
    }

    /** Value of a valid numeric field in storage units (salt in mg); blank optional fields are 0. */
    private fun number(field: MealField): Double {
        val text = text(field)
        prefilledValues[field]?.let { exact ->
            if (text == prefilledTexts[field]) return exact
        }
        val value = parseDecimal(text) ?: return 0.0
        // BigDecimal keeps typed grams exact in mg (1.234 g -> 1234.0, not 1233.9999...).
        return if (field == MealField.Salt) BigDecimal.valueOf(value).movePointRight(3).toDouble() else value
    }

    /** Display text for [value] given in storage units (salt: mg shown as g with up to 3 decimals). */
    private fun formatField(field: MealField, value: Double): String =
        if (field == MealField.Salt) formatDecimal(value / MG_PER_G, locale, maxFractionDigits = 3)
        else formatDecimal(value, locale)

    /** Sets every nutrient to original * (current quantity / original quantity). */
    private fun rescaleFromOriginal() {
        val base = original ?: return
        val quantity = parseDecimal(text(MealField.Quantity)) ?: return
        if (quantity <= 0.0 || base.servingSize_value <= 0.0) return
        val ratio = quantity / base.servingSize_value
        texts[MealField.Calories] = (base.calories * ratio).roundToInt().toString()
        texts[MealField.Carbs] = formatDecimal(base.carbohydrates_g * ratio, locale)
        texts[MealField.Protein] = formatDecimal(base.protein_g * ratio, locale)
        texts[MealField.Fat] = formatDecimal(base.fat_g * ratio, locale)
        texts[MealField.Fiber] = formatDecimal(base.fiber_g * ratio, locale)
        texts[MealField.Salt] = formatField(MealField.Salt, base.sodium_mg * ratio)
    }

    companion object {
        const val DEFAULT_QUANTITY = "100"
        private const val MG_PER_G = 1000.0

        /**
         * Saves the user's input (texts, unit, link toggle, edited flags). [original] and [locale]
         * aren't saved; they come from the caller again on restore.
         */
        fun saver(original: Meal?, locale: Locale): Saver<MealFormState, Any> = listSaver(
            save = { state ->
                buildList<Any> {
                    MealField.entries.forEach { add(state.text(it)) }
                    add(state.unit.name)
                    add(state.isLinked)
                    add(MealField.entries.filter { state.dirty[it] == true }.joinToString(",") { it.name })
                }
            },
            restore = { saved ->
                MealFormState(original, locale).apply {
                    val fields = MealField.entries
                    fields.forEachIndexed { i, field -> texts[field] = saved[i] as String }
                    unit = ServingSizeUnit.entries.firstOrNull { it.name == saved[fields.size] } ?: unit
                    isLinked = saved[fields.size + 1] as Boolean
                    (saved[fields.size + 2] as String).split(',').filter { it.isNotEmpty() }.forEach { name ->
                        MealField.entries.firstOrNull { it.name == name }?.let { dirty[it] = true }
                    }
                }
            }
        )
    }
}

/**
 * Remembers a [MealFormState] across recomposition and configuration changes.
 * Pass the meal being edited, or null for a new meal (quantity defaults to 100 g).
 */
@Composable
fun rememberMealFormState(original: Meal?): MealFormState {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    return rememberSaveable(original?.id, saver = MealFormState.saver(original, locale)) {
        MealFormState(original, locale)
    }
}
