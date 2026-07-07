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

import kotlin.time.Clock
import kotlin.time.Instant

data class IclAuthConfig(
  val baseAuthUrl: String,
  val providerProfileEndpoint: String = "/provider/me",
  val defaultRequestHeaders: Map<String, String> = emptyMap(),
  val requestTimeoutMillis: Long = 15_000,
  val responseMessageKeys: List<String> = listOf("message", "error", "detail"),
  val messages: LoginMessages = LoginMessages(),
  val sessionStore: AuthSessionStore = InMemoryAuthSessionStore,
)

data class LoginMessages(
  val missingLoginUrl: String = "Configure the auth base URL and endpoint to enable sign in.",
  val missingProviderProfileUrl: String =
    "Configure the provider profile endpoint to complete sign in.",
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

data class SetNewPasswordMessages(
  val missingResetPasswordUrl: String = "Configure the reset password URL to continue.",
  val missingIdNumber: String = "Unable to determine which account to update.",
  val emptyTemporaryPassword: String = "Enter your current password to continue.",
  val emptyPassword: String = "Enter your new password to continue.",
  val emptyConfirmPassword: String = "Confirm your new password to continue.",
  val passwordMismatch: String = "New password and confirm password must match.",
  val samePassword: String = "Choose a new password that is different from the current one.",
  val networkError: String = "Unable to reach the password reset service. Please try again.",
  val serverError: String = "Unable to update your password right now. Please try again.",
  val unexpectedError: String = "Something went wrong. Please try again.",
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

data class SetNewPasswordReq(
  val temporaryPassword: String,
  val idNumber: String,
  val password: String,
)

data class ResetPasswordMessages(
  val missingResetPasswordUrl: String = "Configure the reset password URL to continue.",
  val missingIdentifier: String = "Unable to determine which account to reset.",
  val emptyOtp: String = "Enter the OTP sent to your email.",
  val emptyPassword: String = "Enter your new password to continue.",
  val emptyConfirmPassword: String = "Confirm your new password to continue.",
  val passwordTooShort: String = "Password must be at least 8 characters.",
  val passwordMismatch: String = "New password and confirm password must match.",
  val invalidOtp: String = "The OTP you entered is invalid or has expired.",
  val networkError: String = "Unable to reach the password reset service. Please try again.",
  val serverError: String = "Unable to reset your password right now. Please try again.",
  val unexpectedError: String = "Something went wrong. Please try again.",
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

data class ResetPasswordReq(val otp: String, val identifier: String, val password: String)

data class LoginTokenResponse(
  val accessToken: String? = null,
  val expiresIn: Long? = null,
  val refreshExpiresIn: Long? = null,
  val refreshToken: String? = null,
  val tokenType: String? = null,
  val notBeforePolicy: Long? = null,
  val sessionState: String? = null,
  val scope: String? = null,
  val firstLogin: Boolean? = null,
  val status: String? = null,
)

data class ProviderProfile(val status: String? = null, val user: ProviderUser? = null)

data class ProviderUser(
  val firstName: String? = null,
  val lastName: String? = null,
  val fhirPractitionerId: String? = null,
  val practitionerRole: String? = null,
  val role: String? = null,
  val status: Boolean? = null,
  val id: String? = null,
  val idNumber: String? = null,
  val fullNames: String? = null,
  val phone: String? = null,
  val email: String? = null,
  val locationInfo: ProviderLocationInfo? = null,
)

data class ProviderLocationInfo(
  val facility: String? = null,
  val facilityName: String? = null,
  val ward: String? = null,
  val wardName: String? = null,
  val subCounty: String? = null,
  val subCountyName: String? = null,
  val county: String? = null,
  val countyName: String? = null,
  val country: String? = null,
  val countryName: String? = null,
)

interface AuthSessionStore {
  var session: AuthSession?
}

object InMemoryAuthSessionStore : AuthSessionStore {
  override var session: AuthSession? = null
}

data class AuthSession(
  val accessToken: String,
  val tokenType: String,
  val refreshToken: String? = null,
  val issuedAt: Instant,
  val accessTokenExpiresAt: Instant? = null,
  val refreshTokenExpiresAt: Instant? = null,
  val expiresInSeconds: Long? = null,
  val refreshExpiresInSeconds: Long? = null,
  val notBeforePolicy: Long? = null,
  val sessionState: String? = null,
  val scope: String? = null,
  val firstLogin: Boolean? = null,
  val status: String? = null,
) {
  val authorizationHeader: String
    get() = "${tokenType.trim()} ${accessToken.trim()}"

  fun isAccessTokenExpired(now: Instant = Clock.System.now()): Boolean =
    accessTokenExpiresAt?.let { now >= it } ?: false

  fun isAccessTokenValid(now: Instant = Clock.System.now()): Boolean =
    accessToken.isNotBlank() && tokenType.isNotBlank() && !isAccessTokenExpired(now)

  fun isRefreshTokenExpired(now: Instant = Clock.System.now()): Boolean =
    refreshTokenExpiresAt?.let { now >= it } ?: false
}

data class LoginSuccess(
  val statusCode: Int,
  val responseBody: String,
  val username: String? = null,
  val tokenResponse: LoginTokenResponse? = null,
  val session: AuthSession? = null,
  val providerProfile: ProviderProfile? = null,
)

data class LoginFailure(
  val message: String,
  val statusCode: Int? = null,
  val responseBody: String? = null,
)

data class SetNewPasswordSuccess(val statusCode: Int, val responseBody: String)

data class SetNewPasswordFailure(
  val message: String,
  val statusCode: Int? = null,
  val responseBody: String? = null,
)

data class ResetPasswordSuccess(val statusCode: Int, val responseBody: String)

data class ResetPasswordFailure(
  val message: String,
  val statusCode: Int? = null,
  val responseBody: String? = null,
)
