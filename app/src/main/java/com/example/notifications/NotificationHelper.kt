package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Conversation

object NotificationHelper {

    const val CHANNEL_MESSAGES = "univers_chat_messages"
    const val CHANNEL_SYNC_STATUS = "univers_chat_live_sync"
    const val CHANNEL_CALLS = "univers_chat_calls"
    const val SERVICE_NOTIFICATION_ID = 9001

    const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    const val KEY_INLINE_REPLY = "key_inline_reply"
    const val ACTION_INLINE_REPLY = "com.example.notifications.ACTION_INLINE_REPLY"
    const val ACTION_MARK_READ = "com.example.notifications.ACTION_MARK_READ"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val messageChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Real-time direct and group message alerts"
            enableVibration(true)
            enableLights(true)
            lightColor = Color.rgb(0, 122, 255)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            setSound(soundUri, audioAttributes)
        }

        val syncChannel = NotificationChannel(
            CHANNEL_SYNC_STATUS,
            "Real-time delivery status",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Keeps real-time Univers Chat message alerts active"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val callRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val callAudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val callsChannel = NotificationChannel(
            CHANNEL_CALLS,
            "HD Voice & Video Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming HD voice and video call alerts"
            enableVibration(true)
            enableLights(true)
            lightColor = Color.rgb(52, 199, 89)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            setSound(callRingtoneUri, callAudioAttributes)
        }

        manager.createNotificationChannel(messageChannel)
        manager.createNotificationChannel(syncChannel)
        manager.createNotificationChannel(callsChannel)
    }

    fun buildLiveSyncNotification(context: Context): Notification {
        ensureChannels(context)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_SYNC_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Connected for real-time messages")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openPendingIntent)
            .build()
    }

    fun showIncomingMessageNotification(
        context: Context,
        conversation: Conversation,
        conversationTitle: String,
        senderName: String,
        messageText: String,
        timestampMillis: Long
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notifId = notificationIdFor(conversation.id)

        val openChatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CONVERSATION_ID, conversation.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyIntent = Intent(context, MessageNotificationActionReceiver::class.java).apply {
            action = ACTION_INLINE_REPLY
            putExtra(EXTRA_CONVERSATION_ID, conversation.id)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 10_000,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val remoteInput = RemoteInput.Builder(KEY_INLINE_REPLY)
            .setLabel("Reply to $conversationTitle")
            .build()
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val markReadIntent = Intent(context, MessageNotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_CONVERSATION_ID, conversation.id)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 20_000,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Mark Read",
            markReadPendingIntent
        ).build()

        val selfPerson = Person.Builder().setName("You").build()
        val senderPerson = Person.Builder()
            .setName(senderName.ifBlank { conversationTitle })
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(selfPerson)
            .setGroupConversation(conversation.type == Conversation.TYPE_GROUP)
            .also { style ->
                if (conversation.type == Conversation.TYPE_GROUP) {
                    style.setConversationTitle(conversationTitle)
                }
            }
            .addMessage(messageText, timestampMillis, senderPerson)

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setColor(Color.rgb(0, 122, 255))
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setWhen(timestampMillis)
            .setShowWhen(true)
            .setContentIntent(contentPendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }

    fun cancelConversationNotification(context: Context, conversationId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(conversationId))
    }

    fun notificationIdFor(conversationId: String): Int {
        return (conversationId.hashCode() and 0x0FFFFFFF).coerceAtLeast(1000)
    }
}
