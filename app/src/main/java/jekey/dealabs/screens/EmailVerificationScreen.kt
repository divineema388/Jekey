// jeky/dealabs/screens/EmailVerificationScreen.kt
package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import jeky.dealabs.navigation.Screen

@Composable
fun EmailVerificationScreen(
    navController: NavHostController, 
    auth: FirebaseAuth,
    userEmail: String,
    userName: String
) {
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Check verification status periodically
    LaunchedEffect(Unit) {
        // Auto-check verification status every 3 seconds
        while (true) {
            kotlinx.coroutines.delay(3000)
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate to Dashboard
                        navController.navigate(Screen.Dashboard.createRoute(userName)) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify Your Email",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "We've sent a verification link to:",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = userEmail,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Please check your email and click the verification link. This page will automatically update once your email is verified.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Manual check button
        Button(
            onClick = {
                isLoading = true
                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user?.isEmailVerified == true) {
                            Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Dashboard.createRoute(userName)) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Email not verified yet. Please check your email.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to check verification status", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (isLoading) "Checking..." else "I've Verified My Email")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resend verification email button
        Button(
            onClick = {
                auth.currentUser?.sendEmailVerification()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Verification email sent again!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to resend email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Resend Verification Email")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back to signup button
        Button(
            onClick = {
                // Sign out the user and go back to signup
                auth.signOut()
                navController.navigate(Screen.SignUp.route) {
                    popUpTo(Screen.Welcome.route)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Back to Sign Up")
        }
    }
}