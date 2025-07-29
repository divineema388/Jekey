package jeky.dealabs.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jeky.dealabs.models.User // Import the User model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val currentUser = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    
    var profileImageBase64 by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var friendCount by remember { mutableStateOf(0) } // State for friend count

    // Load user profile data
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .addSnapshotListener { document, e -> // Use addSnapshotListener for real-time updates
                    if (e != null) {
                        Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (document != null && document.exists()) {
                        val user = document.toObject(User::class.java)
                        profileImageBase64 = user?.profileImage
                        displayName = user?.displayName ?: displayName
                        email = user?.email ?: email
                        friendCount = user?.friends?.size ?: 0 // Update friend count
                    }
                }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                previewBitmap = bitmap
                showImagePreview = true
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to convert bitmap to base64
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Function to convert base64 to bitmap
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // Function to save profile image
    fun saveProfileImage(bitmap: Bitmap) {
        currentUser?.uid?.let { uid ->
            isLoading = true
            val base64Image = bitmapToBase64(bitmap)
            
            // Only update profileImage; other fields are handled by initial signup or other updates
            firestore.collection("users").document(uid)
                .update("profileImage", base64Image) // Use update instead of set to only change specific field
                .addOnSuccessListener {
                    profileImageBase64 = base64Image
                    isLoading = false
                    Toast.makeText(context, "Profile image updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image Section
            Card(
                modifier = Modifier
                    .size(150.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageBase64 != null) {
                        val bitmap = base64ToBitmap(profileImageBase64!!)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Default Profile",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Camera overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Tap to change profile photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // User Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profile Information",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { /* Read-only for now */ },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { /* Read-only for now */ },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Friends Count
                    Text(
                        text = "Friends: $friendCount",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }

    // Image Preview Dialog
    if (showImagePreview && previewBitmap != null) {
        Dialog(onDismissRequest = { showImagePreview = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Preview Profile Image",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Image Preview",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { 
                                showImagePreview = false
                                previewBitmap = null
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                saveProfileImage(previewBitmap!!)
                                showImagePreview = false
                                previewBitmap = null
                            }
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}