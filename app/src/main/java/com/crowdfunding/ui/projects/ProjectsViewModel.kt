package com.crowdfunding.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crowdfunding.data.ProjectsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false
)

class ProjectsViewModel(
    private val repository: ProjectsRepository = ProjectsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchProjects()
    }

    private fun fetchProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val projects = repository.getProjects()
            _uiState.update { it.copy(projects = projects, isLoading = false) }
        }
    }
}
