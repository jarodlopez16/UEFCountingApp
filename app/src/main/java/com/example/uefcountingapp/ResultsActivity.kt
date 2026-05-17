package com.example.uefcountingapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.WindowInsets
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

class ResultsActivity: AppCompatActivity() {

    lateinit var countedText: TextView
    lateinit var correctText: TextView
    lateinit var incorrectText: TextView
    lateinit var homeButton: ImageButton
    lateinit var resultFile: File

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_results)

        // Hide phone status bars
        window.insetsController?.hide(WindowInsets.Type.statusBars())
        // Retrieve TextViews for three outcomes
        countedText = findViewById(R.id.countedText)
        correctText = findViewById(R.id.correctText)
        incorrectText = findViewById(R.id.incorrectText)

        // Retrieve three outcomes from intent
        val totalCounted = intent.getIntExtra("Counted", 0)
        val correctSubtractions = intent.getIntExtra("Correct", 0)
        val incorrectSubtractions = intent.getIntExtra("Incorrect", 0)

        // Get current time and date to write in result file
        val current = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateFormatted = current.format(dateFormatter)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val timeFormatted = current.format(timeFormatter)
        // Retrieve participant ID from intent
        val participantID = intent.getStringExtra("ParticipantID")
        // In the app's file directory, create a path to "result.csv"
        val appDirectory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        resultFile = File(appDirectory, String.format("Results.csv"))

        try {
            // Check if the file exists at the created path
            val fileExists = resultFile.exists()
            // Initialize a FileWriter to write/append to file
            val resultWriter = FileWriter(resultFile, true)
            resultWriter.use { writer ->
                // If the file did not exist when checked, append a header
                if (!fileExists) {
                    writer.append("Participant,Date,Time,Total_Counted,Total_Correct,Total_Incorrect\n")
                }
                // Append all participant, date/time, and outcome information to the file
                writer.append(
                    "$participantID,$dateFormatted,$timeFormatted,$totalCounted,$correctSubtractions,$incorrectSubtractions\n"
                )
            }
        } catch (e: Exception) {
        }

        // Update TextViews to include the outcomes in their texts
        countedText.text = getString(R.string.counted) + " " + totalCounted
        correctText.text = getString(R.string.correct) + " " +  correctSubtractions
        incorrectText.text = getString(R.string.incorrect) + " " + incorrectSubtractions

        // If the home button is pressed, return to MainActivity
        homeButton = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            val i = Intent(this@ResultsActivity, MainActivity::class.java)
            startActivity(i)
        }
    }
}