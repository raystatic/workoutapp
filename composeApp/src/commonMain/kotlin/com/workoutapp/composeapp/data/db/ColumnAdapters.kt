package com.workoutapp.composeapp.data.db

import app.cash.sqldelight.ColumnAdapter

/** Stores an enum as its [Enum.name] in a TEXT column. */
inline fun <reified T : Enum<T>> enumColumnAdapter(): ColumnAdapter<T, String> =
    object : ColumnAdapter<T, String> {
        override fun decode(databaseValue: String): T = enumValueOf(databaseValue)
        override fun encode(value: T): String = value.name
    }

/** Stores a list of strings as a delimited TEXT column (no element may contain the delimiter). */
val stringListAdapter: ColumnAdapter<List<String>, String> =
    object : ColumnAdapter<List<String>, String> {
        override fun decode(databaseValue: String): List<String> =
            if (databaseValue.isEmpty()) emptyList() else databaseValue.split("|")

        override fun encode(value: List<String>): String = value.joinToString("|")
    }
