package com.froztlass.tafsirmate.model

data class SurahResponse(
    val code: Int,
    val message: String,
    val data: List<Surat>
)