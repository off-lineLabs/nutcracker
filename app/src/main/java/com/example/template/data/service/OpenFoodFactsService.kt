package com.example.template.data.service

import com.example.template.data.model.OpenFoodFactsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): Response<OpenFoodFactsResponse>
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
}
