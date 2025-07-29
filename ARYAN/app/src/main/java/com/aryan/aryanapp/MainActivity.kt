package com.aryan.aryanapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesList
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stfalcon.chatkit.messages.MessagesListAdapter
import android.speech.tts.TextToSpeech
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var messagesList: MessagesList
    private lateinit var input: MessageInput
    private lateinit var adapter: MessagesListAdapter<Message>
    private lateinit var voiceInputButton: ImageButton

    private val user = User("0", "User", null)
    private val ai = User("1", "ARYAN", null)

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messagesList = findViewById(R.id.messagesList)
        input = findViewById(R.id.input)
        voiceInputButton = findViewById(R.id.voice_input_button)

        adapter = MessagesListAdapter(user.id, null)
        messagesList.setAdapter(adapter)

        input.setInputListener { input ->
            val message = Message(UUID.randomUUID().toString(), input.toString(), user, Calendar.getInstance())
            adapter.addToStart(message, true)
            respondTo(message)
            true
        }

        voiceInputButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
            } else {
                startSpeechToText()
            }
        }

        tts = TextToSpeech(this, this)
    }

    private fun respondTo(message: Message) {
        val response = Message(UUID.randomUUID().toString(), "You said: ${message.getText()}", ai, Calendar.getInstance())
        adapter.addToStart(response, true)
        tts.speak(response.getText(), TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language not supported
            }
        }
    }

    private fun startSpeechToText() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        try {
            startActivityForResult(speechRecognizerIntent, RECORD_AUDIO_REQUEST_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                input.getInputEditText().setText(result[0])
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechToText()
        }
    }
}

data class Message(
    private val id: String,
    private val text: String,
    private val user: User,
    private val createdAt: Calendar
) : com.stfalcon.chatkit.commons.models.IMessage {
    override fun getId(): String = id
    override fun getText(): String = text
    override fun getUser(): com.stfalcon.chatkit.commons.models.IUser = user
    override fun getCreatedAt(): Date = createdAt.time
}

data class User(
    private val id: String,
    private val name: String,
    private val avatar: String?
) : com.stfalcon.chatkit.commons.models.IUser {
    override fun getId(): String = id
    override fun getName(): String = name
    override fun getAvatar(): String? = avatar
}
