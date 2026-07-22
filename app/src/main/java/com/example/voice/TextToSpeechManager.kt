package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

/**
 * Text-To-Speech Engine supporting human-like voice synthesis,
 * speed/pitch customization, and amplitude synchronization for 3D visualizer animation.
 */
class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingAmplitude = MutableStateFlow(0f)
    val speakingAmplitude: StateFlow<Float> = _speakingAmplitude.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var animationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language US is not supported or missing data.")
            } else {
                tts?.setPitch(1.05f) // Slightly higher tone for clear voice
                tts?.setSpeechRate(1.05f)
        tts?.setPitch(0.95f)

                // Select a natural sounding voice if available
                tts?.voices?.find { voice ->
                    !voice.isNetworkConnectionRequired && voice.locale.language == "en"
                }?.let { voice ->
                    try {
                        tts?.voice = voice
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set custom voice", e)
                    }
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _isSpeaking.value = true
                            startSpeakingAmplitudeAnimation()
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            stopSpeakingAmplitudeAnimation()
                            _isSpeaking.value = false
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            stopSpeakingAmplitudeAnimation()
                            _isSpeaking.value = false
                        }
                    }
                })

                _isReady.value = true
                Log.i(TAG, "TTS Engine Initialized successfully")
            }
        } else {
            Log.e(TAG, "TTS Engine Initialization Failed with status $status")
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (text.isBlank()) return

        stop()

        val params = HashMap<String, String>()
        val utteranceId = "BN_SPEECH_" + System.currentTimeMillis()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        _isSpeaking.value = true
        startSpeakingAmplitudeAnimation()
    }

    private fun startSpeakingAmplitudeAnimation() {
        animationJob?.cancel()
        animationJob = scope.launch {
            while (_isSpeaking.value) {
                // Generate realistic speech amplitude pulses between 0.35 and 0.95
                val amp = 0.35f + Random.nextFloat() * 0.6f
                _speakingAmplitude.value = amp
                delay(60)
            }
            _speakingAmplitude.value = 0f
        }
    }

    private fun stopSpeakingAmplitudeAnimation() {
        animationJob?.cancel()
        _speakingAmplitude.value = 0f
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        stopSpeakingAmplitudeAnimation()
        _isSpeaking.value = false
    }

    fun destroy() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
        tts = null
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
