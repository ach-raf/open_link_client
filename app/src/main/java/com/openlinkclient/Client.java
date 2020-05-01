package com.openlinkclient;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

public class Client implements Callable<String>{
    private int PORT = 2525;
    private DatagramSocket udpSocket;
    private DatagramPacket packet;
    private byte[] message_byte;
    private String message_text;


    private void initialize_socket() {
        try {
            udpSocket = new DatagramSocket(PORT);
            message_byte = new byte[8000];
            packet = new DatagramPacket(message_byte, message_byte.length);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    public String call() {
        initialize_socket();
        //to be able to use toast (UI element) on a non activity class.
        Looper.prepare();
        boolean run = true;
        while (run) {
            try {
                Log.i("UDP client: ", "about to wait to receive");
                udpSocket.receive(packet);
                message_text = new String(message_byte, 0, packet.getLength());
                Log.d("Received data", message_text);
            } catch (IOException e) {
                Log.e("UDP client has IOException", "error: ", e);
                run = false;
            }
        }
        return message_text;
    }
}
