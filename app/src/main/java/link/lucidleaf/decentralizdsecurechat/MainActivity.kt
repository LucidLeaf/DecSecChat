package link.lucidleaf.decentralizdsecurechat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {
    // ux stuff
    var btnDiscoverPeers: Button? = null
    var icWifi: ImageView? = null
    var icLocation: ImageView? = null
    var recyclerView: RecyclerView? = null

    // wifi p2p stuff
    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    var channel: WifiP2pManager.Channel? = null
    var receiver: BroadcastReceiver? = null
    val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    enum class Icons {
        WIFI, LOCATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize interactive stuffs
        btnDiscoverPeers = findViewById(R.id.btnDiscoverPeers)
        btnDiscoverPeers?.setOnClickListener { discoverPeers() }
        icWifi = findViewById(R.id.icWifi)
        icLocation = findViewById(R.id.icLocation)
        recyclerView = findViewById(R.id.peerList)

        displayPeers(Array(10) { i -> "Test Peer $i" })
        recyclerView?.layoutManager = LinearLayoutManager(this)

        requestLocationPermissions()
        //todo turn on location service

        // enable receiving p2p events
        channel = manager?.initialize(this, mainLooper, null)
        channel?.also { channel ->
            receiver = manager?.let { P2pBroadcastReceiver(it, channel, this) }
        }
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            setIcon(Icons.LOCATION, false)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else
            setIcon(Icons.LOCATION, true)
    }

    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        receiver?.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        receiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    private fun discoverPeers() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Requires Locaiton Services", Toast.LENGTH_SHORT).show()
            println("Location permissions not granted")
            return
        }
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                println("Discovering peers")
            }

            override fun onFailure(reasonCode: Int) {
                println("Failed discovering peers: $reasonCode")
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        println("Location Permision Granted")
                        setIcon(Icons.LOCATION, true)
                    }
                } else {
                    println("Location Permision Denied")
                    setIcon(Icons.LOCATION, false)
                }
                return
            }
        }
    }

    fun setIcon(icon: Icons, mode: Boolean) {
        when (icon) {
            Icons.WIFI -> {
                if (mode)
                    icWifi?.setImageResource(R.drawable.ic_wifi_on)
                else
                    icWifi?.setImageResource(R.drawable.ic_wifi_off)
            }
            Icons.LOCATION -> {
                if (mode)
                    icLocation?.setImageResource(R.drawable.ic_location_on)
                else
                    icLocation?.setImageResource(R.drawable.ic_location_off)
            }
        }
    }

    fun handlePeerListChange(peers: WifiP2pDeviceList) {
        val peerNames: Array<String> =
            peers.deviceList.map { wifiP2pDevice -> wifiP2pDevice.deviceName }.toTypedArray()
        displayPeers(peerNames)
    }

    fun displayPeers(peers: Array<String>) {
        val peerList = PeerListAdapter(peers, this)
        recyclerView?.adapter = peerList
    }

    fun openChat(peer: String) {
        println("Opening chat with $peer")
        val chatIntent = Intent(this, PeerChat::class.java)
        chatIntent.putExtra("peerName", peer)
        startActivity(chatIntent)
    }
}