package link.lucidleaf.decentralizedsecurechat

import android.net.wifi.p2p.WifiP2pDevice
import java.util.*

data class Message(val sender: User, val receiver: User, val body: String, val createdAt: Date)

class User(val device: WifiP2pDevice) {

    val name: String = device.deviceName
    var nickName: String = name

    companion object {
        fun getCurrentUser(): User {
            val user = User(WifiP2pDevice())
            user.device.deviceName = android.os.Build.MODEL
            user.nickName = "You 🐼"
            return user
        }

        fun getTemplateUser(): User {
            val device = WifiP2pDevice()
            device.deviceName = "Test User 0"
            return User(device)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        if (other.javaClass != this.javaClass)
            return false
        val otherUser = other as User
        //todo always keep all members checked
        if (otherUser.device != this.device)
            return false
        if (otherUser.name != this.name)
            return false
        if (otherUser.nickName != this.nickName)
            return false
        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + nickName.hashCode()
        return result
    }

}

object MessagesAndUsersDB {

    //todo implmement saving known user and messages
    val users: HashSet<User> = HashSet()
    private val messages: HashMap<User, MutableList<Message>> = HashMap<User, MutableList<Message>>()
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
        val other = if (message.sender == User.getCurrentUser())
            message.receiver
        else message.sender
        if (!messages.containsKey(other))
            messages[other] = mutableListOf()
        messages[other]?.add(message)
        subscribedActivities.find { chatActivity -> chatActivity.otherUser == other }?.updateMessages()
    }

    fun getMessages(other: User): List<Message>? {
        return if (messages.containsKey(other))
            messages[other]
        else {
            return List(10) { i ->
                if (i < 5) {
                    Message(
                        User.getCurrentUser(),
                        User.getTemplateUser(),
                        "This is a test message that shouldn't appear #$i",
                        Calendar.getInstance().time
                    )
                } else {
                    Message(
                        User.getTemplateUser(),
                        User.getCurrentUser(),
                        "This is a test message that shouldn't appear #$i",
                        Calendar.getInstance().time
                    )
                }
            }
        }
    }

    fun getUserByName(otherName: String): User? {
        return users.find { user -> user.name == otherName }
    }

    fun subscribeToUpdates(chatActivity: ChatActivity) {
        subscribedActivities.add(chatActivity)
    }

    fun unsubscribeUpdates(chatActivity: ChatActivity) {
        subscribedActivities.remove(chatActivity)
    }

}

