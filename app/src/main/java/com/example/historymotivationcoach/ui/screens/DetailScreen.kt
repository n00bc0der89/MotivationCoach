package com.example.historymotivationcoach.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.historymotivationcoach.data.AppDatabase
import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.ui.components.PersonalityImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detail screen that displays the complete information for a motivation item.
 * 
 * Requirements: 9.2, 18.1, 18.2
 * 
 * @param motivationId The ID of the motivation item to display
 * @param onBackClick Callback when the back button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    motivationId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var motivation by remember { mutableStateOf<MotivationWithHistory?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Load motivation data
    LaunchedEffect(motivationId) {
        isLoading = true
        error = null
        
        scope.launch {
            // Get database instance (in production, use DI)
            val database = AppDatabase.getInstance(context)
            val repository = MotivationRepository(
                database.motivationDao(),
                database.historyDao()
            )
            
            // Find the motivation in history
            val allDateKeys = repository.getAllDateKeys()
            for (dateKey in allDateKeys) {
                val items = repository.getHistoryByDate(dateKey)
                val found = items.find { it.item.id == motivationId }
                if (found != null) {
                    motivation = found
                    break
                }
            }
            
            if (motivation == null) {
                error = "Motivation not found"
            }
            
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Motivation Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Animated content with crossfade
        AnimatedContent(
            targetState = when {
                isLoading -> "loading"
                error != null -> "error"
                else -> "content"
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "detail_screen_content"
        ) { state ->
            when (state) {
                "loading" -> {
                    LoadingContent(modifier = Modifier.padding(paddingValues))
                }
                "error" -> {
                    ErrorContent(
                        error = error!!,
                        onBackClick = onBackClick,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                "content" -> {
                    motivation?.let {
                        DetailContent(
                            motivation = it,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
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
        modifier = modifier.fillMaxSize(),
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
                        contentDescription = "Loading motivation details"
                    }
            )
            Text(
                text = "Loading details...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onBackClick) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun DetailContent(
    motivation: MotivationWithHistory,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Full-size personality image - larger than home screen
        PersonalityImage(
            imageUri = motivation.item.imageUri,
            contentDescription = "Image for ${motivation.item.author}",
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentScale = ContentScale.Crop
        )
        
        // Quote text with large typography
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "\"${motivation.item.quote}\"",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "— ${motivation.item.author}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Historical context (if available)
        motivation.item.context?.let { context ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Context",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = context,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // Themes as chips
        if (motivation.item.themes.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Themes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        motivation.item.themes.forEach { theme ->
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
            }
        }
        
        // Source attribution
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Source",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = motivation.item.sourceName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                motivation.item.sourceUrl?.let { url ->
                    TextButton(
                        onClick = { uriHandler.openUri(url) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("View Source")
                    }
                }
                
                Text(
                    text = "License: ${motivation.item.license}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Delivery timestamp
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Delivery Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Delivered on ${formatFullTimestamp(motivation.shownAt)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Status: ${motivation.deliveryStatus.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatFullTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US)
    return format.format(Date(timestamp))
}
