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
import androidx.compose.foundation.layout.Row
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

internal const val SET_NEW_PASSWORD_CURRENT_TAG = "set_new_password_current"
internal const val SET_NEW_PASSWORD_NEW_TAG = "set_new_password_new"
internal const val SET_NEW_PASSWORD_CONFIRM_TAG = "set_new_password_confirm"
internal const val SET_NEW_PASSWORD_BUTTON_TAG = "set_new_password_button"

@Composable
fun SetNewPasswordScreen(
  config: SetNewPasswordScreenConfig,
  initialIdNumber: String = "",
  onPasswordResetSuccess: (SetNewPasswordSuccess) -> Unit,
  modifier: Modifier = Modifier,
  onPasswordResetFailure: (SetNewPasswordFailure) -> Unit = {},
  onBackToLoginClick: (() -> Unit)? = null,
  onTermsAndConditionsClick: () -> Unit = {},
  onPrivacyPolicyClick: (() -> Unit)? = null,
) {
  var idNumber by rememberSaveable(initialIdNumber) { mutableStateOf(initialIdNumber) }
  var currentPassword by rememberSaveable { mutableStateOf("") }
  var newPassword by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }
  var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
  var isSubmitting by rememberSaveable { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()
  val resolvedConfig = resolveSetNewPasswordConfig(screenConfig = config)
  val httpClient =
    remember(resolvedConfig.requestTimeoutMillis) {
      buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
    }
  val loginService = remember(httpClient) { LoginService(httpClient) }

  DisposableEffect(httpClient) { onDispose { httpClient.close() } }

  SetNewPasswordScreenContent(
    currentPassword = currentPassword,
    newPassword = newPassword,
    confirmPassword = confirmPassword,
    modifier = modifier,
    config = config,
    errorMessage = errorMessage,
    isSubmitting = isSubmitting,
    onTermsAndConditionsClick = onTermsAndConditionsClick,
    onPrivacyPolicyClick = onPrivacyPolicyClick,
    onCurrentPasswordChange = {
      currentPassword = it
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
        return@SetNewPasswordScreenContent
      }

      val validationFailure =
        validateSetNewPasswordForm(
          config = resolvedConfig,
          request =
            SetNewPasswordReq(
              temporaryPassword = currentPassword,
              idNumber = idNumber,
              password = newPassword,
            ),
          confirmPassword = confirmPassword,
        )
      if (validationFailure != null) {
        errorMessage = validationFailure.message
        onPasswordResetFailure(validationFailure)
        return@SetNewPasswordScreenContent
      }

      coroutineScope.launch {
        isSubmitting = true
        errorMessage = null

        try {
          when (
            val result =
              loginService.setNewPassword(
                config = resolvedConfig,
                request =
                  SetNewPasswordReq(
                    temporaryPassword = currentPassword,
                    idNumber = idNumber,
                    password = newPassword,
                  ),
              )
          ) {
            is SetNewPasswordAttemptResult.Success -> {
              errorMessage = null
              onPasswordResetSuccess(result.value)
            }

            is SetNewPasswordAttemptResult.Failure -> {
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
private fun SetNewPasswordScreenContent(
  currentPassword: String,
  newPassword: String,
  confirmPassword: String,
  onCurrentPasswordChange: (String) -> Unit,
  onNewPasswordChange: (String) -> Unit,
  onConfirmPasswordChange: (String) -> Unit,
  onSubmitClick: () -> Unit,
  config: SetNewPasswordScreenConfig,
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
  isSubmitting: Boolean = false,
  onBackToLoginClick: (() -> Unit)? = null,
  onTermsAndConditionsClick: () -> Unit = {},
  onPrivacyPolicyClick: (() -> Unit)? = null,
) {
  val canSubmit =
    currentPassword.isNotBlank() &&
      newPassword.isNotBlank() &&
      confirmPassword.isNotBlank() &&
      !isSubmitting

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    bottomBar = {
      if (config.showFooter) {
        LoginFooter(
          onTermsAndConditionsClick = onTermsAndConditionsClick,
          modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
          onPrivacyPolicyClick = onPrivacyPolicyClick,
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
          onDismiss = {},
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
            text = "Reset Your Password",
            modifier = Modifier.padding(top = if (config.showLogo) 20.dp else 0.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text = "Enter your current or temporary password, then choose a new password.",
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
              value = currentPassword,
              onValueChange = onCurrentPasswordChange,
              modifier = Modifier.fillMaxWidth().testTag(SET_NEW_PASSWORD_CURRENT_TAG),
              label = { Text("Current or temporary password") },
              placeholder = { Text("Enter your current or temporary password") },
              singleLine = true,
              enabled = !isSubmitting,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )

            OutlinedTextField(
              value = newPassword,
              onValueChange = onNewPasswordChange,
              modifier = Modifier.fillMaxWidth().testTag(SET_NEW_PASSWORD_NEW_TAG),
              label = { Text("New password") },
              placeholder = { Text("Enter your new password") },
              singleLine = true,
              enabled = !isSubmitting,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )

            OutlinedTextField(
              value = confirmPassword,
              onValueChange = onConfirmPasswordChange,
              modifier = Modifier.fillMaxWidth().testTag(SET_NEW_PASSWORD_CONFIRM_TAG),
              label = { Text("Confirm password") },
              placeholder = { Text("Confirm your new password") },
              singleLine = true,
              enabled = !isSubmitting,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmitClick() }),
            )

            if (onBackToLoginClick != null) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onBackToLoginClick, enabled = !isSubmitting) {
                  Text("Back to login")
                }
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
              modifier = Modifier.fillMaxWidth().testTag(SET_NEW_PASSWORD_BUTTON_TAG),
              enabled = canSubmit,
              shape = RoundedCornerShape(18.dp),
            ) {
              Text(if (isSubmitting) "Updating..." else "Update password")
            }
          }
        }
      }
    }
  }
}

private fun validateSetNewPasswordForm(
  config: ResolvedSetNewPasswordConfig,
  request: SetNewPasswordReq,
  confirmPassword: String,
): SetNewPasswordFailure? =
  when {
    confirmPassword.isBlank() ->
      SetNewPasswordFailure(message = config.messages.emptyConfirmPassword)

    request.password != confirmPassword ->
      SetNewPasswordFailure(message = config.messages.passwordMismatch)

    else -> validateSetNewPasswordRequest(config = config, request = request)
  }
