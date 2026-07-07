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
package icl.ohs.libs.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal const val RESET_PASSWORD_OTP_TAG = "reset_password_otp"
internal const val RESET_PASSWORD_NEW_TAG = "reset_password_new"
internal const val RESET_PASSWORD_CONFIRM_TAG = "reset_password_confirm"
internal const val RESET_PASSWORD_BUTTON_TAG = "reset_password_button"

@Composable
fun ResetPasswordScreen(
  config: ResetPasswordScreenConfig,
  identifier: String,
  onPasswordResetSuccess: (ResetPasswordSuccess) -> Unit,
  modifier: Modifier = Modifier,
  onPasswordResetFailure: (ResetPasswordFailure) -> Unit = {},
  onBackToLoginClick: (() -> Unit)? = null,
  onTermsAndConditionsClick: () -> Unit = {},
) {
  var otp by rememberSaveable { mutableStateOf("") }
  var newPassword by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }
  var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
  var isSubmitting by rememberSaveable { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()
  val resolvedConfig = resolveResetPasswordConfig(screenConfig = config)
  val httpClient =
    remember(resolvedConfig.requestTimeoutMillis) {
      buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
    }
  val loginService = remember(httpClient) { LoginService(httpClient) }

  DisposableEffect(httpClient) { onDispose { httpClient.close() } }

  ResetPasswordScreenContent(
    otp = otp,
    newPassword = newPassword,
    confirmPassword = confirmPassword,
    modifier = modifier,
    config = config,
    errorMessage = errorMessage,
    isSubmitting = isSubmitting,
    onTermsAndConditionsClick = onTermsAndConditionsClick,
    onOtpChange = {
      otp = it
      errorMessage = null
    },
    onNewPasswordChange = {
      newPassword = it
      errorMessage = null
    },
    onConfirmPasswordChange = {
      confirmPassword = it
      errorMessage = null
    },
    onSubmitClick = {
      if (isSubmitting) {
        return@ResetPasswordScreenContent
      }

      val request = ResetPasswordReq(otp = otp, identifier = identifier, password = newPassword)
      val validationFailure =
        validateResetPasswordForm(
          config = resolvedConfig,
          request = request,
          confirmPassword = confirmPassword,
        )
      if (validationFailure != null) {
        errorMessage = validationFailure.message
        onPasswordResetFailure(validationFailure)
        return@ResetPasswordScreenContent
      }

      coroutineScope.launch {
        isSubmitting = true
        errorMessage = null

        try {
          when (
            val result = loginService.resetPassword(config = resolvedConfig, request = request)
          ) {
            is ResetPasswordAttemptResult.Success -> {
              errorMessage = null
              onPasswordResetSuccess(result.value)
            }

            is ResetPasswordAttemptResult.Failure -> {
              errorMessage = result.value.message
              onPasswordResetFailure(result.value)
            }
          }
        } finally {
          isSubmitting = false
        }
      }
    },
    onBackToLoginClick = onBackToLoginClick,
  )
}

@Composable
private fun ResetPasswordScreenContent(
  otp: String,
  newPassword: String,
  confirmPassword: String,
  onOtpChange: (String) -> Unit,
  onNewPasswordChange: (String) -> Unit,
  onConfirmPasswordChange: (String) -> Unit,
  onSubmitClick: () -> Unit,
  config: ResetPasswordScreenConfig,
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
  isSubmitting: Boolean = false,
  onBackToLoginClick: (() -> Unit)? = null,
  onTermsAndConditionsClick: () -> Unit = {},
) {
  val isPasswordLongEnough = newPassword.length >= config.minPasswordLength
  val passwordsMatch = confirmPassword.isNotEmpty() && confirmPassword == newPassword
  val canSubmit =
    otp.isNotBlank() &&
      newPassword.isNotBlank() &&
      confirmPassword.isNotBlank() &&
      isPasswordLongEnough &&
      passwordsMatch &&
      !isSubmitting

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    bottomBar = {
      if (config.showFooter) {
        LoginFooter(
          onTermsAndConditionsClick = onTermsAndConditionsClick,
          modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .systemBarsPadding()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                  MaterialTheme.colorScheme.surface,
                )
            )
          )
          .padding(innerPadding)
          .imePadding(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 460.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          if (config.showLogo) {
            AuthLogo()
          }
          Text(
            text = "Reset Password",
            modifier = Modifier.padding(top = if (config.showLogo) 20.dp else 0.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text = "Enter the OTP sent to your registered email and choose a new password.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(28.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            OutlinedTextField(
              value = otp,
              onValueChange = onOtpChange,
              modifier = Modifier.fillMaxWidth().testTag(RESET_PASSWORD_OTP_TAG),
              label = { Text("OTP") },
              placeholder = { Text("Enter the code sent to your email") },
              singleLine = true,
              enabled = !isSubmitting,
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                modifier = Modifier.fillMaxWidth().testTag(RESET_PASSWORD_NEW_TAG),
                label = { Text("New password") },
                placeholder = { Text("Enter your new password") },
                singleLine = true,
                enabled = !isSubmitting,
                isError = newPassword.isNotEmpty() && !isPasswordLongEnough,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions =
                  KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
              )
              if (newPassword.isNotEmpty()) {
                Text(
                  text =
                    if (isPasswordLongEnough) {
                      "Meets minimum length requirement."
                    } else {
                      "Must be at least ${config.minPasswordLength} characters."
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (isPasswordLongEnough) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.error
                    },
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth().testTag(RESET_PASSWORD_CONFIRM_TAG),
                label = { Text("Confirm password") },
                placeholder = { Text("Confirm your new password") },
                singleLine = true,
                enabled = !isSubmitting,
                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions =
                  KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmitClick() }),
              )
              if (confirmPassword.isNotEmpty()) {
                Text(
                  text = if (passwordsMatch) "Passwords match." else "Passwords do not match.",
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (passwordsMatch) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.error
                    },
                )
              }
            }

            if (errorMessage != null) {
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
              )
            }

            Button(
              onClick = onSubmitClick,
              modifier = Modifier.fillMaxWidth().testTag(RESET_PASSWORD_BUTTON_TAG),
              enabled = canSubmit,
              shape = RoundedCornerShape(18.dp),
            ) {
              Text(if (isSubmitting) "Resetting..." else "Reset password")
            }

            if (onBackToLoginClick != null) {
              TextButton(onClick = onBackToLoginClick, enabled = !isSubmitting) {
                Text("Back to login")
              }
            }
          }
        }
      }
    }
  }
}

private fun validateResetPasswordForm(
  config: ResolvedResetPasswordConfig,
  request: ResetPasswordReq,
  confirmPassword: String,
): ResetPasswordFailure? =
  when {
    confirmPassword.isBlank() ->
      ResetPasswordFailure(message = config.messages.emptyConfirmPassword)
    request.password != confirmPassword ->
      ResetPasswordFailure(message = config.messages.passwordMismatch)
    else -> validateResetPasswordRequest(config = config, request = request)
  }
