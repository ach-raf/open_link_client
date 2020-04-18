package com.barakamntmkrib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpListener implements Runnable {
    private Context context;

    public UdpListener(Context context) {
        this.context = context;
    }
    private int PORT = 2525;
    private DatagramSocket udpSocket;
    private DatagramPacket packet;
    private byte[] message_byte;
    private String message_text;


    private void initialize_socket(){
        try {
            udpSocket = new DatagramSocket(PORT);
            message_byte = new byte[8000];
            packet = new DatagramPacket(message_byte, message_byte.length);
        }catch (SocketException e){
            e.printStackTrace();
        }
    }
    @SuppressLint("LongLogTag")
    @Override
    public void run() {
        initialize_socket();
        //to be able to use toast (UI element) on a non activity class.
        Looper.prepare();
        boolean run = true;
        while (run) {
            try {
                Log.i("UDP client: ", "about to wait to receive");
                udpSocket.receive(packet);
                message_text = new String(message_byte, 0, packet.getLength());
                Toast.makeText(context, message_text, Toast.LENGTH_SHORT).show();
                Log.d("Received data", message_text);
            } catch (IOException e) {
                Log.e("UDP client has IOException", "error: ", e);
                run = false;
            }
        }
    }

}
