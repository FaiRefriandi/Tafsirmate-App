package com.froztlass.tafsirmate

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.froztlass.tafsirmate.databinding.FragmentProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardChatHistory = view.findViewById<MaterialCardView>(R.id.cardChatHistory)
        cardChatHistory.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_chatHistoryFragment)
        }

        auth = FirebaseAuth.getInstance()
        sharedPref = requireActivity().getSharedPreferences("AppPrefs", 0)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val user = auth.currentUser
        if (user != null) {
            binding.nameTextView.text = user.displayName ?: "Tidak Diketahui"
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.profilepict)
                .into(binding.profileImageView)
        } else {
            // Kalau user null, langsung ke ThirdScreen
            findNavController().navigate(
                R.id.thirdScreen,
                null,
                NavOptions.Builder().setPopUpTo(R.id.menuFragment, true).build()
            )
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut().addOnCompleteListener {
                if (it.isSuccessful) {
                    with(sharedPref.edit()) {
                        putBoolean("isLoggedIn", false)
                        apply()
                    }

                    // Logout berhasil, clear backstack dan navigasi ke ThirdScreen
                    findNavController().navigate(
                        R.id.thirdScreen,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.menuFragment, true) // hapus stack sampai ke menuFragment
                            .build()
                    )
                } else {
                    Toast.makeText(requireContext(), "Logout gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
