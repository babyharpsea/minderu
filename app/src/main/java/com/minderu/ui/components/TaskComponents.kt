package com.minderu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.minderu.data.Task
import com.minderu.data.rewardColor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect

@Composable
fun NativeAdItem(adId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val nativeAd = remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { ad ->
                nativeAd.value = ad
            }
            .withAdListener(object : AdListener() {
                // Optional: handle failures
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
        
        onDispose {
            nativeAd.value?.destroy()
        }
    }

    nativeAd.value?.let { ad ->
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                factory = { ctx ->
                    NativeAdView(ctx).apply {
                        // In a real implementation, you'd inflate a XML layout here
                        // For a quick test, we just show that it's loaded
                    }
                },
                update = { view ->
                    // Set the ad to the view
                }
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sponsor: ${ad.headline}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                ad.body?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onTaskCompleted: (Task) -> Unit,
    onTaskDeleted: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onTaskCompleted(task) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TaskPlayButton(
                completed = task.isCompleted,
                onClick = { onTaskCompleted(task) }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (task.isCompleted) "Nice work" else task.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = { onTaskDeleted(task) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }

            RewardBadge(sparks = task.sparks)
        }
    }
}

@Composable
private fun TaskPlayButton(
    completed: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (completed) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.primaryContainer,
        label = "buttonBackground"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (completed) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        label = "iconColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (completed) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = if (completed) Icons.Outlined.Check else Icons.Outlined.PlayArrow,
            contentDescription = if (completed) "Completed task" else "Start task",
            modifier = Modifier.size(30.dp),
            tint = iconColor
        )
    }
}

@Composable
private fun RewardBadge(
    sparks: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(rewardColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Token,
            contentDescription = "Sparks reward",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = "+$sparks",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
