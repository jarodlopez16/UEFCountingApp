package com.example.uefcountingapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Hide phone status bars
        window.insetsController?.hide(WindowInsets.Type.statusBars())

        // Retrieve current screen as a ConstraintLayout variable
        val mainLayout: ConstraintLayout = findViewById(R.id.main)

        // When the screen is pressed
        mainLayout.setOnClickListener {
            // Create intent and move to SelectTestActivity
            val i = Intent(this@MainActivity, SelectTestActivity::class.java)
            startActivity(i)
        }
    }
}