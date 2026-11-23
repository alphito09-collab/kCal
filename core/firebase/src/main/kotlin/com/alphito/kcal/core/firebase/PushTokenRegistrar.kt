package com.alphito.kcal.core.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** Stores this install's FCM token under `users/{uid}` so the backend can send reminders/campaigns. */
class PushTokenRegistrar(private val firebase: KcalFirebase) {

    suspend fun registerCurrentToken() {
        if (!firebase.isAvailable) return
        runCatching { register(FirebaseMessaging.getInstance().token.await()) }
            .onFailure { Log.w(TAG, "FCM token fetch failed", it) }
    }

    suspend fun register(token: String) {
        val uid = firebase.uid() ?: return
        val db = firebase.firestore() ?: return
        runCatching {
            db.collection("users").document(uid)
                .set(mapOf("fcmTokens" to FieldValue.arrayUnion(token), "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL).await()
        }.onFailure { Log.w(TAG, "Token registration failed", it) }
    }

    companion object {
        const val TOPIC_ALL = "all_users"
        private const val TAG = "PushToken"
    }
}
