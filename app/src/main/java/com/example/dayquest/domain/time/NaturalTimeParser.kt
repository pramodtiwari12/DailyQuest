package com.example.dayquest.domain.time

import com.example.dayquest.data.model.ScheduleType
import java.util.regex.Pattern

data class ParsedTimeResult(
    val originalText: String,
    val cleanedTitle: String,
    val scheduleType: ScheduleType,
    val isAmbiguous: Boolean,
    val ambiguityReason: String? = null,
    val targetTime24h: String? = null, // e.g. "18:00"
    val durationMinutes: Int? = null,
    val dateOffsetDays: Int = 0,
    val suggestedCategoryId: String = "Other",
    val suggestedDifficulty: String = "Medium"
)

object NaturalTimeParser {

    fun parse(input: String): ParsedTimeResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedTimeResult(
                originalText = "",
                cleanedTitle = "",
                scheduleType = ScheduleType.ANYTIME,
                isAmbiguous = false
            )
        }

        var workingText = trimmed
        var dateOffset = 0
        var scheduleType = ScheduleType.ANYTIME
        var targetTime24h: String? = null
        var durationMinutes: Int? = null
        var isAmbiguous = false
        var ambiguityReason: String? = null

        // 1. Detect relative date offset: "tomorrow", "kal", "कल", "parso", "परसों"
        val lower = trimmed.lowercase()
        when {
            lower.contains("parso") || lower.contains("परसों") || lower.contains("day after tomorrow") -> {
                dateOffset = 2
                workingText = workingText.replace(Regex("(?i)\\b(parso|day after tomorrow)\\b|परसों"), "").trim()
            }
            lower.contains("tomorrow") || lower.contains("kal ") || lower.startsWith("kal") || lower.contains("कल") -> {
                dateOffset = 1
                workingText = workingText.replace(Regex("(?i)\\b(tomorrow|kal)\\b|कल"), "").trim()
            }
        }

        // 2. Check for Ambiguous cases per acceptance spec:
        // "by 6" (no AM/PM)
        if (Regex("(?i)\\bby\\s+([1-9]|1[0-2])\\b(?!\\s*([:ap]|pm|am|hour|min))").containsMatchIn(workingText)) {
            isAmbiguous = true
            ambiguityReason = "AM/PM is ambiguous (e.g. 6 AM vs 6 PM)"
        }
        // "in 20 minutes" or "clean room in 30 minutes"
        if (Regex("(?i)\\bin\\s+\\d+\\s*(minutes?|mins?)\\b").containsMatchIn(workingText)) {
            isAmbiguous = true
            ambiguityReason = "Relative due time vs task duration is ambiguous"
        }
        // Hindi ambiguous fractions without time of day: "paune saat", "sawa 8"
        if (Regex("(?i)\\b(paune|sawa)\\s+\\w+").containsMatchIn(workingText) &&
            !Regex("(?i)(subah|shaam|raat|dopahar)").containsMatchIn(workingText)) {
            isAmbiguous = true
            ambiguityReason = "AM/PM is ambiguous without context"
        }

        // 3. Time Ranges: e.g. "4 to 5 pm", "6-7pm", "2 se 3 baje", "दोपहर 2 से 3 बजे"
        val rangeRegex = Regex("(?i)(\\d{1,2})\\s*(?:to|-|se|से)\\s*(\\d{1,2})\\s*(pm|am|baje|बजे)?")
        if (rangeRegex.containsMatchIn(workingText) && !isAmbiguous) {
            scheduleType = ScheduleType.RANGE
            workingText = workingText.replace(rangeRegex, "").replace(Regex("(?i)\\bwork from\\b"), "").trim()
        }

        // 4. Check for By Time expressions
        if (!isAmbiguous && scheduleType != ScheduleType.RANGE) {
            // Hindi / Devanagari / Hinglish specific patterns:
            // "रात 10:30 तक", "रात 8 बजे", "शाम 6 बजे तक", "सुबह 7 बजे", "dopahar 1 baje", "kal 9 baje"
            val hindiTimeRegex = Regex("(?:(रात|शाम|सुबह|दोपहर|raat|shaam|subah|dopahar)\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(बजे|baje)(?:\\s*tak|\\s*तक)?|(रात|शाम|सुबह|दोपहर|raat|shaam|subah|dopahar)\\s*(\\d{1,2})(?::(\\d{2}))?(?:\\s*tak|\\s*तक)?", RegexOption.IGNORE_CASE)
            val hindiMatch = hindiTimeRegex.find(workingText)

            if (hindiMatch != null) {
                val period = (hindiMatch.groupValues[1].ifEmpty { hindiMatch.groupValues[5] }).lowercase()
                val hourStr = hindiMatch.groupValues[2].ifEmpty { hindiMatch.groupValues[6] }
                val minStr = hindiMatch.groupValues[3].ifEmpty { hindiMatch.groupValues[7] }
                var hour = hourStr.toIntOrNull() ?: 0
                val minute = if (minStr.isNotEmpty()) minStr.toIntOrNull() ?: 0 else 0

                when {
                    (period == "रात" || period == "raat" || period == "शाम" || period == "shaam") && hour < 12 -> hour += 12
                    (period == "दोपहर" || period == "dopahar") && hour in 1..5 -> hour += 12
                }
                targetTime24h = String.format("%02d:%02d", hour, minute)
                scheduleType = ScheduleType.BY_TIME
                workingText = workingText.replace(hindiMatch.value, "").trim()
            } else {
                // English 12-hour/colon patterns:
                // "by 6:30 PM", "by 6pm", "at 7 am", "8:30 pm", "7:05 am", "at 8 tonight", "10am"
                val englishTimeRegex = Regex("(?i)(?:\\b(?:by|at)\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|tonight)\\b|(?:\\bby\\s+)(\\d{1,2}):(\\d{2})")
                val engMatch = englishTimeRegex.find(workingText)

                if (engMatch != null) {
                    var hour = engMatch.groupValues[1].toIntOrNull() ?: engMatch.groupValues[4].toIntOrNull() ?: 0
                    val minute = engMatch.groupValues[2].toIntOrNull() ?: engMatch.groupValues[5].toIntOrNull() ?: 0
                    val modifier = engMatch.groupValues[3].lowercase()

                    when {
                        (modifier == "pm" || modifier == "tonight") && hour < 12 -> hour += 12
                        modifier == "am" && hour == 12 -> hour = 0
                    }
                    targetTime24h = String.format("%02d:%02d", hour, minute)
                    scheduleType = ScheduleType.BY_TIME
                    workingText = workingText.replace(engMatch.value, "").trim()
                }
            }
        }

        // 5. Check for Durations:
        // "2 hours", "45 minutes", "15 min", "2.5 hours", "1h 20m", "1 hour 30 minutes"
        // "2 ghante", "30 minute", "aadha ghanta" (30), "dedh ghanta" (90), "2 घंटे", "30 मिनट", "डेढ़ घंटे"
        if (!isAmbiguous && durationMinutes == null) {
            when {
                Regex("(?i)\\b(aadha ghanta|half hour)\\b|आधा घंटा").containsMatchIn(workingText) -> {
                    durationMinutes = 30
                    workingText = workingText.replace(Regex("(?i)\\b(aadha ghanta|half hour)\\b|आधा घंटा"), "").trim()
                }
                Regex("(?i)\\b(dedh ghanta)\\b|डेढ़ घंटे").containsMatchIn(workingText) -> {
                    durationMinutes = 90
                    workingText = workingText.replace(Regex("(?i)\\b(dedh ghanta)\\b|डेढ़ घंटे"), "").trim()
                }
                Regex("(?i)\\b(ek ghanta|one hour)\\b|एक घंटा").containsMatchIn(workingText) -> {
                    durationMinutes = 60
                    workingText = workingText.replace(Regex("(?i)\\b(ek ghanta|one hour)\\b|एक घंटा"), "").trim()
                }
                Regex("(?i)\\bquarter hour\\b").containsMatchIn(workingText) -> {
                    durationMinutes = 15
                    workingText = workingText.replace(Regex("(?i)\\bquarter hour\\b"), "").trim()
                }
                else -> {
                    // Compound duration: "1h 20m" or "1 hour 30 minutes"
                    val compoundRegex = Regex("(?i)(\\d+)\\s*(?:h|hours?)\\s*(?:and\\s*)?(\\d+)\\s*(?:m|mins?|minutes?)")
                    val compMatch = compoundRegex.find(workingText)
                    if (compMatch != null) {
                        val h = compMatch.groupValues[1].toIntOrNull() ?: 0
                        val m = compMatch.groupValues[2].toIntOrNull() ?: 0
                        durationMinutes = (h * 60) + m
                        workingText = workingText.replace(compMatch.value, "").trim()
                    } else {
                        // Decimal hours: "2.5 hours"
                        val decRegex = Regex("(?i)(\\d+\\.\\d+)\\s*(?:hours?|hrs?)")
                        val decMatch = decRegex.find(workingText)
                        if (decMatch != null) {
                            val h = decMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                            durationMinutes = (h * 60.0).toInt()
                            workingText = workingText.replace(decMatch.value, "").trim()
                        } else {
                            // Simple hours: "2 hours", "2 ghante", "2 घंटे"
                            val hoursRegex = Regex("(?i)(\\d+)\\s*(?:hours?|hrs?|ghante|घंटे|ghanta)")
                            val hourMatch = hoursRegex.find(workingText)
                            if (hourMatch != null) {
                                val h = hourMatch.groupValues[1].toIntOrNull() ?: 0
                                durationMinutes = h * 60
                                workingText = workingText.replace(hourMatch.value, "").trim()
                            } else {
                                // Minutes: "45 minutes", "15 min", "120 min", "30 minute", "30 मिनट"
                                val minRegex = Regex("(?i)(\\d+)\\s*(?:minutes?|mins?|minute|मिनट)")
                                val minMatch = minRegex.find(workingText)
                                if (minMatch != null) {
                                    durationMinutes = minMatch.groupValues[1].toIntOrNull() ?: 30
                                    workingText = workingText.replace(minMatch.value, "").trim()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clean extra connectors like "for", "by", "at", "tak", "tak", "padhna hai" from remaining title
        var cleaned = workingText
            .replace(Regex("(?i)\\b(for|by|at|tak|tak)\\b|तक"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) {
            cleaned = trimmed
        }

        // Suggest Category based on domain lexicon
        val suggestedCat = guessCategory(cleaned.ifEmpty { trimmed })
        val suggestedDiff = when {
            (durationMinutes ?: 30) >= 90 -> "Hard"
            (durationMinutes ?: 30) >= 45 -> "Medium"
            else -> "Easy"
        }

        return ParsedTimeResult(
            originalText = trimmed,
            cleanedTitle = cleaned,
            scheduleType = scheduleType,
            isAmbiguous = isAmbiguous,
            ambiguityReason = ambiguityReason,
            targetTime24h = targetTime24h,
            durationMinutes = durationMinutes,
            dateOffsetDays = dateOffset,
            suggestedCategoryId = suggestedCat,
            suggestedDifficulty = suggestedDiff
        )
    }

    private fun guessCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.containsAny("gym", "जिम", "run", "running", "workout", "push-up", "yoga", "walk", "stretch", "fitness", "water", "meal", "sleep", "दौड़", "कसरत", "steps") -> "Health & Fitness"
            lower.containsAny("code", "coding", "python", "c++", "leetcode", "sql", "flutter", "git", "api", "neural", "debug", "function", "ml", "dsa") -> "Skill/Coding"
            lower.containsAny("study", "read", "revise", "chapter", "physics", "calculus", "math", "chemistry", "essay", "exam", "notes", "गणित", "भौतिकी", "पढ", "padh") -> "Study"
            lower.containsAny("email", "resume", "job", "interview", "portfolio", "proposal", "meeting", "report", "linkedin", "work", "office") -> "Work/Career"
            lower.containsAny("budget", "expense", "bank", "bill", "saving", "finance", "invest", "spending", "खर्च", "बचत", "paisa", "rupee") -> "Finance"
            lower.containsAny("call", "parents", "friend", "sibling", "dinner", "family", "relative", "outing", "माता-पिता", "दोस्त", "phone") -> "Family & Social"
            lower.containsAny("meditat", "breathing", "gratitude", "journal", "reflection", "self-care", "detox", "affirmation", "ध्यान", "साँस") -> "Mindfulness/Self-care"
            lower.containsAny("clean", "organize", "wash", "room", "laundry", "desk", "groceries", "kitchen", "wardrobe", "shelf", "साफ", "कपड़े") -> "Home & Chores"
            lower.containsAny("sketch", "music", "draw", "melody", "guitar", "story", "podcast", "illustration", "photo", "blog", "रचना") -> "Creativity/Hobby"
            lower.containsAny("movie", "documentary", "game", "comic", "netflix", "relax", "play", "film", "khel") -> "Rest/Entertainment"
            else -> "Other"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        for (kw in keywords) {
            if (this.contains(kw, ignoreCase = true)) return true
        }
        return false
    }
}
