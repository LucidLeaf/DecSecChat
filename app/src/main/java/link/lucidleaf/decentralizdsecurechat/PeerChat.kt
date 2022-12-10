package link.lucidleaf.decentralizdsecurechat

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PeerChat: AppCompatActivity() {
    private var txtPeerName: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat)

        val args: Intent = intent
        val peerName = args.getStringExtra("peerName")

        txtPeerName = findViewById(R.id.peerName)
        txtPeerName?.text = peerName
    }
}