package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.ui.components.AccessibilityBanner
import com.example.ui.components.BnCircularVisualizer
import com.example.ui.components.ChatTranscriptView
import com.example.ui.components.QuickActionChips
import com.example.ui.components.VoiceState
import com.example.ui.theme.AccentBorder
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkVioletCard
import com.example.ui.theme.DarkVioletSurface
import com.example.ui.theme.GlowPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()

    val sttRmsVolume by viewModel.speechToTextManager.rmsVolume.collectAsStateWithLifecycle()
    val ttsAmplitude by viewModel.textToSpeechManager.speakingAmplitude.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAccessibilityStatus()
    }

    // Determine current visualizer amplitude
    val currentAmplitude = when (voiceState) {
        VoiceState.LISTENING -> sttRmsVolume
        VoiceState.SPEAKING -> ttsAmplitude
        VoiceState.THINKING -> 0.7f
        VoiceState.IDLE -> 0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "PulseMic")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian),
        color = DarkObsidian
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BN AI ASSISTANT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Jarvis Engine • Realtime",
                            fontSize = 11.sp,
                            color = CyberBlue
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.openAccessibilitySettings(context) },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextMuted
                    )
                }
            }

            // Accessibility Banner
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                AccessibilityBanner(
                    isEnabled = isAccessibilityEnabled,
                    onOpenSettings = { viewModel.openAccessibilitySettings(context) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3D Animated Circular Visualizer Interface
            BnCircularVisualizer(
                modifier = Modifier.fillMaxWidth(),
                voiceState = voiceState,
                amplitude = currentAmplitude,
                onTapBn = {
                    viewModel.onTapBnCircle()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Command Chips
            QuickActionChips(
                onActionClick = { command ->
                    viewModel.sendTextMessage(command)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chat Transcript List
            ChatTranscriptView(
                modifier = Modifier.weight(1f),
                messages = messages
            )

            // Bottom Mic & Input Control Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkVioletSurface)
                    .border(width = 1.dp, color = AccentBorder)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Text Input Bar
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        placeholder = {
                            Text(
                                text = "Ask BN AI or type a command...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkVioletCard,
                            unfocusedContainerColor = DarkVioletCard,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = AccentBorder,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                viewModel.sendTextMessage(inputText)
                            }
                        ),
                        trailingIcon = {
                            if (inputText.isNotBlank()) {
                                IconButton(onClick = { viewModel.sendTextMessage(inputText) }) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Pulse Glowing Mic FAB
                    FloatingActionButton(
                        onClick = {
                            if (hasMicPermission) {
                                viewModel.toggleListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .scale(if (voiceState == VoiceState.LISTENING) micPulseScale else 1f)
                            .testTag("mic_toggle_button"),
                        containerColor = if (voiceState == VoiceState.LISTENING) GlowPink else NeonCyan,
                        contentColor = Color.Black,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Mic",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
