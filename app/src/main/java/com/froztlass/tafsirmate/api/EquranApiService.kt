package com.froztlass.tafsirmate.api

import com.froztlass.tafsirmate.model.ApiResponse
import com.froztlass.tafsirmate.model.SurahResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface EquranApiService {

    @GET("api/v2/surat")
    fun getSuratList(): Call<SurahResponse>

    @GET("api/v2/surat/{nomor}")
    fun getSurah(@Path("nomor") nomor: Int): Call<ApiResponse>
}
