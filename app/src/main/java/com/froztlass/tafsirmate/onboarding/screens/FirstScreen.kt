package com.froztlass.tafsirmate.onboarding.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.froztlass.tafsirmate.R
import android.widget.TextView

class FirstScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_first_screen, container, false)

        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
        val nextTextView = view.findViewById<TextView>(R.id.next)

        nextTextView.setOnClickListener {
            viewPager?.currentItem = 1
        }

        return view
    }

}