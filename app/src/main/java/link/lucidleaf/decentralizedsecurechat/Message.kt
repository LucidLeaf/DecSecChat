package link.lucidleaf.decentralizedsecurechat

data class Message(val publicKey: String, val messageSentByMe: Boolean, val body: String)