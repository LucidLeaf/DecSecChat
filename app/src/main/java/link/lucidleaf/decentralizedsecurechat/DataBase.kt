package link.lucidleaf.decentralizedsecurechat

import java.util.*

object DataBase {

    //todo implmement saving known user and messages
    val knownKeys: HashSet<String> = HashSet()
    private val messages: HashMap<String, MutableList<Message>> =
        HashMap<String, MutableList<Message>>()
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
        val otherKey = message.publicKey
        if (!messages.containsKey(otherKey))
            messages[otherKey] = mutableListOf()
        messages[otherKey]?.add(message)
        subscribedActivities.find { chatActivity -> chatActivity.publicKey == otherKey }
            ?.updateUI()
    }

    fun getMessages(otherKey: String): List<Message> {
        return if (messages.containsKey(otherKey) && messages[otherKey] != null)
            messages[otherKey]!!
        else {
            return emptyList()
//            return List(10) { i ->
//                if (i < 5) Message(otherKey, true, "This is a test message #$i")
//                else Message(otherKey, false, "This is a test message #$i")
//            }
        }
    }

    fun subscribeUIUpdates(chatActivity: ChatActivity) {
        subscribedActivities.add(chatActivity)
    }

    fun unsubscribeUIUpdates(chatActivity: ChatActivity) {
        subscribedActivities.remove(chatActivity)
    }

}