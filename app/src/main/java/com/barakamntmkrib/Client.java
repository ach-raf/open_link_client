package com.barakamntmkrib;

import android.os.AsyncTask;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client extends AsyncTask<Void, Void, Void> {
    private String host;
    private int port;


    Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected Void doInBackground(Void... arg0) {


        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        //textResponse.setText(response);
        super.onPostExecute(result);
    }


}
