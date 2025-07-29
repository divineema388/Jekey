// jeky/dealabs/screens/VerifyEmailScreen.kt
package jeky.dealabs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import com.google.firebase.auth.ktx.auth // Required for auth.currentUser
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay // For simple delay
import kotlinx.coroutines.launch // For coroutine scope

@Composable
fun VerifyEmailScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val user = auth.currentUser
    var isVerificationEmailSent by remember { mutableStateOf(false) }
    var isCheckInProgress by remember { mutableStateOf(false) } // To prevent rapid checks
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Automatically send verification email when screen loads, if not already sent
        if (user != null && !isVerificationEmailSent) {
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Verification email sent to ${user.email}", Toast.LENGTH_LONG).show()
                        isVerificationEmailSent = true
                    } else {
                        Toast.makeText(context, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hold on! Verify it's you.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "A verification code/link has been sent to your email address:\n${user?.email ?: "N/A"}",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Please check your inbox (and spam folder) and click the verification link. Then click 'I've Verified' below.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Button to check verification status
        Button(
            onClick = {
                if (isCheckInProgress) return@Button // Prevent multiple clicks

                isCheckInProgress = true
                coroutineScope.launch {
                    user?.reload()?.addOnCompleteListener { reloadTask ->
                        if (reloadTask.isSuccessful) {
                            // User reloaded, now check verification status
                            if (user.isEmailVerified) {
                                Toast.makeText(context, "Email successfully verified!", Toast.LENGTH_SHORT).show()
                                // Navigate to Dashboard or wherever the user should go next
                                // Assuming Dashboard is the next logical step after successful verification
                                navController.navigate(jeky.dealabs.navigation.Screen.Dashboard.createRoute(user.displayName ?: "User")) {
                                    // Clear back stack to prevent going back to verification screen
                                    popUpTo(jeky.dealabs.navigation.Screen.Welcome.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Email not yet verified. Please click the link in your email.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to reload user: ${reloadTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                        isCheckInProgress = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isCheckInProgress) "Checking..." else "I've Verified My Email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to resend verification email
        Button(
            onClick = {
                if (isVerificationEmailSent) {
                    Toast.makeText(context, "Please wait a moment before resending.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Verification email resent to ${user.email}", Toast.LENGTH_LONG).show()
                            isVerificationEmailSent = true
                            // Optionally, disable resend button for a short period
                            coroutineScope.launch {
                                delay(60000) // Wait 60 seconds before allowing resend again
                                isVerificationEmailSent = false
                            }
                        } else {
                            Toast.makeText(context, "Failed to resend verification email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    } ?: Toast.makeText(context, "No active user found.", Toast.LENGTH_SHORT).show()
            },
            enabled = !isVerificationEmailSent, // Disable if email was just sent
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Resend Verification Email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout option
        Button(
            onClick = {
                auth.signOut()
                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                navController.navigate(jeky.dealabs.navigation.Screen.Login.route) {
                    popUpTo(jeky.dealabs.navigation.Screen.Welcome.route) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Logout")
        }
    }
}