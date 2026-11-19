# Project-specific R8 rules for release builds.
# Library rules (Retrofit, OkHttp, Gson, Room, Coil, ML Kit, AndroidX) come
# from the consumer rules bundled in each dependency.

# Keep line numbers so stack traces can be retraced with mapping.txt, and keep
# generic signatures / annotations / inner-class metadata, which Gson and
# Retrofit read reflectively.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# --- Open Food Facts models (data/model/OpenFoodFactsResponse.kt) ---
# Deserialized by Gson through Retrofit's GsonConverterFactory. Gson reads and
# writes fields by reflection and needs the constructors, so keep the classes,
# their fields and constructors intact.
-keep class com.offlinelabs.nutcracker.data.model.OpenFoodFactsResponse { *; }
-keep class com.offlinelabs.nutcracker.data.model.Product { *; }
-keep class com.offlinelabs.nutcracker.data.model.Nutriments { *; }
-keep class com.offlinelabs.nutcracker.data.model.SearchResponse { *; }
# Other classes declared in the same file (built manually, not by Gson). Kept
# so the whole file behaves the same in debug and release.
-keep class com.offlinelabs.nutcracker.data.model.FoodInfo { *; }
-keep class com.offlinelabs.nutcracker.data.model.NutritionInfo { *; }
-keep class com.offlinelabs.nutcracker.data.model.NovaClassification { *; }
-keep class com.offlinelabs.nutcracker.data.model.GreenScore { *; }
-keep class com.offlinelabs.nutcracker.data.model.Nutriscore { *; }

# --- External exercise search (data/service/ExternalExerciseService.kt) ---
# exercises.json is deserialized by Gson. These classes have no @SerializedName,
# so the JSON keys map to the Kotlin property names directly: field names must
# not be obfuscated.
-keep class com.offlinelabs.nutcracker.data.service.ExercisesResponse { *; }
-keep class com.offlinelabs.nutcracker.data.model.ExternalExercise { *; }
