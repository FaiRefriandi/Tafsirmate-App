package com.froztlass.tafsirmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.froztlass.tafsirmate.adapter.ChatAdapter
import com.froztlass.tafsirmate.model.ChatItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavOptions

class ChatHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_history, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewChats)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        chatAdapter = ChatAdapter(emptyList()) { chatId ->
            val bundle = Bundle().apply {
                putString("chatId", chatId)
            }
            findNavController().navigate(R.id.action_chatHistoryFragment_to_botFragment, bundle)
        }

        recyclerView.adapter = chatAdapter

        firestore = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        loadChatHistory()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = findNavController()
                val bundle = Bundle().apply {
                    putString("startTab", "profile") // atau bisa pakai konstanta
                }
                navController.navigate(R.id.menuFragment, bundle)
            }
        })
    }

    private fun loadChatHistory() {
        firestore.collection("users")
            .document(userId)
            .collection("chats")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val chats = result.documents.mapNotNull { doc ->
                    val title = doc.getString("title")
                    val id = doc.id
                    if (title != null) ChatItem(id, title) else null
                }
                chatAdapter.updateChats(chats)
            }
            .addOnFailureListener {
                // Tambahkan log atau error handler kalau mau
            }
    }
}
