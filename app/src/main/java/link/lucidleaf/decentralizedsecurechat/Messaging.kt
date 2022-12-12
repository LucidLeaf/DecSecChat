package link.lucidleaf.decentralizedsecurechat

import android.net.wifi.p2p.WifiP2pDevice
import java.util.*

class Message(val user: User, val body: String, val createdAt: Date) {

    fun thisUser(): Boolean {
        return false
    }

}

class User(val device: WifiP2pDevice) {

    val name: String = device.deviceName
    var nickName: String? = null

}

class UserDB
