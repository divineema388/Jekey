package jeky.dealabs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch  
import androidx.compose.material.icons.filled.People
import androidx.navigation.navArgument
import jeky.dealabs.navigation.Screen
import jeky.dealabs.ui.theme.GreenPolishTheme
import jeky.dealabs.screens.DashboardScreen
import jeky.dealabs.screens.LoginScreen
import jeky.dealabs.screens.ProfileScreen
import jeky.dealabs.screens.SignupScreen
import jeky.dealabs.screens.EmailVerificationScreen
import jeky.dealabs.screens.WelcomeScreen
import jeky.dealabs.screens.CreatePostFullScreen
import jeky.dealabs.screens.FindFriendsScreen
import jeky.dealabs.screens.FriendRequestsScreen
import jeky.dealabs.screens.MyFriendsScreen
import jeky.dealabs.screens.SettingsScreen
import jeky.dealabs.screens.ChangePasswordScreen
import jeky.dealabs.screens.ForgotPasswordScreen // ADD THIS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        setContent {
            GreenPolishTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController, auth = auth)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, auth: FirebaseAuth) {
    NavHost(
        navController = navController,
        startDestination = if (auth.currentUser != null && auth.currentUser?.isEmailVerified == true) {
            Screen.Dashboard.createRoute(auth.currentUser?.displayName ?: "User")
        } else {
            Screen.Welcome.route
        }
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(navController = navController)
        }
        composable(Screen.SignUp.route) {
            SignupScreen(navController = navController, auth = auth)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController, auth = auth)
        }
        composable(
            route = Screen.EmailVerification.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            
            EmailVerificationScreen(
                navController = navController,
                auth = auth,
                userEmail = email,
                userName = name
            )
        }
        composable(Screen.Dashboard.route) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: "User"
            DashboardScreen(navController = navController, username = username, auth = auth)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController, auth = auth)
        }
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(navController = navController, auth = auth)
        }
        composable("forgot_password_screen") {
            ForgotPasswordScreen(navController = navController, auth = auth)
        }
        composable("profile_screen") {
            ProfileScreen(navController = navController, auth = auth)
        }
        composable(Screen.CreatePost.route) {
            CreatePostFullScreen(navController = navController, auth = auth)
        }
        composable(Screen.FindFriends.route) {
            FindFriendsScreen(navController = navController, auth = auth) // Use actual composable
        }
        composable(Screen.FriendRequests.route) {
            FriendRequestsScreen(navController = navController, auth = auth) // Use actual composable
        }
        composable(Screen.MyFriends.route) {
            MyFriendsScreen(navController = navController, auth = auth) // Use actual composable
        }
    }
}