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
import icl.ohs.libs.auth.ResetPasswordScreenConfig
import icl.ohs.libs.auth.model.ResetPasswordFailure
import icl.ohs.libs.auth.model.ResetPasswordReq
import icl.ohs.libs.auth.model.ResetPasswordSuccess
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.ResetPasswordAttemptResult
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.resolveResetPasswordConfig
import icl.ohs.libs.auth.network.validateResetPasswordRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns [ResetPasswordScreen][icl.ohs.libs.auth.ResetPasswordScreen]'s form state, the OTP/password
 * cross-field validation, and the network call.
 */
internal class ResetPasswordViewModel(
  config: ResetPasswordScreenConfig,
  private val identifier: String,
) {

  private val resolvedConfig = resolveResetPasswordConfig(screenConfig = config)
  private val httpClient = buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
  private val loginService = LoginService(httpClient)
  private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  var otp by mutableStateOf("")
    private set

  var newPassword by mutableStateOf("")
    private set

  var confirmPassword by mutableStateOf("")
    private set

  var errorMessage by mutableStateOf<String?>(null)
    private set

  var isSubmitting by mutableStateOf(false)
    private set

  val minPasswordLength: Int
    get() = resolvedConfig.minPasswordLength

  val isPasswordLongEnough: Boolean
    get() = newPassword.length >= minPasswordLength

  val passwordsMatch: Boolean
    get() = confirmPassword.isNotEmpty() && confirmPassword == newPassword

  val canSubmit: Boolean
    get() =
      otp.isNotBlank() &&
        newPassword.isNotBlank() &&
        confirmPassword.isNotBlank() &&
        isPasswordLongEnough &&
        passwordsMatch &&
        !isSubmitting

  fun onOtpChange(value: String) {
    otp = value
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
    onSuccess: (ResetPasswordSuccess) -> Unit,
    onFailure: (ResetPasswordFailure) -> Unit = {},
  ) {
    if (isSubmitting) return

    val request = ResetPasswordReq(otp = otp, identifier = identifier, password = newPassword)
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
        when (val result = loginService.resetPassword(config = resolvedConfig, request = request)) {
          is ResetPasswordAttemptResult.Success -> {
            errorMessage = null
            onSuccess(result.value)
          }

          is ResetPasswordAttemptResult.Failure -> {
            errorMessage = result.value.message
            onFailure(result.value)
          }
        }
      } finally {
        isSubmitting = false
      }
    }
  }

  private fun validateForm(request: ResetPasswordReq): ResetPasswordFailure? =
    when {
      confirmPassword.isBlank() ->
        ResetPasswordFailure(message = resolvedConfig.messages.emptyConfirmPassword)

      request.password != confirmPassword ->
        ResetPasswordFailure(message = resolvedConfig.messages.passwordMismatch)

      else -> validateResetPasswordRequest(config = resolvedConfig, request = request)
    }

  fun clear() {
    viewModelScope.cancel()
    httpClient.close()
  }
}
