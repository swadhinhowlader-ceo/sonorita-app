package com.sonorita.assistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sonorita.assistant.R

class ChatAdapter(
    private val messages: List<HomeActivity.ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_message_user
            else -> R.layout.item_message_ai
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount() = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val emojiText: TextView? = itemView.findViewById(R.id.emojiText)
        private val providerText: TextView? = itemView.findViewById(R.id.providerText)

        fun bind(message: HomeActivity.ChatMessage) {
            messageText.text = message.content

            emojiText?.text = message.emoji ?: ""
            emojiText?.visibility = if (message.emoji != null) View.VISIBLE else View.GONE

            providerText?.text = message.provider ?: ""
            providerText?.visibility = if (message.provider != null) View.VISIBLE else View.GONE
        }
    }
}
