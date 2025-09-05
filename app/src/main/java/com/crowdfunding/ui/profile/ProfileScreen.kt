package com.crowdfunding.ui.profile

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.crowdfunding.R
import com.crowdfunding.ui.common.StandardTopAppBar
import com.crowdfunding.util.CrashlyticsUtils
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.flow.collectLatest
import java.util.*

@Composable
fun ProfileScreen(
    email: String,
    onActivated: () -> Unit,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is Event.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                Event.Activated -> onActivated()
            }
        }
    }

    val callbackManager = remember { CallbackManager.Factory.create() }
    val loginManager = remember { LoginManager.getInstance() }

    DisposableEffect(Unit) {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                CrashlyticsUtils.safeRun(rethrow = false) {
                    viewModel.onFacebookLoginSuccess(result.accessToken)
                }
            }
            override fun onCancel() {
                Toast.makeText(context, context.getString(R.string.toast_facebook_login_canceled), Toast.LENGTH_SHORT).show()
            }
            override fun onError(error: FacebookException) {
                CrashlyticsUtils.record(error)
                Toast.makeText(context, context.getString(R.string.toast_facebook_login_error, error.message), Toast.LENGTH_LONG).show()
            }
        })
        onDispose {
            loginManager.unregisterCallback(callbackManager)
        }
    }

    val facebookLoginLauncher = rememberLauncherForActivityResult(
        contract = loginManager.createLogInActivityResultContract(callbackManager)
    ) { /* Result handled by callback */ }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = stringResource(R.string.title_profile))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.complete_your_profile), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = email, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text(stringResource(R.string.first_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text(stringResource(R.string.last_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null
            )
            Spacer(modifier = Modifier.height(16.dp))

            val facebookName = uiState.facebookName
            val facebookPhotoUrl = uiState.facebookPhotoUrl

            if (facebookName != null && facebookPhotoUrl != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(facebookPhotoUrl),
                        contentDescription = stringResource(R.string.user_avatar_content_description),
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = facebookName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = { CrashlyticsUtils.safeRun(rethrow = false) { facebookLoginLauncher.launch(listOf("email", "public_profile")) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isFacebookLinked
            ) {
                val buttonText = if (uiState.isFacebookLinked) {
                    stringResource(id = R.string.facebook_linked_button)
                } else {
                    stringResource(id = R.string.confirm_facebook_link)
                }
                Text(buttonText)
            }
            Spacer(modifier = Modifier.height(8.dp))

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { CrashlyticsUtils.safeRun(rethrow = false) { viewModel.activateProfile() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.activate_profile))
                }
            }
        }
    }
}
