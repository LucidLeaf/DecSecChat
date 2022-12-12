package link.lucidleaf.decentralizedsecurechat

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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


private const val LOCATION_REQUEST = 1

const val EXTRA_USER_ARRAY = "peerName"

//most functionality here is based on https://developer.android.com/guide/topics/connectivity/wifip2p#create-application
class MainActivity : AppCompatActivity() {
    // ux stuff
    private var btnDiscoverPeers: Button? = null
    private var icWifi: ImageView? = null
    private var icLocation: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var txtThisName: TextView? = null
    private var thisUser: User? = null

    // wifi p2p functionality
    private val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null
    private val intentFilter = IntentFilter().apply {
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

        //initialize stuffs
        initializeElements()

        requestLocationPermissions()
        enableLocation()
    }

    private fun initializeElements() {
        thisUser = User.getCurrentUser()
        txtThisName = findViewById(R.id.txtThisName)
        txtThisName?.text = thisUser!!.name
        btnDiscoverPeers = findViewById(R.id.btnDiscoverPeers)
        btnDiscoverPeers?.setOnClickListener {
            discoverPeers()
            displayPeers(arrayOf(User.getTemplateUser()))
        }
        icWifi = findViewById(R.id.icWifi)
        icLocation = findViewById(R.id.icLocation)
        recyclerView = findViewById(R.id.peerList)
        displayPeers(emptyArray())
        recyclerView?.layoutManager = LinearLayoutManager(this)
        // enable receiving p2p events
        channel = manager?.initialize(this, mainLooper, null)
        channel?.also { channel ->
            receiver = manager?.let { P2pBroadcastReceiver(it, channel, this) }
        }
    }

    private fun enableLocation() {
        //todo implement enabling location
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            setIcon(Icons.LOCATION, false)
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST
            )
        } else setIcon(Icons.LOCATION, true)
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

    override fun onDestroy() {
        super.onDestroy()
        MessagesAndUsersDB.dumpDB()
    }

    private fun discoverPeers() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Requires Location Services", Toast.LENGTH_SHORT).show()
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
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    println("Location Permission Granted")
                    setIcon(Icons.LOCATION, true)
                }
            } else {
                println("Location Permission Denied")
                setIcon(Icons.LOCATION, false)
            }
            return
        }
    }

    fun setIcon(icon: Icons, mode: Boolean) {
        when (icon) {
            Icons.WIFI -> if (mode) icWifi?.setImageResource(R.drawable.ic_wifi_on)
            else icWifi?.setImageResource(R.drawable.ic_wifi_off)
            Icons.LOCATION -> if (mode) icLocation?.setImageResource(R.drawable.ic_location_on)
            else icLocation?.setImageResource(R.drawable.ic_location_off)
        }
    }

    fun handlePeerListChange(peers: WifiP2pDeviceList) {
        val users: Array<User> = peers.deviceList.map { d -> User(d) }.toTypedArray()
        displayPeers(users)
    }

    private fun displayPeers(peerDevices: Array<User>) {
        val namesList = PeerListAdapter(peerDevices, this)
        recyclerView?.adapter = namesList
    }

    fun openChat(user: User) {
        println("Opening chat with ${user.name}")
        //todo attempt connection

        //onSuccess
        MessagesAndUsersDB.users.add(user)
        val chatIntent = Intent(this, ChatActivity::class.java)
        chatIntent.putExtra(EXTRA_USER_ARRAY, user.name)
        startActivity(chatIntent)
    }

}
