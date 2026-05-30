package com.agentbook.pro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agentbook.pro.data.model.ChatUiMessage
import com.agentbook.pro.ui.theme.NeonGreen
import com.agentbook.pro.ui.theme.NeonOnSurface
import com.agentbook.pro.ui.theme.NeonPink
import com.agentbook.pro.ui.theme.NeonSurfaceVariant

@Composable
fun ChatBubble(message: ChatUiMessage) {
    val isUser = message.role == ChatUiMessage.Role.USER
    val alignment = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = alignment
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            Brush.linearGradient(
                                listOf(
                                    NeonPink.copy(alpha = 0.85f),
                                    NeonPink.copy(alpha = 0.6f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    NeonSurfaceVariant,
                                    NeonSurfaceVariant
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) NeonPink.copy(alpha = 0.4f) else NeonGreen.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = NeonGreen,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = message.text,
                            color = NeonOnSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.Black else NeonOnSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
