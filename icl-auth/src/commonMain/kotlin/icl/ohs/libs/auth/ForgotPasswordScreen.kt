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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import icl.ohs.libs.auth.viewmodel.ForgotPasswordViewModel

internal const val FORGOT_PASSWORD_EMAIL_TAG = "forgot_password_email"
internal const val FORGOT_PASSWORD_SUBMIT_BUTTON_TAG = "forgot_password_submit_button"
internal const val FORGOT_PASSWORD_HAVE_CODE_TAG = "forgot_password_have_code_button"

/** Pure UI: form state, validation, and the submit call live in [ForgotPasswordViewModel]. */
@Composable
fun ForgotPasswordScreen(
  onSubmit: suspend (identifier: String) -> Result<Unit>,
  onBackToLoginClick: () -> Unit,
  modifier: Modifier = Modifier,
  config: ForgotPasswordScreenConfig = ForgotPasswordScreenConfig(),
  initialIdentifier: String = "",
  onIAlreadyHaveCodeClick: (String) -> Unit = {},
) {
  val viewModel =
    remember(config, initialIdentifier) {
      ForgotPasswordViewModel(config = config, initialIdentifier = initialIdentifier)
    }

  DisposableEffect(viewModel) { onDispose { viewModel.clear() } }

  val canSubmit = viewModel.canSubmit

  Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.surface) {
    innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .systemBarsPadding()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                  MaterialTheme.colorScheme.surface,
                )
            )
          )
          .padding(innerPadding)
          .imePadding(),
      contentAlignment = Alignment.Center,
    ) {
      if (viewModel.errorMessage != null) {
        AuthMessageBanner(
          message = viewModel.errorMessage.orEmpty(),
          type = AuthMessageBannerType.Error,
          onDismiss = viewModel::dismissError,
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
            text = "Forgot Password",
            modifier = Modifier.padding(top = if (config.showLogo) 20.dp else 0.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text = "Enter your username or email and we'll send you a reset link.",
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
            if (viewModel.isSubmitted) {
              Text(
                text = "If an account exists for that username, a reset link is on its way.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              OutlinedTextField(
                value = viewModel.identifier,
                onValueChange = viewModel::onIdentifierChange,
                modifier = Modifier.fillMaxWidth().testTag(FORGOT_PASSWORD_EMAIL_TAG),
                label = { Text("Username or email") },
                placeholder = { Text("Enter your username or email") },
                singleLine = true,
                enabled = !viewModel.isSubmitting,
                keyboardOptions =
                  KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions =
                  KeyboardActions(onDone = { if (canSubmit) viewModel.submit(onSubmit) }),
              )

              Button(
                onClick = { viewModel.submit(onSubmit) },
                modifier = Modifier.fillMaxWidth().testTag(FORGOT_PASSWORD_SUBMIT_BUTTON_TAG),
                enabled = canSubmit,
                shape = RoundedCornerShape(18.dp),
              ) {
                Text(if (viewModel.isSubmitting) "Sending..." else "Send reset link")
              }
            }

            TextButton(
              onClick = { viewModel.submitIAlreadyHaveCode(onIAlreadyHaveCodeClick) },
              modifier = Modifier.testTag(FORGOT_PASSWORD_HAVE_CODE_TAG),
              enabled = !viewModel.isSubmitting,
            ) {
              Text("I already have the code")
            }

            TextButton(onClick = onBackToLoginClick, enabled = !viewModel.isSubmitting) {
              Text("Back to login")
            }
          }
        }
      }
    }
  }
}
