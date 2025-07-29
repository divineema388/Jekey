// jeky/dealabs/utils/FirestoreUtils.kt
package jeky.dealabs.utils

import jeky.dealabs.models.User
import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.models.Post
import jeky.dealabs.models.Comment

/**
 * Utility functions for Firestore operations
 */
object FirestoreUtils {

    /**
     * Save a post to Firestore
     */
    fun savePostToFirestore(
        firestore: FirebaseFirestore,
        userId: String,
        username: String,
        textContent: String?,
        imageUrl: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val post = Post(
            userId = userId,
            username = username,
            textContent = textContent,
            imageUrl = imageUrl,
            likes = emptyList()
        )

        firestore.collection("posts")
            .add(post)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * Save a comment to a post's subcollection in Firestore
     */
    fun saveCommentToFirestore(
        firestore: FirebaseFirestore,
        postId: String,
        userId: String,
        username: String,
        textContent: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val comment = Comment(
            userId = userId,
            username = username,
            textContent = textContent
            // timestamp will be automatically set by @ServerTimestamp
        )

        firestore.collection("posts").document(postId)
            .collection("comments")
            .add(comment)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * Send a friend request with improved error handling and validation
     */
    fun sendFriendRequest(
        firestore: FirebaseFirestore,
        currentUserId: String,
        targetUserId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (currentUserId == targetUserId) {
            onFailure(Exception("Cannot send friend request to yourself"))
            return
        }

        val currentUserRef = firestore.collection("users").document(currentUserId)
        val targetUserRef = firestore.collection("users").document(targetUserId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val targetUserSnapshot = transaction.get(targetUserRef)

            if (!currentUserSnapshot.exists()) {
                throw Exception("Current user document not found")
            }
            if (!targetUserSnapshot.exists()) {
                throw Exception("Target user not found")
            }

            val currentUser = currentUserSnapshot.toObject(User::class.java)
            val targetUser = targetUserSnapshot.toObject(User::class.java)

            val currentUserRequestsSent = currentUser?.friendRequestsSent?.toMutableList() ?: mutableListOf()
            val currentUserFriends = currentUser?.friends?.toMutableList() ?: mutableListOf()
            val targetUserRequestsReceived = targetUser?.friendRequestsReceived?.toMutableList() ?: mutableListOf()
            val targetUserFriends = targetUser?.friends?.toMutableList() ?: mutableListOf()

            // Check if they are already friends
            if (currentUserFriends.contains(targetUserId)) {
                throw Exception("You are already friends with this user")
            }

            // Check if request already sent
            if (currentUserRequestsSent.contains(targetUserId)) {
                throw Exception("Friend request already sent")
            }

            // Check if there's already a pending request from the target user
            if (targetUserRequestsReceived.contains(currentUserId)) {
                throw Exception("This user has already sent you a friend request")
            }

            // Add to current user's sent requests
            currentUserRequestsSent.add(targetUserId)
            transaction.update(currentUserRef, "friendRequestsSent", currentUserRequestsSent)

            // Add to target user's received requests
            targetUserRequestsReceived.add(currentUserId)
            transaction.update(targetUserRef, "friendRequestsReceived", targetUserRequestsReceived)

            null // Transaction return value
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    /**
     * Accept a friend request
     */
    fun acceptFriendRequest(
        firestore: FirebaseFirestore,
        currentUserId: String,
        requesterId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val requesterRef = firestore.collection("users").document(requesterId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val requesterSnapshot = transaction.get(requesterRef)

            if (!currentUserSnapshot.exists()) {
                throw Exception("Current user document not found")
            }
            if (!requesterSnapshot.exists()) {
                throw Exception("Requester user not found")
            }

            val currentUser = currentUserSnapshot.toObject(User::class.java)
            val requester = requesterSnapshot.toObject(User::class.java)

            val currentUserRequestsReceived = currentUser?.friendRequestsReceived?.toMutableList() ?: mutableListOf()
            val currentUserFriends = currentUser?.friends?.toMutableList() ?: mutableListOf()
            val requesterRequestsSent = requester?.friendRequestsSent?.toMutableList() ?: mutableListOf()
            val requesterFriends = requester?.friends?.toMutableList() ?: mutableListOf()

            // Verify the request exists
            if (!currentUserRequestsReceived.contains(requesterId)) {
                throw Exception("No friend request found from this user")
            }

            // Remove from received requests
            currentUserRequestsReceived.remove(requesterId)
            transaction.update(currentUserRef, "friendRequestsReceived", currentUserRequestsReceived)

            // Remove from sent requests
            requesterRequestsSent.remove(currentUserId)
            transaction.update(requesterRef, "friendRequestsSent", requesterRequestsSent)

            // Add to both friends lists
            if (!currentUserFriends.contains(requesterId)) {
                currentUserFriends.add(requesterId)
                transaction.update(currentUserRef, "friends", currentUserFriends)
            }

            if (!requesterFriends.contains(currentUserId)) {
                requesterFriends.add(currentUserId)
                transaction.update(requesterRef, "friends", requesterFriends)
            }

            null // Transaction return value
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    /**
     * Decline a friend request
     */
    fun declineFriendRequest(
        firestore: FirebaseFirestore,
        currentUserId: String,
        requesterId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val requesterRef = firestore.collection("users").document(requesterId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val requesterSnapshot = transaction.get(requesterRef)

            if (!currentUserSnapshot.exists()) {
                throw Exception("Current user document not found")
            }
            if (!requesterSnapshot.exists()) {
                throw Exception("Requester user not found")
            }

            val currentUser = currentUserSnapshot.toObject(User::class.java)
            val requester = requesterSnapshot.toObject(User::class.java)

            val currentUserRequestsReceived = currentUser?.friendRequestsReceived?.toMutableList() ?: mutableListOf()
            val requesterRequestsSent = requester?.friendRequestsSent?.toMutableList() ?: mutableListOf()

            // Verify the request exists
            if (!currentUserRequestsReceived.contains(requesterId)) {
                throw Exception("No friend request found from this user")
            }

            // Remove from received requests
            currentUserRequestsReceived.remove(requesterId)
            transaction.update(currentUserRef, "friendRequestsReceived", currentUserRequestsReceived)

            // Remove from sent requests
            requesterRequestsSent.remove(currentUserId)
            transaction.update(requesterRef, "friendRequestsSent", requesterRequestsSent)

            null // Transaction return value
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    /**
     * Remove a friend
     */
    fun removeFriend(
        firestore: FirebaseFirestore,
        currentUserId: String,
        friendId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val friendRef = firestore.collection("users").document(friendId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val friendSnapshot = transaction.get(friendRef)

            if (!currentUserSnapshot.exists()) {
                throw Exception("Current user document not found")
            }
            if (!friendSnapshot.exists()) {
                throw Exception("Friend user not found")
            }

            val currentUser = currentUserSnapshot.toObject(User::class.java)
            val friend = friendSnapshot.toObject(User::class.java)

            val currentUserFriends = currentUser?.friends?.toMutableList() ?: mutableListOf()
            val friendUserFriends = friend?.friends?.toMutableList() ?: mutableListOf()

            // Verify they are friends
            if (!currentUserFriends.contains(friendId)) {
                throw Exception("You are not friends with this user")
            }

            // Remove from both friends lists
            currentUserFriends.remove(friendId)
            transaction.update(currentUserRef, "friends", currentUserFriends)

            friendUserFriends.remove(currentUserId)
            transaction.update(friendRef, "friends", friendUserFriends)

            null // Transaction return value
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    /**
     * Cancel a sent friend request
     */
    fun cancelFriendRequest(
        firestore: FirebaseFirestore,
        currentUserId: String,
        targetUserId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val targetUserRef = firestore.collection("users").document(targetUserId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val targetUserSnapshot = transaction.get(targetUserRef)

            if (!currentUserSnapshot.exists()) {
                throw Exception("Current user document not found")
            }
            if (!targetUserSnapshot.exists()) {
                throw Exception("Target user not found")
            }

            val currentUser = currentUserSnapshot.toObject(User::class.java)
            val targetUser = targetUserSnapshot.toObject(User::class.java)

            val currentUserRequestsSent = currentUser?.friendRequestsSent?.toMutableList() ?: mutableListOf()
            val targetUserRequestsReceived = targetUser?.friendRequestsReceived?.toMutableList() ?: mutableListOf()

            // Verify the request exists
            if (!currentUserRequestsSent.contains(targetUserId)) {
                throw Exception("No friend request found to this user")
            }

            // Remove from sent requests
            currentUserRequestsSent.remove(targetUserId)
            transaction.update(currentUserRef, "friendRequestsSent", currentUserRequestsSent)

            // Remove from received requests
            targetUserRequestsReceived.remove(currentUserId)
            transaction.update(targetUserRef, "friendRequestsReceived", targetUserRequestsReceived)

            null // Transaction return value
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    /**
     * Get friend status between two users
     */
    fun getFriendStatus(
        firestore: FirebaseFirestore,
        currentUserId: String,
        targetUserId: String,
        onResult: (FriendStatus) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { currentUserSnapshot ->
                if (currentUserSnapshot.exists()) {
                    val currentUser = currentUserSnapshot.toObject(User::class.java)
                    
                    when {
                        currentUser?.friends?.contains(targetUserId) == true -> {
                            onResult(FriendStatus.FRIENDS)
                        }
                        currentUser?.friendRequestsSent?.contains(targetUserId) == true -> {
                            onResult(FriendStatus.REQUEST_SENT)
                        }
                        currentUser?.friendRequestsReceived?.contains(targetUserId) == true -> {
                            onResult(FriendStatus.REQUEST_RECEIVED)
                        }
                        else -> {
                            onResult(FriendStatus.NOT_FRIENDS)
                        }
                    }
                } else {
                    onFailure(Exception("Current user document not found"))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * Enum to represent friend status
     */
    enum class FriendStatus {
        FRIENDS,
        REQUEST_SENT,
        REQUEST_RECEIVED,
        NOT_FRIENDS
    }

    /**
     * Batch update user data (for profile updates)
     */
    fun updateUserProfile(
        firestore: FirebaseFirestore,
        userId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * Initialize user document with default friend-related fields
     */
    fun initializeUserDocument(
        firestore: FirebaseFirestore,
        user: User,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Ensure all friend-related fields are initialized
        val userWithDefaults = user.copy(
            friends = user.friends ?: emptyList(),
            friendRequestsSent = user.friendRequestsSent ?: emptyList(),
            friendRequestsReceived = user.friendRequestsReceived ?: emptyList()
        )

        firestore.collection("users").document(user.uid)
            .set(userWithDefaults)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}