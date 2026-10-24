package com.example.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit service interface for querying Ezan Vakti Pro's Mosque API.
 */
interface EvpMosqueService {
    @GET("v3.0/mosque/list")
    suspend fun getMosques(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radiusMeters: Int,
        @Header("xApiKey") xApiKey: String = EvpAuthTokenGenerator.generateToken()
    ): List<EvpMosque>
}
