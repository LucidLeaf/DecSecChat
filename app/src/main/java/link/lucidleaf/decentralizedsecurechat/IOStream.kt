package link.lucidleaf.decentralizedsecurechat

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class IOStream(
    socket: Socket,
    private val chat: ChatActivity
) : Thread() {
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    var connected = true

    init {
        try {
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        val buffer = ByteArray(1024) { 0 }
        var bytesRead: Int

        while (connected) {
            if (inputStream != null)
                try {
                    bytesRead = inputStream!!.read(buffer)
                    if (bytesRead > 0) {
                        chat.ioHandler.obtainMessage(MESSAGE_READ, bytesRead, -1, buffer)
                            .sendToTarget()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
        }
    }

    fun write(bytes: ByteArray) {
        if (outputStream != null)
            try {
                outputStream!!.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
    }
}