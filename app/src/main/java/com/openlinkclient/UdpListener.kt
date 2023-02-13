package com.openlinkclient

import android.annotation.SuppressLint
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class UdpListener(private val sendDataFromRunnable: TextFromRunnable) : Runnable {
    private val port = 2525
    private var udpSocket: DatagramSocket? = null

    private var packet: DatagramPacket? = null
    private lateinit var messageByte: ByteArray

    @SuppressLint("LongLogTag")
    override fun run() {
        udpSocket = DatagramSocket(null)
        udpSocket!!.reuseAddress = true;
        udpSocket!!.bind( InetSocketAddress(port))
        messageByte = ByteArray(8000)
        packet = DatagramPacket(messageByte, messageByte.size)
        Log.i("UDP client: ", "about to wait to receive")
        udpSocket!!.receive(packet)
        val ipAddress = String(messageByte, 0, packet!!.length)
        sendDataFromRunnable.sendString(ipAddress)
        Log.d("Received data", ipAddress)
    }

    @FunctionalInterface
    interface TextFromRunnable {
        fun sendString(text: String?)
    }

}