package jeky.dealabs.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.utils.NotificationUtils

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload of FCM messages
        remoteMessage.data.let { data ->
            val type = data["type"]
            when (type) {
                "friend_request" -> {
                    val senderName = data["senderName"] ?: "Someone"
                    NotificationUtils.showFriendRequestNotification(this, senderName)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send token to server
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
        }
    }
}