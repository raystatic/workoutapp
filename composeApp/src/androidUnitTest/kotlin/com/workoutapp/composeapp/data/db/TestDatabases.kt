package com.workoutapp.composeapp.data.db

import app.cash.sqldelight.db.SqlDriver
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.RoutineSet
import com.workoutapp.composeapp.db.Workout
import com.workoutapp.composeapp.db.WorkoutSet

/** Builds an [AppDatabase] with every column adapter wired, matching [com.workoutapp.composeapp.di.appModule]. */
fun testAppDatabase(driver: SqlDriver): AppDatabase = AppDatabase(
    driver = driver,
    exerciseAdapter = Exercise.Adapter(secondaryMusclesAdapter = stringListAdapter),
    routineSetAdapter = RoutineSet.Adapter(setTypeAdapter = enumColumnAdapter<SetType>()),
    workoutAdapter = Workout.Adapter(
        privacyAdapter = enumColumnAdapter<WorkoutPrivacy>(),
        mediaAdapter = stringListAdapter,
    ),
    workoutSetAdapter = WorkoutSet.Adapter(setTypeAdapter = enumColumnAdapter<SetType>()),
)
