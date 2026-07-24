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
import icl.ohs.libs.auth.SetNewPasswordScreenConfig
import icl.ohs.libs.auth.model.SetNewPasswordFailure
import icl.ohs.libs.auth.model.SetNewPasswordReq
import icl.ohs.libs.auth.model.SetNewPasswordSuccess
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.SetNewPasswordAttemptResult
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.resolveSetNewPasswordConfig
import icl.ohs.libs.auth.network.validateSetNewPasswordRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns [SetNewPasswordScreen][icl.ohs.libs.auth.SetNewPasswordScreen]'s form state, the cross-field
 * validation, and the network call.
 */
internal class SetNewPasswordViewModel(
  config: SetNewPasswordScreenConfig,
  initialIdNumber: String,
) {

  private val resolvedConfig = resolveSetNewPasswordConfig(screenConfig = config)
  private val httpClient = buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
  private val loginService = LoginService(httpClient)
  private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private val idNumber = initialIdNumber

  var currentPassword by mutableStateOf("")
    private set

  var newPassword by mutableStateOf("")
    private set

  var confirmPassword by mutableStateOf("")
    private set

  var errorMessage by mutableStateOf<String?>(null)
    private set

  var isSubmitting by mutableStateOf(false)
    private set

  val canSubmit: Boolean
    get() =
      currentPassword.isNotBlank() &&
        newPassword.isNotBlank() &&
        confirmPassword.isNotBlank() &&
        !isSubmitting

  fun onCurrentPasswordChange(value: String) {
    currentPassword = value
    errorMessage = null
  }

  fun onNewPasswordChange(value: String) {
    newPassword = value
    errorMessage = null
  }

  fun onConfirmPasswordChange(value: String) {
    confirmPassword = value
    errorMessage = null
  }

  fun dismissError() {
    errorMessage = null
  }

  fun submit(
    onSuccess: (SetNewPasswordSuccess) -> Unit,
    onFailure: (SetNewPasswordFailure) -> Unit = {},
  ) {
    if (isSubmitting) return

    val request =
      SetNewPasswordReq(
        temporaryPassword = currentPassword,
        idNumber = idNumber,
        password = newPassword,
      )
    val validationFailure = validateForm(request)
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
          val result = loginService.setNewPassword(config = resolvedConfig, request = request)
        ) {
          is SetNewPasswordAttemptResult.Success -> {
            errorMessage = null
            onSuccess(result.value)
          }

          is SetNewPasswordAttemptResult.Failure -> {
            errorMessage = result.value.message
            onFailure(result.value)
          }
        }
      } finally {
        isSubmitting = false
      }
    }
  }

  private fun validateForm(request: SetNewPasswordReq): SetNewPasswordFailure? =
    when {
      confirmPassword.isBlank() ->
        SetNewPasswordFailure(message = resolvedConfig.messages.emptyConfirmPassword)

      request.password != confirmPassword ->
        SetNewPasswordFailure(message = resolvedConfig.messages.passwordMismatch)

      else -> validateSetNewPasswordRequest(config = resolvedConfig, request = request)
    }

  fun clear() {
    viewModelScope.cancel()
    httpClient.close()
  }
}
