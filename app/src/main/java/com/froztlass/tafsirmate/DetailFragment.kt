package com.froztlass.tafsirmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.froztlass.tafsirmate.adapter.AyatAdapter
import com.froztlass.tafsirmate.api.EquranRepository
import com.froztlass.tafsirmate.databinding.FragmentDetailBinding
import android.util.Log


class DetailFragment : Fragment() {

    private var surahNumber: Int? = null
    private var surahName: String? = null
    private lateinit var binding: FragmentDetailBinding
    private lateinit var ayatAdapter: AyatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data dari Bundle
        arguments?.let {
            surahNumber = it.getInt("surahNumber")
            surahName = it.getString("surahName")
        }

        if (surahNumber == null || surahName == null) {
            Toast.makeText(context, "Data Surat tidak ditemukan!", Toast.LENGTH_SHORT).show()
            return
        }

        // Menampilkan nama surat di UI
        binding.tvSurahName.text = surahName
        binding.tvSurahNumber.text = surahNumber.toString()

        // Siapkan RecyclerView untuk menampilkan ayat
        ayatAdapter = AyatAdapter()
        binding.recyclerViewAyat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ayatAdapter
        }

        // Memuat detail surat
        loadSurahDetail()
        // Di dalam loadAyat()
        Log.d("DetailFragment", "Surah Number: $surahNumber")
    }

    private fun loadSurahDetail() {
        EquranRepository.getSurahDetail(surahNumber!!) { surat ->
            if (surat != null) {
                Log.d("AYAT_CHECK", "Surat ${surahNumber} jumlah ayat: ${surat.ayat?.size}")
                ayatAdapter.submitData(surat)
            } else {
                Log.e("ERROR", "Ayat tidak ditemukan untuk surat ${surahNumber}")
                Toast.makeText(context, "Gagal memuat ayat", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
