package com.workoutapp.composeapp.data.db

/** Current wall-clock time in epoch milliseconds, for stamping `updatedAt` on writes. */
expect fun currentTimeMillis(): Long
