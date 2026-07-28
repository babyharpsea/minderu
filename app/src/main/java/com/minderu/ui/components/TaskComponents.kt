package com.minderu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.minderu.data.TaskUiModel
import com.minderu.ui.theme.Mauve90

@Composable
fun NativeAdItem(adId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adFailed by remember { mutableStateOf(false) }

    DisposableEffect(adId) {
        var currentAd: NativeAd? = null
        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { ad ->
                // Destroy previous ad before storing new one
                currentAd?.destroy()
                currentAd = ad
                nativeAd = ad
                adFailed = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adFailed = true
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            currentAd?.destroy()
            nativeAd = null
        }
    }

    // Don't render anything if ad failed or hasn't loaded yet
    val ad = nativeAd
    if (ad == null || adFailed) return

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
                // Enforce explicit LayoutParams on NativeAdView container
                val adView = NativeAdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val mainLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                // 1. MediaView for ad media content
                val mediaView = MediaView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(ctx, 130)
                    ).apply {
                        bottomMargin = dpToPx(ctx, 8)
                    }
                }

                // 2. Row containing App Icon + Text Details (Headline & Body)
                val rowLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val iconView = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(ctx, 38),
                        dpToPx(ctx, 38)
                    ).apply {
                        marginEnd = dpToPx(ctx, 8)
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }

                val textLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val headlineView = TextView(ctx).apply {
                    textSize = 14f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(android.graphics.Color.parseColor("#1D1B20"))
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }

                val bodyView = TextView(ctx).apply {
                    textSize = 12f
                    setTextColor(android.graphics.Color.parseColor("#49454F"))
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }

                textLayout.addView(headlineView)
                textLayout.addView(bodyView)

                rowLayout.addView(iconView)
                rowLayout.addView(textLayout)

                // 3. Call to Action Button
                val ctaView = Button(ctx).apply {
                    textSize = 13f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#6750A4"))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(ctx, 8)
                    }
                }

                // Add child views to main layout
                mainLayout.addView(mediaView)
                mainLayout.addView(rowLayout)
                mainLayout.addView(ctaView)

                // Add main layout directly inside NativeAdView
                adView.addView(mainLayout)

                // Register all asset views with NativeAdView
                adView.headlineView = headlineView
                adView.bodyView = bodyView
                adView.iconView = iconView
                adView.mediaView = mediaView
                adView.callToActionView = ctaView

                // Populate and bind ad
                populateNativeAd(ad, adView)

                adView
            },
            update = { adView ->
                populateNativeAd(ad, adView)
            }
        )
    }
}

private fun populateNativeAd(ad: NativeAd, adView: NativeAdView) {
    (adView.headlineView as? TextView)?.text = ad.headline ?: ""
    (adView.bodyView as? TextView)?.text = ad.body ?: ""

    val cta = adView.callToActionView as? Button
    if (ad.callToAction.isNullOrBlank()) {
        cta?.visibility = View.GONE
    } else {
        cta?.text = ad.callToAction
        cta?.visibility = View.VISIBLE
    }

    val icon = adView.iconView as? ImageView
    if (ad.icon != null && ad.icon?.drawable != null) {
        icon?.setImageDrawable(ad.icon?.drawable)
        icon?.visibility = View.VISIBLE
    } else {
        icon?.visibility = View.GONE
    }

    if (ad.mediaContent != null) {
        adView.mediaView?.mediaContent = ad.mediaContent
        adView.mediaView?.visibility = View.VISIBLE
    } else {
        adView.mediaView?.visibility = View.GONE
    }

    adView.setNativeAd(ad)
}

private fun dpToPx(context: android.content.Context, dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}

@Composable
fun TaskCard(
    task: TaskUiModel,
    onTaskCompleted: (TaskUiModel) -> Unit,
    onTaskDeleted: (TaskUiModel) -> Unit,
    onTaskEdit: (TaskUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
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
                    text = if (task.isCompleted) "Nice work" else task.subhead,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RewardBadge(sparks = task.sparks)

            // Overflow menu replaces separate edit/delete buttons
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Task options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onTaskEdit(task)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onTaskDeleted(task)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
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
            contentDescription = if (completed) "Completed — tap to undo" else "Complete task",
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
            .background(Mauve90)
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
            text = "+1–5",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
