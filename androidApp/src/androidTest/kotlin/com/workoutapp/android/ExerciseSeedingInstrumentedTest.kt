package com.workoutapp.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.library.ExerciseSeedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest proving the exercise library is seeded end to end on a real device
 * DB: [WorkoutApplication.onCreate] starts Koin, which kicks off [ExerciseSeeder] in the
 * background (see initKoin in Koin.kt), and this test waits for the library to reach the full
 * catalog size against the app's actual SQLite database.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseSeedingInstrumentedTest {

    @Test
    fun appLaunchSeedsTheExerciseLibrary() = runBlocking {
        val repository = GlobalContext.get().get<ExerciseRepository>()

        val exercises = withTimeout(30_000) {
            repository.observeAll().first { it.size >= ExerciseSeedData.exercises.size }
        }

        assertTrue(exercises.size >= 400)
        assertTrue(exercises.all { it.primaryMuscle.isNotBlank() && it.equipment.isNotBlank() })
    }
}
