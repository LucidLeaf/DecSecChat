package link.lucidleaf.decentralizedsecurechat

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.*


class MainActivity : AppCompatActivity() {
    // ux stuff
    private var pullDiscover: SwipeRefreshLayout? = null
    private var txtToolTip: TextView? = null
    var wifiP2pActive = false

    //todo check if wifi is enabled
    var wifiEnabled = true
    var locationPermission = false

    //todo check if location is enabled
    var locationEnabled = true
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null

    private val wifiP2pManager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    private var P2pChannel: WifiP2pManager.Channel? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private val peers = mutableListOf<WifiP2pDevice>()
    @SuppressLint("NotifyDataSetChanged")
    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
            recyclerView?.adapter?.notifyDataSetChanged()
            pullDiscover?.isRefreshing = false
            txtToolTip?.text = ""
        }

        if (peers.isEmpty()) {
            Log.d(TAG, "No devices found")
            txtToolTip?.text = getString(R.string.pull_to_discover_tooltip)
            return@PeerListListener
        }
    }

    enum class Icons {
        WIFI, LOCATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeElements()
        requestLocationPermissions()
    }

    private fun initializeElements() {
        setSupportActionBar(findViewById(R.id.toolbar))
        txtToolTip = findViewById(R.id.txtRefreshTip)
        pullDiscover = findViewById(R.id.pullToRefresh)
        pullDiscover?.setOnRefreshListener { discoverPeers() }
        recyclerView = findViewById(R.id.peerList)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = PeerListAdapter(peers, this)
        //show hint if no devices available
        txtToolTip?.text = if (peers.isEmpty()) {
            getString(R.string.pull_to_discover_tooltip)
        } else ""
        P2pChannel = wifiP2pManager?.initialize(this, mainLooper, null)
        P2pChannel?.also { channel ->
            broadcastReceiver = wifiP2pManager?.let { P2pBroadcastReceiver(it, channel, this) }
        }
    }

    private fun requestLocationPermissions() {
        val requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = applicationContext.checkCallingOrSelfPermission(requiredPermission)
        if (granted == PackageManager.PERMISSION_GRANTED) {
            locationPermission = true
        } else {
            locationPermission = false
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST)
        }
        updateIcon(Icons.LOCATION)
    }


    fun updateIcon(icon: Icons) {
        when (icon) {
            Icons.WIFI -> if (wifiP2pActive && wifiEnabled)
                menu?.findItem(R.id.menuWifi)?.setIcon(R.drawable.ic_wifi_on)
            else
                menu?.findItem(R.id.menuWifi)?.setIcon(R.drawable.ic_wifi_off)
            Icons.LOCATION -> if (locationPermission && locationEnabled)
                menu?.findItem(R.id.menuLocation)?.setIcon(R.drawable.ic_location_on)
            else
                menu?.findItem(R.id.menuLocation)?.setIcon(R.drawable.ic_location_off)
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        wifiP2pManager?.discoverPeers(P2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}

            override fun onFailure(reasonCode: Int) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to discover ($reasonCode)",
                    Toast.LENGTH_SHORT
                ).show()
                pullDiscover?.isRefreshing = false
            }
        })
    }

    fun openChat(otherDevice: WifiP2pDevice) {
        // todo provide connection to chat activity
        val config = WifiP2pConfig().apply {
            deviceAddress = otherDevice.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        P2pChannel?.also { channel ->
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission not given", Toast.LENGTH_SHORT).show()
                return
            }
            wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    val chatIntent = Intent(this@MainActivity, ChatActivity::class.java)
                    Box.add(chatIntent, OTHER_DEVICE, otherDevice)
                    startActivity(chatIntent)
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(this@MainActivity, "connection refused", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }

    //cancel all actions
    override fun onBackPressed() {
        pullDiscover?.isRefreshing = false
        wifiP2pManager?.cancelConnect(P2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = Unit

            override fun onFailure(reason: Int) = Unit
        })
        wifiP2pManager?.stopPeerDiscovery(P2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = Unit

            override fun onFailure(reason: Int) = Unit
        })
    }

    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        broadcastReceiver?.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
        discoverPeers()
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        broadcastReceiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        title = CURRENT_USER
        updateIcon(Icons.LOCATION)
        updateIcon(Icons.WIFI)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        DataBase.dumpDB()
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
                }
            } else {
                println("Location Permission Denied")
                locationPermission = false
            }
            return
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menuWifi -> {
                if (!wifiEnabled || !wifiP2pActive)
                    Toast.makeText(this, "enable Wifi for usage", Toast.LENGTH_SHORT)
                        .show()
                else Toast.makeText(this, "no action required", Toast.LENGTH_SHORT)
                    .show()
                true
            }
            R.id.menuLocation -> {
                if (!locationEnabled || !locationPermission)
                    Toast.makeText(
                        this,
                        "enable location and provide permission",
                        Toast.LENGTH_SHORT
                    ).show()
                else Toast.makeText(this, "no action required", Toast.LENGTH_SHORT)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

}
