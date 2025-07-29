// jeky/dealabs/screens/SettingsScreen.kt
package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // For back button
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import jeky.dealabs.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Go back to previous screen
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
            horizontalAlignment = Alignment.Start, // Align items to the start for a menu-like feel
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Account Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            Button(
                onClick = {
                    auth.signOut()
                    Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true } // Clear back stack
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Red color for logout
            ) {
                Text("Logout")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Change Password Button
            Button(
                onClick = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Change Password")
            }
            // You can add more settings options here later
        }
    }
}