// jeky/dealabs/screens/LoginScreen.kt
package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton // Import TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import jeky.dealabs.navigation.Screen

@Composable
fun LoginScreen(navController: NavHostController, auth: FirebaseAuth) {
    // State variables to hold user input for email and password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current // For displaying Toast messages

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Login to Your Account", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(32.dp))

        // Email input field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Password input field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(), // Hides password
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Login button
        Button(
            onClick = {
                // Basic input validation
                if (email.isNotBlank() && password.isNotBlank()) {
                    // Firebase: Sign in user with email and password
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Login successful
                                val user = auth.currentUser
                                // Get the user's display name, default to "User" if not set
                                val username = user?.displayName ?: "User"
                                Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                // Navigate to Dashboard on successful login
                                navController.navigate(Screen.Dashboard.createRoute(username)) {
                                    // Clear the back stack up to the Welcome screen
                                    // This prevents the user from going back to Login/Signup after logging in
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            } else {
                                // Login failed (e.g., wrong credentials, no internet)
                                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Login")
        }

        // --- NEW: Forgot Password Button ---
        Spacer(modifier = Modifier.height(8.dp)) // Small spacer
        TextButton(
            onClick = { navController.navigate(Screen.ForgotPassword.route) }, // Navigate to ForgotPassword screen
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Forgot Password?")
        }
        // --- END NEW ---

        Spacer(modifier = Modifier.height(16.dp))

        // Button to navigate to Signup screen if user doesn't have an account
        Button(
            onClick = { navController.navigate(Screen.SignUp.route) },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}