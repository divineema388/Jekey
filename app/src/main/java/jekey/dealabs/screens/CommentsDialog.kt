// jeky/dealabs/screens/CommentsDialog.kt
package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
// In jeky/dealabs/screens/CommentsDialog.kt

// Make sure this import is present at the top with your other imports:
import androidx.compose.material.icons.filled.Close // <--- Add this line
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.models.Comment // Import your Comment data class
import jeky.dealabs.utils.FirestoreUtils // Import the utility functions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsDialog(
    postId: String,
    onDismissRequest: () -> Unit,
    currentUser: String?, // Current user's UID
    currentUsername: String? // Current user's display name or derived username
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    val comments = remember { mutableStateListOf<Comment>() }
    var newCommentText by remember { mutableStateOf("") }
    var isSendingComment by remember { mutableStateOf(false) }

    // Fetch comments for the current post
    LaunchedEffect(postId) {
        firestore.collection("posts").document(postId)
            .collection("comments") // Access the comments subcollection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading comments: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newComments = snapshot.toObjects(Comment::class.java)
                    comments.clear()
                    comments.addAll(newComments)
                }
            }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false) // To allow full width if desired
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f) // Take up most of the screen height
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close comments")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // List of Comments
                if (comments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No comments yet. Be the first to add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f) // Takes remaining space
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentItem(comment = comment)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Add New Comment Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 3,
                        enabled = !isSendingComment && currentUser != null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (currentUser == null || currentUsername == null) {
                                Toast.makeText(context, "You must be logged in to comment.", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            if (newCommentText.isBlank()) {
                                Toast.makeText(context, "Comment cannot be empty.", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }

                            isSendingComment = true
                            FirestoreUtils.saveCommentToFirestore( // This function will be added next
                                firestore,
                                postId,
                                currentUser,
                                currentUsername,
                                newCommentText,
                                onSuccess = {
                                    newCommentText = "" // Clear input
                                    isSendingComment = false
                                    Toast.makeText(context, "Comment added!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    isSendingComment = false
                                    Toast.makeText(context, "Failed to add comment: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = !isSendingComment && newCommentText.isNotBlank() && currentUser != null
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send comment")
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = comment.username,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.textContent,
                style = MaterialTheme.typography.bodySmall
            )
            // Optional: Display timestamp
            // comment.timestamp?.let {
            //     Text(
            //         text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(it),
            //         style = MaterialTheme.typography.labelSmall,
            //         color = MaterialTheme.colorScheme.onSurfaceVariant
            //     )
            // }
        }
    }
}