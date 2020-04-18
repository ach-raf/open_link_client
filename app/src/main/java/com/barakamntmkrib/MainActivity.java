package com.barakamntmkrib;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.net.*;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private TextView txt_url;
    private EditText txt_ip;
    private EditText txt_port;
    private EditText txt_link;


    private String host;
    private int port;


    private Socket socket;
    private PrintWriter printwriter;
    private boolean connection;

    public static final String CONNECTION_PREFS = "connection";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); //For night mode theme
        setContentView(R.layout.activity_main);


        txt_url = findViewById(R.id.txt_url);
        registerForContextMenu(txt_url);
        txt_ip = findViewById(R.id.txt_ip);
        txt_port = findViewById(R.id.txt_port);
        txt_link = findViewById(R.id.txt_link);
        //The listener of a drawableEnd button for clear a TextInputEditText
        txt_link.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    final EditText textView = (EditText)v;
                    if(event.getX() >= textView.getWidth() - textView.getCompoundPaddingEnd()) {
                        textView.setText(""); //Clear a view, example: EditText or TextView
                        textView.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT);
                        return true;
                    }
                }
                return false;
            }
        });
        pref = getSharedPreferences(CONNECTION_PREFS, MODE_PRIVATE);
        if (pref.contains("ip_address") && pref.contains("port_number")) {
            load_preferences();
        }

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            handleSendText(intent); // Handle text being sent
            /*if ("text/plain".equals(type)) {

            }*/
        }


    }
    public void send_command(View view){
        String command;
        switch (view.getId()) {
            case (R.id.volume_reduce_button):
                command = "volume down";
                break;
            case (R.id.volume_increase_button):
                command = "volume up";
                break;
            case (R.id.rewind_button):
                command = "left arrow";
                break;
            case (R.id.play_pause_button):
                command = "space";
                break;
            case (R.id.forward_button):
                command = "right arrow";
                break;
            case (R.id.subtitle_button):
                command = "s";
                break;
            case (R.id.stop_button):
                command = ";";
                break;
            case (R.id.next_chapter_button):
                command = "page down";
                break;
            case (R.id.previous_chapter_button):
                command = "page up";
                break;
            case (R.id.skip_forward_button):
                command = "ctrl+page down";
                break;
            case (R.id.skip_back_button):
                command = "ctrl+page up";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }
        host = txt_ip.getText().toString();
        port = Integer.parseInt(txt_port.getText().toString());
        open_link(command, host, port);
    }

    void handleSendText(Intent intent) {
        host = txt_ip.getText().toString();
        port = Integer.parseInt(txt_port.getText().toString());
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            txt_url.setText(sharedText);
            open_link(sharedText, host, port);
        } else {
            String error = "Error getting the link";
            txt_url.setText(error);
        }
    }

    public String get_ip_address(){
        StringBuilder ip_address = new StringBuilder();
        try {
            for (Enumeration<NetworkInterface> network_enum = NetworkInterface.getNetworkInterfaces(); network_enum.hasMoreElements();) {
                NetworkInterface network_interface = network_enum.nextElement();
                for (Enumeration<InetAddress> ip_address_enum = network_interface.getInetAddresses(); ip_address_enum.hasMoreElements();) {
                    InetAddress inetAddress = ip_address_enum.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        ip_address.append(inetAddress.getHostAddress().toString()).append("\n");
                        Toast.makeText(getApplicationContext(), ip_address.toString(), Toast.LENGTH_SHORT).show();
                    }

                }
            }
        } catch (SocketException ex) {
            Log.e("LOG_TAG", ex.toString());
        }
        return ip_address.toString();
    }

    public void save_preferences(View view) {
        /*editor = getSharedPreferences(CONNECTION_PREFS, MODE_PRIVATE).edit();
        editor.putString("ip_address", txt_ip.getText().toString());
        editor.putInt("port_number", Integer.parseInt(txt_port.getText().toString()));
        editor.apply();*/
        UdpListener udp_listener = new UdpListener(this);
        Thread udp_client = new Thread(udp_listener);
        udp_client.start();
        // txt_ip.setText(get_ip_address());
    }

    public void load_preferences() {
        String ip = pref.getString("ip_address", "192.168.1.106");//"No ip defined" is the default value.
        int port = pref.getInt("port_number", 1007); //1007 is the default port.
        txt_ip.setText(ip);
        txt_port.setText(String.valueOf(port));
    }

    public void open_external_link(View view) {
        host = txt_ip.getText().toString();
        port = Integer.parseInt(txt_port.getText().toString());
        String url = txt_link.getText().toString();
        txt_url.setText(url);
        open_link(url, host, port);
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        // user has long pressed your TextView
        menu.add(0, v.getId(), 0,
                "Copy");

        // cast the received View to TextView so that you can get its text
        TextView yourTextView = (TextView) v;

        // place your TextView's text in clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData clipData = android.content.ClipData
                .newPlainText("last link", yourTextView.getText());
        clipboard.setPrimaryClip(clipData);
    }

    public void open_link(final String command, final String host, final int port) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    //System.out.println("Your current Hostname : " + host);
                    socket = new Socket(host, port);


                    printwriter = new PrintWriter(socket.getOutputStream(), true);
                    printwriter.write(command); // write the message to output stream
                    printwriter.flush();
                    printwriter.close();

                    Log.d("socket", "connected");


                    // Toast in background because Toast cannot be in main thread you have to create runOnuithread.
                    // this is run on ui thread where dialogs and all other GUI will run.
                    if (socket.isConnected()) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Command sent", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (UnknownHostException e2) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            //Do your UI operations like dialog opening or Toast here
                            Toast.makeText(getApplicationContext(), "Unknown host please make sure IP address", Toast.LENGTH_SHORT).show();

                        }
                    });

                } catch (IOException e1) {
                    Log.d("socket", "IOException");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            //Do your UI operations like dialog opening or Toast here
                            Toast.makeText(getApplicationContext(), "Error Occurred ", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
        }).start();
    }


}



