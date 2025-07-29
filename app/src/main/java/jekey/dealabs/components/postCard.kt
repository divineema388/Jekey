// jeky/dealabs/components/PostCard.kt
package jeky.dealabs.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.util.Base64
import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jeky.dealabs.models.Post
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import jeky.dealabs.screens.CommentsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(post: Post, currentUserId: String?, onLikeClick: (String) -> Unit, onCommentClick: (String) -> Unit) {
    var showCommentsDialog by remember { mutableStateOf(false) }

    // Function to convert base64 to bitmap
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Profile picture and username row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (post.profileImage != null) {
                            val bitmap = base64ToBitmap(post.profileImage!!)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Posted by ${post.username}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.textContent ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Display image if available
            post.imageUrl?.let { imageUrl ->
                if (imageUrl.isNotBlank()) {
                    Text(text = "Image Placeholder (URL: $imageUrl)", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Likes and Comment section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onLikeClick(post.id) }) {
                        val isLiked = post.likes.contains(currentUserId)
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.likes.size}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        onCommentClick(post.id)
                        showCommentsDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "0", // Placeholder for comment count
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    // Comments Dialog
    if (showCommentsDialog) {
        CommentsDialog(
            postId = post.id,
            onDismissRequest = { showCommentsDialog = false },
            currentUser = currentUserId,
            currentUsername = post.username
        )
    }
}