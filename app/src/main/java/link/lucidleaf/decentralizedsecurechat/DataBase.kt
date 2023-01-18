package link.lucidleaf.decentralizedsecurechat


object DataBase {

    //todo implement saving known user and messages
    val knownKeys: HashSet<String> = HashSet()
    private val messages: HashMap<String, MutableList<Message>> = HashMap()
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

    fun addMessage(otherKey: String, message: Message) {
        if (!messages.containsKey(otherKey))
            messages[otherKey] = mutableListOf()
        messages[otherKey]?.add(message)
        subscribedActivities.find { chatActivity -> chatActivity.getPeerPublicKey() == otherKey }
            ?.updateUI()
    }

    fun getMessages(otherKey: String): List<Message> =
        if (messages.containsKey(otherKey) && messages[otherKey] != null)
            messages[otherKey]!!
        else
             emptyList()

    fun subscribeUIUpdates(chatActivity: ChatActivity) {
        subscribedActivities.add(chatActivity)
    }

    fun unsubscribeUIUpdates(chatActivity: ChatActivity) {
        subscribedActivities.remove(chatActivity)
    }

}