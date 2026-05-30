package com.agentbook.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentbook.pro.ui.components.ChatBubble
import com.agentbook.pro.ui.components.ChatInputBar
import com.agentbook.pro.ui.components.StatCard
import com.agentbook.pro.ui.theme.NeonBackground
import com.agentbook.pro.ui.theme.NeonGreen
import com.agentbook.pro.ui.theme.NeonMuted
import com.agentbook.pro.ui.theme.NeonOnSurface
import com.agentbook.pro.ui.theme.NeonPink
import com.agentbook.pro.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val sending by viewModel.isSending.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(toast) {
        toast?.let {
            scope.launch {
                snackbar.showSnackbar(it)
                viewModel.consumeToast()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = NeonBackground,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NeonBackground)
        ) {
            Header()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total citas",
                    value = stats.total.toString(),
                    accent = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Hoy",
                    value = stats.today.toString(),
                    accent = NeonPink,
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }

            ChatInputBar(
                text = input,
                enabled = !sending,
                onTextChange = viewModel::onInputChanged,
                onSend = viewModel::send,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(listOf(NeonGreen, NeonPink))
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "AB",
                    color = NeonBackground,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "AgentBook",
                    color = NeonOnSurface,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Asistente dental · ${com.agentbook.pro.AppConfig.PROVIDER_NAME}",
                    color = NeonMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = com.agentbook.pro.AppConfig.supabaseDebug,
                    color = NeonMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(NeonGreen.copy(alpha = 0.6f), NeonPink.copy(alpha = 0.6f))
                    )
                )
        )
    }
}
