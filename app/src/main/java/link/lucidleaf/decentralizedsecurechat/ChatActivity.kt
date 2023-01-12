package link.lucidleaf.decentralizedsecurechat

import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
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
    var otherDevice: WifiP2pDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initializeElements()
    }

    private fun initializeElements() {
        setSupportActionBar(findViewById(R.id.toolbarChat))

        otherDevice = Box.get(intent, OTHER_DEVICE)
        otherDevice?.let { updateMessages(it) }
        txtMessage = findViewById(R.id.txtMessage)
        recyclerChat = findViewById<View>(R.id.recyclerChatMessages) as RecyclerView
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerChat!!.layoutManager = layoutManager
        btnSend = findViewById(R.id.btnSend)
        btnSend?.setOnClickListener {
            val body = trimWhiteSpace(txtMessage?.text.toString())
            if (body == "")
                return@setOnClickListener
            txtMessage?.text = ""
            otherDevice?.let {
                DataBase.addMessage(Message(it, true, body))
            }
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

    fun updateMessages(otherDevice: WifiP2pDevice) {
        val messageList = DataBase.getMessages(otherDevice)
        chatAdapter = ChatAdapter(this, messageList)
        recyclerChat!!.adapter = chatAdapter
        recyclerChat!!.smoothScrollToPosition(messageList.size)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        title = otherDevice?.deviceName
        return super.onCreateOptionsMenu(menu)
    }

    override fun onResume() {
        println("Chat with ${otherDevice?.deviceName} resumed")
        super.onResume()
    }

    override fun onPause() {
        println("Chat with ${otherDevice?.deviceName} paused")
        super.onPause()
    }

    override fun onStop() {
        println("Chat with ${otherDevice?.deviceName} stopped")
        super.onStop()
    }

    override fun onDestroy() {
        //todo close connection
        println("Chat with ${otherDevice?.deviceName} destroyed")
        Box.remove(intent)
        super.onDestroy()
    }

}