package com.friday.assistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Konusma gecmisi (Siz / Friday mesajlari) listesini gosteren adapter.
 * Windows surumundeki sag-kolon konusma gecmisi panelinin mobil karsiligi.
 */
class ConversationAdapter : RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

    data class Message(val sender: String, val text: String, val isError: Boolean = false)

    private val messages = mutableListOf<Message>()

    fun addMessage(sender: String, text: String, isError: Boolean = false) {
        messages.add(Message(sender, text, isError))
        notifyItemInserted(messages.size - 1)
    }

    fun clear() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderLabel: TextView = itemView.findViewById(R.id.senderLabel)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)

        fun bind(message: Message) {
            senderLabel.text = message.sender
            messageText.text = message.text
            val color = if (message.isError) {
                android.graphics.Color.parseColor("#E24B4A")
            } else {
                android.graphics.Color.parseColor("#1D9E75")
            }
            senderLabel.setTextColor(color)
        }
    }
}
