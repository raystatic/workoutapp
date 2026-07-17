package com.workoutapp.composeapp.data.onboarding

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.data.db.testAppDatabase
import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: OnboardingRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = testAppDatabase(driver)
        repository = OnboardingRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun hasSeenWalkthrough_withNothingStored_defaultsToFalse() = runTest {
        assertFalse(repository.hasSeenWalkthrough())
    }

    @Test
    fun setHasSeenWalkthrough_true_thenGet_returnsThePersistedValue() = runTest {
        repository.setHasSeenWalkthrough(true)

        assertTrue(repository.hasSeenWalkthrough())
    }

    @Test
    fun setHasSeenWalkthrough_calledTwice_overwritesThePreviousValue() = runTest {
        repository.setHasSeenWalkthrough(true)
        repository.setHasSeenWalkthrough(false)

        assertFalse(repository.hasSeenWalkthrough())
    }
}
