package com.example.template.data.service

import com.example.template.data.model.OpenFoodFactsResponse
import com.example.template.data.model.SearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import android.util.Log

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
        @Query("lc") languageCode: String? = null
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
                        Log.d("OpenFoodFactsService", "Raw nutriments data for barcode $barcode:")
                        Log.d("OpenFoodFactsService", "Sodium fields: 100g=${nutriments.sodium100g}, regular=${nutriments.sodium}, value=${nutriments.sodiumValue}")
                        Log.d("OpenFoodFactsService", "Vitamin C fields: 100g=${nutriments.vitaminC100g}, regular=${nutriments.vitaminC}, value=${nutriments.vitaminCValue}")
                        Log.d("OpenFoodFactsService", "Calcium fields: 100g=${nutriments.calcium100g}, regular=${nutriments.calcium}, value=${nutriments.calciumValue}")
                        Log.d("OpenFoodFactsService", "Iron fields: 100g=${nutriments.iron100g}, regular=${nutriments.iron}, value=${nutriments.ironValue}")
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
     * Supports multilingual search and returns paginated results.
     */
    suspend fun searchProducts(
        searchTerms: String,
        pageSize: Int = 20,
        languageCode: String? = null
    ): Result<SearchResponse> {
        return try {
            val response = api.searchProducts(
                searchTerms = searchTerms,
                pageSize = pageSize,
                languageCode = languageCode
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("OpenFoodFactsService", "Search successful for '$searchTerms': ${body.count} results found")
                Result.success(body)
            } else {
                Result.failure(Exception("Search request failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("OpenFoodFactsService", "Search failed for '$searchTerms'", e)
            Result.failure(e)
        }
    }
}
