// jeky/dealabs/screens/WelcomeScreen.kt
package jeky.dealabs.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import jeky.dealabs.R // This will point to your resources (e.g., app_logo)
import jeky.dealabs.navigation.Screen

@Composable
fun WelcomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- IMPORTANT: You need to add an image to your res/drawable folder ---
        // For example, name it 'app_logo.png' or 'app_logo.xml' (vector drawable)
        // You can use a placeholder for now if you don't have one:
        // painterResource(id = android.R.drawable.sym_def_app_icon)
        Image(
            painter = painterResource(id = R.drawable.app_logo), // Make sure you have app_logo in drawable
            contentDescription = "App Logo",
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to Green Polish!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate(Screen.SignUp.route) }, // Navigate to Signup screen
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate(Screen.Login.route) }, // Navigate to Login screen
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            Text("Login")
        }
    }
}