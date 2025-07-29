// jeky/dealabs/screens/ChangePasswordScreen.kt
package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar // New import for TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api // New import for ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold // New import for Scaffold
import androidx.compose.material.icons.Icons // New import for Icons
import androidx.compose.material.icons.filled.ArrowBack // New import for ArrowBack icon
import androidx.compose.material3.IconButton // New import for IconButton
import androidx.compose.material3.Icon // New import for Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth // Needed for Firebase.auth.currentUser
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class) // Opt-in for experimental Material 3 APIs
@Composable
fun ChangePasswordScreen(navController: NavHostController, auth: FirebaseAuth) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
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
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Update Your Password", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(32.dp))

            // Re-authenticate with current password (security measure)
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // New Password input field
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Confirm New Password input field
            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = { confirmNewPassword = it },
                label = { Text("Confirm New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val user = auth.currentUser
                    if (user == null) {
                        Toast.makeText(context, "No user is currently logged in.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                        return@Button
                    }

                    if (newPassword.isBlank() || confirmNewPassword.isBlank() || currentPassword.isBlank()) {
                        Toast.makeText(context, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (newPassword != confirmNewPassword) {
                        Toast.makeText(context, "New passwords do not match.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Re-authenticate user before changing password for security
                    user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPassword))
                        .addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                // Re-authentication successful, now update password
                                user.updatePassword(newPassword)
                                    .addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack() // Go back to Dashboard
                                        } else {
                                            Toast.makeText(context, "Failed to update password: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "Re-authentication failed: ${reauthTask.exception?.message}. Please verify your current password.", Toast.LENGTH_LONG).show()
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Change Password")
            }
        }
    }
}