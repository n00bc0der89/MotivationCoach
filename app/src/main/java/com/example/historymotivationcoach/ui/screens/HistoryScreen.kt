package com.example.historymotivationcoach.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.ui.components.PersonalityImage
import com.example.historymotivationcoach.viewmodel.HistoryGroup
import com.example.historymotivationcoach.viewmodel.HistoryUiState
import com.example.historymotivationcoach.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History screen that displays all delivered motivations grouped by date.
 * 
 * Requirements: 7.1, 7.2, 7.3, 8.1, 8.2, 8.4
 * 
 * @param viewModel The HistoryViewModel that manages the screen state
 * @param onMotivationClick Callback when a history item is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onMotivationClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Animated content with crossfade
    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
        },
        label = "history_screen_content"
    ) { state ->
        when (state) {
            is HistoryUiState.Loading -> {
                LoadingContent()
            }
            is HistoryUiState.Success -> {
                SuccessContent(
                    groups = state.groups,
                    onMotivationClick = onMotivationClick
                )
            }
            is HistoryUiState.Empty -> {
                EmptyContent()
            }
            is HistoryUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetryClick = { viewModel.loadHistory() }
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
                        contentDescription = "Loading history"
                    }
            )
            Text(
                text = "Loading your history...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    groups: List<HistoryGroup>,
    onMotivationClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            // Date header with slide-in animation
            item(key = "header_${group.dateKey}") {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                ) {
                    DateHeader(
                        label = group.label,
                        count = group.items.size
                    )
                }
            }
            
            // History items for this date with staggered animation
            items(
                items = group.items,
                key = { it.item.id }
            ) { motivation ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally(
                        initialOffsetX = { 40 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                ) {
                    HistoryCard(
                        motivation = motivation,
                        onClick = { onMotivationClick(motivation.item.id) }
                    )
                }
            }
            
            // Spacer between groups
            item(key = "spacer_${group.dateKey}") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EmptyContent() {
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
                text = "📜",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your motivation history will appear here once you start receiving notifications.",
                style = MaterialTheme.typography.bodyLarge,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateHeader(
    label: String,
    count: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$count ${if (count == 1) "item" else "items"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Divider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCard(
    motivation: MotivationWithHistory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "History item from ${formatTime(motivation.shownAt)}: ${motivation.item.quote.take(50)}... by ${motivation.item.author}. Tap to view details."
            }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail image
            PersonalityImage(
                imageUri = motivation.item.imageUri,
                contentDescription = "Thumbnail for ${motivation.item.author}",
                modifier = Modifier
                    .size(80.dp),
                contentScale = ContentScale.Crop
            )
            
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Time
                Text(
                    text = formatTime(motivation.shownAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Author
                Text(
                    text = motivation.item.author,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Quote preview
                Text(
                    text = motivation.item.quote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val format = SimpleDateFormat("h:mm a", Locale.US)
    return format.format(Date(timestamp))
}
