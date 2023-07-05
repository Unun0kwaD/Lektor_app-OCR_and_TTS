package com.example.cameraxapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.camera.core.ExperimentalGetImage
import com.example.cameraxapp.databinding.ActivityMainMenuBinding

@ExperimentalGetImage class MainMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val startButton = findViewById<Button>(R.id.button1)
        startButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val ttsButton = findViewById<Button>(R.id.button2)
        ttsButton.setOnClickListener {
            val intent = Intent(this, TextToSpeech::class.java)
            startActivity(intent)
        }
        val ocrButton = findViewById<Button>(R.id.button3)
        ocrButton.setOnClickListener {
            val intent = Intent(this, OCRActivity::class.java)
            startActivity(intent)
        }
    }



}


