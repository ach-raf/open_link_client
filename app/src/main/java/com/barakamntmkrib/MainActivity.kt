package com.barakamntmkrib

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.ContextMenu
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.barakamntmkrib.UdpListener.TextFromRunnable
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {
    private var txtServerStatus: TextView? = null
    private var txtIp: EditText? = null
    private var txtLink: EditText? = null
    private var switchBluetooth: Switch? = null

    private var socket: Socket? = null
    private var host: String? = null
    private val port = 1007
    //to implement: detect if the server is connected or not,
    // also timeout udp listener
    private var connection = false

    // This is the activity main thread Handler.
    private var updateUIHandler: Handler? = null
    private val bluetoothOn = "bluetooth_on"
    private val bluetoothOff = "bluetooth_off"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) //For night mode theme
        setContentView(R.layout.activity_main)

        // Initialize Handler.
        createUpdateUiHandler()
        //
        getServerAddress()

        // Get link from the share menu
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            sharedTextHandler(intent) // Handle text being sent
        }

        // Register for broadcasts on BluetoothAdapter state change
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mReceiver, filter)

        //
        txtServerStatus = findViewById(R.id.txt_server_status)
        // Override onCreateContextMenu to change behavior
        registerForContextMenu(txtServerStatus)

        txtIp = findViewById(R.id.txt_ip)
        switchBluetooth = findViewById(R.id.bluetooth_switch)

        txtLink = findViewById(R.id.txt_link)
        //calling the listener of a drawableEnd button to clear a TextInputEditText
        txtLink?.setOnTouchListener(clearTextInputDrawableOnTouchListener())



        switchBluetooth?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled
                sendCommand(bluetoothOn)
            } else {
                // The toggle is disabled
                sendCommand(bluetoothOff)
            }
        }
    }

    private fun clearTextInputDrawableOnTouchListener(): OnTouchListener? {
        //The listener of a drawableEnd button to clear a TextInputEditText
        return OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val textView = v as EditText
                if (event.x >= textView.width - textView.compoundPaddingEnd) {
                    textView.setText("") //Clear a view, example: EditText or TextView
                    textView.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT)
                    return@OnTouchListener true
                }
            }
            false
        }
    }

    @SuppressLint("HandlerLeak")
    private fun createUpdateUiHandler() {
        if (updateUIHandler == null) {
            updateUIHandler = object : Handler() {
                override fun handleMessage(msg: Message) {
                    // Means the message is sent from child thread.
                    if (msg.what == MESSAGE_UPDATE_TEXT_CHILD_THREAD) {
                        // Update ui in main thread.
                        updateIpValue(msg.obj.toString())
                    }
                }
            }
        }
    }

    fun getCommand(view: View) {
        val command: String = when (view.id) {
            R.id.volume_reduce_button -> "volume down"
            R.id.volume_increase_button -> "volume up"
            R.id.rewind_button -> "left arrow"
            R.id.play_pause_button -> "space"
            R.id.forward_button -> "right arrow"
            R.id.subtitle_button -> "s"
            R.id.audio_button -> "a"
            R.id.stop_button -> ";"
            R.id.next_chapter_button -> "page down"
            R.id.previous_chapter_button -> "page up"
            R.id.skip_forward_button -> "ctrl+page down"
            R.id.skip_back_button -> "ctrl+page up"
            R.id.btn_turn_off_screen -> "turn_off_screen"
            else -> throw IllegalStateException("Unexpected value: " + view.id)
        }
        sendCommand(command)
    }

    fun sendCommand(command: String) {
        host = txtIp?.text.toString()
        openLink(command, host)
    }

    private fun sharedTextHandler(intent: Intent) {
        host = txtIp!!.text.toString()
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            // Update UI to reflect text being shared
            txtServerStatus!!.text = sharedText
            openLink(sharedText, host)
        } else {
            val error = "Error getting the link"
            txtServerStatus!!.text = error
        }
    }

    fun updateIpValue(ipAddress: String?) {
        txtIp!!.setText(ipAddress)
    }

    @Throws(InterruptedException::class)
    fun getServerAddress() {
        // udp
        val udpListener = UdpListener(object : TextFromRunnable {
            override fun sendString(text: String?) {
                // Build message object.
                val message = Message()
                // Set message type.
                message.what = MESSAGE_UPDATE_TEXT_CHILD_THREAD
                message.obj = text
                // Send message to main thread Handler.
                updateUIHandler!!.sendMessage(message)
            }
        })
        val udpClient = Thread(udpListener)
        udpClient.start()
        udpClient.join()
    }


    fun openExternalLink(v: View) {
        host = txtIp!!.text.toString()
        val url = txtLink!!.text.toString()
        txtServerStatus!!.text = url
        openLink(url, host)
    }



    private fun openLink(command: String, host: String?) {
        Thread(Runnable {
            try {
                var printwriter: PrintWriter? = null
                //System.out.println("Your current Hostname : " + host);
                socket = Socket(host, port)
                printwriter = PrintWriter(socket!!.getOutputStream(), true)
                printwriter.write(command) // write the message to output stream
                printwriter.flush()
                printwriter.close()
                Log.d("socket", "connected")


                // Toast in background because Toast cannot be in main thread you have to create runOnuithread.
                // this is run on ui thread where dialogs and all other GUI will run.
                if (socket!!.isConnected) {
                    runOnUiThread { Toast.makeText(applicationContext, "Command sent", Toast.LENGTH_SHORT).show() }
                }
            } catch (e2: UnknownHostException) {
                runOnUiThread { //Do your UI operations like dialog opening or Toast here
                    Toast.makeText(applicationContext, "Unknown host please make sure IP address", Toast.LENGTH_SHORT).show()
                }
            } catch (e1: IOException) {
                Log.d("socket", "IOException")
                runOnUiThread { //Do your UI operations like dialog opening or Toast here
                    Toast.makeText(applicationContext, "Error Occurred ", Toast.LENGTH_SHORT).show()
                }
            }
        }).start()
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action!!
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> bluetoothStateChange("STATE_OFF")
                    BluetoothAdapter.STATE_TURNING_OFF -> bluetoothStateChange("STATE_TURNING_OFF")
                    BluetoothAdapter.STATE_ON -> bluetoothStateChange("STATE_ON")
                    BluetoothAdapter.STATE_TURNING_ON -> bluetoothStateChange("STATE_TURNING_ON")
                }
            }
        }

        private fun bluetoothStateChange(bluetooth_state: String) {
            if (bluetooth_state == "STATE_TURNING_OFF") {
                // when the state is off turn bluetooth on on computer
                sendCommand(bluetoothOn)

            } else if (bluetooth_state == "STATE_TURNING_ON") {
                // to disconnect the bluetooth on the computer
                sendCommand(bluetoothOff)

            }
        }
    }
    override fun onCreateContextMenu(menu: ContextMenu, view: View,
                                     menuInfo: ContextMenu.ContextMenuInfo) {
        // user has long pressed your TextView
        menu.add(0, view.id, 0,
                "Copy")

        // cast the received View to TextView so that you can get its text
        val yourTextView = view as TextView

        // place your TextView's text in clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData
                .newPlainText("last link", yourTextView.text)
        clipboard.primaryClip = clipData
    }

    public override fun onDestroy() {
        super.onDestroy()

        /* ... */

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver)
    }

    companion object {
        // Message type code.
        private const val MESSAGE_UPDATE_TEXT_CHILD_THREAD = 1
    }
}