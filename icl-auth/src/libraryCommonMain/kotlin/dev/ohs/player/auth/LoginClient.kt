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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class ResolvedLoginConfig(
  val loginUrl: String?,
  val providerProfileUrl: String?,
  val requestHeaders: Map<String, String>,
  val usernameFieldName: String,
  val passwordFieldName: String,
  val requestTimeoutMillis: Long,
  val responseMessageKeys: List<String>,
  val messages: LoginMessages,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)?,
  val sessionStore: AuthSessionStore?,
)

internal sealed interface LoginAttemptResult {
  data class Success(val value: LoginSuccess) : LoginAttemptResult

  data class Failure(val value: LoginFailure) : LoginAttemptResult
}

internal sealed interface ProviderProfileRequestResult {
  data class Success(val providerProfile: ProviderProfile?) : ProviderProfileRequestResult

  data class Failure(val value: LoginFailure) : ProviderProfileRequestResult
}

internal class LoginService(val httpClient: HttpClient) {

  suspend fun login(
    config: ResolvedLoginConfig,
    username: String,
    password: String,
  ): LoginAttemptResult {
    val validationFailure =
      validateLoginRequest(config = config, username = username, password = password)
    if (validationFailure != null) {
      return LoginAttemptResult.Failure(validationFailure)
    }
    val loginUrl =
      config.loginUrl
        ?: return LoginAttemptResult.Failure(LoginFailure(config.messages.missingLoginUrl))

    return try {
      val response =
        httpClient.post(loginUrl) {
          header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
          accept(ContentType.Application.Json)
          config.requestHeaders.forEach { (name, value) -> header(name, value) }
          setBody(buildLoginRequestBody(config = config, username = username, password = password))
        }
      val responseBody = response.bodyAsText()
      val tokenResponse = parseLoginTokenResponse(responseBody)
      val session = tokenResponse?.toAuthSession(issuedAt = Clock.System.now())

      if (response.status.isSuccess()) {
        val providerProfileResult = fetchProviderProfile(config = config, session = session)
        if (providerProfileResult is ProviderProfileRequestResult.Failure) {
          IclAuth.updateSession(session = null, sessionStore = config.sessionStore)
          IclAuth.updateProviderProfile(null)
          return providerProfileResult.value.asFailure()
        }

        val providerProfile =
          (providerProfileResult as? ProviderProfileRequestResult.Success)?.providerProfile
        IclAuth.updateSession(session = session, sessionStore = config.sessionStore)
        IclAuth.updateProviderProfile(providerProfile)
        LoginAttemptResult.Success(
          LoginSuccess(
            statusCode = response.status.value,
            responseBody = responseBody,
            tokenResponse = tokenResponse,
            session = session,
            providerProfile = providerProfile,
          )
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

private suspend fun LoginService.fetchProviderProfile(
  config: ResolvedLoginConfig,
  session: AuthSession?,
): ProviderProfileRequestResult {
  if (session == null) {
    return ProviderProfileRequestResult.Success(providerProfile = null)
  }

  val providerProfileUrl =
    config.providerProfileUrl
      ?: return ProviderProfileRequestResult.Failure(
        LoginFailure(message = config.messages.missingProviderProfileUrl)
      )

  return try {
    val response =
      httpClient.get(providerProfileUrl) {
        accept(ContentType.Application.Json)
        config.requestHeaders.forEach { (name, value) -> header(name, value) }
        header(HttpHeaders.Authorization, session.authorizationHeader)
      }
    val responseBody = response.bodyAsText()

    if (response.status.isSuccess()) {
      ProviderProfileRequestResult.Success(
        providerProfile = parseProviderProfile(responseBody = responseBody)
      )
    } else {
      ProviderProfileRequestResult.Failure(
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

    ProviderProfileRequestResult.Failure(LoginFailure(message = config.messages.networkError))
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

internal fun resolveFailureMessage(
  config: ResolvedLoginConfig,
  statusCode: Int,
  responseBody: String,
): String =
  config.responseMessageResolver?.invoke(statusCode, responseBody)?.takeIf(String::isNotBlank)
    ?: extractResponseMessage(responseBody = responseBody, keys = config.responseMessageKeys)
    ?: when {
      statusCode in 400..499 -> config.messages.invalidCredentials
      statusCode >= 500 -> config.messages.serverError
      else -> config.messages.unexpectedError
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
      keys.firstNotNullOfOrNull { key ->
        json[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
      }
    }
}

internal fun parseLoginTokenResponse(responseBody: String): LoginTokenResponse? {
  val json = parseJsonObject(responseBody) ?: return null

  return LoginTokenResponse(
    accessToken = json.stringValue("access_token"),
    expiresIn = json.longValue("expires_in"),
    refreshExpiresIn = json.longValue("refresh_expires_in"),
    refreshToken = json.stringValue("refresh_token"),
    tokenType = json.stringValue("token_type"),
    notBeforePolicy = json.longValue("not-before-policy"),
    sessionState = json.stringValue("session_state"),
    scope = json.stringValue("scope"),
    firstLogin = json.booleanValue("firstLogin"),
    status = json.stringValue("status"),
  )
}

internal fun parseProviderProfile(responseBody: String): ProviderProfile? {
  val json = parseJsonObject(responseBody) ?: return null

  return ProviderProfile(
    status = json.rawStringValue("status"),
    user = json.objectValue("user")?.toProviderUser(),
  )
}

internal fun LoginTokenResponse.toAuthSession(issuedAt: Instant): AuthSession? {
  val accessToken = accessToken?.trim().orEmpty()
  val tokenType = tokenType?.trim().orEmpty()
  if (accessToken.isBlank() || tokenType.isBlank()) {
    return null
  }

  return AuthSession(
    accessToken = accessToken,
    tokenType = tokenType,
    refreshToken = refreshToken,
    issuedAt = issuedAt,
    accessTokenExpiresAt = expiresIn?.let { issuedAt + it.seconds },
    refreshTokenExpiresAt = refreshExpiresIn?.let { issuedAt + it.seconds },
    expiresInSeconds = expiresIn,
    refreshExpiresInSeconds = refreshExpiresIn,
    notBeforePolicy = notBeforePolicy,
    sessionState = sessionState,
    scope = scope,
    firstLogin = firstLogin,
    status = status,
  )
}

internal fun JsonObject.toProviderUser(): ProviderUser =
  ProviderUser(
    firstName = rawStringValue("firstName"),
    lastName = rawStringValue("lastName"),
    fhirPractitionerId = rawStringValue("fhirPractitionerId"),
    practitionerRole = rawStringValue("practitionerRole"),
    role = rawStringValue("role"),
    status = booleanValue("status"),
    id = rawStringValue("id"),
    idNumber = rawStringValue("idNumber"),
    fullNames = rawStringValue("fullNames"),
    phone = rawStringValue("phone"),
    email = rawStringValue("email"),
    locationInfo = objectValue("locationInfo")?.toProviderLocationInfo(),
  )

internal fun JsonObject.toProviderLocationInfo(): ProviderLocationInfo =
  ProviderLocationInfo(
    facility = rawStringValue("facility"),
    facilityName = rawStringValue("facilityName"),
    ward = rawStringValue("ward"),
    wardName = rawStringValue("wardName"),
    subCounty = rawStringValue("subCounty"),
    subCountyName = rawStringValue("subCountyName"),
    county = rawStringValue("county"),
    countyName = rawStringValue("countyName"),
    country = rawStringValue("country"),
    countryName = rawStringValue("countryName"),
  )

internal fun parseJsonObject(responseBody: String): JsonObject? =
  runCatching { Json.parseToJsonElement(responseBody.trim()).jsonObject }.getOrNull()

internal fun LoginFailure.asFailure(): LoginAttemptResult.Failure = LoginAttemptResult.Failure(this)

internal fun JsonObject.rawStringValue(key: String): String? =
  this[key]?.jsonPrimitive?.contentOrNull

internal fun JsonObject.stringValue(key: String): String? =
  this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

internal fun JsonObject.objectValue(key: String): JsonObject? =
  runCatching { this[key]?.jsonObject }.getOrNull()

internal fun JsonObject.longValue(key: String): Long? =
  this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()

internal fun JsonObject.booleanValue(key: String): Boolean? =
  this[key]?.jsonPrimitive?.booleanOrNull

internal fun resolveLoginConfig(
  screenConfig: LoginScreenConfig,
  authConfig: IclAuthConfig? = IclAuth.currentConfiguration(),
): ResolvedLoginConfig =
  ResolvedLoginConfig(
    loginUrl =
      resolveAuthUrl(baseAuthUrl = authConfig?.baseAuthUrl, endpoint = screenConfig.endpoint),
    providerProfileUrl =
      resolveAuthUrl(
        baseAuthUrl = authConfig?.baseAuthUrl,
        endpoint = authConfig?.providerProfileEndpoint.orEmpty(),
      ),
    requestHeaders = authConfig?.defaultRequestHeaders.orEmpty() + screenConfig.requestHeaders,
    usernameFieldName = screenConfig.usernameFieldName,
    passwordFieldName = screenConfig.passwordFieldName,
    requestTimeoutMillis =
      screenConfig.requestTimeoutMillis ?: authConfig?.requestTimeoutMillis ?: 15_000,
    responseMessageKeys =
      screenConfig.responseMessageKeys
        ?: authConfig?.responseMessageKeys
        ?: listOf("message", "error", "detail"),
    messages = screenConfig.messages ?: authConfig?.messages ?: LoginMessages(),
    responseMessageResolver = screenConfig.responseMessageResolver,
    sessionStore = authConfig?.sessionStore,
  )

internal fun resolveAuthUrl(baseAuthUrl: String?, endpoint: String): String? {
  val normalizedEndpoint = endpoint.trim()
  if (normalizedEndpoint.isBlank()) {
    return null
  }

  if (
    normalizedEndpoint.startsWith(prefix = "https://", ignoreCase = true) ||
      normalizedEndpoint.startsWith(prefix = "http://", ignoreCase = true)
  ) {
    return normalizedEndpoint
  }

  val normalizedBase = baseAuthUrl?.trim()?.takeIf(String::isNotBlank) ?: return null
  return normalizedBase.removeSuffix("/") + "/" + normalizedEndpoint.removePrefix("/")
}
