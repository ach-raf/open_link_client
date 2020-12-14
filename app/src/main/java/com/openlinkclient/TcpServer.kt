package com.openlinkclient

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

class TcpServer(){
    private val mainActivity: MainActivity = MainActivity()

    fun openLink(context: Context, command: String, host: String?) {
        var socket: Socket? = null
        val port = 1007
        Thread(Runnable {
            try {
                //System.out.println("Your current Hostname : " + host);
                socket = Socket(host, port)
                val printwriter: PrintWriter = PrintWriter(socket!!.getOutputStream(), true)
                printwriter.apply {
                    write(command) // write the message to output stream
                    flush()
                    close()
                }
                Log.d("socket", "connected")


                // Toast in background because Toast cannot be in main thread you have to create runOnuithread.
                // this is run on ui thread where dialogs and all other GUI will run.
                if (socket!!.isConnected) {
                    //Toast.makeText(context, "Command sent", Toast.LENGTH_SHORT).show()
                    mainActivity.showToast("Unknown host please make sure IP address")
                }
            } catch (e2: UnknownHostException) {
                //Toast.makeText(context, "Unknown host please make sure IP address", Toast.LENGTH_SHORT).show() /mainActivity.showToast("Unknown host please make sure IP address")

            } catch (e1: IOException) {
                Log.d("socket", "IOException")
                mainActivity.showToast("Unknown host please make sure IP address")
                /*Toast.makeText(context, "Error Occurred ", Toast.LENGTH_SHORT).show()
                context.runOnUiThread { //Do your UI operations like dialog opening or Toast here
                    Toast.makeText(context, "Error Occurred ", Toast.LENGTH_SHORT).show()
                }*/
            }
        }).start()
    }

}
