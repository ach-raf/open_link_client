package com.openlinkclient

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
import com.openlinkclient.UdpListener.TextFromRunnable
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var txtServerStatus: TextView? = null
    private var txtIp: EditText? = null
    private var txtLink: EditText? = null

    private var socket: Socket? = null
    private var host: String? = null
    private val port = 1007

    //to implement: detect if the server is connected or not,
    // also timeout udp listener
    private var connection = false

    // This is the activity main thread Handler.
    private var updateUIHandler: Handler? = null

    private lateinit var pref: SharedPreferences

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) //For night mode theme
        setContentView(R.layout.activity_main)

        pref = getSharedPreferences("preferences", Context.MODE_PRIVATE)


        // Initialize Handler.
        createUpdateUiHandler()
        //
        getServerAddress()

        // Register for broadcasts on BluetoothAdapter state change

        txtServerStatus = findViewById(R.id.txt_server_status)
        // Override onCreateContextMenu to change behavior
        registerForContextMenu(txtServerStatus)

        txtIp = findViewById(R.id.txt_ip)


        txtLink = findViewById(R.id.txt_link)
        //calling the listener of a drawableEnd button to clear a TextInputEditText
        txtLink?.setOnTouchListener(clearTextInputDrawableOnTouchListener())


        // Get link from the share menu
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            sharedTextHandler(intent) // Handle text being sent
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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
                        updateServerStatusValue("Connected")
                        val editor = pref.edit()
                        editor.putString("host", msg.obj.toString())
                        editor.apply()
                    }
                }
            }
        }
    }

    fun getCommand(view: View) {
        val command: String = when (view.id) {
            R.id.volume_reduce_button -> "volume down"
            R.id.volume_increase_button -> "volume up"
            R.id.mute_volume -> "mute"
            R.id.minus_10_seconds -> "minus_10"
            R.id.plus_10_seconds -> "plus_10"
            R.id.minus_20_seconds -> "minus_20"
            R.id.plus_20_seconds -> "plus_20"
            R.id.play_pause_button -> "play_pause"
            R.id.subtitle_button -> "s"
            R.id.subtitle_search -> "d"
            R.id.audio_button -> "a"
            R.id.stop_button -> "stop"
            R.id.next_chapter_button -> "next_chapter"
            R.id.previous_chapter_button -> "previous_chapter"
            R.id.skip_forward_button -> "skip_forward"
            R.id.skip_back_button -> "skip_back"
            R.id.btn_turn_off_screen -> "turn_off_screen"
            R.id.skip_anime_intro -> "skip_anime_intro"
            R.id.stretch_wide -> "16_9"
            R.id.sub_delay_minus -> "sub_delay_minus"
            R.id.sub_delay_plus -> "sub_delay_plus"
            R.id.esc -> "esc"
            R.id.history -> "history"
            R.id.enter -> "enter"
            R.id.shaders -> "shaders"
            R.id.overlay -> "overlay"
            R.id.subtitle_style -> "subtitle_style"

            else -> throw IllegalStateException("Unexpected value: " + view.id)
        }
        sendCommand(command)

    }


    fun sendCommand(command: String) {
        host = txtIp?.text.toString()
        openLink(host, command)
    }

    private fun sharedTextHandler(intent: Intent) {
        host = pref.getString("host", "192.168.11.111") //txtIp?.text.toString()
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
        // Update UI to reflect text being shared
        //txtServerStatus!!.text = sharedText
        //txtLink!!.setText(sharedText)
        Toast.makeText(this, host, Toast.LENGTH_SHORT).show()
        openLink(host, sharedText)

        // there is an error when I try to change the value of any edit text
        updateLinkValue(sharedText)
    }

    private fun updateLinkValue(_link: String?) {
        txtLink!!.setText(_link)
    }


    fun updateIpValue(ipAddress: String?) {
        txtIp!!.setText(ipAddress)
    }

    fun updateServerStatusValue(message: String?) {
        txtServerStatus!!.text = message
    }

    @SuppressLint("SetTextI18n")
    @Throws(InterruptedException::class)
    fun getServerAddress() {
        val handler = Handler()
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
        //handler.post(udpListener)
        val udpClient = Thread(udpListener)
        udpClient.start()
    }


    fun openExternalLink(v: View) {
        host = txtIp!!.text.toString()
        val url = txtLink!!.text.toString()
        txtServerStatus!!.text = url
        openLink(host, url)
    }


    private fun openLink(host: String?, command: String) {
        val tcpRunnable = TcpServer(host, command)
        val tcpServer = Thread(tcpRunnable)
        tcpServer.start()
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View,
                                     menuInfo: ContextMenu.ContextMenuInfo) {
        // user has long pressed your TextView
        menu.add(0, view.id, 0, "Copy")

        // cast the received View to TextView so that you can get its text
        val yourTextView = view as TextView

        // place your TextView's text in clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData
                .newPlainText("last link", yourTextView.text)
        //clipboard.primaryClip = clipData
    }
    companion object {
        // Message type code.
        private const val MESSAGE_UPDATE_TEXT_CHILD_THREAD = 1
    }

    fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(applicationContext, toast, Toast.LENGTH_SHORT).show() }
    }
}