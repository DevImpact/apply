package com.crowdfunding.ui.projects

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crowdfunding.R
import com.crowdfunding.data.IntentionKeys
import com.crowdfunding.ui.common.StandardTopAppBar

@Composable
fun ProjectDetailScreen(
    projectId: String,
    onNavigateToIntentionList: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProjectDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(projectId) {
        viewModel.fetchProjectDetails(projectId)
    }

    val project = uiState.project

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = stringResource(R.string.title_project_details),
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (project != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = project.title.ifEmpty { project.name }, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = project.description.ifEmpty { project.detailedDescription }, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.goal_prefix) + " ${project.goal ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.status_prefix) + " ${project.status ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.statistics), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.investors_count, project.stats.investors))
                Text(stringResource(R.string.donors_count, project.stats.donors))
                Text(stringResource(R.string.advertisers_count, project.stats.advertisers))
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.attachments), style = MaterialTheme.typography.titleMedium)
                project.pdfLinks.values.forEach { link ->
                    TextButton(onClick = { downloadFile(context, link, "project_file.pdf") }) {
                        Text(stringResource(R.string.download_pdf_prefix) + " ${Uri.parse(link).lastPathSegment}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.my_intention_prefix) + " ${uiState.currentUserIntention ?: stringResource(R.string.intention_none)}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.handleIntention(IntentionKeys.INVESTORS) }) { Text(stringResource(R.string.invest_button)) }
                    Button(onClick = { viewModel.handleIntention(IntentionKeys.DONORS) }) { Text(stringResource(R.string.donate_button)) }
                    Button(onClick = { viewModel.handleIntention(IntentionKeys.ADVERTISERS) }) { Text(stringResource(R.string.advertise_button)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { onNavigateToIntentionList(project.id, IntentionKeys.INVESTORS) }) {
                        Text(stringResource(R.string.see_investors_button))
                    }
                    Button(onClick = { onNavigateToIntentionList(project.id, IntentionKeys.DONORS) }) {
                        Text(stringResource(R.string.see_donors_button))
                    }
                    Button(onClick = { onNavigateToIntentionList(project.id, IntentionKeys.ADVERTISERS) }) {
                        Text(stringResource(R.string.see_advertisers_button))
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.error_project_not_found))
            }
        }
    }
}

fun downloadFile(context: Context, url: String, fileName: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription(context.getString(R.string.downloading))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    } catch (e: Exception) {
        // Handle exceptions, e.g., invalid URL
    }
}
