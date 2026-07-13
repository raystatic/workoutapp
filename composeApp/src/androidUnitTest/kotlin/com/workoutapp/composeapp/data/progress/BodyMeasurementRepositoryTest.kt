package com.workoutapp.composeapp.data.progress

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.data.db.testAppDatabase
import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BodyMeasurementRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: BodyMeasurementRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = BodyMeasurementRepositoryImpl(testAppDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedMeasurement() = runTest {
        repository.add(takenAt = 1000L, type = "weight", value = 82.5, updatedAt = 1000L)

        val measurements = repository.observeAll().first()

        assertEquals(1, measurements.size)
        assertEquals("weight", measurements.single().type)
        assertEquals(82.5, measurements.single().value)
    }

    @Test
    fun add_multipleMeasurements_returnsNewestFirst() = runTest {
        repository.add(takenAt = 1000L, type = "weight", value = 83.0, updatedAt = 1000L)
        repository.add(takenAt = 2000L, type = "weight", value = 82.5, updatedAt = 2000L)

        val measurements = repository.observeAll().first()

        assertEquals(listOf(82.5, 83.0), measurements.map { it.value })
    }

    @Test
    fun delete_removesOnlyTheMatchingMeasurement() = runTest {
        repository.add(takenAt = 1000L, type = "weight", value = 83.0, updatedAt = 1000L)
        val idToDelete = repository.observeAll().first().single().id
        repository.add(takenAt = 2000L, type = "weight", value = 82.5, updatedAt = 2000L)

        repository.delete(idToDelete)

        val remaining = repository.observeAll().first()
        assertEquals(listOf(82.5), remaining.map { it.value })
    }
}
