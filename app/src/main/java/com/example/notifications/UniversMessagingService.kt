package com.example.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class UniversMessagingService : FirebaseMessagingService() {

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("UniversMessagingService", "Refreshed FCM token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data.isNotEmpty()) {
            val title = data["title"] ?: "Univers Chat"
            val body = data["body"] ?: "New message received"
            val convId = data["conversation_id"]
            if (convId != null) {
                // Route notification via NotificationHelper if payload is present
                Log.d("UniversMessagingService", "Received FCM message for conversation: $convId")
            }
        }
    }
}
