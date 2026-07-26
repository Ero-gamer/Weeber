package org.koitharu.kotatsu.stats.data

/**
 * Snapshot of the user's current reading streak.
 *
 * @property currentStreak Number of consecutive days (including today) with at least
 *                         one reading session. 0 if the user has not read today or yesterday.
 * @property todayDurationMs Total milliseconds read today (since midnight local time).
 * @property goalMinutes     Daily reading goal in minutes set by the user.
 */
data class ReadingStreak(
	val currentStreak: Int,
	val todayDurationMs: Long,
	val goalMinutes: Int,
)
