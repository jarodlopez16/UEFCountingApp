package com.example.uefcountingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveTestActivity: AppCompatActivity() {
    val tag: String = "MicrophoneActivity"
    val requestMicPermission = 200
    val sampleRate = 16000
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    var audioRecord: AudioRecord? = null
    var bufferSize = 0
    var executor: ExecutorService? = null
    var webSocket: WebSocket? = null
    var isRecording = false
    var testButton: Button? = null
    var latestStartTime = 0.0
    var latestEndTime = 0.0
    var latestTranscript = ""
    var testWriter: FileWriter? = null
    var testFile: File? = null
    var lastWrittenEndTime = 0.0
    var ipAddress = ""
    val port = "9090"
    var participantID = ""
    var stopRequested = false
    var root: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_test)
        // Retrieve participant ID from intent
        participantID = intent.getStringExtra("ParticipantID").toString()
        // Retrieve IP address from intent
        ipAddress = intent.getStringExtra("IPAddress").toString()
        // Executor to run functions off main thread
        executor = Executors.newSingleThreadExecutor()
        // Retrieve test button to start/stop counting task
        testButton = findViewById(R.id.testButton)
        // Check whether microphone use is enabled for app
        checkMicrophonePermission()

        // When test button is pressed
        testButton!!.setOnClickListener {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onBackPressedDispatcher.addCallback(this) {
                // Do nothing to block back
            }
            // If not recording (pressed to start test)
            if (!isRecording) {
                // Reset latest start time, end time, and transcript variables
                latestStartTime = 0.0
                latestEndTime = 0.0
                latestTranscript = ""
                // Reset last written end time variable
                lastWrittenEndTime = 0.0
                try {
                    // Connect to the Python server for WhisperLive and start audio capture
                    connectWebSocketAndStartAudio()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            } else {
                try {
                    // If currently recording (pressed to stop test), end audio capture
                    stopRequested = true
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    // Check for microphone permission
    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),
                requestMicPermission
            )
        }
    }

    // Function to connect to TCP server for WhisperLive
    @SuppressLint("DefaultLocale")
    @Throws(IOException::class)
    private fun connectWebSocketAndStartAudio() {
        // Request a connection to TCP server with port for live transcription
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://$ipAddress:$port").build()

        // Create a TSV file to temporarily save transcription from WhisperLive
        val appDirectory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        testFile = File(appDirectory, String.format("Temp.tsv"))
        /* Data will later be written to a file on computer to be saved long term */

        try {
            // Initialize a FileWriter and create header for file's columns
            testWriter = FileWriter(testFile)
            // Speech segment start time, speech segment end time, and transcribed segment
            testWriter!!.append("Start_time\tEnd_time\tTranscription\n")
        } catch (exception: IOException) {
            exception.printStackTrace()
        }

        // Connect to websocket
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            // Upon opening of websocket
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connected")
                // Create a Toast notification to show success
                runOnUiThread {
                    Toast.makeText(this@LiveTestActivity, "Connected", Toast.LENGTH_SHORT).show()
                    // Update testButton to show method of ending test
                    testButton!!.text = "Stop Recording"
                }

                // Send configuration of Whisper model to the transcription server
                sendConfiguration(ws)
                // Start capture of audio to be transcribed
                startAudioCapture()
            }

            // Upon reception of a message from websocket
            @SuppressLint("DefaultLocale")
            override fun onMessage(ws: WebSocket, text: String) {
                //try {
                    // Get JSON message (WhisperLive transcriptions) from server
                    root = JSONObject(text)
                    Log.i("Message", root.toString())
                    // If test stop has been requested
                    if (stopRequested) {
                        // Stop audio capture with microphone
                        stopAudioCapture()
                    }
            }

            // If failed to connect to websocket, show error popup
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket error: ", t)
                runOnUiThread {
                    Toast.makeText(this@LiveTestActivity, "Connection Failed", Toast.LENGTH_SHORT)
                        .show()
                    try {
                        stopAudioCapture() // Stop audio capture if socket fails
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            }

            // Upon closing websocket
            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }
        })
    }

    // Send WhisperLive configuration to transcription server
    private fun sendConfiguration(ws: WebSocket) {
        try {
            // Create a JSONObject and add information
            val json = JSONObject()
            // Language of speech is english
            json.put("language", "en")
            // Task for model is to transcribe
            json.put("task", "transcribe")
            // Send current time as id
            json.put("uid", "android_" + System.currentTimeMillis())
            // Whisper version to use is large model
            json.put("model", "large")
            json.put("type", "config")
            // Send to transcription server
            ws.send(json.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        // Return is a test is somehow underway
        if (isRecording) return

        // Calculate the minimum buffer size so as to not lose audio data
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        // If buffer is small, force it to 4096 bytes
        if (bufferSize < 4096) {
            bufferSize = 4096
        }
        // Initialize an AudioRecord to use the mobile device microphone
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
            channelConfig, audioFormat, bufferSize
        )

        try {
            // Start recording audio
            audioRecord!!.startRecording()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            return
        }

        // Set recording tracker to true
        isRecording = true

        // On other thread
        executor!!.submit {
            // Read as shorts (Int16)
            val audioBuffer = ShortArray(bufferSize / 2)
            // While recording is active and websocket connection is going
            while (isRecording && webSocket != null) {
                // Read the audio data from the microphone into a buffer
                val readCount = audioRecord!!.read(audioBuffer, 0, audioBuffer.size)

                if (readCount > 0) {
                    // Convert to Float32 for whisper_live
                    // Float is 4 bytes so 4x space of readCount
                    val floatBuffer = ByteBuffer.allocate(readCount * 4)
                    floatBuffer.order(ByteOrder.LITTLE_ENDIAN) // Python uses Little Endian

                    for (i in 0..<readCount) {
                        // Normalize to a float
                        val normalized = audioBuffer[i] / 32768.0f
                        floatBuffer.putFloat(normalized)
                    }

                    // Send audioBytes to the Python server
                    val audioBytes = ByteString.of(*floatBuffer.array())
                    webSocket!!.send(audioBytes)
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    @Throws(IOException::class)
    private fun stopAudioCapture() {
        if (root!!.has("segments")) {
            // Loop through all segments
            val segments = root!!.getJSONArray("segments")
            for (i in 0..<segments.length()) {
                // Retrieve segment at current index
                val segment = segments.getJSONObject(i)
                // Check start index of speech segment
                val start = segment.optDouble("start", 0.0)
                // Check end index of speech segment
                val end = segment.optDouble("end", 0.0)
                // Check transcription content of speech segment
                val textContent = segment.optString("text", "")
                // Write the information for each speech segment to the temp file
                testWriter?.apply {
                    append(String.format("%f\t%f\t%s\n", start, end, textContent))
                    flush()
                }
            }
        }

        // Set recording and stop requested trackers to false
        isRecording = false
        stopRequested = false

        // Stop and release the AudioRecord
        if (audioRecord != null) {
            try {
                audioRecord!!.stop()
                audioRecord!!.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Empty the AudioRecord
            audioRecord = null
        }

        // Close connection with the server
        if (webSocket != null) {
            webSocket!!.close(1000, "Stopping recording")
            webSocket = null
        }

        // Post Processing
        sendToBackend()
    }

    private fun sendToBackend() {
        runOnUiThread {
            // Prevent user from swiping to other activities
            onBackPressedDispatcher.addCallback(this) {
                // Do nothing to block back
            }
            // Prevent screen from turning off
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Create a dialog to show that data is being processed
            val dialog = ProgressDialog(this)
            dialog.setMessage("Processing…")
            dialog.setCancelable(false)
            dialog.show()

            Thread {
                try {
                    // Connect to the Python server on the port for postprocessing of a live transcription file
                    val socket = Socket(ipAddress, 9092)
                    // Get input stream of the test file
                    val input = testFile?.inputStream()
                    val output = socket.getOutputStream()

                    // Write the data to the socket using a buffer
                    val buffer1 = ByteArray(4096)
                    var len: Int
                    // Write to socket until the end of the temp file has been reached
                    while ((input!!.read(buffer1).also { len = it }) != -1) {
                        output.write(buffer1, 0, len)
                    }

                    output.flush()
                    // Shut down output but wait for response
                    socket.shutdownOutput()
                    // Close the temp file stream
                    input.close()

                    // Open stream from socket to get response
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
                    val i = Intent(this@LiveTestActivity, ResultsActivity::class.java)
                    i.putExtra("Counted", stats[0])
                    i.putExtra("Correct", stats[1])
                    i.putExtra("Incorrect", stats[2])
                    i.putExtra("ParticipantID", participantID)
                    // Open results activity
                    startActivity(i)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }
}