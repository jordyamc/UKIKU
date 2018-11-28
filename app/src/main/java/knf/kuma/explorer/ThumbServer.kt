package knf.kuma.explorer

import fi.iki.elonen.NanoHTTPD
import knf.kuma.commons.Network
import java.io.File
import java.io.FileInputStream

object ThumbServer {
    private var SERVERINSTANCE: Server? = null

    fun loadFile(file: File): String? {
        return try {
            //stop()
            //SERVERINSTANCE = Server(file)
            setFile(file)
            "http://${Network.ipAddress}:4691"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setFile(file: File) {
        if (SERVERINSTANCE == null)
            SERVERINSTANCE = Server(file)
        else
            SERVERINSTANCE?.loadedFile = file
    }

    fun stop() {
        if (SERVERINSTANCE?.isAlive == true) {
            SERVERINSTANCE?.stop()
            SERVERINSTANCE = null
        }
    }

    private class Server(var loadedFile: File) : NanoHTTPD(4691) {
        init {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        }

        override fun serve(session: IHTTPSession?): Response {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "image/png", FileInputStream(loadedFile), loadedFile.length())
        }
    }


}