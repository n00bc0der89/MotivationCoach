package com.example.historymotivationcoach.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.historymotivationcoach.R

/**
 * Composable for loading and displaying personality images.
 * Uses Coil for efficient loading and caching.
 * 
 * This component provides:
 * - Automatic image loading with Coil's AsyncImage
 * - Crossfade animation for smooth transitions
 * - Placeholder display during loading
 * - Error fallback to placeholder image
 * - Support for different content scales for different contexts
 * - Memory and disk caching for performance
 * 
 * Supports multiple URI schemes:
 * - android.resource:// for drawable resources
 * - https:// for remote images
 * - file:// for local files
 * 
 * Requirements:
 * - 1.1: Load and display personality images with valid imageUri
 * - 1.2: Display placeholder during loading
 * - 1.3: Display fallback placeholder on error
 * - 9.1: Support drawable resource URIs (android.resource://)
 * - 9.2: Support HTTPS URIs
 * - 9.4: Show placeholder for null or empty imageUri
 * 
 * @param imageUri The URI of the image to load (supports android.resource://, https://, file://)
 * @param contentDescription Accessibility description for the image
 * @param modifier Modifier for customizing the composable's layout and appearance
 * @param contentScale How to scale the image within its bounds (default: Crop)
 */
@Composable
fun PersonalityImage(
    imageUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .crossfade(true)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
