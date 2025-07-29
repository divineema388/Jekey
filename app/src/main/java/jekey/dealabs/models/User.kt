package jeky.dealabs.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileImage: String? = null,
    @PropertyName("friendRequestsSent") val friendRequestsSent: List<String> = emptyList(), // UIDs of users this user has sent requests to
    @PropertyName("friendRequestsReceived") val friendRequestsReceived: List<String> = emptyList(), // UIDs of users who have sent requests to this user
    val friends: List<String> = emptyList() // UIDs of confirmed friends
)