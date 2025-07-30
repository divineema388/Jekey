package jeky.dealabs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import jeky.dealabs.screens.ForgotPasswordScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.utils.NotificationUtils

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        
        // Setup notifications
        setupNotifications()

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

    // Add this to your setupNotifications() method in MainActivity
private fun setupNotifications() {
    // Create notification channel
    NotificationUtils.createNotificationChannel(this)
    
    // Get FCM token and save to user document
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            return@addOnCompleteListener
        }

        // Get new FCM registration token
        val token = task.result
        val currentUser = auth.currentUser
        
        // DEBUG: Log the token for testing
        println("FCM Token: $token")
        android.util.Log.d("FCM_TOKEN", "Token: $token")
        
        if (currentUser != null && token != null) {
            firestore.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
        }
    }
    
    // Check for pending notifications when user logs in
    auth.currentUser?.let { user ->
        NotificationUtils.checkPendingNotifications(this, firestore, user.uid)
    }
    
    // Handle navigation from notification
    handleNotificationNavigation()
}

    private fun handleNotificationNavigation() {
        val navigationTarget = intent.getStringExtra("navigation_target")
        if (navigationTarget == "friend_requests") {
            // The navigation will be handled in the composable when it detects this intent
            intent.putExtra("should_navigate_to_requests", true)
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, auth: FirebaseAuth) {
    // Check if we should navigate to friend requests from notification
    LaunchedEffect(Unit) {
        // This will be triggered when the app starts from a notification
        val shouldNavigate = navController.context.let { context ->
            if (context is MainActivity) {
                context.intent.getBooleanExtra("should_navigate_to_requests", false)
            } else false
        }
        
        if (shouldNavigate && auth.currentUser != null) {
            navController.navigate(Screen.FriendRequests.route) {
                // Clear the flag to prevent repeated navigation
                if (navController.context is MainActivity) {
                    (navController.context as MainActivity).intent.removeExtra("should_navigate_to_requests")
                }
            }
        }
    }

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
            FindFriendsScreen(navController = navController, auth = auth)
        }
        composable(Screen.FriendRequests.route) {
            FriendRequestsScreen(navController = navController, auth = auth)
        }
        composable(Screen.MyFriends.route) {
            MyFriendsScreen(navController = navController, auth = auth)
        }
    }
}