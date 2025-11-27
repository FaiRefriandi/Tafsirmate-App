package com.froztlass.tafsirmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.froztlass.tafsirmate.databinding.ItemSuratBinding
import com.froztlass.tafsirmate.model.Surat

class SuratAdapter(private val onClick: (Surat) -> Unit) :
    RecyclerView.Adapter<SuratAdapter.SuratViewHolder>() {

    private val suratList = mutableListOf<Surat>()

    fun submitList(list: List<Surat>) {
        suratList.clear()
        suratList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuratViewHolder {
        val binding = ItemSuratBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SuratViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuratViewHolder, position: Int) {
        holder.bind(suratList[position])
    }

    override fun getItemCount(): Int = suratList.size

    inner class SuratViewHolder(private val binding: ItemSuratBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(surat: Surat) {
            binding.suratName.text = surat.namaLatin
            binding.tvSuratArti.text = surat.arti
            binding.tvSuratJumlahAyat.text = "Jumlah Ayat: ${surat.jumlahAyat} | ${surat.tempatTurun}"

            // Pass the Surat object to the onClick listener
            binding.root.setOnClickListener { onClick(surat) }
        }
    }
}
