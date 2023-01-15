package link.lucidleaf.decentralizedsecurechat

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class Server : Thread() {
    var socket: Socket? = null
    var serverSocket: ServerSocket? = null

    override fun run() {
        try{
            serverSocket = ServerSocket(SERVER_SOCKET)
            socket = serverSocket?.accept()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}