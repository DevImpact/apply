package com.crowdfunding.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.os.Bundle
import com.crowdfunding.App
import com.crowdfunding.data.AuthRepository
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileState(
    val firstName: String = "",
    val lastName: String = "",
    val isFacebookLinked: Boolean = false,
    val facebookName: String? = null,
    val facebookPhotoUrl: String? = null,
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
        checkInitialFacebookLinkStatus()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch(exceptionHandler) {
            val userId = Firebase.auth.currentUser?.uid ?: return@launch
            val profile = authRepository.getUserProfile(userId)
            if (profile != null) {
                _uiState.update { it.copy(
                    facebookName = profile.facebookName,
                    facebookPhotoUrl = profile.facebookPhotoUrl
                ) }
            }
        }
    }

    private fun checkInitialFacebookLinkStatus() {
        val isLinked = Firebase.auth.currentUser?.providerData?.any { it.providerId == "facebook.com" } ?: false
        if (isLinked) {
            _uiState.update { it.copy(isFacebookLinked = true) }
        }
    }

    fun onFirstNameChange(firstName: String) {
        _uiState.update { it.copy(firstName = firstName, errorMessage = null) }
    }

    fun onLastNameChange(lastName: String) {
        _uiState.update { it.copy(lastName = lastName, errorMessage = null) }
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
                _eventFlow.emit(Event.ShowToast("Profile activated"))
                _eventFlow.emit(Event.Activated) // الآن نرسل حدث التنقل بعد التأكد من النجاح
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to activate profile"
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(Event.ShowToast("Activation failed: $msg"))
            }
        }
    }

    fun onFacebookLoginSuccess(token: AccessToken) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.linkFacebookAccount(token)

            if (result.isSuccess) {
                _uiState.update { it.copy(isFacebookLinked = true) }
                _eventFlow.emit(Event.ShowToast("Facebook account linked successfully!"))
                fetchAndSaveFacebookProfile(token)
            } else {
                val error = result.exceptionOrNull()?.message ?: "An unknown error occurred."
                _uiState.update { it.copy(isLoading = false, errorMessage = error) }
                _eventFlow.emit(Event.ShowToast("Error linking account: $error"))
            }
        }
    }

    private fun fetchAndSaveFacebookProfile(token: AccessToken) {
        val request = GraphRequest.newMeRequest(token) { me, _ ->
            if (me != null) {
                val name = me.optString("name")
                val photoUrl = me.optJSONObject("picture")?.optJSONObject("data")?.optString("url")

                if (name != null && photoUrl != null) {
                    viewModelScope.launch(exceptionHandler) {
                        val result = authRepository.updateUserFacebookProfile(name, photoUrl)
                        if (result.isSuccess) {
                            _uiState.update { it.copy(isLoading = false, facebookName = name, facebookPhotoUrl = photoUrl) }
                            _eventFlow.emit(Event.ShowToast("Facebook profile info updated."))
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                            _eventFlow.emit(Event.ShowToast("Failed to save Facebook info to our database."))
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                 _uiState.update { it.copy(isLoading = false) }
                 _eventFlow.tryEmit(Event.ShowToast("Failed to fetch Facebook profile details."))
            }
        }
        val parameters = Bundle()
        parameters.putString("fields", "name,picture.type(large)")
        request.parameters = parameters
        request.executeAsync()
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
