package com.xevil.board

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var soundContainer: LinearLayout

    private val audioPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {
                addSound(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        soundContainer = findViewById(R.id.soundContainer)

        val addSoundButton: Button =
            findViewById(R.id.addSoundButton)

        val stopAllButton: Button =
            findViewById(R.id.stopAllButton)

        addSoundButton.setOnClickListener {
            audioPicker.launch("audio/*")
        }

        stopAllButton.setOnClickListener {
            stopAll()
        }
    }

    private fun addSound(uri: Uri) {

        val soundButton = Button(this)

        soundButton.text = "▶  Play Sound"

        soundButton.setOnClickListener {

            mediaPlayer?.release()

            mediaPlayer = MediaPlayer.create(this, uri)

            mediaPlayer?.start()

            Toast.makeText(
                this,
                "Playing sound",
                Toast.LENGTH_SHORT
            ).show()
        }

        soundContainer.addView(soundButton)
    }

    private fun stopAll() {

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        Toast.makeText(
            this,
            "All sounds stopped",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {

        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroy()
    }
}
