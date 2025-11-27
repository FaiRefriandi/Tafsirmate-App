package com.froztlass.tafsirmate.model

data class ApiResponse(
    val code: Int,
    val message: String,
    val data: Surat
)
