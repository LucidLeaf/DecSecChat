package link.lucidleaf.decentralizedsecurechat

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
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
import java.net.InetAddress


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

    private var wifiP2pManager: WifiP2pManager? = null
    private var p2pChannel: WifiP2pManager.Channel? = null
    private var p2pBroadcastReceiver: BroadcastReceiver? = null
    private val p2pIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private val peers = mutableListOf<WifiP2pDevice>()
    var requestedDevice: WifiP2pDevice? = null

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
    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        val groupOwnerAddress: InetAddress = info.groupOwnerAddress
        // After the group negotiation, we can determine the group owner
        // (server).
        var connection: Connection? = null
        if (info.groupFormed && info.isGroupOwner) {
            connection = Server()
            connection.start()
            //wait for server to come online
            while (connection.getSocket() == null)
                Thread.sleep(10)
        } else if (info.groupFormed) {
            connection = Client(groupOwnerAddress)
            connection.start()
        }
        connection?.let { openChat(it) }
    }

    enum class Icons {
        WIFI, LOCATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Encryption.loadKeys(applicationContext)
        DataBase.readDB(applicationContext)
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
        txtToolTip?.text = if (peers.isEmpty()) {
            getString(R.string.pull_to_discover_tooltip)
        } else ""
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
        p2pChannel = wifiP2pManager?.initialize(this, mainLooper, null)
        p2pChannel?.also { channel ->
            p2pBroadcastReceiver = wifiP2pManager?.let { P2pBroadcastReceiver(it, channel, this) }
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
        wifiP2pManager?.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener {
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

    @SuppressLint("MissingPermission")
    fun requestConnection(otherDevice: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = otherDevice.deviceAddress
        }
        p2pChannel?.also { channel ->
            wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    requestedDevice = otherDevice
                    Toast.makeText(this@MainActivity, "connection requested", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(this@MainActivity, "connection refused", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }

    private fun openChat(connection: Connection) {
        val chatIntent = Intent(this@MainActivity, ChatActivity::class.java)
        Box.add(chatIntent, CONNECTION, connection)
        Box.add(chatIntent, MAIN_ACTIVITY, this)
        startActivity(chatIntent)
    }

    //cancel all actions
    override fun onBackPressed() {
        pullDiscover?.isRefreshing = false
        closeP2pConnection()
        cancelConnection()
        cancelDiscovery()
    }

    private fun cancelConnection() {
        wifiP2pManager?.cancelConnect(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = Unit

            override fun onFailure(reason: Int) = Unit
        })
    }

    private fun cancelDiscovery() {
        wifiP2pManager?.stopPeerDiscovery(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = Unit

            override fun onFailure(reason: Int) = Unit
        })
    }

    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        p2pBroadcastReceiver?.also { receiver ->
            registerReceiver(receiver, p2pIntentFilter)
        }
        discoverPeers()
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        p2pBroadcastReceiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        updateIcon(Icons.LOCATION)
        updateIcon(Icons.WIFI)
        return super.onCreateOptionsMenu(menu)
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
                    locationPermission = true
                }
            } else {
                locationPermission = false
            }
            updateIcon(Icons.LOCATION)
            return
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuWifi -> {
                if (!wifiEnabled || !wifiP2pActive)
                    Toast.makeText(this, "enable Wifi for usage", Toast.LENGTH_SHORT)
                        .show()
                else Toast.makeText(this, "no action required", Toast.LENGTH_SHORT)
                    .show()
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
            }
            else -> super.onOptionsItemSelected(item)
        }
        updateIcon(Icons.LOCATION)
        updateIcon(Icons.WIFI)
        return true
    }

    @SuppressLint("MissingPermission")
    fun closeP2pConnection() {
        if (wifiP2pManager != null && p2pChannel != null) {
            wifiP2pManager!!.requestGroupInfo(p2pChannel) {
                wifiP2pManager!!.removeGroup(
                    p2pChannel,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "removeGroup onSuccess -")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d(TAG, "removeGroup onFailure -$reason")
                        }
                    })
            }
        }
    }

    override fun onDestroy() {
        DataBase.dumpDB(applicationContext)
        super.onDestroy()
    }
}
