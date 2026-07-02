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

data class IclAuthConfig(
  val baseAuthUrl: String,
  val defaultRequestHeaders: Map<String, String> = emptyMap(),
  val requestTimeoutMillis: Long = 15_000,
  val responseMessageKeys: List<String> = listOf("message", "error", "detail"),
  val messages: LoginMessages = LoginMessages(),
)

data class LoginMessages(
  val missingLoginUrl: String = "Configure the auth base URL and endpoint to enable sign in.",
  val emptyCredentials: String = "Enter your username and password to continue.",
  val emptyUsername: String = "Enter your username to continue.",
  val emptyPassword: String = "Enter your password to continue.",
  val invalidCredentials: String = "Invalid username or password.",
  val networkError: String = "Unable to reach the login service. Please try again.",
  val serverError: String = "Unable to sign in right now. Please try again.",
  val unexpectedError: String = "Something went wrong. Please try again.",
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

data class LoginSuccess(val statusCode: Int, val responseBody: String)

data class LoginFailure(
  val message: String,
  val statusCode: Int? = null,
  val responseBody: String? = null,
)
