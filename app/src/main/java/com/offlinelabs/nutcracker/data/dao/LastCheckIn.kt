package com.offlinelabs.nutcracker.data.dao

/**
 * Most recent check-in of a meal or exercise ([id]); [lastCheckIn] is the stored
 * "yyyy-MM-dd HH:mm:ss" text, so it sorts chronologically as a string.
 */
data class LastCheckIn(
    val id: Long,
    val lastCheckIn: String?
)
