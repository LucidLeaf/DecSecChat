package link.lucidleaf.decentralizedsecurechat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import java.net.InetAddress

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
class P2pBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity
) : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                when (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)) {
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                        activity.wifiP2pActive = true
                    }
                    else -> {
                        activity.wifiP2pActive = false
                    }
                }
                activity.updateIcon(MainActivity.Icons.WIFI)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel, activity.peerListListener)
            }
            // Respond to new connection or disconnections
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo: NetworkInfo? = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)!!
                if (networkInfo?.isConnected == true) {
                    // We are connected with the other device, request connection
                    // info to find group owner IP
                    manager.requestConnectionInfo(channel, activity.connectionInfoListener)
                } else print("not connected")
            }
            // Respond to this device's wifi state changing
            // Android 10 or higher (non-sticky): Applications can use requestDeviceInfo() to
            //  retrieve the current connection information.
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
            }
        }
    }
}
