package com.crowdfunding.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.ui.res.stringResource
import com.crowdfunding.R
import com.crowdfunding.ui.common.StandardTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    viewModel: PublicProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.fetchUserProfile(userId)
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = stringResource(R.string.title_public_profile),
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.userProfile != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.userProfile!!.fullName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.userProfile!!.email,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Text(stringResource(R.string.error_user_not_found))
            }
        }
    }
}
