package com.crowdfunding.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.os.Bundle
import com.crowdfunding.App
import com.crowdfunding.data.AuthRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileState(
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class Event {
    data class ShowToast(val message: String) : Event()
    object Activated : Event()
}

class ProfileViewModel(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<Event>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val exceptionHandler = (application as App).coroutineExceptionHandler

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch(exceptionHandler) {
            val userId = Firebase.auth.currentUser?.uid ?: return@launch
            val profile = authRepository.getUserProfile(userId)
            if (profile != null) {
                _uiState.update { it.copy(
                    phoneNumber = profile.phoneNumber ?: ""
                ) }
            }
        }
    }

    fun onFirstNameChange(firstName: String) {
        _uiState.update { it.copy(firstName = firstName, errorMessage = null) }
    }

    fun onLastNameChange(lastName: String) {
        _uiState.update { it.copy(lastName = lastName, errorMessage = null) }
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = phoneNumber, errorMessage = null) }
    }

    fun activateProfile() {
        if (_uiState.value.firstName.isBlank() || _uiState.value.lastName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "First and last name are required.") }
            return
        }

        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // تأكد من وجود مستخدم مُسجّل
            if (authRepository.getCurrentUserId() == null) {
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(Event.ShowToast("Please log in first"))
                return@launch
            }

            val fullName = "${_uiState.value.firstName} ${_uiState.value.lastName}"
            val result = authRepository.saveUserProfile(fullName = fullName, activated = true)

            if (result.isSuccess) {
                val phoneResult = authRepository.updateUserPhoneNumber(_uiState.value.phoneNumber)
                if (phoneResult.isSuccess) {
                    _eventFlow.emit(Event.ShowToast("Profile activated"))
                    _eventFlow.emit(Event.Activated) // الآن نرسل حدث التنقل بعد التأكد من النجاح
                } else {
                    val msg = phoneResult.exceptionOrNull()?.message ?: "Failed to update phone number"
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(Event.ShowToast("Activation failed: $msg"))
                }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to activate profile"
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(Event.ShowToast("Activation failed: $msg"))
            }
        }
    }
}

class ProfileViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
