package com.example.ui.components

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val clockFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
}

private val weekdayFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("EEE", Locale.getDefault())
}

private val shortDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
}

private val dayHeaderThisYearFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
}

private val dayHeaderFullFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
}

private val scratchCalA = object : ThreadLocal<Calendar>() {
    override fun initialValue(): Calendar = Calendar.getInstance()
}

private val scratchCalB = object : ThreadLocal<Calendar>() {
    override fun initialValue(): Calendar = Calendar.getInstance()
}

fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val diffMs = System.currentTimeMillis() - date.time
    val minutes = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    val days = diffMs / 86_400_000L

    return when {
        minutes < 1L -> "Just now"
        minutes < 60L -> "${minutes}m"
        hours < 24L -> "${hours}h"
        days < 7L -> (weekdayFormatter.get() ?: SimpleDateFormat("EEE", Locale.getDefault())).format(date)
        else -> (shortDateFormatter.get() ?: SimpleDateFormat("MMM d", Locale.getDefault())).format(date)
    }
}

fun formatMessageClock(timestamp: Timestamp?): String {
    val date: Date = timestamp?.toDate() ?: return "Sending…"
    return (clockFormatter.get() ?: SimpleDateFormat("h:mm a", Locale.getDefault())).format(date)
}

fun formatDayHeader(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "Today"
    val cal = (scratchCalA.get() ?: Calendar.getInstance()).apply { timeInMillis = date.time }
    val today = (scratchCalB.get() ?: Calendar.getInstance()).apply { timeInMillis = System.currentTimeMillis() }

    val targetYear = cal.get(Calendar.YEAR)
    val targetDay = cal.get(Calendar.DAY_OF_YEAR)
    val todayYear = today.get(Calendar.YEAR)
    val todayDay = today.get(Calendar.DAY_OF_YEAR)

    if (targetYear == todayYear && targetDay == todayDay) return "Today"

    today.add(Calendar.DAY_OF_YEAR, -1)
    if (targetYear == today.get(Calendar.YEAR) && targetDay == today.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday"
    }

    return if (targetYear == todayYear) {
        (dayHeaderThisYearFormatter.get() ?: SimpleDateFormat("EEE, MMM d", Locale.getDefault())).format(date)
    } else {
        (dayHeaderFullFormatter.get() ?: SimpleDateFormat("MMM d, yyyy", Locale.getDefault())).format(date)
    }
}

fun isSameCalendarDay(first: Timestamp?, second: Timestamp?): Boolean {
    if (first == null && second == null) return true
    if (first == null || second == null) return false
    val c1 = (scratchCalA.get() ?: Calendar.getInstance()).apply { timeInMillis = first.toDate().time }
    val c2 = (scratchCalB.get() ?: Calendar.getInstance()).apply { timeInMillis = second.toDate().time }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
