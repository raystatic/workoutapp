package com.workoutapp.composeapp.data.sample

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.SampleNote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Reference repository proving the SQLDelight + Flow plumbing: reads react
 * to writes without any manual refresh. Real domain repositories follow this
 * same shape once the logging schema lands.
 */
interface SampleNoteRepository {
    fun observeAll(): Flow<List<SampleNote>>
    suspend fun add(text: String)
    suspend fun delete(id: Long)
}

class SampleNoteRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SampleNoteRepository {
    private val queries = database.sampleNoteQueries

    override fun observeAll(): Flow<List<SampleNote>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)

    override suspend fun add(text: String) = withContext(ioDispatcher) {
        queries.insertNote(text)
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
