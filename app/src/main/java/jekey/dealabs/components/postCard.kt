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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import jeky.dealabs.screens.CommentsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    post: Post, 
    currentUserId: String?, 
    onLikeClick: (String) -> Unit, 
    onCommentClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit = {},
    onRepostClick: (String) -> Unit = {},
    commentCount: Int = 0
) {
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

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
            // Show repost indicator if this is a repost
            if (post.isRepost == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = "Repost",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reposted by ${post.reposterUsername ?: "Unknown"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Profile picture, username and menu row
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                // Three dots menu
                Box {
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        // Delete option (only for post owner)
                        if (currentUserId == post.userId) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDeleteClick(post.id)
                                    showDropdownMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete"
                                    )
                                }
                            )
                        }
                        
                        // Copy option
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(post.textContent ?: ""))
                                showDropdownMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy"
                                )
                            }
                        )
                        
                        // Repost option
                        DropdownMenuItem(
                            text = { Text("Repost") },
                            onClick = {
                                onRepostClick(post.id)
                                showDropdownMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Repeat,
                                    contentDescription = "Repost"
                                )
                            }
                        )
                    }
                }
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
                        text = "$commentCount",
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