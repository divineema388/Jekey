// jeky/dealabs/screens/DashboardScreen.kt
package jeky.dealabs.screens

import android.content.Intent
import android.net.Uri
import jeky.dealabs.utils.FirestoreUtils // Keep this import!
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.components.PostCard
import jeky.dealabs.models.Post
import jeky.dealabs.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, username: String, auth: FirebaseAuth) {
    val context = LocalContext.current
    val currentUser = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val posts = remember { mutableStateListOf<Post>() }

    LaunchedEffect(Unit) {
        firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading posts: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newPosts = snapshot.toObjects(Post::class.java)
                    posts.clear()
                    posts.addAll(newPosts)
                }
            }
    }

    val onLikeClick: (String) -> Unit = { postId ->
        currentUser?.let { user ->
            val postRef = firestore.collection("posts").document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentLikes = snapshot.toObject(Post::class.java)?.likes?.toMutableList() ?: mutableListOf()

                if (currentLikes.contains(user.uid)) {
                    currentLikes.remove(user.uid)
                } else {
                    currentLikes.add(user.uid)
                }
                transaction.update(postRef, "likes", currentLikes)
                null
            }.addOnSuccessListener {
                // UI will update automatically via snapshot listener
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update like: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(context, "Please log in to like posts.", Toast.LENGTH_SHORT).show()
    }

    // This onCommentClick doesn't directly open the dialog here,
    // it's passed to PostCard where the dialog state is managed.
    // However, it fulfills the PostCard's function signature.
    val onCommentClick: (String) -> Unit = { postId ->
        // No action needed here as PostCard manages its own dialog state.
        // This is just to satisfy the PostCard signature.
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Menu",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Divider()
                NavigationDrawerItem(
    label = { Text("• Profile") },
    selected = false,
    onClick = {
        scope.launch { drawerState.close() }
        navController.navigate(Screen.Profile.route)
    },
    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
)
                NavigationDrawerItem(
                    label = { Text("• Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("• Support") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pupchat.infy.uk"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("• Find Friends") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.FindFriends.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("• Friend Requests") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.FriendRequests.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("• My Friends") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.MyFriends.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation drawer")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.CreatePost.route) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Create New Post")
                        }
                    }
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = "Welcome, ${currentUser?.displayName ?: username}!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No posts yet. Be the first to post!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentUserId = currentUser?.uid,
                                onLikeClick = onLikeClick,
                                onCommentClick = onCommentClick // Pass the onCommentClick lambda here
                            )
                        }
                    }
                }
            }
        }
    }
}
// The redundant private fun FirestoreUtils.savePostToFirestore has been removed from here.
// The public version in jeky.dealabs.utils.FirestoreUtils handles this now.