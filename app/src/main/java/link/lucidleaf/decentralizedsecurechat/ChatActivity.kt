package link.lucidleaf.decentralizedsecurechat

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ChatActivity : AppCompatActivity() {
    private var txtPeerName: TextView? = null
    private var txtMessageBody: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val args: Intent = intent
        val peerName = args.getStringExtra("peerName")

        txtPeerName = findViewById(R.id.txtChatPeerName)
        txtPeerName?.text = peerName
        txtMessageBody = findViewById(R.id.txtChatMessage)
        txtMessageBody?.setOnClickListener {
            txtMessageBody!!.text = ""
        }
    }
}