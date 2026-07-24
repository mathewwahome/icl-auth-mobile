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
import androidx.compose.runtime.remember
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
import icl.ohs.libs.auth.model.ResetPasswordFailure
import icl.ohs.libs.auth.model.ResetPasswordSuccess
import icl.ohs.libs.auth.viewmodel.ResetPasswordViewModel

internal const val RESET_PASSWORD_OTP_TAG = "reset_password_otp"
internal const val RESET_PASSWORD_NEW_TAG = "reset_password_new"
internal const val RESET_PASSWORD_CONFIRM_TAG = "reset_password_confirm"
internal const val RESET_PASSWORD_BUTTON_TAG = "reset_password_button"

/**
 * Pure UI: form state, OTP/password validation, and the network call live in
 * [ResetPasswordViewModel].
 */
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
  val viewModel =
    remember(config, identifier) {
      ResetPasswordViewModel(config = config, identifier = identifier)
    }

  DisposableEffect(viewModel) { onDispose { viewModel.clear() } }

  ResetPasswordScreenContent(
    otp = viewModel.otp,
    newPassword = viewModel.newPassword,
    confirmPassword = viewModel.confirmPassword,
    modifier = modifier,
    config = config,
    errorMessage = viewModel.errorMessage,
    isSubmitting = viewModel.isSubmitting,
    isPasswordLongEnough = viewModel.isPasswordLongEnough,
    passwordsMatch = viewModel.passwordsMatch,
    canSubmit = viewModel.canSubmit,
    onTermsAndConditionsClick = onTermsAndConditionsClick,
    onOtpChange = viewModel::onOtpChange,
    onNewPasswordChange = viewModel::onNewPasswordChange,
    onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
    onSubmitClick = {
      viewModel.submit(onSuccess = onPasswordResetSuccess, onFailure = onPasswordResetFailure)
    },
    onBackToLoginClick = onBackToLoginClick,
    onErrorDismiss = viewModel::dismissError,
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
  onErrorDismiss: () -> Unit,
  config: ResetPasswordScreenConfig,
  isPasswordLongEnough: Boolean,
  passwordsMatch: Boolean,
  canSubmit: Boolean,
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
  isSubmitting: Boolean = false,
  onBackToLoginClick: (() -> Unit)? = null,
  onTermsAndConditionsClick: () -> Unit = {},
) {
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
      if (errorMessage != null) {
        AuthMessageBanner(
          message = errorMessage,
          type = AuthMessageBannerType.Error,
          onDismiss = onErrorDismiss,
          modifier =
            Modifier.align(Alignment.TopCenter).padding(start = 20.dp, top = 12.dp, end = 20.dp),
        )
      }
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
