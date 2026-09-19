package com.xevil.board

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

data class SoundItem(
    val uri: String,
    val name: String,
    var favorite: Boolean
)

class MainActivity : AppCompatActivity() {

    private lateinit var soundsContainer: LinearLayout
    private lateinit var favoritesContainer: LinearLayout

    private var mediaPlayer: MediaPlayer? = null

    private val sounds = mutableListOf<SoundItem>()

    private val preferences by lazy {
        getSharedPreferences("xevil_board", MODE_PRIVATE)
    }

    private val audioPicker =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val uri = result.data?.data ?: return@registerForActivityResult

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val name = getFileName(uri)

            sounds.add(
                SoundItem(
                    uri = uri.toString(),
                    name = name,
                    favorite = false
                )
            )

            saveSounds()
            refreshUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        soundsContainer = findViewById(R.id.soundsContainer)
        favoritesContainer = findViewById(R.id.favoritesContainer)

        val addSoundButton: Button =
            findViewById(R.id.addSoundButton)

        val stopAllButton: Button =
            findViewById(R.id.stopAllButton)

        loadSounds()
        refreshUI()

        addSoundButton.setOnClickListener {
            openAudioPicker()
        }

        stopAllButton.setOnClickListener {
            stopAll()
        }
    }

    private fun openAudioPicker() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        intent.addCategory(Intent.CATEGORY_OPENABLE)

        intent.type = "audio/*"

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )

        audioPicker.launch(intent)
    }

    private fun getFileName(uri: Uri): String {

        var name = "Unknown Sound"

        val cursor = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {

            if (it.moveToFirst()) {

                val index =
                    it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }

        return name
    }

    private fun refreshUI() {

        soundsContainer.removeAllViews()
        favoritesContainer.removeAllViews()

        val favoriteSounds =
            sounds.filter { it.favorite }

        if (favoriteSounds.isEmpty()) {

            val empty = TextView(this)

            empty.text = "No favorite sounds yet"

            empty.setTextColor(Color.rgb(120, 120, 130))

            empty.textSize = 14f

            empty.setPadding(4, 4, 4, 12)

            favoritesContainer.addView(empty)

        } else {

            favoriteSounds.forEach {
                favoritesContainer.addView(
                    createSoundCard(it)
                )
            }
        }

        val normalSounds =
            sounds.filter { !it.favorite }

        if (normalSounds.isEmpty()) {

            if (sounds.isEmpty()) {

                val empty = TextView(this)

                empty.text = "No sounds added yet"

                empty.setTextColor(Color.rgb(120, 120, 130))

                empty.textSize = 14f

                empty.gravity = Gravity.CENTER

                empty.setPadding(4, 20, 4, 20)

                soundsContainer.addView(empty)
            }

        } else {

            normalSounds.forEach {

                soundsContainer.addView(
                    createSoundCard(it)
                )
            }
        }
    }

    private fun createSoundCard(sound: SoundItem): View {

        val card = LinearLayout(this)

        card.orientation = LinearLayout.VERTICAL

        card.setPadding(
            16,
            14,
            12,
            12
        )

        val background =
            GradientDrawable()

        background.setColor(
            Color.rgb(20, 20, 29)
        )

        background.cornerRadius = 24f

        card.background = background

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            0,
            0,
            12
        )

        card.layoutParams = params

        val topRow = LinearLayout(this)

        topRow.orientation =
            LinearLayout.HORIZONTAL

        topRow.gravity =
            Gravity.CENTER_VERTICAL

        val name = TextView(this)

        name.text = "🔊  ${sound.name}"

        name.textSize = 16f

        name.setTextColor(Color.WHITE)

        name.setTypeface(
            null,
            Typeface.BOLD
        )

        name.maxLines = 1

        name.ellipsize =
            android.text.TextUtils.TruncateAt.END

        val nameParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        topRow.addView(
            name,
            nameParams
        )

        val favoriteButton =
            Button(this)

        favoriteButton.text =
            if (sound.favorite) "⭐"
            else "☆"

        favoriteButton.textSize = 18f

        favoriteButton.setTextColor(
            Color.WHITE
        )

        favoriteButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                Color.TRANSPARENT
            )

        favoriteButton.setOnClickListener {

            sound.favorite =
                !sound.favorite

            saveSounds()
            refreshUI()
        }

        topRow.addView(
            favoriteButton,
            LinearLayout.LayoutParams(
                52,
                52
            )
        )

        val deleteButton =
            Button(this)

        deleteButton.text = "🗑"

        deleteButton.textSize = 17f

        deleteButton.setTextColor(
            Color.WHITE
        )

        deleteButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                Color.TRANSPARENT
            )

        deleteButton.setOnClickListener {

            deleteSound(sound)
        }

        topRow.addView(
            deleteButton,
            LinearLayout.LayoutParams(
                52,
                52
            )
        )

        card.addView(topRow)

        val playButton =
            Button(this)

        playButton.text =
            "▶  PLAY"

        playButton.textSize = 14f

        playButton.setTextColor(
            Color.WHITE
        )

        playButton.setTypeface(
            null,
            Typeface.BOLD
        )

        playButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                Color.rgb(43, 42, 70)
            )

        val playParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                52
            )

        playParams.setMargins(
            0,
            6,
            0,
            0
        )

        card.addView(
            playButton,
            playParams
        )

        playButton.setOnClickListener {

            playSound(sound)
        }

        return card
    }

    private fun playSound(sound: SoundItem) {

        try {

            mediaPlayer?.release()

            mediaPlayer = MediaPlayer.create(
                this,
                Uri.parse(sound.uri)
            )

            if (mediaPlayer == null) {

                Toast.makeText(
                    this,
                    "Cannot play this sound",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            mediaPlayer?.setOnCompletionListener {

                it.release()

                mediaPlayer = null
            }

            mediaPlayer?.start()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Sound could not be played",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun deleteSound(sound: SoundItem) {

        mediaPlayer?.release()

        mediaPlayer = null

        sounds.remove(sound)

        saveSounds()

        refreshUI()

        Toast.makeText(
            this,
            "Sound deleted",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveSounds() {

        val array = JSONArray()

        sounds.forEach {

            val objectData =
                JSONObject()

            objectData.put(
                "uri",
                it.uri
            )

            objectData.put(
                "name",
                it.name
            )

            objectData.put(
                "favorite",
                it.favorite
            )

            array.put(objectData)
        }

        preferences.edit()
            .putString(
                "sounds",
                array.toString()
            )
            .apply()
    }

    private fun loadSounds() {

        sounds.clear()

        val saved =
            preferences.getString(
                "sounds",
                null
            ) ?: return

        try {

            val array =
                JSONArray(saved)

            for (i in 0 until array.length()) {

                val objectData =
                    array.getJSONObject(i)

                sounds.add(
                    SoundItem(
                        uri =
                            objectData.getString(
                                "uri"
                            ),

                        name =
                            objectData.getString(
                                "name"
                            ),

                        favorite =
                            objectData.getBoolean(
                                "favorite"
                            )
                    )
                )
            }

        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {

        mediaPlayer?.release()

        mediaPlayer = null

        super.onDestroy()
    }
}
