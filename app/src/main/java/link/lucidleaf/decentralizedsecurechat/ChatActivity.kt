package link.lucidleaf.decentralizedsecurechat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class ChatActivity : AppCompatActivity() {
    private var txtPeerName: TextView? = null
    private var txtMessage: TextView? = null
    private var btnSend: ImageView? = null
    private var recyclerChat: RecyclerView? = null
    private var chatAdapter: ChatAdapter? = null
    var otherUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        otherUser = MessagesAndUsersDB.getUserByName(intent.getStringExtra(EXTRA_USER_ARRAY)!!)

        recyclerChat = findViewById<View>(R.id.recyclerChatMessages) as RecyclerView
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerChat!!.layoutManager = layoutManager
        updateMessages()

        txtPeerName = findViewById(R.id.txtChatPeerName)
        txtPeerName?.text = otherUser?.nickName

        txtMessage = findViewById(R.id.txtMessage)

        btnSend = findViewById(R.id.btnSend)
        btnSend?.setOnClickListener {
            val body = trimWhiteSpace(txtMessage?.text.toString())
            if (body == "")
                return@setOnClickListener
            txtMessage?.text = ""
            MessagesAndUsersDB.addMessage(
                Message(
                    User.getCurrentUser(),
                    otherUser!!,
                    body,
                    Calendar.getInstance().time
                )
            )
            updateMessages()
        }
    }

    private fun trimWhiteSpace(string: String): String {
        var rString: String = string
        while (rString.endsWith(" ") || rString.endsWith("\n"))
            rString = rString.substring(0..rString.length - 2)
        while (rString.startsWith(" ") || rString.startsWith("\n"))
            rString = rString.substring(1 until rString.length)
        return rString
    }

    override fun onResume() {
        super.onResume()
        MessagesAndUsersDB.subscribeToUpdates(this)
    }

    override fun onPause() {
        super.onPause()
        MessagesAndUsersDB.unsubscribeUpdates(this)
    }

    fun updateMessages() {
        val messageList = MessagesAndUsersDB.getMessages(otherUser!!)
        chatAdapter = ChatAdapter(this, messageList!!)
        recyclerChat!!.adapter = chatAdapter
        recyclerChat!!.smoothScrollToPosition(messageList.size)
    }

}