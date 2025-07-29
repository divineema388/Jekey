package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.utils.FirestoreUtils
import jeky.dealabs.utils.TextUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostFullScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val currentUser = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isPosting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        val selection = textFieldValue.selection
                        textFieldValue = TextFieldValue(
                            annotatedString = TextUtils.toggleStyle(
                                textFieldValue.annotatedString,
                                selection,
                                SpanStyle(fontWeight = FontWeight.Bold)
                            ),
                            selection = selection
                        )
                    }
                ) {
                    Text("B")
                }

                Button(
                    onClick = {
                        val selection = textFieldValue.selection
                        textFieldValue = TextFieldValue(
                            annotatedString = TextUtils.toggleStyle(
                                textFieldValue.annotatedString,
                                selection,
                                SpanStyle(fontStyle = FontStyle.Italic)
                            ),
                            selection = selection
                        )
                    }
                ) {
                    Text("I")
                }
            }

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                },
                label = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp),
                singleLine = false,
                maxLines = Int.MAX_VALUE
            )

            Button(
                onClick = {
                    if (currentUser == null) {
                        Toast.makeText(context, "You must be logged in to post.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (textFieldValue.text.isBlank()) {
                        Toast.makeText(context, "Please enter some text for your post.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isPosting = true
                    FirestoreUtils.savePostToFirestore(
                        firestore,
                        currentUser.uid,
                        currentUser.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "User",
                        textFieldValue.annotatedString.toString(),
                        null,
                        onSuccess = {
                            Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                            isPosting = false
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Failed to create post: ${e.message}", Toast.LENGTH_LONG).show()
                            isPosting = false
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isPosting && textFieldValue.text.isNotBlank()
            ) {
                Text(if (isPosting) "Posting..." else "Post")
            }
        }
    }
}