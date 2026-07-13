package com.workoutapp.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * connectedDebugAndroidTest smoke test proving CI's Firebase Local Emulator
 * Suite wiring (main-ci.yml + firebase.json) works end to end: an anonymous
 * sign-in against the Auth emulator, followed by a Firestore document write
 * that round-trips through the Firestore emulator, both at 10.0.2.2.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreEmulatorSmokeTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val options = FirebaseOptions.Builder()
            .setProjectId("workoutapp-dev")
            .setApplicationId("1:0:android:0")
            .setApiKey("emulator-smoke-test")
            .build()
        val app = FirebaseApp.getApps(context).find { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(context, options, APP_NAME)

        auth = FirebaseAuth.getInstance(app)
        auth.useEmulator(EMULATOR_HOST, AUTH_EMULATOR_PORT)

        firestore = FirebaseFirestore.getInstance(app)
        firestore.useEmulator(EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)
    }

    @Test
    fun documentWrittenToEmulatorRoundTrips() {
        val uid = Tasks.await(auth.signInAnonymously(), 10, TimeUnit.SECONDS).user!!.uid

        val docRef = firestore.collection("users").document(uid)
            .collection("ci_smoke_test").document("wiring-check")

        Tasks.await(docRef.set(mapOf("ping" to "pong")), 10, TimeUnit.SECONDS)
        val snapshot = Tasks.await(docRef.get(), 10, TimeUnit.SECONDS)

        assertEquals("pong", snapshot.getString("ping"))

        Tasks.await(docRef.delete(), 10, TimeUnit.SECONDS)
    }

    private companion object {
        const val APP_NAME = "firestore-emulator-smoke-test"
        const val EMULATOR_HOST = "10.0.2.2"
        const val AUTH_EMULATOR_PORT = 9099
        const val FIRESTORE_EMULATOR_PORT = 8080
    }
}
