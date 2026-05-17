package com.example.uefcountingapp

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.net.Socket

class SelectTestActivity: AppCompatActivity() {

    lateinit var audioUploadButton: Button
    lateinit var liveTestButton: Button
    lateinit var selectedFileUri: Uri
    lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var ipInputET: EditText
    lateinit var ipAddress: String
    lateinit var participantIDET: EditText
    lateinit var participantID: String
    lateinit var alertText: TextView

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_type)

        // Hide phone status bars
        window.insetsController?.hide(WindowInsets.Type.statusBars())

        // Retrieve alert TextView
        alertText = findViewById(R.id.alert)

        // Retrieve server IP Address EditText
        ipInputET = findViewById(R.id.ipAddress)
        /* For live tests, update IP addresses in network_security_config.xml within res/xml folder */
        /* Python code for Whisper ASR/postprocessing currently runs on local computer server. In future, need update to
        run on cloud */

        // Create a launcher for user to select audio file from files on phone
        filePickerLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
            // Take selected file and send it to backend (Python server)
            if (result!!.resultCode == RESULT_OK && result.data != null) {
                selectedFileUri = result.data!!.data!!
                // Create a Toast popup showing selected file
                Toast.makeText(this, "Selected: " + getFileNameFromUri(selectedFileUri), Toast.LENGTH_LONG).show()
                sendToBackend()
            }
        }

        // Retrieve participant ID EditText
        participantIDET = findViewById(R.id.participantID)

        // Get upload test button
        audioUploadButton = findViewById(R.id.uploadTest)
        // When audioUploadButton is pressed
        audioUploadButton.setOnClickListener {
            getTestInfo()
            // Only move forward if fields are filled
            if (!ipAddress.isEmpty() and !participantID.isEmpty()) {
                openFilePicker()
            }
        }

        // Get live test button
        liveTestButton = findViewById(R.id.liveTest)
        // When liveTestButton is pressed
        liveTestButton.setOnClickListener {
            getTestInfo()
            // Only move forward if fields are filled
            if (!ipAddress.isEmpty() and !participantID.isEmpty()) {
                // Create intent
                val i = Intent(this@SelectTestActivity, LiveTestActivity::class.java)
                // Add IP address and participant ID to intent
                i.putExtra("IPAddress", ipAddress)
                i.putExtra("ParticipantID", participantID)
                // Move to LiveTestActivity
                startActivity(i)
            }
        }

    }

    // Get IP address and participant ID
    @SuppressLint("SetTextI18n")
    fun getTestInfo() {
        ipAddress = ipInputET.text.toString().trim()
        participantID = participantIDET.text.toString().trim()
        // If EditText fields are empty, alert user to fill them in
        if (ipAddress.isEmpty() or participantID.isEmpty()) {
            runOnUiThread { alertText.text = "Please fill all fields." }
        }
    }

    // Launch the filePickerLauncher to allow selection of audio file
    fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        filePickerLauncher.launch(intent)
    }

    // Get the file name from the select Uri to display to user
    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = "audiofile.wav"
        val cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor != null) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                result = cursor.getString(idx)
            }
            cursor.close()
        }
        return result
    }

    // Send the audio file to the Python backend
    private fun sendToBackend() {
        // Prevent user from moving back to other activities
        onBackPressedDispatcher.addCallback(this) {
            // Do nothing to block back
        }
        // Prevent phone screen from turning off while file processing is ongoing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Create a dialog, make it not cancelable, and show it
        val dialog = ProgressDialog(this)
        dialog.setMessage("Processing…")
        dialog.setCancelable(false)
        dialog.show()

        // On a thread
        Thread {
            try {
                // Create socket variable with computer IP Address and port for processing of prerecorded audio
                val socket = Socket(ipAddress, 9091)
                // Use a contentResolver to read the selected audio file
                val input = contentResolver.openInputStream(selectedFileUri)
                // Get the output stream of the socket
                val output = socket.getOutputStream()

                // Create a buffer to hold 4096 bytes of data at a time
                val buffer1 = ByteArray(4096)
                var len: Int
                // Continue to retrieve bytes from audio file until end is reached
                while ((input!!.read(buffer1).also { len = it }) != -1) {
                    // Write the bytes from the buffer to the output stream for the Python code to use
                    output.write(buffer1, 0, len)
                }

                // Tell socket server that data is done being written
                output.flush()
                socket.shutdownOutput()

                // Close the audio file stream
                input.close()

                // Open stream from socket to receive data
                val inputStream = socket.getInputStream()
                // Create a byteArrayOutputStream to write incoming data
                val byteArrayOutputStream = ByteArrayOutputStream()

                // Create another buffer to hold increments of data
                val buffer2 = ByteArray(4096)
                var bytesRead: Int

                // Write data to byteArrayOutputStream until end of data is reached
                while ((inputStream.read(buffer2).also { bytesRead = it }) != -1) {
                    byteArrayOutputStream.write(buffer2, 0, bytesRead)
                }

                // Convert bytes to UTF-8
                val response = byteArrayOutputStream.toString("UTF-8")
                // Convert string to JSONArray to use data
                val jsonArray = JSONArray(response)
                // Create a list for the 3 counting outcomes
                val stats = IntArray(3)

                // Get each outcome from the jsonArray and add it to the stats list
                for (i in 0..2) {
                    stats[i] = jsonArray.getInt(i)
                }

                // Close socket and dismiss "Processing..." dialog
                input.close()
                output.close()
                socket.close()
                dialog.dismiss()

                // Create a new intent and add participant ID and counting outcomes
                val i = Intent(this@SelectTestActivity, ResultsActivity::class.java)
                i.putExtra("ParticipantID", participantID)
                i.putExtra("Counted", stats[0])
                i.putExtra("Correct", stats[1])
                i.putExtra("Incorrect", stats[2])
                // Open results activity
                startActivity(i)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

}