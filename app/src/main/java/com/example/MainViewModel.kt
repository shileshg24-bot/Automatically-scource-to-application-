package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.BNAccessibilityService
import com.example.device.VoiceAssistantRepository
import com.example.device.VoiceCommandResult
import com.example.service.BNVoiceForegroundService
import com.example.ui.components.ChatMessage
import com.example.ui.components.MessageSender
import com.example.ui.components.VoiceState
import com.example.voice.SpeechToTextManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VoiceAssistantRepository(application)
    val speechToTextManager = SpeechToTextManager(application)
    val textToSpeechManager = TextToSpeechManager(application)

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isContinuousListening = MutableStateFlow(false)
    val isContinuousListening: StateFlow<Boolean> = _isContinuousListening.asStateFlow()

    init {
        speechToTextManager.onResultCallback = { spokenText ->
            if (spokenText.isNotBlank()) {
                handleUserSpokenText(spokenText)
            }
        }

        speechToTextManager.onErrorCallback = { _ ->
            if (_isContinuousListening.value && !textToSpeechManager.isSpeaking.value) {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(800)
                    if (_isContinuousListening.value && !textToSpeechManager.isSpeaking.value && !speechToTextManager.isListening.value) {
                        speechToTextManager.startListening()
                    }
                }
            }
        }

        viewModelScope.launch {
            speechToTextManager.isListening.collect { listening ->
                if (listening) {
                    _voiceState.value = VoiceState.LISTENING
                } else if (_voiceState.value == VoiceState.LISTENING) {
                    if (!textToSpeechManager.isSpeaking.value) {
                        _voiceState.value = VoiceState.IDLE
                    }
                }
            }
        }

        viewModelScope.launch {
            textToSpeechManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    _voiceState.value = VoiceState.SPEAKING
                } else if (_voiceState.value == VoiceState.SPEAKING) {
                    _voiceState.value = VoiceState.IDLE
                    if (_isContinuousListening.value) {
                        speechToTextManager.startListening()
                    }
                }
            }
        }

        checkAccessibilityStatus()
    }

    fun checkAccessibilityStatus() {
        _isAccessibilityEnabled.value = BNAccessibilityService.isEnabled(getApplication())
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Could not open accessibility settings", e)
        }
    }

    fun onTapBnCircle() {
        textToSpeechManager.stop()
        speechToTextManager.stopListening()
        
        val initialGreeting = "Hi, I'm BN AI. How can I help you?"
        addMessage(MessageSender.BN_AI, initialGreeting)
        _isContinuousListening.value = true
        BNVoiceForegroundService.startService(getApplication())
        textToSpeechManager.speak(initialGreeting)
    }

    fun toggleListening() {
        if (speechToTextManager.isListening.value) {
            speechToTextManager.stopListening()
            _isContinuousListening.value = false
            _voiceState.value = VoiceState.IDLE
        } else {
            textToSpeechManager.stop()
            _isContinuousListening.value = true
            speechToTextManager.startListening()
            BNVoiceForegroundService.startService(getApplication())
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        _inputText.value = ""
        handleUserSpokenText(text)
    }

    private fun handleUserSpokenText(userText: String) {
        addMessage(MessageSender.USER, userText)
        _voiceState.value = VoiceState.THINKING
        _isContinuousListening.value = true
        BNVoiceForegroundService.startService(getApplication())

        viewModelScope.launch {
            val history = _messages.value.map {
                val sender = if (it.sender == MessageSender.USER) "user" else "model"
                Pair(sender, it.text)
            }

            when (val result = repository.processUserCommand(userText, history)) {
                is VoiceCommandResult.ActionExecuted -> {
                    addMessage(MessageSender.SYSTEM, result.spokenFeedback)
                    textToSpeechManager.speak(result.spokenFeedback)
                }
                is VoiceCommandResult.AiResponse -> {
                    addMessage(MessageSender.BN_AI, result.spokenText)
                    textToSpeechManager.speak(result.spokenText)
                }
                is VoiceCommandResult.Error -> {
                    addMessage(MessageSender.BN_AI, result.message)
                    textToSpeechManager.speak("Sorry, ${result.message}")
                }
            }
        }
    }

    private fun addMessage(sender: MessageSender, text: String) {
        val newList = _messages.value.toMutableList()
        newList.add(ChatMessage(sender = sender, text = text))
        _messages.value = newList
    }

    override fun onCleared() {
        super.onCleared()
        speechToTextManager.destroy()
        textToSpeechManager.destroy()
    }
}
