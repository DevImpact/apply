package com.crowdfunding.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crowdfunding.data.AuthRepository
import com.crowdfunding.data.ProjectsRepository
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val project: Project? = null,
    val currentUserIntention: String? = null,
    val isLoading: Boolean = false
)

class ProjectDetailViewModel(
    private val projectsRepository: ProjectsRepository = ProjectsRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var statsListener: ValueEventListener? = null
    private val userId = authRepository.getCurrentUserId()

    fun fetchProjectDetails(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val project = projectsRepository.getProjectById(projectId)
            _uiState.update { it.copy(project = project, isLoading = false) }

            // Listen for real-time updates on stats
            statsListener = projectsRepository.addStatsListener(projectId) { newStats ->
                _uiState.update { it.copy(project = it.project?.copy(stats = newStats)) }
            }

            // Observe the current user's intention for this project
            if (userId != null) {
                authRepository.getUserIntentionForProject(userId, projectId).collect { intentionRecord ->
                    _uiState.update { it.copy(currentUserIntention = intentionRecord?.type) }
                }
            }
        }
    }

    fun handleIntention(intentionType: String) {
        viewModelScope.launch {
            if (userId == null) return@launch
            val project = _uiState.value.project ?: return@launch
            val previousIntention = _uiState.value.currentUserIntention

            if (intentionType == previousIntention) return@launch

            // This single repository call now handles the atomic update and the stats transaction
            projectsRepository.recordUserIntention(project.id, userId, intentionType, previousIntention)

            // The UI will update automatically from the listeners, but we can update it locally
            // for immediate feedback if desired. The listener will correct it anyway.
            _uiState.update { it.copy(currentUserIntention = intentionType) }
        }
    }

    override fun onCleared() {
        statsListener?.let { listener ->
            uiState.value.project?.id?.let { projectId ->
                projectsRepository.removeStatsListener(projectId, listener)
            }
        }
        super.onCleared()
    }
}
