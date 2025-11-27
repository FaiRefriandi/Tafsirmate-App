package com.froztlass.tafsirmate


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.froztlass.tafsirmate.api.EquranRepository
import com.froztlass.tafsirmate.model.Ayat
import com.froztlass.tafsirmate.model.Surat

class HomeViewModel : ViewModel() {
    private val _suratList = MutableLiveData<List<Surat>>()
    val suratList: LiveData<List<Surat>> get() = _suratList

    private val _ayatList = MutableLiveData<List<Ayat>>()
    val ayatList: LiveData<List<Ayat>> get() = _ayatList

    fun loadSuratList() {
        EquranRepository.getSuratList { suratList ->
            _suratList.postValue(suratList ?: emptyList())
        }
    }

    fun loadAyatList(surahNumber: Int) {
        EquranRepository.getSurahDetail(surahNumber) { surat ->
            _ayatList.postValue(surat?.ayat ?: emptyList())
        }
    }
}