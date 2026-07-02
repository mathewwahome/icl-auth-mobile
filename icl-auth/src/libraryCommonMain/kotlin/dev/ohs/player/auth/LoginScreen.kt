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
package dev.ohs.player.auth

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal const val LOGIN_USERNAME_TAG = "login_username"
internal const val LOGIN_PASSWORD_TAG = "login_password"
internal const val LOGIN_BUTTON_TAG = "login_button"

@Composable
fun LoginScreen(
  config: LoginScreenConfig,
  onLoginSuccess: (LoginSuccess) -> Unit,
  modifier: Modifier = Modifier,
  onLoginFailure: (LoginFailure) -> Unit = {},
  onForgotPasswordClick: () -> Unit = {},
  onTermsAndConditionsClick: () -> Unit = {},
) {
  var username by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
  var isSubmitting by rememberSaveable { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()
  val httpClient =
    remember(config.requestTimeoutMillis) { buildLoginHttpClient(config.requestTimeoutMillis) }
  val loginService = remember(httpClient) { LoginService(httpClient) }

  DisposableEffect(httpClient) { onDispose { httpClient.close() } }

  LoginScreenContent(
    username = username,
    password = password,
    modifier = modifier,
    errorMessage = errorMessage,
    isSubmitting = isSubmitting,
    config = config,
    onUsernameChange = {
      username = it
      errorMessage = null
    },
    onPasswordChange = {
      password = it
      errorMessage = null
    },
    onLoginClick = {
      if (isSubmitting) {
        return@LoginScreenContent
      }

      val validationFailure =
        validateLoginRequest(config = config, username = username, password = password)
      if (validationFailure != null) {
        errorMessage = validationFailure.message
        onLoginFailure(validationFailure)
        return@LoginScreenContent
      }

      coroutineScope.launch {
        isSubmitting = true
        errorMessage = null

        try {
          when (
            val result =
              loginService.login(config = config, username = username, password = password)
          ) {
            is LoginAttemptResult.Success -> {
              errorMessage = null
              onLoginSuccess(result.value)
            }
            is LoginAttemptResult.Failure -> {
              errorMessage = result.value.message
              onLoginFailure(result.value)
            }
          }
        } finally {
          isSubmitting = false
        }
      }
    },
    onForgotPasswordClick = onForgotPasswordClick,
    onTermsAndConditionsClick = onTermsAndConditionsClick,
  )
}

@Composable
private fun LoginScreenContent(
  username: String,
  password: String,
  onUsernameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLoginClick: () -> Unit,
  onForgotPasswordClick: () -> Unit,
  onTermsAndConditionsClick: () -> Unit,
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
  isSubmitting: Boolean = false,
  config: LoginScreenConfig,
) {
  val canSubmit = username.isNotBlank() && password.isNotBlank() && !isSubmitting

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
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
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
            text = "Welcome Back",
            modifier = Modifier.padding(top = if (config.showLogo) 20.dp else 0.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text = "Sign in to continue to the ICL reference experience.",
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
              value = username,
              onValueChange = onUsernameChange,
              modifier = Modifier.fillMaxWidth().testTag(LOGIN_USERNAME_TAG),
              label = { Text("Username") },
              placeholder = { Text("Enter your username") },
              singleLine = true,
              enabled = !isSubmitting,
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            )

            OutlinedTextField(
              value = password,
              onValueChange = onPasswordChange,
              modifier = Modifier.fillMaxWidth().testTag(LOGIN_PASSWORD_TAG),
              label = { Text("Password") },
              placeholder = { Text("Enter your password") },
              singleLine = true,
              enabled = !isSubmitting,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { if (canSubmit) onLoginClick() }),
            )

            if (config.showForgotPassword) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onForgotPasswordClick, enabled = !isSubmitting) {
                  Text("Forgot password?")
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
              onClick = onLoginClick,
              modifier = Modifier.fillMaxWidth().testTag(LOGIN_BUTTON_TAG),
              enabled = canSubmit,
              shape = RoundedCornerShape(18.dp),
            ) {
              Text(if (isSubmitting) "Signing in..." else "Log in")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LoginFooter(onTermsAndConditionsClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    tonalElevation = 4.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "By continuing, you agree to the platform Terms and Conditions.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      TextButton(onClick = onTermsAndConditionsClick) { Text("Terms and Conditions") }
    }
  }
}

@Composable
private fun AuthLogo(modifier: Modifier = Modifier) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  val onPrimary = MaterialTheme.colorScheme.onPrimary

  Surface(
    modifier = modifier.semantics { contentDescription = "ICL logo" },
    shape = RoundedCornerShape(28.dp),
    color = Color.Transparent,
  ) {
    Canvas(
      modifier =
        Modifier.size(92.dp)
          .background(
            color = primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(28.dp),
          )
          .padding(14.dp)
    ) {
      val width = size.width
      val height = size.height

      val outerHex =
        Path().apply {
          moveTo(width * 0.50f, 0f)
          lineTo(width * 0.90f, height * 0.22f)
          lineTo(width * 0.90f, height * 0.78f)
          lineTo(width * 0.50f, height)
          lineTo(width * 0.10f, height * 0.78f)
          lineTo(width * 0.10f, height * 0.22f)
          close()
        }

      val innerLeaf =
        Path().apply {
          moveTo(width * 0.52f, height * 0.18f)
          quadraticTo(width * 0.78f, height * 0.34f, width * 0.66f, height * 0.58f)
          quadraticTo(width * 0.58f, height * 0.74f, width * 0.42f, height * 0.74f)
          quadraticTo(width * 0.24f, height * 0.74f, width * 0.26f, height * 0.54f)
          quadraticTo(width * 0.28f, height * 0.32f, width * 0.52f, height * 0.18f)
          close()
        }

      val centerPulse =
        Path().apply {
          moveTo(width * 0.22f, height * 0.52f)
          lineTo(width * 0.38f, height * 0.52f)
          lineTo(width * 0.46f, height * 0.36f)
          lineTo(width * 0.56f, height * 0.70f)
          lineTo(width * 0.64f, height * 0.52f)
          lineTo(width * 0.78f, height * 0.52f)
          lineTo(width * 0.78f, height * 0.60f)
          lineTo(width * 0.60f, height * 0.60f)
          lineTo(width * 0.54f, height * 0.74f)
          lineTo(width * 0.44f, height * 0.42f)
          lineTo(width * 0.36f, height * 0.60f)
          lineTo(width * 0.22f, height * 0.60f)
          close()
        }

      drawPath(path = outerHex, color = primary, style = Fill)
      drawPath(path = innerLeaf, color = secondary, style = Fill)
      drawPath(path = centerPulse, color = onPrimary, style = Fill)
    }
  }
}
