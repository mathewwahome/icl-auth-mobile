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
package icl.ohs.libs.auth.network

import icl.ohs.libs.auth.model.LoginFailure
import icl.ohs.libs.auth.model.ResetPasswordFailure
import icl.ohs.libs.auth.model.ResetPasswordReq
import icl.ohs.libs.auth.model.SetNewPasswordFailure
import icl.ohs.libs.auth.model.SetNewPasswordReq
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** Field-level validation and request-body construction for each auth endpoint. */
internal fun validateLoginRequest(
  config: ResolvedLoginConfig,
  username: String,
  password: String,
): LoginFailure? =
  when {
    config.loginUrl.isNullOrBlank() -> LoginFailure(message = config.messages.missingLoginUrl)
    username.isBlank() && password.isBlank() ->
      LoginFailure(message = config.messages.emptyCredentials)

    username.isBlank() -> LoginFailure(message = config.messages.emptyUsername)
    password.isBlank() -> LoginFailure(message = config.messages.emptyPassword)
    else -> null
  }

internal fun buildLoginRequestBody(
  config: ResolvedLoginConfig,
  username: String,
  password: String,
): String {
  val payload = buildJsonObject {
    put(config.usernameFieldName, JsonPrimitive(username))
    put(config.passwordFieldName, JsonPrimitive(password))
  }

  return Json.encodeToString(JsonObject.serializer(), payload)
}

internal fun validateSetNewPasswordRequest(
  config: ResolvedSetNewPasswordConfig,
  request: SetNewPasswordReq,
): SetNewPasswordFailure? =
  when {
    config.resetPasswordUrl.isNullOrBlank() ->
      SetNewPasswordFailure(message = config.messages.missingResetPasswordUrl)
    request.idNumber.isBlank() -> SetNewPasswordFailure(message = config.messages.missingIdNumber)
    request.temporaryPassword.isBlank() ->
      SetNewPasswordFailure(message = config.messages.emptyTemporaryPassword)
    request.password.isBlank() -> SetNewPasswordFailure(message = config.messages.emptyPassword)
    request.temporaryPassword == request.password ->
      SetNewPasswordFailure(message = config.messages.samePassword)
    else -> null
  }

internal fun buildSetNewPasswordRequestBody(request: SetNewPasswordReq): String {
  val payload = buildJsonObject {
    put("temporaryPassword", JsonPrimitive(request.temporaryPassword))
    put("idNumber", JsonPrimitive(request.idNumber))
    put("password", JsonPrimitive(request.password))
  }

  return Json.encodeToString(JsonObject.serializer(), payload)
}

internal fun validateResetPasswordRequest(
  config: ResolvedResetPasswordConfig,
  request: ResetPasswordReq,
): ResetPasswordFailure? =
  when {
    config.resetPasswordUrl.isNullOrBlank() ->
      ResetPasswordFailure(message = config.messages.missingResetPasswordUrl)
    request.identifier.isBlank() ->
      ResetPasswordFailure(message = config.messages.missingIdentifier)
    request.otp.isBlank() -> ResetPasswordFailure(message = config.messages.emptyOtp)
    request.password.isBlank() -> ResetPasswordFailure(message = config.messages.emptyPassword)
    request.password.length < config.minPasswordLength ->
      ResetPasswordFailure(message = config.messages.passwordTooShort)
    else -> null
  }

internal fun buildResetPasswordRequestBody(request: ResetPasswordReq): String {
  val payload = buildJsonObject {
    put("otp", JsonPrimitive(request.otp))
    put("identifier", JsonPrimitive(request.identifier))
    put("password", JsonPrimitive(request.password))
  }

  return Json.encodeToString(JsonObject.serializer(), payload)
}
