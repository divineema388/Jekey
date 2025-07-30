package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import jeky.dealabs.models.User
import jeky.dealabs.utils.FirestoreUtils
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val friendRequests = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }
    var requestCount by remember { mutableStateOf(0) }

    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            listenerRegistration = firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject(User::class.java)
                        val requestIds = user?.friendRequestsReceived ?: emptyList()
                        requestCount = requestIds.size

                        if (requestIds.isNotEmpty()) {
                            // Fetch user details for each request ID
                            val requestUsers = mutableListOf<User>()
                            var completed = 0

                            requestIds.forEach { requestId ->
                                firestore.collection("users").document(requestId)
                                    .get()
                                    .addOnSuccessListener { userDoc ->
                                        userDoc.toObject(User::class.java)?.let { requestUser ->
                                            requestUsers.add(requestUser)
                                        }
                                        completed++
                                        if (completed == requestIds.size) {
                                            friendRequests.clear()
                                            friendRequests.addAll(requestUsers.sortedBy { it.displayName })
                                            isLoading = false
                                        }
                                    }
                                    .addOnFailureListener {
                                        completed++
                                        if (completed == requestIds.size) {
                                            friendRequests.clear()
                                            friendRequests.addAll(requestUsers.sortedBy { it.displayName })
                                            isLoading = false
                                        }
                                    }
                            }
                        } else {
                            friendRequests.clear()
                            isLoading = false
                        }
                    } else {
                        friendRequests.clear()
                        requestCount = 0
                        isLoading = false
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend Requests ($requestCount)") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (friendRequests.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No pending friend requests.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(friendRequests, key = { it.uid }) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    Button(
                                        onClick = {
                                            currentUser?.uid?.let { currentId ->
                                                FirestoreUtils.acceptFriendRequest(
                                                    firestore = firestore,
                                                    currentUserId = currentId,
                                                    requesterId = user.uid,
                                                    context = context,
                                                    onSuccess = { 
                                                        Toast.makeText(context, "Accepted ${user.displayName}", Toast.LENGTH_SHORT).show() 
                                                    },
                                                    onFailure = { e -> 
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() 
                                                    }
                                                )
                                            }
                                        }
                                    ) {
                                        Text("Accept")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            currentUser?.uid?.let { currentId ->
                                                FirestoreUtils.declineFriendRequest(
                                                    firestore = firestore,
                                                    currentUserId = currentId,
                                                    requesterId = user.uid,
                                                    context = context,
                                                    onSuccess = { 
                                                        Toast.makeText(context, "Declined ${user.displayName}", Toast.LENGTH_SHORT).show() 
                                                    },
                                                    onFailure = { e -> 
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() 
                                                    }
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Decline")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
