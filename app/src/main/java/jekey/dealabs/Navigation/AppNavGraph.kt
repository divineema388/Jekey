package jeky.dealabs.navigation

// Define sealed class for your screens
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome_screen")
    object SignUp : Screen("signup_screen")
    object Login : Screen("login_screen")
    
    // Dashboard needs a username argument
    object Dashboard : Screen("dashboard_screen/{username}") {
        fun createRoute(username: String) = "dashboard_screen/$username"
    }
    
    // Email verification screen with email and name arguments
    object EmailVerification : Screen("email_verification_screen/{email}/{name}") {
        fun createRoute(email: String, name: String) = "email_verification_screen/$email/$name"
    }

    object Settings : Screen("settings_screen")

    // NEW SCREEN: ChangePassword - Re-adding this!
    object ChangePassword : Screen("change_password_screen")
    
    // NEW SCREEN: ForgotPassword
    object ForgotPassword : Screen("forgot_password_screen")
    
    object CreatePost : Screen("create_post_screen")
    
    object Profile : Screen("profile_screen")
}