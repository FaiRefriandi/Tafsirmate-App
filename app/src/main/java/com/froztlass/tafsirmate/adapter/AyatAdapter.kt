package com.froztlass.tafsirmate.adapter

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.froztlass.tafsirmate.R
import com.froztlass.tafsirmate.model.Ayat
import com.froztlass.tafsirmate.model.Surat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class AyatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ayatList = mutableListOf<Ayat>()
    private var surat: Surat? = null

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_AYAT = 1
    }

    fun submitData(surat: Surat) {
        this.surat = surat
        ayatList.clear()
        ayatList.addAll(surat.ayat)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = ayatList.size + 1 // +1 untuk header

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_AYAT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_surat_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ayat, parent, false)
            AyatViewHolder(view, parent.context)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            surat?.let { holder.bind(it) }
        } else if (holder is AyatViewHolder) {
            val ayat = ayatList[position - 1]
            holder.bind(ayat)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNamaSurat: TextView = itemView.findViewById(R.id.tvNamaSurat)
        private val tvNamaLatin: TextView = itemView.findViewById(R.id.tvNamaLatin)
        private val tvTempatTurun: TextView = itemView.findViewById(R.id.tvTempatTurun)
        private val tvJumlahAyat: TextView = itemView.findViewById(R.id.tvJumlahAyat)
        private val tvArti: TextView = itemView.findViewById(R.id.tvArti)

        fun bind(surat: Surat) {
            tvNamaSurat.text = surat.nama
            tvNamaLatin.text = surat.namaLatin
            tvTempatTurun.text = surat.tempatTurun
            tvJumlahAyat.text = "Jumlah Ayat: ${surat.jumlahAyat}"
            tvArti.text = surat.arti
        }
    }

    class AyatViewHolder(itemView: View, context: Context) : RecyclerView.ViewHolder(itemView),
        TextToSpeech.OnInitListener {

        private val tvAyatNumber: TextView = itemView.findViewById(R.id.tvAyatNumber)
        private val tvAyatText: TextView = itemView.findViewById(R.id.tvAyatText)
        private val tvAyatLatin: TextView = itemView.findViewById(R.id.tvAyatLatin)
        private val tvAyatTerjemahan: TextView = itemView.findViewById(R.id.tvAyatTerjemahan)
        private val btnAI: ImageButton = itemView.findViewById(R.id.btnAI)
        private val tvAiResponse: TextView = itemView.findViewById(R.id.tvAiResponse)
        private val btnSpeak: Button = itemView.findViewById(R.id.btnSpeak)

        private var tts: TextToSpeech? = null
        private var lastResponseText: String = ""

        init {
            tts = TextToSpeech(context, this)
            btnSpeak.setOnClickListener {
                if (lastResponseText.isNotBlank()) {
                    tts?.speak(lastResponseText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }

        fun bind(ayat: Ayat) {
            tvAyatNumber.text = ayat.nomorAyat.toString()
            tvAyatText.text = ayat.teksArab
            tvAyatLatin.text = ayat.teksLatin
            tvAyatTerjemahan.text = ayat.teksIndonesia
            tvAiResponse.visibility = View.GONE
            tvAiResponse.text = ""
            btnSpeak.visibility = View.GONE

            btnAI.setOnClickListener {
                getMaknaDariAI(ayat.teksIndonesia)
            }
        }

        private fun getMaknaDariAI(teks: String?) {
            if (teks.isNullOrBlank()) {
                tvAiResponse.visibility = View.VISIBLE
                tvAiResponse.text = "Teks kosong atau null, tidak dapat memuat makna."
                return
            }

            tvAiResponse.visibility = View.VISIBLE
            tvAiResponse.text = "Memuat makna ayat..."
            btnSpeak.visibility = View.GONE

            val apiKey = "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" // Ganti dengan API key kamu
            val prompt = "Jelaskan makna mendalam dari ayat ini :\n\n\"$teks\""

            val client = OkHttpClient()
            val mediaType = "application/json".toMediaTypeOrNull()
            val json = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("temperature", 0.1)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }))
                put("max_tokens", 500)
            }

            val body = RequestBody.create(mediaType, json.toString())
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    itemView.post {
                        tvAiResponse.text = "Gagal memuat makna."
                        tvAiResponse.visibility = View.VISIBLE
                        btnSpeak.visibility = View.GONE
                        e.printStackTrace()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        itemView.post {
                            tvAiResponse.text = "Response tidak berhasil, status: ${response.code}"
                            tvAiResponse.visibility = View.VISIBLE
                            btnSpeak.visibility = View.GONE
                        }
                        return
                    }

                    try {
                        val responseBody = response.body?.string() ?: ""
                        val aiText = JSONObject(responseBody)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        itemView.post {
                            lastResponseText = aiText.trim()
                            tvAiResponse.text = lastResponseText
                            tvAiResponse.visibility = View.VISIBLE
                            btnSpeak.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        itemView.post {
                            tvAiResponse.text = "Terjadi kesalahan saat memproses respons."
                            tvAiResponse.visibility = View.VISIBLE
                            btnSpeak.visibility = View.GONE
                        }
                        e.printStackTrace()
                    }
                }
            })
        }

        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
            }
        }
    }
}
