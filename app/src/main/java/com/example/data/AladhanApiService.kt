package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query

interface AladhanApiService {
    // method 13 is Diyanet İşleri Başkanlığı
    @GET("timings/{date}")
    suspend fun getTimingsByCoordinates(
        @retrofit2.http.Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 13
    ): AladhanResponse

    @GET("calendar/{year}/{month}")
    suspend fun getCalendarByCoordinates(
        @retrofit2.http.Path("year") year: Int,
        @retrofit2.http.Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 13
    ): CalendarResponse
}
