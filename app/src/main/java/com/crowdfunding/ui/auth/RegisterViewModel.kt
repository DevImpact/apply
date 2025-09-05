package com.crowdfunding.ui.auth

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crowdfunding.App
import com.crowdfunding.data.AuthRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationSuccess: Boolean = false
)

class RegisterViewModel(
    application: Application,
    private val repository: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val exceptionHandler = (application as App).coroutineExceptionHandler

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun onPasswordVisibilityChange() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onConfirmPasswordVisibilityChange() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun register() {
        val state = _uiState.value
        if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(errorMessage = "البريد الإلكتروني غير صالح") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "كلمة المرور يجب أن تكون 6 أحرف على الأقل") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "كلمتا المرور غير متطابقتين") }
            return
        }

        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.createUser(state.email, state.password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
            }.onFailure { exception ->
                val errorMessage = when (exception) {
                    is FirebaseAuthUserCollisionException -> "هذا البريد مستخدم مسبقًا"
                    else -> "حدث خطأ غير متوقع"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }
}

class RegisterViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
