package com.froztlass.tafsirmate

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.froztlass.tafsirmate.databinding.FragmentMenuBinding

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val homeFragment = HomeFragment()
    private val botFragment = BotFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        setupBottomNavigation()
        return binding.root
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = binding.bottomNavigation

        // Tambahkan semua fragment hanya sekali
        childFragmentManager.beginTransaction().apply {
            add(R.id.frame_container, homeFragment, "HomeFragment").show(homeFragment)
            add(R.id.frame_container, botFragment, "BotFragment").hide(botFragment)
            add(R.id.frame_container, profileFragment, "ProfileFragment").hide(profileFragment)
            commit()
        }

        // Listener Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navHome -> {
                    showFragment(homeFragment)
                    true
                }
                R.id.navBot -> {
                    showFragment(botFragment)
                    true
                }
                R.id.navProfile -> {
                    showFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startTab = arguments?.getString("startTab")
        if (startTab == "profile") {
            binding.bottomNavigation.selectedItemId = R.id.navProfile
            showFragment(profileFragment)
        } else {
            // Default ke home (opsional, karena sudah default di deklarasi awal)
            binding.bottomNavigation.selectedItemId = R.id.navHome
            showFragment(homeFragment)
        }
    }


    private fun showFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(fragment)
            commit()
        }
        activeFragment = fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
