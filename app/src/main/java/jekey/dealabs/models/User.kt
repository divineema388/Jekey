package jeky.dealabs.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileImage: String? = null,
    @PropertyName("friendRequestsSent") val friendRequestsSent: List<String> = emptyList(),
    @PropertyName("friendRequestsReceived") val friendRequestsReceived: List<String> = emptyList(),
    val friends: List<String> = emptyList(),
    val fcmToken: String? = null // Add FCM token for notifications
)