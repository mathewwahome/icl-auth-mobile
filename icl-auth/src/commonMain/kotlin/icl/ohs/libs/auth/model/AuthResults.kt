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

/**
 * Request payloads and success/failure outcomes surfaced by the auth network layer to view models
 * and, from there, to the screens' public callbacks.
 */
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

data class SetNewPasswordReq(
  val temporaryPassword: String,
  val idNumber: String,
  val password: String,
)

data class SetNewPasswordSuccess(val statusCode: Int, val responseBody: String)

data class SetNewPasswordFailure(
  val message: String,
  val statusCode: Int? = null,
  val responseBody: String? = null,
)

data class ResetPasswordReq(val otp: String, val identifier: String, val password: String)

data class ResetPasswordSuccess(val statusCode: Int, val responseBody: String)

data class ResetPasswordFailure(
  val message: String,
  val statusCode: Int? = null,
  val responseBody: String? = null,
)
