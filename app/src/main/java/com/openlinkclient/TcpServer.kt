package com.openlinkclient

import android.util.Log
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

class TcpServer(private val host: String?, private val command: String) : Runnable {
    private val port = 1007
    private var socket: Socket? = null


    override fun run() {
        try {
            socket = Socket(host, port)
            val printwriter = PrintWriter(socket!!.getOutputStream(), true)
            printwriter.write(command) // write the message to output stream
            printwriter.flush()
            printwriter.close()
            Log.d("socket", "connected")
        }
        catch (e2: UnknownHostException) {
            Log.d("socket", e2.toString())
        }
        catch (e2: IOException) {
            Log.d("socket", e2.toString())
        }

    }


}