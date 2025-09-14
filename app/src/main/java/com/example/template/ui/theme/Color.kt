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
val BrandNavyLightTheme = Color(0xFF2C3E50)  // Rich deep blue-gray
val BrandRedLightTheme = Color(0xFFC0392B)   // Vibrant warm red
val BrandNavyDarkLightTheme = Color(0xFF34495E) // Darker navy blue
val BrandGoldLightTheme = Color(0xFFE67E22)  // Warm energetic orange-gold
val BrandGoldLightLightTheme = Color(0xFFF39C12) // Bright cheerful yellow-gold

// ===== SEMANTIC COLORS =====
// Success colors (using your brand green-ish tones)
val Success = Color(0xFF0D8043)          // Deeper green for better contrast
val SuccessLight = Color(0xFF34D399)     // Keep light variant
val SuccessDark = Color(0xFF059669)      // Keep dark variant

// Warning colors (using your brand gold tones)
val Warning = Color(0xFFEA8600)          // Warmer orange for light mode
val WarningLight = Color(0xFFD9A25F)     // Keep light variant
val WarningDark = Color(0xFF8B5A1C)      // Keep dark variant

// Error colors (using your brand red)
val Error = Color(0xFFD93025)            // Clearer red for light mode
val ErrorLight = Color(0xFFC73E4A)       // Keep light variant
val ErrorDark = Color(0xFF8B1F2A)        // Keep dark variant

// Info colors (using your brand navy)
val Info = Color(0xFF1A73E8)             // Vibrant blue for light mode
val InfoLight = Color(0xFF3A4F6B)       // Keep light variant
val InfoDark = Color(0xFF1A2A3F)        // Keep dark variant

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
val NeutralLight50 = Color(0xFFFAFBFC)   // Softer background
val NeutralLight100 = Color(0xFFF1F3F4)  // Warmer surface variant
val NeutralLight200 = Color(0xFFE1E5E9)  // Softer border/divider
val NeutralLight300 = Color(0xFFD1D5DB)  // Medium neutral
val NeutralLight400 = Color(0xFF9CA3AF)  // Updated neutral
val NeutralLight500 = Color(0xFF6B7280)  // Medium text
val NeutralLight600 = Color(0xFF5F6368)  // Better contrast secondary text
val NeutralLight700 = Color(0xFF374151)  // Darker text
val NeutralLight800 = Color(0xFF1F2937)  // Very dark text
val NeutralLight900 = Color(0xFF1A1A1A)  // Warmest dark text

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

// Dashboard-specific colors
val ExceededColor = Color(0xFFB65755)     // Red for exceeded nutrient/goal values
val ProteinFiberColor = Color(0xFF5D916D) // Green for protein/fiber when exceeded

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
val TextPrimary = Color(0xFF1A1A1A)      // Warmer dark text
val TextSecondary = Color(0xFF5F6368)    // Better contrast secondary
val TextTertiary = Color(0xFF9CA3AF)     // Keep tertiary as is
val TextInverse = Color(0xFFFFFFFF)      // Keep white as is

// ===== LEGACY COLORS (for backward compatibility) =====
// Keep these for now to avoid breaking existing code
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)