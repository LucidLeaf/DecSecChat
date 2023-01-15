package link.lucidleaf.decentralizedsecurechat;

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class Client(
    hostIPAddress: InetAddress
) : Thread() {
    var socket: Socket = Socket()
    private val hostIP: String? = hostIPAddress.hostAddress

    override fun run() {
        try {
            socket.connect(InetSocketAddress(hostIP, SERVER_SOCKET), 500)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
