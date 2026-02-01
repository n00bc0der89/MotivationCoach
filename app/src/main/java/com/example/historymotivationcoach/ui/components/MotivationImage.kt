package com.example.historymotivationcoach.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.historymotivationcoach.R

/**
 * Reusable composable for loading and displaying motivation images with Coil.
 * 
 * This composable provides:
 * - Automatic image loading with Coil
 * - Loading indicator while image is being fetched
 * - Placeholder icon for failed loads or missing images
 * - Proper content scaling
 * - Accessibility support via content descriptions
 * 
 * The image loader is configured in MotivationApplication with:
 * - Memory caching for fast repeated access
 * - Disk caching for offline support
 * - Automatic cache management
 * 
 * Requirements:
 * - 17.2: Use Coil library with offline caching enabled
 * - 17.3: Display placeholder for loading and error states
 * - 18.4: Provide accessible content descriptions
 * 
 * @param imageUri The URI of the image to load (can be android.resource://, http://, file://, etc.)
 * @param contentDescription Accessibility description for the image
 * @param modifier Modifier for customizing the composable's layout and appearance
 * @param contentScale How to scale the image within its bounds (default: Crop)
 */
@Composable
fun MotivationImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    SubcomposeAsyncImage(
        model = imageUri,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    ) {
        val state = painter.state
        
        when (state) {
            is AsyncImagePainter.State.Loading -> {
                // Show loading indicator while image is being fetched
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                // Show placeholder icon when image fails to load
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image_placeholder),
                        contentDescription = "Image placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is AsyncImagePainter.State.Success -> {
                // Show the successfully loaded image
                SubcomposeAsyncImageContent()
            }
            is AsyncImagePainter.State.Empty -> {
                // Show placeholder for empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image_placeholder),
                        contentDescription = "Image placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
