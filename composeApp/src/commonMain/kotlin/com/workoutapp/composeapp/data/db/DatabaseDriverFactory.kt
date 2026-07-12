package com.workoutapp.composeapp.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates the platform-specific [SqlDriver] backing [com.workoutapp.composeapp.db.AppDatabase].
 * Android drives SQLite via a bundled Context; iOS drives it natively.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
