package com.workoutapp.composeapp.data.profile

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.data.db.testAppDatabase
import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UserProfileRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: UserProfileRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = UserProfileRepositoryImpl(testAppDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedProfileWithDefaults() = runTest {
        repository.add(displayName = "Rahul", updatedAt = 1000L)

        val profiles = repository.observeAll().first()

        assertEquals(1, profiles.size)
        val profile = profiles.single()
        assertEquals("Rahul", profile.displayName)
        assertFalse(profile.isPublic)
        assertEquals(0L, profile.streak)
    }

    @Test
    fun delete_removesOnlyTheMatchingProfile() = runTest {
        repository.add(displayName = "Rahul", updatedAt = 1000L)
        val idToDelete = repository.observeAll().first().single().id
        repository.add(displayName = "Sam", updatedAt = 1000L)

        repository.delete(idToDelete)

        val remaining = repository.observeAll().first()
        assertEquals(listOf("Sam"), remaining.map { it.displayName })
    }
}
