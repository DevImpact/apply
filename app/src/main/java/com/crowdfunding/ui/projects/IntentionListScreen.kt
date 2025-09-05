package com.crowdfunding.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crowdfunding.R
import com.crowdfunding.data.UserProfile
import com.crowdfunding.ui.common.StandardTopAppBar
import com.crowdfunding.util.safeClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionListScreen(
    title: String,
    projectId: String,
    intentionType: String,
    viewModel: IntentionListViewModel = viewModel(),
    onNavigateToPublicProfile: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchIntentingUsers(projectId, intentionType)
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.users) { user ->
                    UserItem(
                        user = user,
                        onItemClick = { onNavigateToPublicProfile(user.uid) } // Assuming UserProfile has a uid
                    )
                }
            }
        }
    }
}

@Composable
fun UserItem(user: UserProfile, onItemClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(rethrow = true, onClick = onItemClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = stringResource(R.string.user_avatar_content_description),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = user.fullName, style = MaterialTheme.typography.bodyLarge)
    }
}
