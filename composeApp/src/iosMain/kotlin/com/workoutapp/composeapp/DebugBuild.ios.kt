package com.workoutapp.composeapp

// iOS has no release distribution pipeline yet (see issue #28's iOS
// follow-up), so there is no release/debug signal to key off; revisit once
// one exists.
actual val isDebugBuild: Boolean = true
