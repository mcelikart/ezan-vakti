package com.example.data

import retrofit2.http.GET
import retrofit2.http.Path

interface AlQuranApiService {
    @GET("surah/{id}")
    suspend fun getSurahArabic(@Path("id") id: Int): QuranApiResponse

    @GET("surah/{id}/tr.diyanet")
    suspend fun getSurahTranslation(@Path("id") id: Int): QuranApiResponse

    @GET("surah/{id}/{edition}")
    suspend fun getSurahEdition(@Path("id") id: Int, @Path("edition") edition: String): QuranApiResponse
}

data class QuranApiResponse(
    val code: Int,
    val status: String,
    val data: SurahDataResponse
)

data class SurahDataResponse(
    val number: Int,
    val name: String,
    val englishName: String,
    val numberOfAyahs: Int,
    val ayahs: List<AyahResponse>
)

data class AyahResponse(
    val number: Int,
    val text: String,
    val numberInSurah: Int
)
