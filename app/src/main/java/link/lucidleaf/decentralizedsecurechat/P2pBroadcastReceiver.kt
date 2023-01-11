package link.lucidleaf.decentralizedsecurechat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager

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
                        // Wifi P2P is enabled
                        activity.wifiP2pActive = true
                        activity.updateIcon(MainActivity.Icons.WIFI)
                    }
                    else -> {
                        // Wi-Fi P2P is not enabled
                        activity.wifiP2pActive = false
                        activity.updateIcon(MainActivity.Icons.WIFI)
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel, activity.peerListListener)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                // Android 10 or higher (non-sticky): can use requestConnectionInfo(),
                //   requestNetworkInfo(), or requestGroupInfo() to retrieve the current connection information.
                println("P2PBroadCastReceiver: Connection changed")
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
                // Android 10 or higher (non-sticky): Applications can use requestDeviceInfo() to
                //  retrieve the current connection information.
                println("P2PBroadCastReceiver: This device changed")
            }
        }
    }
}
