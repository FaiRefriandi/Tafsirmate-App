package com.froztlass.tafsirmate.model

data class Surat(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String,
    val audioFull: Map<String, String>,
    val ayat: List<Ayat>,
    var suratSelanjutnya: Any?, // Ubah menjadi var
    var suratSebelumnya: Any?  // Ubah menjadi var
)

data class Ayat(
    val nomorAyat: Int,
    val nomorSurat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String? = null,
    val terjemahan: String,
    val audio: Map<String, String>
)

data class ChatItem(
    val chatId: String,
    val title: String
)


