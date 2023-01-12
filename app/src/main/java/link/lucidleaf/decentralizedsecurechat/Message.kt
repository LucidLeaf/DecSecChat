package link.lucidleaf.decentralizedsecurechat

import android.net.wifi.p2p.WifiP2pDevice

data class Message(val otherDevice: WifiP2pDevice, val messageSentByMe: Boolean, val body: String)