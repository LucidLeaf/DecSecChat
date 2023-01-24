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


class ChatActivity : AppCompatActivity() {
    private var menu: Menu? = null
    private var txtMessage: TextView? = null
    private var btnSend: ImageView? = null
    private var recyclerChat: RecyclerView? = null
    private var chatAdapter: ChatAdapter? = null
    private var peerPublicKey: String = ""
    private var connection: Connection? = null
    private var mainActivity: MainActivity? = null
    private var sendReceiveStream: SendReceiveStream? = null
    val ioHandler = Handler { message ->
        when (message.what) {
            MESSAGE_READ -> {
                val readBuffer = message.obj as ByteArray
                val body = String(readBuffer, 0, message.arg1)
                //check if this message is the one containing the key
                if (body.startsWith(KEY_HEADER) && body.endsWith(KEY_FOOTER)) {
                    peerPublicKey = body
                    peerPublicKey = peerPublicKey.removePrefix(KEY_HEADER)
                    peerPublicKey = peerPublicKey.removeSuffix(KEY_FOOTER)
                    DataBase.addMessage(peerPublicKey, Message(MESSAGE_STATUS, "peer public RSA key:\n$peerPublicKey"))

                    DataBase.subscribeUIUpdates(this)
                    updateUI()
                } else {
                    val plainText = Encryption.decryptMessage(body)
                    val msg = Message(MESSAGE_RECEIVED, plainText)
                    DataBase.addMessage(peerPublicKey, msg)
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
        connection = Box.get(intent, CONNECTION)
        mainActivity = Box.get(intent, MAIN_ACTIVITY)
        //get UI updates on new messages
        sendReceiveStream =
            connection?.let { it.getSocket()?.let { it1 -> SendReceiveStream(it1, this) } }
        sendReceiveStream?.start()
        sendReceiveStream?.write(keyMessage().toByteArray())

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
            sendReceiveStream?.write(cipher.toByteArray())
            DataBase.addMessage(peerPublicKey, Message(MESSAGE_SENT, body))
        }
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

    fun getPeerPublicKey(): String = peerPublicKey

    override fun onDestroy() {
        DataBase.unsubscribeUIUpdates(this)
        Box.remove(intent)
        connection?.closeConnection()
        sendReceiveStream?.closeConnection()
        sendReceiveStream?.join()
        mainActivity?.closeP2pConnection()
        super.onDestroy()
    }
}