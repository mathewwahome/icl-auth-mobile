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
package icl.ohs.libs.auth.model

/** User-facing copy for each auth flow. Overridable via the screen configs. */
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
