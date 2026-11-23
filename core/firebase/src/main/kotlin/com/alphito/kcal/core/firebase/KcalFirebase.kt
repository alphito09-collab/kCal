package com.alphito.kcal.core.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Single entry point to Firebase.
 *
 * kCal is offline-first: when the project has no `google-services.json` (local builds, CI) or
 * Firebase fails to start, [isAvailable] is false and every caller falls back to on-device data
 * instead of crashing. Users are signed in **anonymously** – no account UI – which gives each
 * install a stable uid for Firestore security rules and purchase verification.
 */
class KcalFirebase(private val context: Context) {

    val isAvailable: Boolean by lazy {
        runCatching { FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null }
            .onFailure { Log.w(TAG, "Firebase unavailable", it) }
            .getOrDefault(false)
    }

    private val authMutex = Mutex()

    fun firestore(): FirebaseFirestore? = if (isAvailable) FirebaseFirestore.getInstance() else null

    fun functions(): FirebaseFunctions? = if (isAvailable) FirebaseFunctions.getInstance() else null

    /** The anonymous uid for this install (signs in on first use). Null when offline or unconfigured. */
    suspend fun uid(): String? {
        if (!isAvailable) return null
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        return authMutex.withLock {
            auth.currentUser?.uid ?: runCatching { auth.signInAnonymously().await().user?.uid }
                .onFailure { Log.w(TAG, "Anonymous sign-in failed", it) }
                .getOrNull()
        }
    }

    private companion object { const val TAG = "KcalFirebase" }
}
