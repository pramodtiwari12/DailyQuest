package com.example.dayquest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AvatarPersona(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
    val subtitle: String
)

object AvatarRoster {
    val ALL = listOf(
        AvatarPersona(
            id = "vanguard",
            name = "Vanguard",
            primaryColor = Color(0xFF5B5BD6),
            secondaryColor = Color(0xFF3730A3),
            icon = Icons.Default.Shield,
            subtitle = "Resilient & Bold"
        ),
        AvatarPersona(
            id = "scholar",
            name = "Scholar",
            primaryColor = Color(0xFF2563EB),
            secondaryColor = Color(0xFF0D9488),
            icon = Icons.Default.AutoStories,
            subtitle = "Knowledge & Focus"
        ),
        AvatarPersona(
            id = "artificer",
            name = "Artificer",
            primaryColor = Color(0xFFD97706),
            secondaryColor = Color(0xFFEA580C),
            icon = Icons.Default.PrecisionManufacturing,
            subtitle = "Craft & Invention"
        ),
        AvatarPersona(
            id = "sentinel",
            name = "Sentinel",
            primaryColor = Color(0xFF059669),
            secondaryColor = Color(0xFF047857),
            icon = Icons.Default.Security,
            subtitle = "Guardian of Habit"
        ),
        AvatarPersona(
            id = "chronicler",
            name = "Chronicler",
            primaryColor = Color(0xFF7C3AED),
            secondaryColor = Color(0xFF6D28D9),
            icon = Icons.Default.HistoryEdu,
            subtitle = "Memory & Precision"
        ),
        AvatarPersona(
            id = "ranger",
            name = "Ranger",
            primaryColor = Color(0xFF10B981),
            secondaryColor = Color(0xFF0284C7),
            icon = Icons.Default.Explore,
            subtitle = "Pathfinder & Speed"
        )
    )

    fun fromId(id: String?): AvatarPersona {
        return ALL.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL[0]
    }
}

@Composable
fun DayQuestAvatar(
    avatarId: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    showRing: Boolean = true
) {
    val persona = AvatarRoster.fromId(avatarId)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(persona.primaryColor, persona.secondaryColor)
                )
            )
            .then(
                if (showRing) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Geometric patterned backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(0f, 0f),
                end = Offset(w, h),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = w * 0.38f,
                center = Offset(w / 2f, h / 2f)
            )
        }

        Icon(
            imageVector = persona.icon,
            contentDescription = persona.name,
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}
