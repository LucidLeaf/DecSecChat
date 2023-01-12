package link.lucidleaf.decentralizedsecurechat

import android.net.wifi.p2p.WifiP2pDevice
import java.util.*

object DataBase {

    //todo implmement saving known user and messages
    val knownPeers: HashSet<WifiP2pDevice> = HashSet()
    private val messages: HashMap<WifiP2pDevice, MutableList<Message>> =
        HashMap<WifiP2pDevice, MutableList<Message>>()
    private val subscribedActivities: HashSet<ChatActivity> = HashSet()

    init {
        readDB()
    }

    private fun readDB() {
        //todo implement
    }

    fun dumpDB() {
        //todo write all information to file
        //todo now do all that encrypted
    }

    fun addMessage(message: Message) {
        val otherDevice = message.otherDevice
        if (!messages.containsKey(otherDevice))
            messages[otherDevice] = mutableListOf()
        messages[otherDevice]?.add(message)
        subscribedActivities.find { chatActivity -> chatActivity.otherDevice == otherDevice }
            ?.updateMessages(otherDevice)
    }

    fun getMessages(otherDevice: WifiP2pDevice): List<Message> {
        return if (messages.containsKey(otherDevice) && messages[otherDevice] != null)
            messages[otherDevice]!!
        else {
//            return emptyList()
            return List(10) { i ->
                if (i < 5) {
                    Message(
                        otherDevice,
                        true,
                        "This is a test message that shouldn't appear #$i"
                    )
                } else {
                    Message(
                        otherDevice,
                        false,
                        "This is a test message that shouldn't appear #$i"
                    )
                }
            }
        }
    }

}