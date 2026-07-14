package com.workoutapp.composeapp.data.resttimer

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.data.db.testAppDatabase
import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class RestTimerSettingsRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: RestTimerSettingsRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = testAppDatabase(driver)
        repository = RestTimerSettingsRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun getDefaultRestSeconds_withNothingStored_returnsTheBuiltInDefault() = runTest {
        assertEquals(RestTimerSettingsRepositoryImpl.DEFAULT_REST_SECONDS, repository.getDefaultRestSeconds())
    }

    @Test
    fun setDefaultRestSeconds_thenGet_returnsThePersistedValue() = runTest {
        repository.setDefaultRestSeconds(120)

        assertEquals(120, repository.getDefaultRestSeconds())
    }

    @Test
    fun setDefaultRestSeconds_calledTwice_overwritesThePreviousValue() = runTest {
        repository.setDefaultRestSeconds(60)
        repository.setDefaultRestSeconds(150)

        assertEquals(150, repository.getDefaultRestSeconds())
    }
}
