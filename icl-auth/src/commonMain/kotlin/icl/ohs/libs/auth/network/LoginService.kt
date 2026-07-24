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

import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.model.AuthSession
import icl.ohs.libs.auth.model.LoginFailure
import icl.ohs.libs.auth.model.LoginSuccess
import icl.ohs.libs.auth.model.ProviderProfile
import icl.ohs.libs.auth.model.ResetPasswordFailure
import icl.ohs.libs.auth.model.ResetPasswordReq
import icl.ohs.libs.auth.model.ResetPasswordSuccess
import icl.ohs.libs.auth.model.SetNewPasswordFailure
import icl.ohs.libs.auth.model.SetNewPasswordReq
import icl.ohs.libs.auth.model.SetNewPasswordSuccess
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
import kotlinx.coroutines.CancellationException

/** Outcome wrappers for each network call - the only shapes view models switch on. */
internal sealed interface LoginAttemptResult {
  data class Success(val value: LoginSuccess) : LoginAttemptResult

  data class Failure(val value: LoginFailure) : LoginAttemptResult
}

internal sealed interface SetNewPasswordAttemptResult {
  data class Success(val value: SetNewPasswordSuccess) : SetNewPasswordAttemptResult

  data class Failure(val value: SetNewPasswordFailure) : SetNewPasswordAttemptResult
}

internal sealed interface ResetPasswordAttemptResult {
  data class Success(val value: ResetPasswordSuccess) : ResetPasswordAttemptResult

  data class Failure(val value: ResetPasswordFailure) : ResetPasswordAttemptResult
}

internal sealed interface ProviderProfileRequestResult {
  data class Success(val providerProfile: ProviderProfile?) : ProviderProfileRequestResult

  data class Failure(val value: LoginFailure) : ProviderProfileRequestResult
}

/** Thin wrapper around [HttpClient] for the provider auth API. No UI state, no Compose. */
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
        if (tokenResponse?.firstLogin == true) {
          IclAuth.updateSession(session = session, sessionStore = config.sessionStore)
          IclAuth.updateProviderProfile(null)
          return LoginAttemptResult.Success(
            LoginSuccess(
              statusCode = response.status.value,
              responseBody = responseBody,
              username = username,
              tokenResponse = tokenResponse,
              session = session,
              providerProfile = null,
            )
          )
        }

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
            username = username,
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

  suspend fun setNewPassword(
    config: ResolvedSetNewPasswordConfig,
    request: SetNewPasswordReq,
  ): SetNewPasswordAttemptResult {
    val validationFailure = validateSetNewPasswordRequest(config = config, request = request)
    if (validationFailure != null) {
      return SetNewPasswordAttemptResult.Failure(validationFailure)
    }

    val resetPasswordUrl =
      config.resetPasswordUrl
        ?: return SetNewPasswordAttemptResult.Failure(
          SetNewPasswordFailure(message = config.messages.missingResetPasswordUrl)
        )

    return try {
      val response =
        httpClient.post(resetPasswordUrl) {
          header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
          accept(ContentType.Application.Json)
          config.requestHeaders.forEach { (name, value) -> header(name, value) }
          setBody(buildSetNewPasswordRequestBody(request))
        }
      val responseBody = response.bodyAsText()

      if (response.status.isSuccess()) {
        SetNewPasswordAttemptResult.Success(
          SetNewPasswordSuccess(statusCode = response.status.value, responseBody = responseBody)
        )
      } else {
        SetNewPasswordAttemptResult.Failure(
          SetNewPasswordFailure(
            message =
              resolveSetNewPasswordFailureMessage(
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

      SetNewPasswordAttemptResult.Failure(
        SetNewPasswordFailure(message = config.messages.networkError)
      )
    }
  }

  suspend fun resetPassword(
    config: ResolvedResetPasswordConfig,
    request: ResetPasswordReq,
  ): ResetPasswordAttemptResult {
    val validationFailure = validateResetPasswordRequest(config = config, request = request)
    if (validationFailure != null) {
      return ResetPasswordAttemptResult.Failure(validationFailure)
    }

    val resetPasswordUrl =
      config.resetPasswordUrl
        ?: return ResetPasswordAttemptResult.Failure(
          ResetPasswordFailure(message = config.messages.missingResetPasswordUrl)
        )

    return try {
      val response =
        httpClient.post(resetPasswordUrl) {
          header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
          accept(ContentType.Application.Json)
          config.requestHeaders.forEach { (name, value) -> header(name, value) }
          setBody(buildResetPasswordRequestBody(request))
        }
      val responseBody = response.bodyAsText()

      if (response.status.isSuccess()) {
        ResetPasswordAttemptResult.Success(
          ResetPasswordSuccess(statusCode = response.status.value, responseBody = responseBody)
        )
      } else {
        ResetPasswordAttemptResult.Failure(
          ResetPasswordFailure(
            message =
              resolveResetPasswordFailureMessage(
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

      ResetPasswordAttemptResult.Failure(
        ResetPasswordFailure(message = config.messages.networkError)
      )
    }
  }
}

internal suspend fun LoginService.fetchProviderProfile(
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

internal fun LoginFailure.asFailure(): LoginAttemptResult.Failure = LoginAttemptResult.Failure(this)
