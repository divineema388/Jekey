package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore // Import FirebaseFirestore
import jeky.dealabs.navigation.Screen
import jeky.dealabs.models.User // Import the User model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Initialize Firestore
    val firestore = FirebaseFirestore.getInstance() // Get Firestore instance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (confirmPasswordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || confirmPassword.isBlank() || username.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build()

                            user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { verificationTask ->
                                            if (verificationTask.isSuccessful) {
                                                // Save initial user data to Firestore
                                                val newUser = User(
                                                    uid = user.uid,
                                                    displayName = username,
                                                    email = email,
                                                    profileImage = null, // No profile image initially
                                                    friendRequestsSent = emptyList(),
                                                    friendRequestsReceived = emptyList(),
                                                    friends = emptyList()
                                                )
                                                firestore.collection("users").document(user.uid)
                                                    .set(newUser)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(context, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                                                        navController.navigate(Screen.EmailVerification.createRoute(email, username)) {
                                                            popUpTo(Screen.SignUp.route) { inclusive = true }
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                Toast.makeText(context, "Failed to send verification email: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Failed to update profile: ${profileTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign Up", fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Already have an account? Log In",
            modifier = Modifier.clickable { navController.navigate(Screen.Login.route) },
            color = MaterialTheme.colorScheme.primary
        )
    }
}