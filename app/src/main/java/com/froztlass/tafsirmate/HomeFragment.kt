package com.froztlass.tafsirmate

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.froztlass.tafsirmate.adapter.AyatAdapter
import com.froztlass.tafsirmate.adapter.SuratAdapter
import com.froztlass.tafsirmate.databinding.FragmentHomeBinding
import com.froztlass.tafsirmate.model.Surat
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var suratAdapter: SuratAdapter
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var ayatAdapter: AyatAdapter
    private var allSuratList: List<Surat> = listOf()
    private var doubleBackToExitPressedOnce = false
    private var currentSurat: Surat? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔥 Ambil Data User dari Firebase
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userName = user.displayName ?: "Pengguna"
            binding.tvGreetingUser.text = "Halo, $userName"
            binding.tvWelcomeMessage.text = "Selamat datang di Tafsirmate!\uD83D\uDC4B"

            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.akun_icon) // Ganti dengan placeholder default
                .into(binding.ivUserProfile)
        }

        try {
            val searchEditText = binding.searchView.findViewById<EditText>(
                resources.getIdentifier("android:id/search_src_text", null, null)
            )
            searchEditText.isCursorVisible = false
            searchEditText.setBackgroundResource(0)
            searchEditText.background = null

            val searchPlate = binding.searchView.findViewById<View>(
                resources.getIdentifier("android:id/search_plate", null, null)
            )
            searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            searchPlate.background = null

            val searchIcon = binding.searchView.findViewById<ImageView>(
                resources.getIdentifier("android:id/search_mag_icon", null, null)
            )
            searchIcon.setImageResource(R.drawable.search_icon)

            val customFont = ResourcesCompat.getFont(requireContext(), R.font.dmsans_medium)
            searchEditText.typeface = customFont
        } catch (e: Exception) {
            e.printStackTrace()
        }

        suratAdapter = SuratAdapter { surat ->
            showSurahDetails(surat)
        }

        ayatAdapter = AyatAdapter()

        binding.recyclerViewSurat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = suratAdapter
        }

        binding.recyclerViewAyat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ayatAdapter
            visibility = View.GONE
        }

        homeViewModel.suratList.observe(viewLifecycleOwner) { suratList ->
            if (suratList != null) {
                allSuratList = suratList
                suratAdapter.submitList(suratList)
            } else {
                allSuratList = emptyList()
                suratAdapter.submitList(emptyList())
            }
        }

        homeViewModel.ayatList.observe(viewLifecycleOwner) { ayatList ->
            if (ayatList != null && currentSurat != null) {
                val suratWithAyat = currentSurat!!.copy(ayat = ayatList)
                ayatAdapter.submitData(suratWithAyat)
                binding.recyclerViewAyat.visibility = View.VISIBLE
                binding.recyclerViewSurat.visibility = View.GONE
            } else {
                binding.recyclerViewAyat.visibility = View.GONE
                binding.recyclerViewSurat.visibility = View.VISIBLE
            }
        }

        // Tombol Filter
        binding.btnFilter.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.menu_filter, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.filter_all -> filterSurat2(null)
                    R.id.filter_makkah -> filterSurat2("Mekah")
                    R.id.filter_madinah -> filterSurat2("Madinah")
                }
                true
            }
            popupMenu.show()
        }

        homeViewModel.loadSuratList()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSurat(newText)
                return true
            }
        })

        // Tombol Back Arrow
        binding.btnBackArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }


        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.recyclerViewAyat.visibility == View.VISIBLE) {
                    binding.recyclerViewAyat.visibility = View.GONE
                    binding.recyclerViewSurat.visibility = View.VISIBLE
                    binding.searchView.visibility = View.VISIBLE
                    binding.btnFilter.visibility = View.VISIBLE
                    binding.searchContainer.visibility = View.VISIBLE
                    binding.tvGreetingUser.visibility = View.VISIBLE
                    binding.tvWelcomeMessage.visibility = View.VISIBLE
                    binding.ivUserProfile.visibility = View.VISIBLE
                    binding.btnBackArrow.visibility = View.GONE
                } else {
                    binding.searchView.setQuery("", false)
                    binding.searchView.clearFocus()
                    filterSurat(null)

                    if (doubleBackToExitPressedOnce) {
                        requireActivity().finish()
                    } else {
                        doubleBackToExitPressedOnce = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            doubleBackToExitPressedOnce = false
                        }, 2000)
                    }
                }
            }
        })
    }

    private fun showSurahDetails(surat: Surat) {
        currentSurat = surat
        homeViewModel.loadAyatList(surat.nomor)
        binding.tvSurahName.text = surat.namaLatin
        binding.searchView.visibility = View.GONE
        binding.btnFilter.visibility = View.GONE
        binding.recyclerViewSurat.visibility = View.GONE
        binding.recyclerViewAyat.visibility = View.VISIBLE
        binding.searchContainer.visibility = View.GONE
        binding.tvGreetingUser.visibility = View.GONE
        binding.tvWelcomeMessage.visibility = View.GONE
        binding.ivUserProfile.visibility = View.GONE
        binding.btnBackArrow.visibility = View.VISIBLE
    }

    private fun filterSurat(query: String?) {
        val filteredList = if (!query.isNullOrEmpty()) {
            allSuratList.filter { surat ->
                surat.namaLatin.contains(query, ignoreCase = true)
            }
        } else {
            allSuratList
        }
        suratAdapter.submitList(filteredList)
    }

    private fun filterSurat2(tempatTurun: String?) {
        val filteredList = if (tempatTurun == null) {
            allSuratList
        } else {
            allSuratList.filter { it.tempatTurun.equals(tempatTurun, ignoreCase = true) }
        }

        suratAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
