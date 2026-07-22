package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBorder
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DarkVioletCard
import com.example.ui.theme.TextLight

data class QuickActionItem(
    val label: String,
    val command: String,
    val icon: ImageVector
)

val defaultQuickActions = listOf(
    QuickActionItem("Open YouTube", "Open YouTube", Icons.Default.PlayArrow),
    QuickActionItem("Go Home", "Go home", Icons.Default.Home),
    QuickActionItem("Volume Up", "Volume up", Icons.Default.VolumeUp),
    QuickActionItem("Battery Level", "What is my battery level?", Icons.Default.BatteryChargingFull),
    QuickActionItem("Take Screenshot", "Take screenshot", Icons.Default.Screenshot)
)

/**
 * Quick Action Command Chips for immediate voice command testing.
 */
@Composable
fun QuickActionChips(
    modifier: Modifier = Modifier,
    actions: List<QuickActionItem> = defaultQuickActions,
    onActionClick: (String) -> Unit
) {
    LazyRow(
        modifier = modifier.testTag("quick_action_chips"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkVioletCard)
                    .border(1.dp, AccentBorder, RoundedCornerShape(20.dp))
                    .clickable { onActionClick(action.command) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = action.label,
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
            }
        }
    }
}
