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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFriendsScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val friendsList = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }
    var friendsCount by remember { mutableStateOf(0) }

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
                        val friendIds = user?.friends ?: emptyList()
                        friendsCount = friendIds.size

                        if (friendIds.isNotEmpty()) {
                            // Fetch user details for each friend ID
                            val friends = mutableListOf<User>()
                            var completed = 0

                            friendIds.forEach { friendId ->
                                firestore.collection("users").document(friendId)
                                    .get()
                                    .addOnSuccessListener { userDoc ->
                                        userDoc.toObject(User::class.java)?.let { friendUser ->
                                            friends.add(friendUser)
                                        }
                                        completed++
                                        if (completed == friendIds.size) {
                                            friendsList.clear()
                                            friendsList.addAll(friends.sortedBy { it.displayName })
                                            isLoading = false
                                        }
                                    }
                                    .addOnFailureListener {
                                        completed++
                                        if (completed == friendIds.size) {
                                            friendsList.clear()
                                            friendsList.addAll(friends.sortedBy { it.displayName })
                                            isLoading = false
                                        }
                                    }
                            }
                        } else {
                            friendsList.clear()
                            isLoading = false
                        }
                    } else {
                        friendsList.clear()
                        friendsCount = 0
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
                title = { Text("My Friends ($friendsCount)") },
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
            } else if (friendsList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "You don't have any friends yet. Find some in the Find Friends section!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(friendsList, key = { it.uid }) { friendUser ->
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
                                        text = friendUser.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = friendUser.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        currentUser?.uid?.let { currentId ->
                                            FirestoreUtils.removeFriend(
                                                firestore = firestore,
                                                currentUserId = currentId,
                                                friendId = friendUser.uid,
                                                context = context,
                                                onSuccess = { 
                                                    Toast.makeText(context, "Removed ${friendUser.displayName}", Toast.LENGTH_SHORT).show() 
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
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}