/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package icl.ohs.libs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import icl.ohs.libs.auth.LoginScreenConfig
import icl.ohs.libs.auth.model.LoginFailure
import icl.ohs.libs.auth.model.LoginSuccess
import icl.ohs.libs.auth.network.LoginAttemptResult
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.resolveLoginConfig
import icl.ohs.libs.auth.network.validateLoginRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns [LoginScreen][icl.ohs.libs.auth.LoginScreen]'s form state, field validation, and the network
 * call - none of which belongs in the composable itself. The screen only reads state and forwards
 * user actions.
 */
internal class LoginViewModel(config: LoginScreenConfig) {

  private val resolvedConfig = resolveLoginConfig(screenConfig = config)
  private val httpClient = buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
  private val loginService = LoginService(httpClient)
  private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  var username by mutableStateOf("")
    private set

  var password by mutableStateOf("")
    private set

  var errorMessage by mutableStateOf<String?>(null)
    private set

  var isSubmitting by mutableStateOf(false)
    private set

  val canSubmit: Boolean
    get() = username.isNotBlank() && password.isNotBlank() && !isSubmitting

  fun onUsernameChange(value: String) {
    username = value
    errorMessage = null
  }

  fun onPasswordChange(value: String) {
    password = value
    errorMessage = null
  }

  fun dismissError() {
    errorMessage = null
  }

  fun login(onSuccess: (LoginSuccess) -> Unit, onFailure: (LoginFailure) -> Unit = {}) {
    if (isSubmitting) return

    val validationFailure =
      validateLoginRequest(config = resolvedConfig, username = username, password = password)
    if (validationFailure != null) {
      errorMessage = validationFailure.message
      onFailure(validationFailure)
      return
    }

    viewModelScope.launch {
      isSubmitting = true
      errorMessage = null

      try {
        when (
          val result =
            loginService.login(config = resolvedConfig, username = username, password = password)
        ) {
          is LoginAttemptResult.Success -> {
            errorMessage = null
            onSuccess(result.value)
          }

          is LoginAttemptResult.Failure -> {
            errorMessage = result.value.message
            onFailure(result.value)
          }
        }
      } finally {
        isSubmitting = false
      }
    }
  }

  /** Validates there's a username to look up, then hands off navigation to the caller. */
  fun submitForgotPassword(onNavigate: (username: String) -> Unit) {
    val trimmedUsername = username.trim()
    if (trimmedUsername.isBlank()) {
      errorMessage = resolvedConfig.messages.emptyUsername
      return
    }

    onNavigate(trimmedUsername)
  }

  /** Cancels in-flight work and releases the HTTP client. Call from the screen's onDispose. */
  fun clear() {
    viewModelScope.cancel()
    httpClient.close()
  }
}
