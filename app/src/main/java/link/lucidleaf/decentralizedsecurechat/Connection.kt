package link.lucidleaf.decentralizedsecurechat

import java.net.Socket

interface Connection {
    fun closeConnection()
    fun getSocket(): Socket?
}