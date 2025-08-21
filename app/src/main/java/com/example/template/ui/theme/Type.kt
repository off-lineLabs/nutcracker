@file:JvmName("TypeKt")

package com.example.template.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.template.R // Your project's R file

// Define the Inter FontFamily using your variable fonts
val InterFontFamily = FontFamily(
    Font(R.font.inter_italic_variable)
)

// Now, update your Typography object to use InterFontFamily
// and specify the desired FontWeight (and FontStyle if needed) for each text style.
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W400, // Example: Corresponds to Regular in Inter
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    // ... (other display styles) ...

    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W600, // Example: Corresponds to SemiBold in Inter
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    // ... (other headline styles) ...

    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W500, // Example: Corresponds to Medium in Inter
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // ... (other title styles) ...

    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W400, // Regular
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W400, // Regular
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        // Example if you wanted this specific style to be italic:
        // fontStyle = FontStyle.Italic
    ),
    // ... (other body and label styles, adjusting FontWeight and FontStyle as needed) ...

    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W500, // Medium
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)