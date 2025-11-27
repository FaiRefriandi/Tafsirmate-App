package com.froztlass.tafsirmate.api

import android.util.Log
import com.froztlass.tafsirmate.model.ApiResponse
import com.froztlass.tafsirmate.model.SurahResponse
import com.froztlass.tafsirmate.model.Surat
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object EquranRepository {
    private val api: EquranApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://equran.id/") // Base URL API EQURAN
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(EquranApiService::class.java)
    }

    fun getSuratList(callback: (List<Surat>?) -> Unit) {
        api.getSuratList().enqueue(object : retrofit2.Callback<SurahResponse> {
            override fun onResponse(
                call: retrofit2.Call<SurahResponse>,
                response: retrofit2.Response<SurahResponse>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Log.d("API_RESPONSE", "Surat list: ${response.body()?.data}") // ✅ Tambahkan di sini
                    callback(response.body()?.data)
                } else {
                    Log.e("API_ERROR", "Gagal mendapatkan daftar surat: ${response.errorBody()?.string()}")
                    callback(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<SurahResponse>, t: Throwable) {
                Log.e("API_ERROR", "Request gagal: ${t.localizedMessage}")
                t.printStackTrace()
                callback(null)
            }
        })
    }

    fun getSurahDetail(nomor: Int, callback: (Surat?) -> Unit) {
        api.getSurah(nomor).enqueue(object : retrofit2.Callback<ApiResponse> {
            override fun onResponse(
                call: retrofit2.Call<ApiResponse>,
                response: retrofit2.Response<ApiResponse>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val surat = response.body()?.data

                    // Cek apakah suratSebelumnya dan suratSelanjutnya adalah Boolean
                    if (surat != null) {
                        val prevSurah = surat.suratSebelumnya
                        val nextSurah = surat.suratSelanjutnya

                        if (prevSurah is Boolean) surat.suratSebelumnya = null
                        if (nextSurah is Boolean) surat.suratSelanjutnya = null
                    }

                    callback(surat)
                } else {
                    Log.e("API_ERROR", "Gagal mendapatkan detail surat ${nomor}: ${response.errorBody()?.string()}")
                    callback(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {
                t.printStackTrace()
                callback(null)
            }
        })
    }
}
