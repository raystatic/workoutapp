package com.workoutapp.composeapp.mvi

/** Marker for a screen's immutable UI state. */
interface MviState

/** Marker for a user/system action a screen can handle. */
interface MviIntent

/** Marker for a one-off side effect (navigation, snackbar, ...). */
interface MviEffect
