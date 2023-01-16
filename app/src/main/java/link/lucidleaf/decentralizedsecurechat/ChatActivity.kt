package link.lucidleaf.decentralizedsecurechat

import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.Socket


class ChatActivity : AppCompatActivity() {
    private var menu: Menu? = null
    private var txtMessage: TextView? = null
    private var btnSend: ImageView? = null
    private var recyclerChat: RecyclerView? = null
    private var chatAdapter: ChatAdapter? = null
    var peerPublicKey: String = ""
    private var socket: Socket? = null
    private var ioStream: SendReceiveStream? = null
    val ioHandler = Handler { message ->
        when (message.what) {
            MESSAGE_READ -> {
                val readBuffer = message.obj as ByteArray
                var body = String(readBuffer, 0, message.arg1)
                //check if this message is the one containing the key
                if (body.startsWith(KEY_HEADER) && body.endsWith(KEY_FOOTER)) {
                    peerPublicKey = body
                    peerPublicKey = peerPublicKey.removePrefix(KEY_HEADER)
                    peerPublicKey = peerPublicKey.removeSuffix(KEY_FOOTER)

                    DataBase.addMessage(peerPublicKey, Message(true, "my key: ${Encryption.getPublicKey()}"))
                    DataBase.addMessage(peerPublicKey, Message(true, "their key: $peerPublicKey"))
                } else {
                    body = Encryption.decryptMessage(body)
                    DataBase.addMessage(peerPublicKey, Message(false, body))
                }
            }
        }
        return@Handler true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //enable running Network code in the main loop
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        initializeElements()
    }

    private fun initializeElements() {
        setSupportActionBar(findViewById(R.id.toolbarChat))
        socket = Box.get(intent, SOCKET)
        //get UI updates on new messages
        DataBase.subscribeUIUpdates(this)
        ioStream = socket?.let { SendReceiveStream(it, this) }
        ioStream?.start()
        ioStream?.write(keyMessage().toByteArray())

        txtMessage = findViewById(R.id.txtMessage)
        recyclerChat = findViewById<View>(R.id.recyclerChatMessages) as RecyclerView
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerChat?.layoutManager = layoutManager
        btnSend = findViewById(R.id.btnSend)
        btnSend?.setOnClickListener {
            val body = trimWhiteSpace(txtMessage?.text.toString())
            if (body == "")
                return@setOnClickListener
            txtMessage?.text = ""
            val cipher = Encryption.encryptMessage(body, peerPublicKey)
            ioStream?.write(cipher.toByteArray())
            DataBase.addMessage(peerPublicKey, Message(true, body))
        }
        //display past messages
        updateUI()
    }

    private fun keyMessage(): String {
        return KEY_HEADER + Encryption.getPublicKey() + KEY_FOOTER
    }

    private fun trimWhiteSpace(string: String): String {
        var rString: String = string
        while (rString.endsWith(" ") || rString.endsWith("\n"))
            rString = rString.substring(0..rString.length - 2)
        while (rString.startsWith(" ") || rString.startsWith("\n"))
            rString = rString.substring(1 until rString.length)
        return rString
    }

    fun updateUI() {
        val messageList = DataBase.getMessages(peerPublicKey)
        chatAdapter = ChatAdapter(this, messageList)
        recyclerChat?.adapter = chatAdapter
        recyclerChat?.smoothScrollToPosition(messageList.size)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        DataBase.unsubscribeUIUpdates(this)
        Box.remove(intent)
        ioStream?.connected = false
        ioStream?.join()
        socket?.close()
        super.onDestroy()
    }
}