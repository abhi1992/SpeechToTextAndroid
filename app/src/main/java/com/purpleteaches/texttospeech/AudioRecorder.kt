package com.purpleteaches.texttospeech


import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AudioRecorder(
    private val app: Application
) : RecognitionListener {

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app)
    private val _state = MutableStateFlow(TextToVoiceParser())
    var state = _state.asStateFlow()
    override fun onReadyForSpeech(p0: Bundle?) {
        _state.update { it.copy(error = null) }
    }

    fun startListening(languageCode: String = "hi") {
        _state.update { TextToVoiceParser() }

        if(!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update {
                it.copy("Recognizer Not Available")
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                languageCode
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
            )
        }
        speechRecognizer.setRecognitionListener(this)
        speechRecognizer.startListening(intent)

        _state.update {
            it.copy(isSpeaking = true)
        }
    }

    fun stopListening() {
        _state.update {
            it.copy(isSpeaking = false)
        }
        speechRecognizer.stopListening()
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(p0: Float) = Unit

    override fun onBufferReceived(p0: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        _state.update {
            it.copy(isSpeaking = false)
        }
    }

    override fun onError(error: Int) {
        if(error == SpeechRecognizer.ERROR_CLIENT) {
            return
        }
        _state.update {
            it.copy(
                error = "Error: $error"
            )
        }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.let { result ->
                _state.update {
                    it.copy(
                        spokenText = result
                    )
                }
            }
    }

    override fun onPartialResults(p0: Bundle?) = Unit

    override fun onEvent(p0: Int, p1: Bundle?) = Unit

}

data class TextToVoiceParser (
    val error: String? = "",
    val spokenText: ArrayList<String> = ArrayList<String>(),
    val isSpeaking: Boolean = false
)