package com.example.historymotivationcoach.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.ui.components.PersonalityImage
import com.example.historymotivationcoach.viewmodel.HomeUiState
import com.example.historymotivationcoach.viewmodel.HomeViewModel
import com.example.historymotivationcoach.viewmodel.ManualTriggerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen that displays the latest motivation and today's statistics.
 * 
 * Requirements: 2.1, 2.2, 2.4, 2.5, 2.7, 3.1, 12.1, 12.2, 12.3, 12.4, 18.1, 18.2
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
    val manualTriggerState by viewModel.manualTriggerState.collectAsState()
    
    // Show snackbar for manual trigger feedback
    val snackbarHostState = androidx.compose.material3.SnackbarHostState()
    
    androidx.compose.runtime.LaunchedEffect(manualTriggerState) {
        when (manualTriggerState) {
            is ManualTriggerState.Success -> {
                snackbarHostState.showSnackbar("Motivation delivered!")
            }
            is ManualTriggerState.Error -> {
                snackbarHostState.showSnackbar((manualTriggerState as ManualTriggerState.Error).message)
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
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
                            onSendNowClick = { viewModel.triggerManualNotification() },
                            isManualTriggerLoading = manualTriggerState is ManualTriggerState.Loading
                        )
                    }
                    is HomeUiState.Empty -> {
                        EmptyContent(
                            unseenCount = state.unseenCount,
                            onSendNowClick = { viewModel.triggerManualNotification() },
                            isManualTriggerLoading = manualTriggerState is ManualTriggerState.Loading
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
    onSendNowClick: () -> Unit,
    isManualTriggerLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header with philosophical styling
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Daily Wisdom",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Inspiration from history's greatest minds",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Statistics cards with enhanced design
        StatsRow(todayCount = todayCount, unseenCount = unseenCount)
        
        // Latest motivation card with philosophical theme
        PhilosophicalMotivationCard(
            motivation = motivation,
            onClick = { onMotivationClick(motivation.item.id) }
        )
        
        // Send Manual Notification button
        Button(
            onClick = onSendNowClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isManualTriggerLoading,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isManualTriggerLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isManualTriggerLoading) "Delivering..." else "Send Manual Notification",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun EmptyContent(
    unseenCount: Int,
    onSendNowClick: () -> Unit,
    isManualTriggerLoading: Boolean
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "Begin Your Journey",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You have $unseenCount pieces of wisdom waiting to inspire you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSendNowClick,
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = !isManualTriggerLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isManualTriggerLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isManualTriggerLoading) "Delivering..." else "Receive Wisdom Now",
                    style = MaterialTheme.typography.labelLarge
                )
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            label = "Received Today",
            value = todayCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Awaiting Discovery",
            value = unseenCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhilosophicalMotivationCard(
    motivation: MotivationWithHistory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Motivation card: ${motivation.item.quote.take(50)}... by ${motivation.item.author}. Tap to view details."
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Personality Image with enhanced styling
            PersonalityImage(
                imageUri = motivation.item.imageUri,
                contentDescription = "Portrait of ${motivation.item.author}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Content section with padding
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quote with enhanced typography
                Text(
                    text = "\"${motivation.item.quote}\"",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.headlineSmall.lineHeight
                )
                
                // Author with philosophical styling
                Text(
                    text = "— ${motivation.item.author}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Context (if available)
                motivation.item.context?.let { context ->
                    Text(
                        text = context,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
                
                // Themes with enhanced chip design
                if (motivation.item.themes.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        motivation.item.themes.take(3).forEach { theme ->
                            SuggestionChip(
                                onClick = {},
                                label = { 
                                    Text(
                                        theme,
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Theme: $theme"
                                }
                            )
                        }
                    }
                }
                
                // Timestamp with subtle styling
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "Delivered ${formatTimestamp(motivation.shownAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("h:mm a", Locale.US)
    return format.format(Date(timestamp))
}
