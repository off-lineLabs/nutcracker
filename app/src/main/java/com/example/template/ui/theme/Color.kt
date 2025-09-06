package com.example.template.ui.theme

import androidx.compose.ui.graphics.Color

// ===== BRAND COLORS =====
// Your brand colors - these define your app's main identity

// Dark theme brand colors
val BrandNavy = Color(0xFF0B1426)        // Dark navy blue - background
val BrandNavyLight = Color(0xFF283D59)   // Lighter navy blue - main elements
val BrandRed = Color(0xFFA62934)         // Red - details and accents
val BrandGold = Color(0xFFA66A21)        // Goldish yellow - details
val BrandGoldLight = Color(0xFFD9A25F)   // Lighter yellow - highlights

// Light theme brand colors
val BrandNavyLightTheme = Color(0xFFA3AFBF)  // Lighter navy blue
val BrandRedLightTheme = Color(0xFF59161C)   // Red
val BrandNavyDarkLightTheme = Color(0xFFAAB1BF) // Dark navy blue
val BrandGoldLightTheme = Color(0xFF403426)  // Goldish yellow
val BrandGoldLightLightTheme = Color(0xFF736A60) // Lighter yellow

// ===== SEMANTIC COLORS =====
// Success colors (using your brand green-ish tones)
val Success = Color(0xFF10B981)
val SuccessLight = Color(0xFF34D399)
val SuccessDark = Color(0xFF059669)

// Warning colors (using your brand gold tones)
val Warning = Color(0xFFA66A21)          // Your brand gold
val WarningLight = Color(0xFFD9A25F)     // Your brand light gold
val WarningDark = Color(0xFF8B5A1C)

// Error colors (using your brand red)
val Error = Color(0xFFA62934)            // Your brand red
val ErrorLight = Color(0xFFC73E4A)
val ErrorDark = Color(0xFF8B1F2A)

// Info colors (using your brand navy)
val Info = Color(0xFF283D59)             // Your brand navy light
val InfoLight = Color(0xFF3A4F6B)
val InfoDark = Color(0xFF1A2A3F)

// ===== NEUTRAL COLORS =====
// Dark theme neutrals (based on your brand colors)
val NeutralDark50 = Color(0xFF0B1426)    // Your brand navy
val NeutralDark100 = Color(0xFF1A2A3F)
val NeutralDark200 = Color(0xFF283D59)   // Your brand navy light
val NeutralDark300 = Color(0xFF3A4F6B)
val NeutralDark400 = Color(0xFF4B5F7A)
val NeutralDark500 = Color(0xFF5C6F89)
val NeutralDark600 = Color(0xFF6D7F98)
val NeutralDark700 = Color(0xFF7E8FA7)
val NeutralDark800 = Color(0xFF8F9FB6)
val NeutralDark900 = Color(0xFFA0AFC5)

// Light theme neutrals
val NeutralLight50 = Color(0xFFF8F9FA)
val NeutralLight100 = Color(0xFFE9ECEF)
val NeutralLight200 = Color(0xFFDEE2E6)
val NeutralLight300 = Color(0xFFCED4DA)
val NeutralLight400 = Color(0xFFAAB1BF)  // Your brand navy dark light theme
val NeutralLight500 = Color(0xFF6C757D)
val NeutralLight600 = Color(0xFF495057)
val NeutralLight700 = Color(0xFF343A40)
val NeutralLight800 = Color(0xFF212529)
val NeutralLight900 = Color(0xFF121416)

// ===== EXISTING COLORS (PRESERVED) =====
// Keep your existing calorie ring and nutrient colors exactly as they are
val CalorieRingTrack = Color(0xFF374151)
val CalorieRingProgress = Color(0xFFFFA94D)

// Nutrient colors (keep these exactly as they are)
val NutrientCarbs = Color(0xFF60A5FA)     // Blue for carbohydrates
val NutrientCarbsDark = Color(0xFF2563EB)
val NutrientProtein = Color(0xFFF87171)   // Red for protein
val NutrientProteinDark = Color(0xFFDC2626)
val NutrientFat = Color(0xFFFBBF24)       // Yellow for fat
val NutrientFatDark = Color(0xFFD97706)
val NutrientFiber = Color(0xFF10B981)     // Green for fiber
val NutrientFiberDark = Color(0xFF059669)

// ===== COMPONENT-SPECIFIC COLORS =====
// Colors for specific UI components
val PillTaken = Color(0xFF4CAF50)         // Green for taken pills
val PillNotTaken = Color(0xFF9E9E9E)      // Gray for not taken pills
val ExerciseEnabled = Color(0xFF2196F3)   // Blue for enabled exercises
val ExerciseDisabled = Color(0xFF9E9E9E)  // Gray for disabled exercises

// ===== FAB AND ACTION COLORS =====
// Colors for Floating Action Buttons and action items
val FabExercise = Color(0xFFA62934)       // Your brand red for exercise FAB
val FabExerciseLight = Color(0xFFC73E4A)  // Lighter red for exercise FAB
val FabMeal = Color(0xFFA66A21)           // Your brand gold for meal FAB
val FabMealLight = Color(0xFFD9A25F)      // Lighter gold for meal FAB

// Exercise item colors in logs
val ExerciseItemBackground = Color(0xFFA62934)  // Your brand red for exercise items
val ExerciseItemBackgroundLight = Color(0xFFC73E4A)  // Lighter red variant

// Meal item colors in logs
val MealItemBackground = Color(0xFFA66A21)      // Your brand gold for meal items
val MealItemBackgroundLight = Color(0xFFD9A25F) // Lighter gold variant

// ===== BACKGROUND COLORS =====
val BackgroundPrimary = Color(0xFF091020) // Your existing splash background
val BackgroundSecondary = Color(0xFF374151)
val BackgroundTertiary = Color(0xFF4B5563)

// ===== TEXT COLORS =====
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)
val TextInverse = Color(0xFFFFFFFF)

// ===== LEGACY COLORS (for backward compatibility) =====
// Keep these for now to avoid breaking existing code
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)