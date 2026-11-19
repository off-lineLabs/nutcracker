package com.offlinelabs.nutcracker.ui.components.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.ui.components.dialogs.UnitSelectorDialog

private val FieldShape = RoundedCornerShape(10.dp)

/**
 * Compact single-line number field (no floating label) for use inside rows. Right-aligned text,
 * decimal keyboard, accent border when focused and error border when [isError].
 *
 * Stateless: the caller owns [value]. Input isn't filtered so invalid text can be reported inline;
 * pass a filtering lambda (see [isPartialDecimalInput]) as [onValueChange] if you prefer blocking.
 *
 * @param accessibilityLabel read by screen readers, since the visible label lives outside the field.
 */
@Composable
fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.End,
    colors: FormColors = FormDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val horizontalPadding = if (FormDefaults.isCompactWidth) 10.dp else 12.dp
    val borderColor = when {
        isError -> colors.error
        focused -> colors.fieldBorderFocused
        else -> colors.fieldBorder
    }
    val borderWidth = if (focused || isError) 2.dp else 1.dp
    val contentColor = if (enabled) colors.content else colors.content.copy(alpha = 0.38f)

    val textState = rememberFieldTextState(value)

    BasicTextField(
        value = textState.field,
        onValueChange = { new -> textState.onFieldChange(new)?.let(onValueChange) },
        modifier = modifier
            .onFocusChanged(textState::onFocusChanged)
            .bringIntoViewWhileFocused(focused)
            .heightIn(min = FormDefaults.FieldMinHeight)
            .semantics {
                if (accessibilityLabel != null) contentDescription = accessibilityLabel
                if (isError) error(errorText ?: "")
            },
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle.merge(TextStyle(color = contentColor, textAlign = textAlign)),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(if (isError) colors.error else colors.accent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .background(colors.field, FieldShape)
                    .border(borderWidth, borderColor, FieldShape)
                    .padding(horizontal = horizontalPadding, vertical = 10.dp),
                contentAlignment = if (textAlign == TextAlign.Start) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = colors.contentSecondary.copy(alpha = 0.55f),
                        textAlign = textAlign,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        }
    )
}

/** Leading icon for a form row, sized to [FormDefaults.LeadingSize]. */
@Composable
fun FormRowIcon(imageVector: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(20.dp)
    )
}

/** Leading colored dot for a form row (alternative to [FormRowIcon]). */
@Composable
fun FormRowDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(10.dp).background(color, CircleShape))
}

/**
 * One labelled numeric row: `[leading] Label ........ [ value ] unit`, with an inline error
 * message under the field.
 *
 * @param unit suffix shown after the field (e.g. "g", "kcal"); space is reserved even when null so
 *   rows in one section line up.
 * @param emphasized larger, bolder label for the headline value of a section (e.g. calories).
 * @param errorText inline error; also turns the field border red.
 */
@Composable
fun NumericInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    leading: (@Composable () -> Unit)? = null,
    errorText: String? = null,
    emphasized: Boolean = false,
    placeholder: String? = "0",
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    fieldWidth: Dp = FormDefaults.numberFieldWidth(),
    unitWidth: Dp = FormDefaults.UnitSuffixWidth,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    colors: FormColors = FormDefaults.colors()
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Box(Modifier.size(FormDefaults.LeadingSize), contentAlignment = Alignment.Center) {
                    leading()
                }
                Spacer(Modifier.width(if (FormDefaults.isCompactWidth) 8.dp else 10.dp))
            }
            FormRowLabel(
                text = label,
                style = if (emphasized) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
                },
                color = colors.content,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            CompactNumberField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.width(fieldWidth),
                accessibilityLabel = if (unit != null) "$label ($unit)" else label,
                isError = errorText != null,
                errorText = errorText,
                placeholder = placeholder,
                enabled = enabled,
                imeAction = imeAction,
                keyboardActions = keyboardActions,
                keyboardType = keyboardType,
                textStyle = if (emphasized) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                colors = colors
            )
            val unitStyle = MaterialTheme.typography.bodyMedium
            BasicText(
                text = unit.orEmpty(),
                style = unitStyle.copy(color = colors.contentSecondary),
                softWrap = false,
                maxLines = 1,
                // "kcal" just fits at font scale 1.0; shrink rather than clip at larger scales.
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 9.sp,
                    maxFontSize = if (unitStyle.fontSize.isSp) unitStyle.fontSize else 14.sp,
                    stepSize = 0.5.sp
                ),
                modifier = Modifier.width(unitWidth).padding(start = 8.dp)
            )
        }
        if (errorText != null) {
            FieldErrorText(errorText, Modifier.padding(end = unitWidth), colors)
        }
    }
}

/** Right-aligned inline error text, placed under a field. */
@Composable
fun FieldErrorText(text: String, modifier: Modifier = Modifier, colors: FormColors = FormDefaults.colors()) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.error,
        textAlign = TextAlign.End,
        modifier = modifier.fillMaxWidth().padding(top = 2.dp)
    )
}

/**
 * Full-width text input with a floating label (e.g. a name), styled for form dialogs, with an
 * inline error as supporting text.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    colors: FormColors = FormDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val textState = rememberFieldTextState(value)
    OutlinedTextField(
        value = textState.field,
        onValueChange = { new -> textState.onFieldChange(new)?.let(onValueChange) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        colors = FormDefaults.outlinedTextFieldColors(colors),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .onFocusChanged(textState::onFocusChanged)
            .bringIntoViewWhileFocused(focused)
            .fillMaxWidth()
    )
}

/**
 * Keeps a focused field visible in its scrolling container while the keyboard opens/closes and the
 * dialog relayouts around it (e.g. [FormDialogSurface] switching to its compact-height layout,
 * where a different scroll container takes over).
 */
@Composable
private fun Modifier.bringIntoViewWhileFocused(focused: Boolean): Modifier {
    val requester = remember { BringIntoViewRequester() }
    if (focused) {
        val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
        LaunchedEffect(requester, imeBottom) {
            // Let the resize caused by the inset change be measured and placed first.
            repeat(2) { withFrameNanos { } }
            requester.bringIntoView()
        }
    }
    return this.bringIntoViewRequester(requester)
}

/**
 * Row label that wraps only at word boundaries. When a single word is wider than the available
 * width (small phones, large font scale), the text shrinks in small steps (down to 75 %) instead of
 * breaking mid-word, e.g. "Carbohidrato / s". Below that it ellipsizes.
 */
@Composable
private fun FormRowLabel(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 2
) {
    val maxFontSize = if (style.fontSize.isSp) style.fontSize else 16.sp
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        autoSize = WholeWordAutoSize(minFontSize = maxFontSize * 0.75f, maxFontSize = maxFontSize)
    )
}

/** [TextAutoSize] picking the largest size (step 0.5 sp) that fits without breaking a word. */
private data class WholeWordAutoSize(val minFontSize: TextUnit, val maxFontSize: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var size = maxFontSize.value
        while (size > minFontSize.value) {
            if (performLayout(constraints, text, size.sp).fitsWithoutBreakingWords(text)) return size.sp
            size -= 0.5f
        }
        return minFontSize
    }

    private fun TextLayoutResult.fitsWithoutBreakingWords(text: AnnotatedString): Boolean {
        if (didOverflowWidth || didOverflowHeight) return false
        if (lineCount > 0 && isLineEllipsized(lineCount - 1)) return false
        for (line in 0 until lineCount - 1) {
            val end = getLineEnd(line)
            // A soft wrap between two letters/digits means a word was split.
            if (end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()) {
                return false
            }
        }
        return true
    }
}

/**
 * Compact button showing a unit's localized abbreviation with a drop-down arrow. Doesn't open
 * anything by itself; see [QuantityUnitInput] for the version wired to [UnitSelectorDialog].
 */
@Composable
fun UnitButton(
    unit: ServingSizeUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: FormColors = FormDefaults.colors()
) {
    val abbreviation = stringResource(unit.abbreviationResId)
    val description = stringResource(R.string.form_unit_button_description, abbreviation)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = FieldShape,
        color = colors.field,
        contentColor = colors.content,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.fieldBorder),
        modifier = modifier
            .heightIn(min = FormDefaults.FieldMinHeight)
            .semantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = abbreviation,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = colors.contentSecondary
            )
        }
    }
}

/**
 * Amount + unit input on one row: `Label ..... [ 100 ] [ g ▾ ] [trailing]`. Tapping the unit
 * button opens the shared [UnitSelectorDialog].
 *
 * @param trailing optional extra control after the unit button (e.g. a link/rescale toggle).
 */
@Composable
fun QuantityUnitInput(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    unit: ServingSizeUnit,
    onUnitChange: (ServingSizeUnit) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    fieldWidth: Dp = FormDefaults.numberFieldWidth(),
    trailing: (@Composable () -> Unit)? = null,
    colors: FormColors = FormDefaults.colors()
) {
    var showUnitSelector by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormRowLabel(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.content,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            CompactNumberField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.width(fieldWidth),
                accessibilityLabel = label,
                isError = errorText != null,
                errorText = errorText,
                imeAction = imeAction,
                keyboardActions = keyboardActions,
                colors = colors
            )
            Spacer(Modifier.width(8.dp))
            UnitButton(unit = unit, onClick = { showUnitSelector = true }, colors = colors)
            trailing?.invoke()
        }
        if (errorText != null) {
            FieldErrorText(errorText, colors = colors)
        }
    }

    if (showUnitSelector) {
        UnitSelectorDialog(
            onDismiss = { showUnitSelector = false },
            onUnitSelected = {
                onUnitChange(it)
                showUnitSelector = false
            }
        )
    }
}
