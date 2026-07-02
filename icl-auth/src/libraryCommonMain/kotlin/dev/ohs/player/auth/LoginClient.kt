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

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal sealed interface LoginAttemptResult {
  data class Success(val value: LoginSuccess) : LoginAttemptResult

  data class Failure(val value: LoginFailure) : LoginAttemptResult
}

internal class LoginService(private val httpClient: HttpClient) {

  suspend fun login(
    config: LoginScreenConfig,
    username: String,
    password: String,
  ): LoginAttemptResult {
    val validationFailure =
      validateLoginRequest(config = config, username = username, password = password)
    if (validationFailure != null) {
      return LoginAttemptResult.Failure(validationFailure)
    }

    return try {
      val response =
        httpClient.post(config.loginUrl) {
          header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
          accept(ContentType.Application.Json)
          config.requestHeaders.forEach { (name, value) -> header(name, value) }
          setBody(buildLoginRequestBody(config = config, username = username, password = password))
        }
      val responseBody = response.bodyAsText()

      if (response.status.value in 200..299) {
        LoginAttemptResult.Success(
          LoginSuccess(statusCode = response.status.value, responseBody = responseBody)
        )
      } else {
        LoginAttemptResult.Failure(
          LoginFailure(
            message =
              resolveFailureMessage(
                config = config,
                statusCode = response.status.value,
                responseBody = responseBody,
              ),
            statusCode = response.status.value,
            responseBody = responseBody,
          )
        )
      }
    } catch (error: Throwable) {
      if (error is CancellationException) {
        throw error
      }

      LoginAttemptResult.Failure(LoginFailure(message = config.messages.networkError))
    }
  }
}

internal fun buildLoginHttpClient(requestTimeoutMillis: Long): HttpClient =
  HttpClient(CIO) {
    expectSuccess = false

    if (requestTimeoutMillis > 0) {
      install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        connectTimeoutMillis = requestTimeoutMillis
        socketTimeoutMillis = requestTimeoutMillis
      }
    }
  }

internal fun validateLoginRequest(
  config: LoginScreenConfig,
  username: String,
  password: String,
): LoginFailure? =
  when {
    config.loginUrl.isBlank() -> LoginFailure(message = config.messages.missingLoginUrl)
    username.isBlank() && password.isBlank() ->
      LoginFailure(message = config.messages.emptyCredentials)
    username.isBlank() -> LoginFailure(message = config.messages.emptyUsername)
    password.isBlank() -> LoginFailure(message = config.messages.emptyPassword)
    else -> null
  }

internal fun buildLoginRequestBody(
  config: LoginScreenConfig,
  username: String,
  password: String,
): String {
  val payload = buildJsonObject {
    put(config.usernameFieldName, JsonPrimitive(username))
    put(config.passwordFieldName, JsonPrimitive(password))
  }

  return Json.encodeToString(JsonObject.serializer(), payload)
}

internal fun resolveFailureMessage(
  config: LoginScreenConfig,
  statusCode: Int,
  responseBody: String,
): String {
  val customMessage =
    config.responseMessageResolver?.invoke(statusCode, responseBody)?.takeIf(String::isNotBlank)
  if (customMessage != null) {
    return customMessage
  }

  val serverMessage =
    extractResponseMessage(responseBody = responseBody, keys = config.responseMessageKeys)
  if (serverMessage != null) {
    return serverMessage
  }

  return when {
    statusCode in 400..499 -> config.messages.invalidCredentials
    statusCode >= 500 -> config.messages.serverError
    else -> config.messages.unexpectedError
  }
}

internal fun extractResponseMessage(responseBody: String, keys: List<String>): String? {
  val trimmedBody = responseBody.trim()
  if (trimmedBody.isBlank()) {
    return null
  }

  if (!trimmedBody.startsWith("{") && !trimmedBody.startsWith("[") && trimmedBody.length <= 160) {
    return trimmedBody
  }

  return runCatching { Json.parseToJsonElement(trimmedBody).jsonObject }
    .getOrNull()
    ?.let { json ->
      keys
        .asSequence()
        .mapNotNull { key -> json[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } }
        .firstOrNull()
    }
}
