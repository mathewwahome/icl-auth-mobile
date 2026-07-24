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

import icl.ohs.libs.auth.model.AuthSessionStore
import icl.ohs.libs.auth.model.InMemoryAuthSessionStore
import icl.ohs.libs.auth.model.LoginMessages
import icl.ohs.libs.auth.model.ResetPasswordMessages
import icl.ohs.libs.auth.model.SetNewPasswordMessages

/**
 * Public configuration surface of the auth library. These types stay in the top-level
 * `icl.ohs.libs.auth` package (rather than under `model`/`network`) because they are the library's
 * public API - hosting apps construct and pass them to [IclAuth.initialize] and to the screen
 * composables.
 */
data class IclAuthConfig(
  val baseAuthUrl: String,
  val providerProfileEndpoint: String = "/provider/me",
  val defaultRequestHeaders: Map<String, String> = emptyMap(),
  val requestTimeoutMillis: Long = 15_000,
  val responseMessageKeys: List<String> = listOf("message", "error", "detail"),
  val messages: LoginMessages = LoginMessages(),
  val sessionStore: AuthSessionStore = InMemoryAuthSessionStore,
)

data class LoginScreenConfig(
  val endpoint: String,
  val showLogo: Boolean = true,
  val showFooter: Boolean = true,
  val showForgotPassword: Boolean = true,
  val requestHeaders: Map<String, String> = emptyMap(),
  val usernameFieldName: String = "idNumber",
  val passwordFieldName: String = "password",
  val requestTimeoutMillis: Long? = null,
  val responseMessageKeys: List<String>? = null,
  val messages: LoginMessages? = null,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)? = null,
)

data class ForgotPasswordScreenConfig(
  val showLogo: Boolean = true,
  val emptyEmailMessage: String = "Enter your username or email to continue.",
)

data class SetNewPasswordScreenConfig(
  val endpoint: String = "/provider/reset-password",
  val showLogo: Boolean = true,
  val showFooter: Boolean = true,
  val requestHeaders: Map<String, String> = emptyMap(),
  val requestTimeoutMillis: Long? = null,
  val responseMessageKeys: List<String>? = null,
  val messages: SetNewPasswordMessages? = null,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)? = null,
)

data class ResetPasswordScreenConfig(
  val endpoint: String = "/provider/reset-password",
  val showLogo: Boolean = true,
  val showFooter: Boolean = true,
  val minPasswordLength: Int = 8,
  val requestHeaders: Map<String, String> = emptyMap(),
  val requestTimeoutMillis: Long? = null,
  val responseMessageKeys: List<String>? = null,
  val messages: ResetPasswordMessages? = null,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)? = null,
)
