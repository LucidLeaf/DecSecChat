package link.lucidleaf.decentralizedsecurechat

import android.content.Context
import java.io.File


object DataBase {

    private val messagesHashMap: HashMap<String, MutableList<Message>> = HashMap()
    private val subscribedActivitiesSet: HashSet<ChatActivity> = HashSet()

    fun readDB(context: Context) {
        return
        val dataBaseDirectory = File(context.filesDir, DATABASE_PREFIX)
        val publicKeyDirs = dataBaseDirectory.listFiles()
        if (publicKeyDirs != null)
            for (publicKeyDir in publicKeyDirs) {
                val publicKey = publicKeyDir.name
                val messageFiles = publicKeyDir.listFiles()
                if (messageFiles != null) {
                    val sortedMessageFiles = messageFiles.sortedArray()
                    for (messageFile in sortedMessageFiles) {
                        val fileContents = messageFile.readText()
                        val message = messageFromString(fileContents)
                        addMessage(publicKey, message)
                    }
                }
            }
    }

    private fun messageFromString(string: String): Message {
        val head = string[0]
        val body = string.removePrefix(head.toString())
        val sentByMe = if (head == MESSAGE_SENT.toChar())
            MESSAGE_SENT
        else MESSAGE_RECEIVED
        return Message(sentByMe, body)
    }

    fun dumpDB(context: Context) {
        return
        val databaseDir = File(context.filesDir, DATABASE_PREFIX)
        databaseDir.mkdir()
        for (peerPublicKey in messagesHashMap.keys) {
            val peerPublicKeyDir = File(databaseDir, peerPublicKey)
            peerPublicKeyDir.mkdir()
            val messages = messagesHashMap[peerPublicKey]
            if (messages != null) {
                for (messageIndex in messages.indices) {
                    val messageString = messageToString(messages[messageIndex])
                    val file = File(peerPublicKeyDir, messageIndex.toString())
                    file.createNewFile()
                    file.writeText(messageString)
                }
            }
        }
    }

    private fun messageToString(message: Message): String {
        val sentByMe = if (message.messageType == MESSAGE_SENT)
            MESSAGE_SENT.toString()
        else MESSAGE_RECEIVED.toString()

        return sentByMe + message.body
    }

    fun addMessage(peerPublicKey: String, message: Message) {
        if (!messagesHashMap.containsKey(peerPublicKey))
            messagesHashMap[peerPublicKey] = mutableListOf()
        messagesHashMap[peerPublicKey]?.add(message)
        subscribedActivitiesSet.find { chatActivity -> chatActivity.getPeerPublicKey() == peerPublicKey }
            ?.updateUI()
    }

    fun getMessages(peerPublicKey: String): List<Message> =
        if (messagesHashMap.containsKey(peerPublicKey) && messagesHashMap[peerPublicKey] != null)
            messagesHashMap[peerPublicKey]!!
        else
            emptyList()

    fun subscribeUIUpdates(chatActivity: ChatActivity) {
        subscribedActivitiesSet.add(chatActivity)
    }

    fun unsubscribeUIUpdates(chatActivity: ChatActivity) {
        subscribedActivitiesSet.remove(chatActivity)
    }

}