package link.lucidleaf.decentralizedsecurechat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


//credit goes to https://sendbird.com/developer/tutorials/android-chat-tutorial-building-a-messaging-ui
class MessageListAdapter(context: Context, messageList: List<Message>) :
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
        return if (message.thisUser()) {
            // If the current user is the sender of the message
            VIEW_TYPE_MESSAGE_SENT
        } else {
            // If some other user sent the message
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_from_me, parent, false)
            SentMessageHolder(view)
        } else {
            if (viewType != VIEW_TYPE_MESSAGE_RECEIVED) {
                println("Unkown message type")
            }
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_from_them, parent, false)
            ReceivedMessageHolder(view)
        }
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = mMessageList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageHolder?)!!.bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageHolder?)!!.bind(message)
        }
    }

    private inner class SentMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var messageText: TextView
        var timeText: TextView

        init {
            messageText = itemView.findViewById(R.id.text_gchat_message_me)
            timeText = itemView.findViewById(R.id.text_gchat_timestamp_me)
        }

        fun bind(message: Message) {
            messageText.text = message.body

            // Format the stored timestamp into a readable String.
            //todo this might look ugly, formatting function might be necessary
            timeText.text = message.createdAt.toString()
        }
    }

    private inner class ReceivedMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var messageText: TextView
        var timeText: TextView
        var nameText: TextView

        init {
            messageText = itemView.findViewById(R.id.text_gchat_message_other)
            timeText = itemView.findViewById(R.id.text_gchat_timestamp_other)
            nameText = itemView.findViewById(R.id.text_gchat_user_other)
        }

        fun bind(message: Message) {
            messageText.text = message.body

            // Format the stored timestamp into a readable String using method.
            //todo this might look ugly, formatting function might be necessary
            timeText.text = message.createdAt.toString()
            nameText.text = message.user.nickName
        }
    }

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }
}