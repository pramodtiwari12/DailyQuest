package com.example.dayquest.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dayquest.ui.theme.*

data class DayQuestCategory(
    val id: String,
    val name: String,
    val color: Color,
    val icon: ImageVector,
    val description: String
)

object Categories {
    val STUDY = DayQuestCategory(
        id = "Study",
        name = "Study",
        color = CategoryStudy,
        icon = Icons.Default.Book,
        description = "Academics, reading, formulas, notes & exam prep"
    )
    val WORK = DayQuestCategory(
        id = "Work/Career",
        name = "Work / Career",
        color = CategoryWork,
        icon = Icons.Default.Work,
        description = "Professional emails, resumes, projects & meetings"
    )
    val HEALTH = DayQuestCategory(
        id = "Health & Fitness",
        name = "Health & Fitness",
        color = CategoryHealth,
        icon = Icons.Default.DirectionsRun,
        description = "Workouts, runs, hydration, yoga & sleep"
    )
    val SKILL = DayQuestCategory(
        id = "Skill/Coding",
        name = "Skill / Coding",
        color = CategorySkill,
        icon = Icons.Default.Code,
        description = "Programming, algorithms, LeetCode & tools"
    )
    val FINANCE = DayQuestCategory(
        id = "Finance",
        name = "Finance",
        color = CategoryFinance,
        icon = Icons.Default.AccountBalance,
        description = "Budgets, expenses, savings & bills"
    )
    val FAMILY = DayQuestCategory(
        id = "Family & Social",
        name = "Family & Social",
        color = CategoryFamily,
        icon = Icons.Default.People,
        description = "Parents, friends, gatherings & calls"
    )
    val MINDFULNESS = DayQuestCategory(
        id = "Mindfulness/Self-care",
        name = "Mindfulness",
        color = CategoryMindfulness,
        icon = Icons.Default.SelfImprovement,
        description = "Meditation, reflections, gratitude & breathing"
    )
    val HOME = DayQuestCategory(
        id = "Home & Chores",
        name = "Home & Chores",
        color = CategoryHome,
        icon = Icons.Default.CleaningServices,
        description = "Cleaning, groceries, laundry & organizing"
    )
    val CREATIVITY = DayQuestCategory(
        id = "Creativity/Hobby",
        name = "Creativity / Hobby",
        color = CategoryCreativity,
        icon = Icons.Default.Palette,
        description = "Sketching, music, writing, photography & crafts"
    )
    val REST = DayQuestCategory(
        id = "Rest/Entertainment",
        name = "Rest / Leisure",
        color = CategoryRest,
        icon = Icons.Default.Movie,
        description = "Gaming, documentaries, music playlists & breaks"
    )
    val OTHER = DayQuestCategory(
        id = "Other",
        name = "Other",
        color = CategoryOther,
        icon = Icons.Default.Category,
        description = "Errands, forms, appointments & reminders"
    )

    val ALL: List<DayQuestCategory> = listOf(
        STUDY, WORK, HEALTH, SKILL, FINANCE, FAMILY,
        MINDFULNESS, HOME, CREATIVITY, REST, OTHER
    )

    fun fromId(id: String): DayQuestCategory {
        return ALL.firstOrNull { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: OTHER
    }
}
