package com.example.template.data.service

import com.example.template.data.model.OpenFoodFactsResponse
import com.example.template.data.model.Product
import com.example.template.data.model.SearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.template.util.logger.AppLogger

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): Response<OpenFoodFactsResponse>
    
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("lc") languageCode: String? = null,
        @Query("sort_by") sortBy: String? = "popularity",
        @Query("page") page: Int = 1,
        @Query("tagtype_0") tagType: String? = null,
        @Query("tag_contains_0") tagContains: String? = null,
        @Query("tag_0") tagValue: String? = null
    ): Response<SearchResponse>
}

class OpenFoodFactsService(private val api: OpenFoodFactsApi) {
    
    /**
     * Fetches product information by barcode from Open Food Facts API.
     * The response includes nutrition data in various units, but our mapper
     * prioritizes per-100g/100ml values for consistency and comparability.
     */
    suspend fun getProductByBarcode(barcode: String): Result<OpenFoodFactsResponse> {
        return try {
            val response = api.getProductByBarcode(barcode)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == 1 && body.product != null) {
                    // Debug logging to see the raw nutriments data
                    body.product.nutriments?.let { nutriments ->
                        AppLogger.d("OpenFoodFactsService", "Raw nutriments data for barcode $barcode:")
                        AppLogger.d("OpenFoodFactsService", "Salt fields: 100g=${nutriments.salt100g}, regular=${nutriments.salt}, value=${nutriments.saltValue}")
                        AppLogger.d("OpenFoodFactsService", "Vitamin C fields: 100g=${nutriments.vitaminC100g}, regular=${nutriments.vitaminC}, value=${nutriments.vitaminCValue}")
                        AppLogger.d("OpenFoodFactsService", "Calcium fields: 100g=${nutriments.calcium100g}, regular=${nutriments.calcium}, value=${nutriments.calciumValue}")
                        AppLogger.d("OpenFoodFactsService", "Iron fields: 100g=${nutriments.iron100g}, regular=${nutriments.iron}, value=${nutriments.ironValue}")
                    }
                    Result.success(body)
                } else {
                    Result.failure(Exception("Product not found or invalid response"))
                }
            } else {
                Result.failure(Exception("API request failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Searches for products by text query from Open Food Facts API.
     * Supports multilingual search, popularity sorting, and whole foods filtering.
     * Includes retry logic for network timeouts and connection issues.
     */
    suspend fun searchProducts(
        searchTerms: String,
        pageSize: Int = 20,
        languageCode: String? = null,
        wholeFoodsOnly: Boolean = false
    ): Result<SearchResponse> {
        var lastException: Exception? = null
        
        // Retry up to 3 times for network issues
        for (attempt in 0 until 3) {
            try {
                // Get more results when filtering to ensure we have enough after filtering
                val searchPageSize = if (wholeFoodsOnly) pageSize * 3 else pageSize
                
                val response = api.searchProducts(
                    searchTerms = searchTerms,
                    pageSize = searchPageSize,
                    languageCode = languageCode,
                    sortBy = "popularity"
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    // Apply client-side filtering for whole foods using NOVA classification
                    val filteredProducts = if (wholeFoodsOnly) {
                        body.products.filter { product ->
                            isWholeFood(product)
                        }.take(pageSize)
                    } else {
                        body.products.take(pageSize)
                    }
                    
                    val filteredResponse = body.copy(products = filteredProducts)
                    AppLogger.d("OpenFoodFactsService", "Search successful for '$searchTerms' (wholeFoods: $wholeFoodsOnly): ${filteredProducts.size} results found")
                    return Result.success(filteredResponse)
                } else {
                    lastException = Exception("Search request failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                lastException = e
                AppLogger.w("OpenFoodFactsService", "Search attempt ${attempt + 1} failed for '$searchTerms'", e)
                
                // Don't retry for non-network errors
                if (e.message?.contains("timeout") != true && 
                    e.message?.contains("connect") != true &&
                    e.message?.contains("network") != true) {
                    break
                }
                
                // Wait before retrying (exponential backoff)
                if (attempt < 2) {
                    kotlinx.coroutines.delay(1000L * (attempt + 1))
                }
            }
        }
        
        AppLogger.e("OpenFoodFactsService", "Search failed for '$searchTerms' after all retries", lastException)
        return Result.failure(lastException ?: Exception("Unknown error"))
    }
    
    /**
     * Determines if a product is considered a "whole food" based on NOVA classification
     * and specific category tags. Combines both approaches for maximum accuracy.
     */
    private fun isWholeFood(product: Product): Boolean {
        // Primary check: NOVA classification
        product.novaGroup?.let { novaGroup ->
            // NOVA Group 1: Unprocessed or minimally processed foods
            // NOVA Group 2: Processed culinary ingredients (also considered whole foods)
            if (novaGroup == 1 || novaGroup == 2) {
                return true
            }
            // NOVA Group 3: Processed foods
            // NOVA Group 4: Ultra-processed foods
            // Both should be excluded from whole foods
            if (novaGroup == 3 || novaGroup == 4) {
                return false
            }
        }
        
        // Secondary check: Specific whole food category tags
        product.categoriesTags?.let { categoryTags ->
            val wholeFoodCategories = setOf(
                // Fruits and vegetables
                "en:fruits",
                "en:vegetables", 
                "en:fresh-foods",
                
                // Nuts and seeds
                "en:nuts",
                "en:seeds",
                
                // Legumes
                "en:legumes",
                "en:pulses",
                
                // Whole grains (unprocessed)
                "en:whole-grains",
                
                // Meat and fish
                "en:meat",
                "en:fish",
                "en:seafood",
                
                // Dairy (basic only - exclude processed dairy products)
                "en:dairy",
                "en:milk",
                
                // Eggs
                "en:eggs"
            )
            
            // Check if any category tag matches whole food categories
            val hasWholeFoodCategory = categoryTags.any { tag -> 
                wholeFoodCategories.any { wholeFoodTag -> tag.startsWith(wholeFoodTag) }
            }
            
            // But exclude processed foods even if they have whole food categories
            val processedFoodCategories = setOf(
                "en:breakfast-cereals",
                "en:cereals-and-potatoes",
                "en:snacks",
                "en:beverages",
                "en:fruit-juices",
                "en:processed-foods",
                "en:frozen-foods",
                "en:canned-foods",
                "en:desserts",
                "en:dairy-desserts",
                "en:fermented-dairy-desserts",
                "en:yogurts-with-fruits",
                "en:flavored-yogurts",
                "en:chocolate-products",
                "en:sweetened-products"
            )
            
            val hasProcessedCategory = categoryTags.any { tag ->
                processedFoodCategories.any { processedTag -> tag.startsWith(processedTag) }
            }
            
            return hasWholeFoodCategory && !hasProcessedCategory
        }
        
        // Last resort: Raw categories with processed food exclusion
        val categories = product.categories?.lowercase() ?: ""
        
        // Exclude processed foods
        val processedKeywords = listOf(
            "cereales", "cereals", "granola", "muesli", "desayunos", "breakfast",
            "snacks", "bocadillos", "bebidas", "beverages", "jugos", "juices",
            "procesado", "processed", "enlatado", "canned", "congelado", "frozen",
            "pan", "bread", "pasta", "galletas", "cookies",
            "postres", "desserts", "yogures de frutas", "yogurts with fruits",
            "chocolate", "sweetened", "edulcorado", "flavored", "saborizado"
        )
        
        val isProcessed = processedKeywords.any { keyword -> 
            categories.contains(keyword) 
        }
        
        if (isProcessed) {
            return false
        }
        
        // Include whole foods
        val wholeFoodKeywords = listOf(
            "fruits", "frutas", "vegetables", "verduras", "fresh", "fresco",
            "nuts", "nueces", "seeds", "semillas", "legumes", "legumbres",
            "meat", "carne", "fish", "pescado", "dairy", "lacteos", "eggs", "huevos"
        )
        
        return wholeFoodKeywords.any { keyword ->
            categories.contains(keyword)
        }
    }
}
