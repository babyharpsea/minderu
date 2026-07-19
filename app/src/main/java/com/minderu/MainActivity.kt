package com.minderu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minderu.ui.theme.MinderuTheme

private val rewardColor = Color(0xFFFFD8E4)

private val sampleTasks = listOf(
    Task("Put 3 clothes in your closet", 3, true),
    Task("Drink a glass of water", 2, false),
    Task("Clear one surface", 3, false),
    Task("Open the window", 1, false),
    Task("Write down one thought", 2, false)
)

data class Task(
    val title: String,
    val sparks: Int,
    val completed: Boolean
)

enum class MinderuDestination(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Outlined.Home),
    Binder("Card binder", Icons.Outlined.Style),
    Impact("Impact", Icons.Outlined.Park)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinderuTheme {
                MinderuApp(tasks = sampleTasks)
            }
        }
    }
}

@Composable
fun MinderuApp(
    tasks: List<Task>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FloatingNavDock(
                selectedDestination = MinderuDestination.Dashboard,
                onDestinationSelected = {}
            )
        }
    ) { innerPadding ->
        TodayTasksScreen(
            tasks = tasks,
            contentPadding = innerPadding
        )
    }
}

@Composable
fun TodayTasksScreen(
    tasks: List<Task>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 32.dp,
            end = 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Minderu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Today’s tasks",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 40.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Thin,
                        lineHeight = 44.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Small steps make a real difference.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(tasks, key = { it.title }) { task ->
            TaskCard(task = task, onClick = {})
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
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
                completed = task.completed,
                onClick = onClick
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
                    text = if (task.completed) "Nice work" else "One tiny step",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Icon(
            imageVector = if (completed) Icons.Outlined.Check else Icons.Outlined.PlayArrow,
            contentDescription = if (completed) "Completed task" else "Start task",
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
fun FloatingNavDock(
    selectedDestination: MinderuDestination,
    onDestinationSelected: (MinderuDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MinderuDestination.entries.forEach { destination ->
                    NavigationDockItem(
                        destination = destination,
                        selected = destination == selectedDestination,
                        onClick = { onDestinationSelected(destination) }
                    )
                }
                Spacer(Modifier.width(4.dp))
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Add task"
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationDockItem(
    destination: MinderuDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                else Modifier
            )
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            modifier = Modifier.size(24.dp),
            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MinderuPreview() {
    MinderuTheme {
        MinderuApp(tasks = sampleTasks)
    }
}
