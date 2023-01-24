package link.lucidleaf.decentralizedsecurechat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//credit goes to https://sendbird.com/developer/tutorials/android-chat-tutorial-building-a-messaging-ui
class ChatAdapter(context: Context, messageList: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mContext: Context
    private val mMessageList: List<Message>

    init {
        mContext = context
        mMessageList = messageList
    }

    override fun getItemCount(): Int {
        return mMessageList.size
    }

    // Determines the appropriate ViewType according to the sender of the message.
    override fun getItemViewType(position: Int): Int {
        val message = mMessageList[position]
        return message.messageType
    }

    // Inflates the appropriate layout according to the ViewType.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return if (viewType == MESSAGE_SENT) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_sent, parent, false)
            SentMessageHolder(view)
        } else if (viewType == MESSAGE_RECEIVED) {

            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_received, parent, false)
            ReceivedMessageHolder(view)
        } else {
            if (viewType != MESSAGE_STATUS) {
                print("Unknown message type")
            }
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_status, parent, false)
            StatusMessageHolder(view)
        }
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = mMessageList[position]
        when (holder.itemViewType) {
            MESSAGE_SENT -> (holder as SentMessageHolder?)!!.bind(message)
            MESSAGE_RECEIVED -> (holder as ReceivedMessageHolder?)!!.bind(message)
            MESSAGE_STATUS -> (holder as StatusMessageHolder?)!!.bind(message)
        }
    }

    private inner class SentMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var messageText: TextView

        init {
            messageText = itemView.findViewById(R.id.text_message_status)
        }

        fun bind(message: Message) {
            messageText.text = message.body
        }
    }

    private inner class ReceivedMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var messageText: TextView

        init {
            messageText = itemView.findViewById(R.id.text_gchat_message_other)
        }

        fun bind(message: Message) {
            messageText.text = message.body
        }
    }

    private inner class StatusMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var messageText: TextView

        init {
            messageText = itemView.findViewById(R.id.text_message_status)
        }

        fun bind(message: Message) {
            messageText.text = message.body
        }
    }
}