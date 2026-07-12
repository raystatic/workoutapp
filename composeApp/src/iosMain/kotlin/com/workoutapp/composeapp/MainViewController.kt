package com.workoutapp.composeapp

import androidx.compose.ui.window.ComposeUIViewController
import com.workoutapp.composeapp.di.initKoinIos
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoinIos()
    return ComposeUIViewController { App() }
}
