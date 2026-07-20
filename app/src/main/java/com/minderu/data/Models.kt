package com.minderu.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// --- Navigation ---

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Binder : Screen("binder")
    object Impact : Screen("impact")
}

// --- Data Models (DTOs) ---

@Serializable
data class RoutineDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class TaskDto(
    val id: String? = null,
    @SerialName("routine_id") val routineId: String,
    @SerialName("position_index") val positionIndex: Int,
    val title: String,
    val subhead: String,
    @SerialName("sparks_reward") val sparksReward: Int
)

@Serializable
data class ProfileDto(
    val id: String,
    val sparks: Int,
    @SerialName("is_premium") val isPremium: Boolean = false
)

// --- UI Models ---

data class TaskUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val sparks: Int,
    val positionIndex: Int = 0,
    val isCompleted: Boolean = false
)

fun TaskDto.toUiModel(isCompleted: Boolean = false): TaskUiModel = TaskUiModel(
    id = requireNotNull(id) { "TaskDto.id required for UI mapping" },
    title = title,
    subtitle = subhead,
    sparks = sparksReward,
    positionIndex = positionIndex,
    isCompleted = isCompleted
)

@Serializable
data class BinderCard(
    val id: String,
    val title: String,
    val rarity: CardRarity,
    @Transient val illustration: ImageVector = Icons.Outlined.Token
)

@Serializable
data class Planting(
    val id: String,
    val species: String,
    val location: String,
    val date: String
)

// --- Enums ---

@Serializable
enum class CardRarity {
    Common,
    Rare,
    Epic
}

enum class MinderuDestination(val label: String, val icon: ImageVector, val screen: Screen) {
    Dashboard("Dashboard", Icons.Outlined.Home, Screen.Dashboard),
    Binder("Card binder", Icons.Outlined.Style, Screen.Binder),
    Impact("Impact", Icons.Outlined.Park, Screen.Impact)
}

// --- Colors & Mock Data ---

val rewardColor = Color(0xFFFFD8E4)
val binderIllustrationColors = listOf(
    Color(0xFFE8DEF8),
    Color(0xFFFFD8E4),
    Color(0xFFD9EAD3),
    Color(0xFFFFE0B2),
    Color(0xFFD7E3FC),
    Color(0xFFF5D0FE)
)

val mockBinderCards = listOf(
    BinderCard("rooty", "Compañero Rooty", CardRarity.Common, Icons.Outlined.Park),
    BinderCard("focus", "Focus Sprout", CardRarity.Rare, Icons.Outlined.AutoAwesome),
    BinderCard("calm", "Calm Current", CardRarity.Common, Icons.Outlined.WaterDrop),
    BinderCard("sunbeam", "Sunbeam Buddy", CardRarity.Epic, Icons.Outlined.WbSunny),
    BinderCard("nest", "Tiny Nest", CardRarity.Common, Icons.Outlined.Home),
    BinderCard("spark", "Spark Keeper", CardRarity.Rare, Icons.Outlined.Token)
)

data class DefaultTaskSeed(
    val title: String,
    val subhead: String,
    val sparksReward: Int
)

val defaultTaskSeeds = listOf(
    DefaultTaskSeed("Put 3 clothes in your closet", "One tiny step", 3),
    DefaultTaskSeed("Drink a glass of water", "One tiny step", 2),
    DefaultTaskSeed("Clear one surface", "One tiny step", 3),
    DefaultTaskSeed("Open the window", "One tiny step", 1),
    DefaultTaskSeed("Write down one thought", "One tiny step", 2)
)

/** @deprecated Use [defaultTaskSeeds] for DB seeding; kept for previews. */
val mockTasks = defaultTaskSeeds.mapIndexed { index, seed ->
    TaskUiModel(
        id = "preview-$index",
        title = seed.title,
        subtitle = seed.subhead,
        sparks = seed.sparksReward,
        positionIndex = index
    )
}

val mockPlantings = listOf(
    Planting("mangrove-madagascar", "Mangrove", "Madagascar", "July 20"),
    Planting("oak-california", "Coast Live Oak", "California", "July 12"),
    Planting("cedar-lebanon", "Cedar", "Lebanon", "June 28"),
    Planting("baobab-kenya", "Baobab", "Kenya", "June 14")
)
