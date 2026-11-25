package com.alphito.kcal.core.notifications

import com.alphito.kcal.core.common.di.ApplicationScope
import com.alphito.kcal.core.firebase.PushTokenRegistrar
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * FCM entry point. Notification messages received in the background are shown by the system on
 * the "updates" channel; foreground and data-only messages are posted here.
 */
class KcalMessagingService : FirebaseMessagingService() {

    private val notifications: KcalNotifications by inject()
    private val registrar: PushTokenRegistrar by inject()
    private val appScope: CoroutineScope by inject(ApplicationScope)

    override fun onNewToken(token: String) {
        appScope.launch { registrar.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        notifications.post(message.messageId?.hashCode() ?: title.hashCode(), KcalNotifications.CHANNEL_UPDATES, title, body)
    }
}
