package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.models.User
import jeky.dealabs.utils.FirestoreUtils // Import the FirestoreUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendsScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var searchText by remember { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(false) }

    // State to hold the current user's friend-related data
    val currentUserData = remember { mutableStateOf<User?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        currentUserData.value = snapshot.toObject(User::class.java)
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Friends") },
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
            OutlinedTextField(
                value = searchText,
                onValueChange = { newValue ->
                    searchText = newValue
                    if (newValue.isNotBlank()) {
                        isLoading = true
                        firestore.collection("users")
                            .whereGreaterThanOrEqualTo("displayName", newValue)
                            .whereLessThanOrEqualTo("displayName", newValue + "\uf8ff")
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                searchResults.clear()
                                val users = querySnapshot.documents.mapNotNull { it.toObject(User::class.java) }
                                // Filter out current user from search results
                                searchResults.addAll(users.filter { it.uid != currentUser?.uid })
                                isLoading = false
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error searching: ${e.message}", Toast.LENGTH_SHORT).show()
                                isLoading = false
                            }
                    } else {
                        searchResults.clear()
                    }
                },
                label = { Text("Search by Display Name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (searchText.isNotBlank() && searchResults.isEmpty()) {
                Text(
                    text = "No users found with that name.",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (searchResults.isNotEmpty()) {
                LazyColumn {
                    items(searchResults) { user ->
                        val isFriend = currentUserData.value?.friends?.contains(user.uid) == true
                        val hasSentRequest = currentUserData.value?.friendRequestsSent?.contains(user.uid) == true
                        val hasReceivedRequest = currentUserData.value?.friendRequestsReceived?.contains(user.uid) == true

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

                                Spacer(modifier = Modifier.width(8.dp))

                                if (isFriend) {
                                    Text("Friends", color = MaterialTheme.colorScheme.primary)
                                } else if (hasSentRequest) {
                                    Text("Request Sent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else if (hasReceivedRequest) {
                                    Button(
                                        onClick = {
                                            currentUser?.uid?.let { currentId ->
                                                FirestoreUtils.acceptFriendRequest(
                                                    firestore = firestore,
                                                    currentUserId = currentId,
                                                    requesterId = user.uid,
                                                    context = context,
                                                    onSuccess = { Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show() },
                                                    onFailure = { e -> Toast.makeText(context, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show() }
                                                )
                                            }
                                        }
                                    ) {
                                        Text("Accept")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            currentUser?.uid?.let { currentId ->
                                                FirestoreUtils.sendFriendRequest(
                                                    firestore = firestore,
                                                    currentUserId = currentId,
                                                    targetUserId = user.uid,
                                                    context = context,
                                                    onSuccess = { Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show() },
                                                    onFailure = { e -> Toast.makeText(context, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show() }
                                                )
                                            }
                                        },
                                        enabled = currentUser?.uid != user.uid // Disable sending request to self
                                    ) {
                                        Text("Add Friend")
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