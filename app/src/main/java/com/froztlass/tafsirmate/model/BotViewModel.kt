package com.froztlass.tafsirmate.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class BotViewModel(private val state: SavedStateHandle) : ViewModel() {

    companion object {
        private const val CHAT_HISTORY_KEY = "chat_history"
    }

    // Menyimpan riwayat chat dalam Pair<String, Boolean>
    var chatHistory: MutableList<Pair<String, Boolean>> =
        state.get<MutableList<Pair<String, Boolean>>>(CHAT_HISTORY_KEY) ?: mutableListOf()

    fun addMessage(message: String, isUser: Boolean) {
        chatHistory.add(Pair(message, isUser))
        state[CHAT_HISTORY_KEY] = chatHistory
    }
}
