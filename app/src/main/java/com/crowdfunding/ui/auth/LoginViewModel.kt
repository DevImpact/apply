package com.crowdfunding.ui.auth

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crowdfunding.App
import com.crowdfunding.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false
)

class LoginViewModel(
    application: Application,
    private val repository: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val exceptionHandler = (application as App).coroutineExceptionHandler

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onPasswordVisibilityChange() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "البريد الإلكتروني غير صالح") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "كلمة المرور قصيرة جدًا") }
            return
        }

        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.signIn(email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "بيانات الدخول غير صحيحة") }
            }
        }
    }
}

class LoginViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
