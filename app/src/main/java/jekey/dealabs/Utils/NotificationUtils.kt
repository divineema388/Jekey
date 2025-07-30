package jeky.dealabs.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.MainActivity
import jeky.dealabs.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationUtils {
    private const val CHANNEL_ID = "friend_requests"
    private const val CHANNEL_NAME = "Friend Requests"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for friend requests"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showFriendRequestNotification(context: Context, senderName: String) {
        createNotificationChannel(context)
        
        // Intent to open FriendRequestsScreen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigation_target", "friend_requests")
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle("Friend Request")
            .setContentText("$senderName sent you a friend request")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun sendFriendRequestNotification(
        context: Context,
        firestore: FirebaseFirestore,
        targetUserId: String,
        senderName: String
    ) {
        // Get target user's FCM token
        firestore.collection("users").document(targetUserId)
            .get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                if (fcmToken != null) {
                    // Send FCM notification using Cloud Functions
                    // For now, we'll show local notification if user is active
                    showFriendRequestNotification(context, senderName)
                } else {
                    // Store notification for when user comes online
                    storePendingNotification(firestore, targetUserId, senderName)
                }
            }
    }
    
    private fun storePendingNotification(
        firestore: FirebaseFirestore,
        targetUserId: String,
        senderName: String
    ) {
        val notification = mapOf(
            "type" to "friend_request",
            "senderName" to senderName,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "isRead" to false
        )
        
        firestore.collection("users")
            .document(targetUserId)
            .collection("notifications")
            .add(notification)
    }

    fun checkPendingNotifications(
        context: Context,
        firestore: FirebaseFirestore,
        userId: String
    ) {
        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { doc ->
                    val type = doc.getString("type")
                    val senderName = doc.getString("senderName")
                    
                    if (type == "friend_request" && senderName != null) {
                        showFriendRequestNotification(context, senderName)
                        
                        // Mark as read
                        doc.reference.update("isRead", true)
                    }
                }
            }
    }
}