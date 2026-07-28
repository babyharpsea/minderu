package com.minderu.data

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

// --- Navigation ---

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Binder : Screen("binder")
    object Impact : Screen("impact")
}

// --- Remote DTOs (1:1 with PostgreSQL) ---

@Serializable
data class ProfileDto(
    val id: String,
    val sparks: Int = 0,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("max_streak") val maxStreak: Int = 0,
    @SerialName("last_active_date") val lastActiveDate: String? = null
)

@Serializable
data class RoutineDto(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("routine_id") val routineId: String,
    @SerialName("position_index") val positionIndex: Int,
    val title: String,
    val subhead: String? = "",
    @SerialName("sparks_reward") val sparksReward: Int
)

@Serializable
data class PlantedTreeDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val provider: String? = null,
    @SerialName("external_receipt_id") val externalReceiptId: String? = null,
    @SerialName("tree_species") val treeSpecies: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("planted_at") val plantedAt: String? = null,
    @SerialName("is_individual") val isIndividual: Boolean = false,
    @SerialName("sparks_spent") val sparksSpent: Int = 400,
    @SerialName("certificate_url") val certificateUrl: String? = null
)

@Serializable
data class AdBoostLogDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("sparks_awarded") val sparksAwarded: Int = 10,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TaskCompletionDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("task_id") val taskId: String = "",
    @SerialName("completed_at") val completedAt: String? = null
)

@Serializable
enum class CardRarity {
    @SerialName("common") COMMON,
    @SerialName("rare") RARE,
    @SerialName("ultra_rare") ULTRA_RARE,
    @SerialName("legendary") LEGENDARY;

    val displayName: String
        get() = when (this) {
            COMMON -> "Common"
            RARE -> "Rare"
            ULTRA_RARE -> "Ultra Rare"
            LEGENDARY -> "Legendary"
        }

    val costSparks: Int
        get() = when (this) {
            COMMON -> 10
            RARE -> 25
            ULTRA_RARE -> 50
            LEGENDARY -> 100
        }
}

@Serializable
data class CardDto(
    val id: Int,
    val title: String = "",
    val description: String? = "",
    val rarity: CardRarity = CardRarity.COMMON,
    @SerialName("asset_url") val assetUrl: String? = "",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserInventoryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("card_id") val cardId: Int,
    val quantity: Int = 1,
    @SerialName("is_shiny") val isShiny: Boolean = false,
    @SerialName("acquired_at") val acquiredAt: String? = null
)

// --- UI Presentation Models ---

data class TaskUiModel(
    val id: String,
    val title: String,
    val subhead: String,
    val sparks: Int,
    val isCompleted: Boolean = false
)

data class CardUiModel(
    val id: Int,
    val title: String,
    val description: String,
    val rarity: CardRarity,
    val rawAssetUrl: String?,
    val sanitizedImageUrl: String?,
    val costSparks: Int,
    val isOwned: Boolean = false,
    val quantity: Int = 0
)

object ImageUrlSanitizer {
    private const val DEFAULT_SUPABASE_STORAGE_URL = "https://omozxnmdptgeyxspgcro.supabase.co/storage/v1/object/public/cards/"

    fun sanitize(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
        if (trimmed.isEmpty()) return null

        return try {
            when {
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true) -> trimmed

                trimmed.startsWith("/") -> "https://omozxnmdptgeyxspgcro.supabase.co$trimmed"

                else -> "$DEFAULT_SUPABASE_STORAGE_URL$trimmed"
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Legacy model for mock fallbacks.
 */
data class BinderCard(
    val id: String,
    val title: String,
    val rarity: CardRarity,
    val illustration: ImageVector = Icons.Outlined.Token
)

@Serializable
data class Planting(
    val id: String,
    val species: String,
    val location: String,
    val date: String
)

enum class MinderuDestination(val label: String, val icon: ImageVector, val screen: Screen) {
    Dashboard("Dashboard", Icons.Outlined.Home, Screen.Dashboard),
    Binder("Card binder", Icons.Outlined.Style, Screen.Binder),
    Impact("Impact", Icons.Outlined.Park, Screen.Impact)
}

// --- Mock Data ---

val mockBinderCards = listOf(
    BinderCard("rooty", "Compañero Rooty", CardRarity.COMMON, Icons.Outlined.Park),
    BinderCard("focus", "Focus Sprout", CardRarity.RARE, Icons.Outlined.AutoAwesome),
    BinderCard("calm", "Calm Current", CardRarity.COMMON, Icons.Outlined.WaterDrop),
    BinderCard("sunbeam", "Sunbeam Buddy", CardRarity.LEGENDARY, Icons.Outlined.WbSunny),
    BinderCard("nest", "Tiny Nest", CardRarity.COMMON, Icons.Outlined.Home),
    BinderCard("spark", "Spark Keeper", CardRarity.RARE, Icons.Outlined.Token)
)

val mockPlantings = listOf(
    Planting("mangrove-madagascar", "Mangrove", "Madagascar", "July 20"),
    Planting("oak-california", "Coast Live Oak", "California", "July 12"),
    Planting("cedar-lebanon", "Cedar", "Lebanon", "June 28"),
    Planting("baobab-kenya", "Baobab", "Kenya", "June 14")
)
