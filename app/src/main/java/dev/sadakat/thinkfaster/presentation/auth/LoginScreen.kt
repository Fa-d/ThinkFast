package dev.sadakat.thinkfaster.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.activity.ComponentActivity
import com.facebook.CallbackManager
import dev.sadakat.thinkfaster.data.sync.auth.FacebookLoginHelper
import org.koin.androidx.compose.koinViewModel

/**
 * LoginScreen - Social authentication screen
 * Phase 5: UI Integration
 * 
 * Allows users to sign in with Facebook or Google to enable sync
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Facebook callback manager - must be remembered
    val callbackManager = remember { FacebookLoginHelper.createCallbackManager() }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                viewModel.handleGoogleSignInResult(data)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon/Illustration
            Text(
                text = "☁️",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Title
            Text(
                text = "Sync Your Data",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Description
            Text(
                text = "Sign in to sync your data across multiple devices and never lose your progress",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            
            // Benefits list
            BenefitItem("📱", "Sync across all your devices")
            BenefitItem("☁️", "Your data is safely backed up")
            BenefitItem("🔐", "Privacy-first: only your data, encrypted")
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Facebook Login Button
         /*   SocialLoginButton(
                text = "Continue with Facebook",
                icon = "f",
                backgroundColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    activity?.let { act ->
                        FacebookLoginHelper.login(
                            activity = act,
                            callbackManager = callbackManager,
                            onSuccess = { accessToken ->
                                viewModel.handleFacebookLoginResult(accessToken)
                            },
                            onError = { error ->
                                viewModel.handleFacebookLoginError(error)
                            },
                            onCancel = {
                                viewModel.handleFacebookLoginCancel()
                            }
                        )
                    }
                },
                enabled = !uiState.isLoading && activity != null
            )*/

            Spacer(modifier = Modifier.height(12.dp))

            // Google Login Button
            SocialLoginButton(
                text = "Continue with Google",
                icon = "G",
                backgroundColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    val intent = viewModel.getGoogleSignInIntent()
                    googleSignInLauncher.launch(intent)
                },
                enabled = !uiState.isLoading
            )
            
            // Loading indicator
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator()
            }
            
            // Error message
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Privacy note
            Text(
                text = "Your data is encrypted and private. We'll never share your information.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
    
    // Handle login success
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }
}

@Composable
private fun BenefitItem(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = text,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    icon: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
