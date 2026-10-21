package com.example.dayquest.domain.time

import com.example.dayquest.data.model.ScheduleType
import org.junit.Assert.*
import org.junit.Test

class NaturalTimeParserTest {

    @Test
    fun testExplicit12HourEnglish() {
        val result = NaturalTimeParser.parse("gym by 6pm")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("18:00", result.targetTime24h)
        assertEquals("Health & Fitness", result.suggestedCategoryId)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun testColonTimeEnglish() {
        val result = NaturalTimeParser.parse("finish assignment by 8:30 pm")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("20:30", result.targetTime24h)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun testMorningTimeEnglish() {
        val result = NaturalTimeParser.parse("workout at 7 am")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("07:00", result.targetTime24h)
        assertEquals("Health & Fitness", result.suggestedCategoryId)
    }

    @Test
    fun testDurationOnlyEnglish() {
        val result = NaturalTimeParser.parse("study physics 2 hours")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(120, result.durationMinutes)
        assertEquals("Study", result.suggestedCategoryId)
    }

    @Test
    fun testMinutesDurationEnglish() {
        val result = NaturalTimeParser.parse("revise calculus for 45 minutes")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(45, result.durationMinutes)
        assertEquals("Study", result.suggestedCategoryId)
    }

    @Test
    fun testAbbreviatedDurationEnglish() {
        val result = NaturalTimeParser.parse("read for 15 min")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(15, result.durationMinutes)
    }

    @Test
    fun testTomorrowRelativeDateEnglish() {
        val result = NaturalTimeParser.parse("submit form tomorrow by 10am")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("10:00", result.targetTime24h)
        assertEquals(1, result.dateOffsetDays)
    }

    @Test
    fun testDecimalDuration() {
        val result = NaturalTimeParser.parse("practice coding for 2.5 hours")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(150, result.durationMinutes)
        assertEquals("Skill/Coding", result.suggestedCategoryId)
    }

    @Test
    fun testHinglishTimePhrase() {
        val result = NaturalTimeParser.parse("shaam 6 baje tak gym")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("18:00", result.targetTime24h)
        assertEquals("Health & Fitness", result.suggestedCategoryId)
    }

    @Test
    fun testHinglishMorningTime() {
        val result = NaturalTimeParser.parse("subah 7 baje walk")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("07:00", result.targetTime24h)
    }

    @Test
    fun testHinglishDuration() {
        val result = NaturalTimeParser.parse("2 ghante physics padhna hai")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(120, result.durationMinutes)
        assertEquals("Study", result.suggestedCategoryId)
    }

    @Test
    fun testHinglishHalfHour() {
        val result = NaturalTimeParser.parse("aadha ghanta meditation")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(30, result.durationMinutes)
        assertEquals("Mindfulness/Self-care", result.suggestedCategoryId)
    }

    @Test
    fun testHinglishOneAndHalfHour() {
        val result = NaturalTimeParser.parse("dedh ghanta ML padhna")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(90, result.durationMinutes)
        assertEquals("Skill/Coding", result.suggestedCategoryId)
    }

    @Test
    fun testHinglishRelativeTomorrow() {
        val result = NaturalTimeParser.parse("kal 9 baje assignment submit")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("09:00", result.targetTime24h)
        assertEquals(1, result.dateOffsetDays)
    }

    @Test
    fun testHinglishParso() {
        val result = NaturalTimeParser.parse("parso 6 pm meeting prepare")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("18:00", result.targetTime24h)
        assertEquals(2, result.dateOffsetDays)
        assertEquals("Work/Career", result.suggestedCategoryId)
    }

    @Test
    fun testDevanagariHindiEveningTime() {
        val result = NaturalTimeParser.parse("शाम 6 बजे तक जिम")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("18:00", result.targetTime24h)
        assertEquals("Health & Fitness", result.suggestedCategoryId)
    }

    @Test
    fun testDevanagariHindiMorningTime() {
        val result = NaturalTimeParser.parse("सुबह 7 बजे दौड़ना")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("07:00", result.targetTime24h)
        assertEquals("Health & Fitness", result.suggestedCategoryId)
    }

    @Test
    fun testDevanagariHindiDuration() {
        val result = NaturalTimeParser.parse("2 घंटे भौतिकी पढ़नी है")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(120, result.durationMinutes)
        assertEquals("Study", result.suggestedCategoryId)
    }

    @Test
    fun testDevanagariHindiMinutes() {
        val result = NaturalTimeParser.parse("30 मिनट ध्यान करना")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(30, result.durationMinutes)
        assertEquals("Mindfulness/Self-care", result.suggestedCategoryId)
    }

    @Test
    fun testDevanagariHindiTomorrow() {
        val result = NaturalTimeParser.parse("कल सुबह 9 बजे तक रिपोर्ट")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("09:00", result.targetTime24h)
        assertEquals(1, result.dateOffsetDays)
    }

    @Test
    fun testDevanagariHindiLateNight() {
        val result = NaturalTimeParser.parse("रात 10:30 तक पढ़ाई")
        assertEquals(ScheduleType.BY_TIME, result.scheduleType)
        assertEquals("22:30", result.targetTime24h)
        assertEquals("Study", result.suggestedCategoryId)
    }

    @Test
    fun testAmbiguousBy6() {
        val result = NaturalTimeParser.parse("by 6")
        assertTrue(result.isAmbiguous)
        assertNotNull(result.ambiguityReason)
    }

    @Test
    fun testAmbiguousInMinutes() {
        val result = NaturalTimeParser.parse("clean room in 30 minutes")
        assertTrue(result.isAmbiguous)
    }

    @Test
    fun testTimeRangeEnglish() {
        val result = NaturalTimeParser.parse("work from 4 to 5 pm")
        assertEquals(ScheduleType.RANGE, result.scheduleType)
    }

    @Test
    fun testCompoundDuration() {
        val result = NaturalTimeParser.parse("study 1 hour 30 minutes")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(90, result.durationMinutes)
    }

    @Test
    fun testCompoundShortDuration() {
        val result = NaturalTimeParser.parse("read for 1h 20m")
        assertEquals(ScheduleType.ANYTIME, result.scheduleType)
        assertEquals(80, result.durationMinutes)
    }
}
