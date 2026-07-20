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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlin.math.absoluteValue

private val rewardColor = Color(0xFFFFD8E4)
private val binderIllustrationColors = listOf(
    Color(0xFFE8DEF8),
    Color(0xFFFFD8E4),
    Color(0xFFD9EAD3),
    Color(0xFFFFE0B2),
    Color(0xFFD7E3FC),
    Color(0xFFF5D0FE)
)

private val mockBinderCards = listOf(
    BinderCard("rooty", "Compañero Rooty", CardRarity.Common, Icons.Outlined.Park),
    BinderCard("focus", "Focus Sprout", CardRarity.Rare, Icons.Outlined.AutoAwesome),
    BinderCard("calm", "Calm Current", CardRarity.Common, Icons.Outlined.WaterDrop),
    BinderCard("sunbeam", "Sunbeam Buddy", CardRarity.Epic, Icons.Outlined.WbSunny),
    BinderCard("nest", "Tiny Nest", CardRarity.Common, Icons.Outlined.Home),
    BinderCard("spark", "Spark Keeper", CardRarity.Rare, Icons.Outlined.Token)
)

private val mockTasks = listOf(
    Task(
        id = "closet",
        title = "Put 3 clothes in your closet",
        subtitle = "One tiny step",
        sparks = 3
    ),
    Task(
        id = "water",
        title = "Drink a glass of water",
        subtitle = "One tiny step",
        sparks = 2
    ),
    Task(
        id = "surface",
        title = "Clear one surface",
        subtitle = "One tiny step",
        sparks = 3
    ),
    Task(
        id = "window",
        title = "Open the window",
        subtitle = "One tiny step",
        sparks = 1
    ),
    Task(
        id = "thought",
        title = "Write down one thought",
        subtitle = "One tiny step",
        sparks = 2
    )
)

data class Task(
    val id: String,
    val title: String,
    val subtitle: String,
    val sparks: Int,
    val isCompleted: Boolean = false
)

data class BinderCard(
    val id: String,
    val title: String,
    val rarity: CardRarity,
    val illustration: ImageVector
)

enum class CardRarity {
    Common,
    Rare,
    Epic
}

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
                MinderuApp()
            }
        }
    }
}

@Composable
fun MinderuApp(
    initialTasks: List<Task> = mockTasks,
    modifier: Modifier = Modifier
) {
    val tasksState = remember { mutableStateOf(initialTasks) }
    val selectedDestination = remember { mutableStateOf(MinderuDestination.Dashboard) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FloatingNavDock(
                selectedDestination = selectedDestination.value,
                onDestinationSelected = { selectedDestination.value = it }
            )
        }
    ) { innerPadding ->
        when (selectedDestination.value) {
            MinderuDestination.Binder -> CardBinderScreen(
                cards = mockBinderCards,
                sparksBalance = 350,
                contentPadding = innerPadding
            )
            MinderuDestination.Dashboard,
            MinderuDestination.Impact -> TodayTasksScreen(
                tasks = tasksState.value,
                contentPadding = innerPadding,
                onTaskCompleted = { task ->
                    tasksState.value = tasksState.value.map { currentTask ->
                        if (currentTask.id == task.id) {
                            currentTask.copy(isCompleted = !currentTask.isCompleted)
                        } else {
                            currentTask
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun TodayTasksScreen(
    tasks: List<Task>,
    contentPadding: PaddingValues,
    onTaskCompleted: (Task) -> Unit,
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
        items(tasks, key = { it.id }) { task ->
            TaskCard(task = task, onTaskCompleted = onTaskCompleted)
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onTaskCompleted: (Task) -> Unit,
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
            .background(
                if (completed) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer
            )
    ) {
        Icon(
            imageVector = if (completed) Icons.Outlined.Check else Icons.Outlined.PlayArrow,
            contentDescription = if (completed) "Completed task" else "Start task",
            modifier = Modifier.size(30.dp),
            tint = if (completed) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer
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
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit tasks"
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

@Composable
fun CardBinderScreen(
    cards: List<BinderCard>,
    sparksBalance: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 32.dp,
            end = 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            BinderHeader(sparksBalance = sparksBalance)
        }
        items(cards, key = { it.id }) { card ->
            CollectibleCard(card = card)
        }
    }
}

@Composable
private fun BinderHeader(
    sparksBalance: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Your Binder",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Token,
                contentDescription = "Sparks",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = sparksBalance.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun CollectibleCard(
    card: BinderCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(
                        binderIllustrationColors[
                            card.id.hashCode().absoluteValue % binderIllustrationColors.size
                        ]
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.illustration,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = card.rarity.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MinderuPreview() {
    MinderuTheme {
        MinderuApp()
    }
}
