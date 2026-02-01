package com.example.historymotivationcoach.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.ui.components.MotivationImage
import com.example.historymotivationcoach.viewmodel.HomeUiState
import com.example.historymotivationcoach.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen that displays the latest motivation and today's statistics.
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 18.1, 18.2
 * 
 * @param viewModel The HomeViewModel that manages the screen state
 * @param onMotivationClick Callback when a motivation card is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMotivationClick: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Animated content with crossfade
    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
        },
        label = "home_screen_content"
    ) { state ->
        when (state) {
            is HomeUiState.Loading -> {
                LoadingContent()
            }
            is HomeUiState.Success -> {
                SuccessContent(
                    motivation = state.latestMotivation,
                    todayCount = state.todayCount,
                    unseenCount = state.unseenCount,
                    onMotivationClick = onMotivationClick,
                    onSendNowClick = { viewModel.triggerManualNotification() }
                )
            }
            is HomeUiState.Empty -> {
                EmptyContent(
                    unseenCount = state.unseenCount,
                    onSendNowClick = { viewModel.triggerManualNotification() }
                )
            }
            is HomeUiState.ContentExhausted -> {
                ContentExhaustedContent(
                    onReplayClassicsClick = onNavigateToSettings
                )
            }
            is HomeUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetryClick = { viewModel.loadLatestMotivation() }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    // Pulsing animation for loading indicator
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_alpha"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .alpha(alpha)
                    .semantics {
                        contentDescription = "Loading latest motivation"
                    }
            )
            Text(
                text = "Loading your motivation...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    motivation: MotivationWithHistory,
    todayCount: Int,
    unseenCount: Int,
    onMotivationClick: (Long) -> Unit,
    onSendNowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with statistics
        Text(
            text = "Today's Motivation",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        StatsRow(todayCount = todayCount, unseenCount = unseenCount)
        
        // Latest motivation card
        MotivationCard(
            motivation = motivation,
            onClick = { onMotivationClick(motivation.item.id) }
        )
        
        // Send one now button
        Button(
            onClick = onSendNowClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send one now")
        }
    }
}

@Composable
private fun EmptyContent(
    unseenCount: Int,
    onSendNowClick: () -> Unit
) {
    // Scale animation for empty state
    val scale by rememberInfiniteTransition(label = "empty_scale").animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "No motivations yet today",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You have $unseenCount unseen motivations waiting for you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSendNowClick,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Send one now")
            }
        }
    }
}

@Composable
private fun ContentExhaustedContent(
    onReplayClassicsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "All Content Seen!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Congratulations! You've seen all available motivations. " +
                      "You can replay classics to see them again.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onReplayClassicsClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Replay Classics")
            }
            Text(
                text = "Go to Settings to clear history and start over",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun StatsRow(
    todayCount: Int,
    unseenCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            label = "Today",
            value = todayCount.toString()
        )
        StatCard(
            label = "Unseen",
            value = unseenCount.toString()
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.width(150.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotivationCard(
    motivation: MotivationWithHistory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Motivation card: ${motivation.item.quote.take(50)}... by ${motivation.item.author}. Tap to view details."
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image
            MotivationImage(
                imageUri = motivation.item.imageUri,
                contentDescription = "Motivation image for ${motivation.item.author}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            
            // Quote
            Text(
                text = "\"${motivation.item.quote}\"",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Author
            Text(
                text = "— ${motivation.item.author}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Context (if available)
            motivation.item.context?.let { context ->
                Text(
                    text = context,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Themes
            if (motivation.item.themes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    motivation.item.themes.take(3).forEach { theme ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(theme) },
                            modifier = Modifier.semantics {
                                contentDescription = "Theme: $theme"
                            }
                        )
                    }
                }
            }
            
            // Timestamp
            Text(
                text = "Delivered ${formatTimestamp(motivation.shownAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("h:mm a", Locale.US)
    return format.format(Date(timestamp))
}
