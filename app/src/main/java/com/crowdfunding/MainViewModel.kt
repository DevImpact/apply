package com.crowdfunding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crowdfunding.data.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class AppState {
    object Loading : AppState()
    object NeedsLogin : AppState()
    data class NeedsActivation(val user: FirebaseUser) : AppState()
    object Ready : AppState()
}

class MainViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    val appState: StateFlow<AppState> = repository.getAuthState()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(AppState.NeedsLogin)
            } else {
                repository.observeUserProfile(user.uid).map { userProfile ->
                    if (userProfile?.activated == true) {
                        AppState.Ready
                    } else {
                        AppState.NeedsActivation(user)
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppState.Loading
        )
}
