package com.crowdfunding.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crowdfunding.data.AuthRepository
import com.crowdfunding.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false
)

class PublicProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = authRepository.getUserProfile(userId)
            _uiState.update { it.copy(userProfile = profile, isLoading = false) }
        }
    }
}
