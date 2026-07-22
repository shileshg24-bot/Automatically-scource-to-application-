package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBorder
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DarkVioletCard
import com.example.ui.theme.DarkVioletSurface
import com.example.ui.theme.GlowPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

/**
 * Status Banner displaying whether device Accessibility Service control is enabled.
 */
@Composable
fun AccessibilityBanner(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(DarkVioletCard, DarkVioletSurface)
                )
            )
            .border(
                width = 1.dp,
                color = if (isEnabled) NeonCyan.copy(alpha = 0.5f) else GlowPink.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onOpenSettings() }
            .padding(14.dp)
            .testTag("accessibility_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Accessibility,
                contentDescription = null,
                tint = if (isEnabled) NeonCyan else GlowPink,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Device Control Active" else "Enable Accessibility Control",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isEnabled) "Hands-free tap, back, home & app launching enabled" else "Tap to enable hands-free phone control features",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
