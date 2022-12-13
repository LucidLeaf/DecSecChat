package link.lucidleaf.decentralizedsecurechat

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

private const val LOCATION_REQUEST = 1

const val EXTRA_USER_ARRAY = "peerName"

//most functionality here is based on https://developer.android.com/guide/topics/connectivity/wifip2p#create-application
class MainActivity : AppCompatActivity() {
    // ux stuff
    private var pullDiscover: SwipeRefreshLayout? = null
    private var txtToolTip: TextView? = null
    var wifiEnabled = false
    var locationPermission = false
    var locationEnabled = false
    private var recyclerView: RecyclerView? = null
    private var thisUser: User? = null
    private var menu: Menu? = null

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

        initializeElements()
        requestLocationPermissions()
        enableLocation()
        enableWifi()
    }

    private fun initializeElements() {
        setSupportActionBar(findViewById(R.id.toolbar))
        thisUser = User.getCurrentUser()
        txtToolTip = findViewById(R.id.txtRefreshTip)
        pullDiscover = findViewById(R.id.pullToRefresh)
        pullDiscover?.setOnRefreshListener {
            discoverPeers()
//            displayPeers(arrayOf(User.getTemplateUser()))
        }
        recyclerView = findViewById(R.id.peerList)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        displayPeers(emptyArray())
        // enable receiving p2p events
        channel = manager?.initialize(this, mainLooper, null)
        channel?.also { channel ->
            receiver = manager?.let { P2pBroadcastReceiver(it, channel, this) }
        }
    }

    private fun requestLocationPermissions() {
//        if (ContextCompat.checkSelfPermission(
//                this, Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            locationEnabled = false
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST)
//        }
//        else locationEnabled = true
    }

    private fun enableWifi() {
        println("Enabling Wifi")
        Toast.makeText(this, "Enabling Wifi", Toast.LENGTH_SHORT).show()
        //todo implement enabling wifi
    }

    private fun enableLocation() {
        println("Enabling Location")
        Toast.makeText(this, "Enabling Location", Toast.LENGTH_SHORT).show()
        requestLocationPermissions()
        //todo implement enabling location
    }

    fun updateIcon(icon: Icons) {
        when (icon) {
            Icons.WIFI -> if (wifiEnabled)
                menu?.findItem(R.id.menuWifi)?.setIcon(R.drawable.ic_wifi_on)
            else
                menu?.findItem(R.id.menuWifi)?.setIcon(R.drawable.ic_wifi_off)
            Icons.LOCATION -> if (locationPermission && locationEnabled)
                menu?.findItem(R.id.menuLocation)?.setIcon(R.drawable.ic_location_on)
            else
                menu?.findItem(R.id.menuLocation)?.setIcon(R.drawable.ic_location_off)
        }
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
                pullDiscover?.isRefreshing = false
            }

            override fun onFailure(reasonCode: Int) {
                println("Failed discovering peers: $reasonCode")
                Toast.makeText(
                    this@MainActivity,
                    "Failed: $reasonCode, permissions insufficient",
                    Toast.LENGTH_SHORT
                ).show()
                pullDiscover?.isRefreshing = false
            }
        })
    }

    fun handlePeerListChange(peers: WifiP2pDeviceList) {
        val users: Array<User> = peers.deviceList.map { d -> User(d) }.toTypedArray()
        displayPeers(users)
    }

    private fun displayPeers(peerDevices: Array<User>) {
        val namesList = PeerListAdapter(peerDevices, this)
        recyclerView?.adapter = namesList
        txtToolTip?.text = if (peerDevices.isEmpty()) {
            getString(R.string.pull_to_discover_tooltip)
        } else ""
    }

    fun openChat(user: User) {
        println("Connecting to chat with ${user.nickName}")
        val device: WifiP2pDevice = user.device
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        channel?.also { channel ->
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission not given", Toast.LENGTH_SHORT).show()
                return
            }
            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    MessagesAndUsersDB.users.add(user)
                    val chatIntent = Intent(this@MainActivity, ChatActivity::class.java)
                    chatIntent.putExtra(EXTRA_USER_ARRAY, user.name)
                    startActivity(chatIntent)
                }

                override fun onFailure(reason: Int) {
                    //failure logic
                    Toast.makeText(
                        this@MainActivity,
                        "Unable to connect to ${user.nickName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        title = thisUser?.nickName
        updateIcon(Icons.LOCATION)
        updateIcon(Icons.WIFI)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        MessagesAndUsersDB.dumpDB()
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
                    locationPermission = true
                    updateIcon(Icons.LOCATION)
                }
            } else {
                println("Location Permission Denied")
                locationPermission = false
                updateIcon(Icons.LOCATION)
            }
            return
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menuWifi -> {
                enableWifi()
                true
            }
            R.id.menuLocation -> {
                enableLocation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
