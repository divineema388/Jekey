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
import jeky.dealabs.models.User
import jeky.dealabs.utils.FirestoreUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val friendRequests = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .addSnapshotListener { currentUserSnapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Error loading requests: ${e.message}", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (currentUserSnapshot != null && currentUserSnapshot.exists()) {
                        val user = currentUserSnapshot.toObject(User::class.java)
                        val receivedRequestUids = user?.friendRequestsReceived ?: emptyList()

                        if (receivedRequestUids.isNotEmpty()) {
                            firestore.collection("users")
                                .whereIn("uid", receivedRequestUids)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    friendRequests.clear()
                                    friendRequests.addAll(querySnapshot.documents.mapNotNull { it.toObject(User::class.java) })
                                    isLoading = false
                                }
                                .addOnFailureListener { innerE ->
                                    Toast.makeText(context, "Error fetching request user details: ${innerE.message}", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                }
                        } else {
                            friendRequests.clear()
                            isLoading = false
                        }
                    } else {
                        friendRequests.clear()
                        isLoading = false
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend Requests") },
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
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (friendRequests.isEmpty()) {
                Text(
                    text = "No pending friend requests.",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(friendRequests) { user ->
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
                                Column {
                                    Text(text = user.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                    onSuccess = { Toast.makeText(context, "Accepted ${user.displayName}", Toast.LENGTH_SHORT).show() },
                                                    onFailure = { e -> Toast.makeText(context, "Error accepting: ${e.message}", Toast.LENGTH_SHORT).show() }
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
                                                    onSuccess = { Toast.makeText(context, "Declined ${user.displayName}", Toast.LENGTH_SHORT).show() },
                                                    onFailure = { e -> Toast.makeText(context, "Error declining: ${e.message}", Toast.LENGTH_SHORT).show() }
                                                )
                                            }
                                        }
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