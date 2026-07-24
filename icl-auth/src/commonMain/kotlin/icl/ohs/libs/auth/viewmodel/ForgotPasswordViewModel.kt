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
import icl.ohs.libs.auth.ForgotPasswordScreenConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns [ForgotPasswordScreen][icl.ohs.libs.auth.ForgotPasswordScreen]'s form state. The actual
 * "forgot password" request is supplied by the hosting app (there's no fixed backend contract for
 * it yet), so this view model validates input and drives that suspend callback rather than talking
 * to the network layer directly.
 */
internal class ForgotPasswordViewModel(
  private val config: ForgotPasswordScreenConfig,
  initialIdentifier: String,
) {

  private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  var identifier by mutableStateOf(initialIdentifier)
    private set

  var errorMessage by mutableStateOf<String?>(null)
    private set

  var isSubmitting by mutableStateOf(false)
    private set

  var isSubmitted by mutableStateOf(false)
    private set

  val canSubmit: Boolean
    get() = identifier.isNotBlank() && !isSubmitting

  fun onIdentifierChange(value: String) {
    identifier = value
    errorMessage = null
  }

  fun dismissError() {
    errorMessage = null
  }

  fun submit(onSubmit: suspend (identifier: String) -> Result<Unit>) {
    if (isSubmitting) return

    if (identifier.isBlank()) {
      errorMessage = config.emptyEmailMessage
      return
    }

    viewModelScope.launch {
      isSubmitting = true
      errorMessage = null
      onSubmit(identifier)
        .onSuccess { isSubmitted = true }
        .onFailure { errorMessage = it.message ?: config.emptyEmailMessage }
      isSubmitting = false
    }
  }

  fun submitIAlreadyHaveCode(onNavigate: (identifier: String) -> Unit) {
    val trimmedIdentifier = identifier.trim()
    if (trimmedIdentifier.isBlank()) {
      errorMessage = config.emptyEmailMessage
      return
    }

    onNavigate(trimmedIdentifier)
  }

  fun clear() {
    viewModelScope.cancel()
  }
}
