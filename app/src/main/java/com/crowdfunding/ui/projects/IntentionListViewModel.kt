package com.crowdfunding.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crowdfunding.data.AuthRepository
import com.crowdfunding.data.ProjectsRepository
import com.crowdfunding.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IntentionListUiState(
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false
)

class IntentionListViewModel(
    private val projectsRepository: ProjectsRepository = ProjectsRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntentionListUiState())
    val uiState = _uiState.asStateFlow()

    fun fetchIntentingUsers(projectId: String, intentionType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userIds = projectsRepository.getIntentingUserIds(projectId, intentionType)
            val userProfiles = userIds.mapNotNull { authRepository.getUserProfile(it) }
            _uiState.update { it.copy(users = userProfiles, isLoading = false) }
        }
    }
}
