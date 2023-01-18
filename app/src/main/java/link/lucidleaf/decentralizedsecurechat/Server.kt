package link.lucidleaf.decentralizedsecurechat

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class Server : Thread(), Connection {
    private var socket: Socket? = null
    var serverSocket: ServerSocket? = null

    override fun run() {
        try{
            serverSocket = ServerSocket(SERVER_SOCKET)
            socket = serverSocket?.accept()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun closeConnection() {
        socket?.close()
        serverSocket?.close()
    }

    override fun getSocket() = socket

}