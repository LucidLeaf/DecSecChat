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
    var publicKey: String = ""
    private var socket: Socket? = null
    private var ioStream: IOStream? = null
    val ioHandler = Handler { message ->
        when (message.what) {
            MESSAGE_READ -> {
                val readBuffer = message.obj as ByteArray
                val body = String(readBuffer, 0, message.arg1)
                DataBase.addMessage(Message(publicKey, false, body))
            }
        }
        return@Handler true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initializeElements()
        //enable running Network code in the main loop
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun initializeElements() {
        setSupportActionBar(findViewById(R.id.toolbarChat))
        socket = Box.get(intent, SOCKET)
        ioStream = socket?.let { IOStream(it, this) }
        ioStream?.start()
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
            ioStream?.write(body.toByteArray())
            DataBase.addMessage(Message(publicKey, true, body))
        }
        //get UI updates on new messages
        DataBase.subscribeUIUpdates(this)
        //display past messages
        updateUI()
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
        val messageList = DataBase.getMessages(publicKey)
        chatAdapter = ChatAdapter(this, messageList)
        recyclerChat?.adapter = chatAdapter
        recyclerChat?.smoothScrollToPosition(messageList.size)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        title = publicKey
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