package com.crowdfunding.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.crowdfunding.R
import com.crowdfunding.ui.common.StandardTopAppBar

@Composable
fun ProjectsScreen(
    onNavigateToProjectDetail: (String) -> Unit,
    viewModel: ProjectsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            StandardTopAppBar(title = stringResource(R.string.title_projects))
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.projects.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(stringResource(R.string.no_projects_yet), style = MaterialTheme.typography.titleMedium)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.projects) { project ->
                        ProjectItem(
                            project = project,
                            onItemClick = { onNavigateToProjectDetail(project.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectItem(project: Project, onItemClick: () -> Unit) {
    Card(
        onClick = onItemClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Use new fields, with fallback to old fields for compatibility
            Text(
                text = project.title.ifEmpty { project.name },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = project.description.ifEmpty { project.shortDescription },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            project.goal?.let {
                Text(
                    text = stringResource(R.string.goal_prefix) + " $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            project.status?.let {
                Text(
                    text = stringResource(R.string.status_prefix) + " $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
