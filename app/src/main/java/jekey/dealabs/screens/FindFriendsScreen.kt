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
import com.google.firebase.firestore.ListenerRegistration
import jeky.dealabs.models.User
import jeky.dealabs.utils.FirestoreUtils

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
    
    // Remember the listener to avoid memory leaks
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    DisposableEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            listenerRegistration = firestore.collection("users").document(uid)
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

        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Function to perform search
    fun performSearch(query: String) {
        if (query.isNotBlank()) {
            isLoading = true
            firestore.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
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
                    performSearch(newValue)
                },
                label = { Text("Search by Display Name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (searchText.isNotBlank() && searchResults.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No users found with that name.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchText.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "Start typing to search for friends by their display name.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else if (searchResults.isNotEmpty()) {
                LazyColumn {
                    items(searchResults, key = { it.uid }) { user ->
                        val currentUserState = currentUserData.value
                        val isFriend = currentUserState?.friends?.contains(user.uid) == true
                        val hasSentRequest = currentUserState?.friendRequestsSent?.contains(user.uid) == true
                        val hasReceivedRequest = currentUserState?.friendRequestsReceived?.contains(user.uid) == true

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

                                Spacer(modifier = Modifier.width(8.dp))

                                when {
                                    isFriend -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Text(
                                                text = "✓ Friends",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    hasSentRequest -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        ) {
                                            Text(
                                                text = "Request Sent",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    hasReceivedRequest -> {
                                        Button(
                                            onClick = {
                                                currentUser?.uid?.let { currentId ->
                                                    FirestoreUtils.acceptFriendRequest(
                                                        firestore = firestore,
                                                        currentUserId = currentId,
                                                        requesterId = user.uid,
                                                        context = context,
                                                        onSuccess = { 
                                                            Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show() 
                                                        },
                                                        onFailure = { e -> 
                                                            Toast.makeText(context, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show() 
                                                        }
                                                    )
                                                }
                                            }
                                        ) {
                                            Text("Accept Request")
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = {
                                                currentUser?.uid?.let { currentId ->
                                                    FirestoreUtils.sendFriendRequest(
                                                        firestore = firestore,
                                                        currentUserId = currentId,
                                                        targetUserId = user.uid,
                                                        context = context,
                                                        onSuccess = { 
                                                            Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show() 
                                                        },
                                                        onFailure = { e -> 
                                                            Toast.makeText(context, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show() 
                                                        }
                                                    )
                                                }
                                            },
                                            enabled = currentUser?.uid != user.uid
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
}