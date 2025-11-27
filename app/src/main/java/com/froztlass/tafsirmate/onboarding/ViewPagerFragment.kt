package com.froztlass.tafsirmate.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.froztlass.tafsirmate.R
import com.froztlass.tafsirmate.onboarding.screens.FirstScreen
import com.froztlass.tafsirmate.onboarding.screens.SecondScreen
import com.froztlass.tafsirmate.onboarding.screens.ThirdScreen

class ViewPagerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_pager, container, false)

        val fragmentList = arrayListOf(
            FirstScreen(),
            SecondScreen(),
            ThirdScreen()
        )

        val adapter = ViewPagerAdapter(
            fragmentList,
            requireActivity().supportFragmentManager,
            lifecycle
        )

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cek apakah pengguna sudah login
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", 0)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Jika sudah login, langsung ke HomeFragment
            findNavController().navigate(R.id.action_viewPagerFragment_to_homeFragment)
        }
    }
}
