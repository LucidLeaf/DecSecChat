package link.lucidleaf.decentralizedsecurechat

import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class ChatActivity : AppCompatActivity() {
    private var menu: Menu? = null
    private var txtMessage: TextView? = null
    private var btnSend: ImageView? = null
    private var recyclerChat: RecyclerView? = null
    private var chatAdapter: ChatAdapter? = null
    var otherUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initializeElements()
    }

    private fun initializeElements() {
        setSupportActionBar(findViewById(R.id.toolbarChat))
        otherUser = MessagesAndUsersDB.getUserByName(intent.getStringExtra(EXTRA_USER_ARRAY)!!)

        recyclerChat = findViewById<View>(R.id.recyclerChatMessages) as RecyclerView
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerChat!!.layoutManager = layoutManager
        updateMessages()

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

    fun updateMessages() {
        val messageList = MessagesAndUsersDB.getMessages(otherUser!!)
        chatAdapter = ChatAdapter(this, messageList!!)
        recyclerChat!!.adapter = chatAdapter
        recyclerChat!!.smoothScrollToPosition(messageList.size)
    }

    override fun onResume() {
        super.onResume()
        MessagesAndUsersDB.subscribeToUpdates(this)
    }

    override fun onPause() {
        super.onPause()
        MessagesAndUsersDB.unsubscribeUpdates(this)
        println("Chat with ${otherUser?.nickName} paused")
    }

    override fun onStop() {
        super.onStop()
        println("Chat with ${otherUser?.nickName} stopped")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        title = otherUser?.nickName
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menuChangeNickname -> {
            val alert: AlertDialog.Builder = AlertDialog.Builder(this)

            alert.setTitle("Change nickname of ${otherUser?.nickName}")
//            alert.setMessage("New Name")

            // Set an EditText view to get user input
            val input = EditText(this)
            input.setText(otherUser?.nickName)
            alert.setView(input)
            alert.setPositiveButton("Ok") { dialog, whichButton ->
                val value: Editable? = input.text
                // Do something with value!
                otherUser?.nickName = value.toString()
                title = otherUser?.nickName
            }
            alert.setNegativeButton("Cancel") { dialog, whichButton ->
                // Canceled.
            }
            alert.show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        //todo close connection
        println("Chat with ${otherUser?.nickName}")
    }

}