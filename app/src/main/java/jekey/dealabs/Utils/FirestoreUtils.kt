// jeky/dealabs/utils/FirestoreUtils.kt
package jeky.dealabs.utils


import jeky.dealabs.models.User
import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.models.Post
import jeky.dealabs.models.Comment // New import for your Comment data class

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
            .collection("comments") // Access the 'comments' subcollection
            .add(comment) // Add the new comment document
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    fun sendFriendRequest(
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

            val currentUserRequestsSent = currentUserSnapshot.toObject(User::class.java)?.friendRequestsSent?.toMutableList() ?: mutableListOf()
            val targetUserRequestsReceived = targetUserSnapshot.toObject(User::class.java)?.friendRequestsReceived?.toMutableList() ?: mutableListOf()

            if (!currentUserRequestsSent.contains(targetUserId) && !targetUserRequestsReceived.contains(currentUserId)) {
                currentUserRequestsSent.add(targetUserId)
                targetUserRequestsReceived.add(currentUserId)

                transaction.update(currentUserRef, "friendRequestsSent", currentUserRequestsSent)
                transaction.update(targetUserRef, "friendRequestsReceived", targetUserRequestsReceived)
                null // Indicate successful transaction
            } else {
                throw Exception("Friend request already sent or received.")
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
            onFailure(e)
        }
    }

    fun acceptFriendRequest(
        firestore: FirebaseFirestore,
        currentUserId: String,
        requesterId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val requesterUserRef = firestore.collection("users").document(requesterId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val requesterUserSnapshot = transaction.get(requesterUserRef)

            val currentUserRequestsReceived = currentUserSnapshot.toObject(User::class.java)?.friendRequestsReceived?.toMutableList() ?: mutableListOf()
            val currentUserFriends = currentUserSnapshot.toObject(User::class.java)?.friends?.toMutableList() ?: mutableListOf()

            val requesterRequestsSent = requesterUserSnapshot.toObject(User::class.java)?.friendRequestsSent?.toMutableList() ?: mutableListOf()
            val requesterFriends = requesterUserSnapshot.toObject(User::class.java)?.friends?.toMutableList() ?: mutableListOf()

            if (currentUserRequestsReceived.contains(requesterId) && !currentUserFriends.contains(requesterId)) {
                // Remove from received requests for current user
                currentUserRequestsReceived.remove(requesterId)
                transaction.update(currentUserRef, "friendRequestsReceived", currentUserRequestsReceived)

                // Add to friends list for current user
                currentUserFriends.add(requesterId)
                transaction.update(currentUserRef, "friends", currentUserFriends)

                // Remove from sent requests for requester
                requesterRequestsSent.remove(currentUserId)
                transaction.update(requesterUserRef, "friendRequestsSent", requesterRequestsSent)

                // Add to friends list for requester
                requesterFriends.add(currentUserId)
                transaction.update(requesterUserRef, "friends", requesterFriends)
                
                null // Indicate successful transaction
            } else {
                throw Exception("Friend request not found or already friends.")
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show()
            onFailure(e)
        }
    }

    fun declineFriendRequest(
        firestore: FirebaseFirestore,
        currentUserId: String,
        requesterId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val requesterUserRef = firestore.collection("users").document(requesterId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val requesterUserSnapshot = transaction.get(requesterUserRef)

            val currentUserRequestsReceived = currentUserSnapshot.toObject(User::class.java)?.friendRequestsReceived?.toMutableList() ?: mutableListOf()
            val requesterRequestsSent = requesterUserSnapshot.toObject(User::class.java)?.friendRequestsSent?.toMutableList() ?: mutableListOf()

            if (currentUserRequestsReceived.contains(requesterId)) {
                currentUserRequestsReceived.remove(requesterId)
                transaction.update(currentUserRef, "friendRequestsReceived", currentUserRequestsReceived)

                requesterRequestsSent.remove(currentUserId)
                transaction.update(requesterUserRef, "friendRequestsSent", requesterRequestsSent)
                
                null // Indicate successful transaction
            } else {
                throw Exception("Friend request not found.")
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to decline request: ${e.message}", Toast.LENGTH_SHORT).show()
            onFailure(e)
        }
    }

    fun removeFriend(
        firestore: FirebaseFirestore,
        currentUserId: String,
        friendId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val friendUserRef = firestore.collection("users").document(friendId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val friendUserSnapshot = transaction.get(friendUserRef)

            val currentUserFriends = currentUserSnapshot.toObject(User::class.java)?.friends?.toMutableList() ?: mutableListOf()
            val friendUserFriends = friendUserSnapshot.toObject(User::class.java)?.friends?.toMutableList() ?: mutableListOf()

            if (currentUserFriends.contains(friendId) && friendUserFriends.contains(currentUserId)) {
                currentUserFriends.remove(friendId)
                transaction.update(currentUserRef, "friends", currentUserFriends)

                friendUserFriends.remove(currentUserId)
                transaction.update(friendUserRef, "friends", friendUserFriends)
                
                null // Indicate successful transaction
            } else {
                throw Exception("User is not your friend.")
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to remove friend: ${e.message}", Toast.LENGTH_SHORT).show()
            onFailure(e)
        }
    }
}