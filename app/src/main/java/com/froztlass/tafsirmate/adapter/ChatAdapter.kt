package com.froztlass.tafsirmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.froztlass.tafsirmate.R
import com.froztlass.tafsirmate.model.ChatItem

class ChatAdapter(
    private var chats: List<ChatItem>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.chatTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_history, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = chats[position]
        holder.titleText.text = item.title
        holder.itemView.setOnClickListener {
            onItemClick(item.chatId)
        }
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<ChatItem>) {
        chats = newChats
        notifyDataSetChanged()
    }
}