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

import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.IclAuthConfig
import icl.ohs.libs.auth.LoginScreenConfig
import icl.ohs.libs.auth.model.AuthSession
import icl.ohs.libs.auth.model.AuthSessionStore
import icl.ohs.libs.auth.model.InMemoryAuthSessionStore
import icl.ohs.libs.auth.model.LoginTokenResponse
import icl.ohs.libs.auth.network.LoginAttemptResult
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.buildLoginRequestBody
import icl.ohs.libs.auth.network.parseLoginTokenResponse
import icl.ohs.libs.auth.network.parseProviderProfile
import icl.ohs.libs.auth.network.resolveLoginConfig
import icl.ohs.libs.auth.network.toAuthSession
import icl.ohs.libs.auth.network.validateLoginRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class LoginClientTest {

  @Test
  fun validateLoginRequest_returnsMissingUrlMessageWhenAuthIsNotInitialized() {
    IclAuth.clear()
    val config = resolveLoginConfig(screenConfig = LoginScreenConfig(endpoint = "/login"))

    val failure = validateLoginRequest(config = config, username = "nurse", password = "secret")

    assertEquals(config.messages.missingLoginUrl, failure?.message)
  }

  @Test
  fun resolveLoginConfig_combinesBaseUrlAndEndpoint() {
    IclAuth.initialize(IclAuthConfig(baseAuthUrl = "https://auth.example.com"))

    val config = resolveLoginConfig(screenConfig = LoginScreenConfig(endpoint = "/provider/login"))

    assertEquals("https://auth.example.com/provider/login", config.loginUrl)
  }

  @Test
  fun buildLoginRequestBody_usesConfiguredFieldNames() {
    val config =
      resolveLoginConfig(
        screenConfig =
          LoginScreenConfig(
            endpoint = "/login",
            usernameFieldName = "email",
            passwordFieldName = "pin",
          ),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )

    val requestBody = buildLoginRequestBody(config = config, username = "demo", password = "1234")

    assertEquals("""{"email":"demo","pin":"1234"}""", requestBody)
  }

  @Test
  fun login_returnsSuccessForSuccessfulResponses() = runTest {
    IclAuth.clear()
    val sessionStore = InMemoryAuthSessionStore.also { it.session = null }
    val config =
      resolveLoginConfig(
        screenConfig = LoginScreenConfig(endpoint = "/login"),
        authConfig =
          IclAuthConfig(baseAuthUrl = "https://auth.example.com", sessionStore = sessionStore),
      )
    val client =
      HttpClient(
        MockEngine { request ->
          when {
            request.method == HttpMethod.Post && request.url.encodedPath == "/login" ->
              respond(
                content =
                  """{"access_token":"abc123","refresh_token":"refresh123","expires_in":15552000,"refresh_expires_in":72000,"token_type":"Bearer","session_state":"session-1","scope":"email organization profile openid","firstLogin":false,"status":"success"}""",
                status = HttpStatusCode.OK,
                headers =
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )

            request.method == HttpMethod.Get && request.url.encodedPath == "/provider/me" -> {
              assertEquals("Bearer abc123", request.headers[HttpHeaders.Authorization])
              respond(
                content =
                  """{"status":"success","user":{"firstName":"Japheth","lastName":"Kiprotich","fhirPractitionerId":"cd40811a-b174-45d0-ad63-6ff56ed249df","practitionerRole":"ADMINISTRATOR","role":"ADMINISTRATOR","status":true,"id":"cd40811a-b174-45d0-ad63-6ff56ed249df","idNumber":"32645167","fullNames":"Japheth Kiprotich","phone":"0724743788","email":"jkiprotich@intellisoftkenya.com","locationInfo":{"facility":"","facilityName":"","ward":"","wardName":"","subCounty":"","subCountyName":"","county":"","countyName":"","country":"0","countryName":"Kenya"}}}""",
                status = HttpStatusCode.OK,
                headers =
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )
            }

            else -> error("Unexpected request: ${request.method.value} ${request.url}")
          }
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result = service.login(config = config, username = "nurse", password = "secret")

      val success = assertIs<LoginAttemptResult.Success>(result)
      assertEquals(200, success.value.statusCode)
      val tokenResponse = assertNotNull(success.value.tokenResponse)
      assertEquals("abc123", tokenResponse.accessToken)
      assertEquals("refresh123", tokenResponse.refreshToken)
      assertEquals(15_552_000L, tokenResponse.expiresIn)
      assertEquals(false, tokenResponse.firstLogin)
      assertEquals("success", tokenResponse.status)
      assertEquals("Bearer abc123", success.value.session?.authorizationHeader)
      assertEquals("Bearer abc123", sessionStore.session?.authorizationHeader)
      val providerProfile = assertNotNull(success.value.providerProfile)
      assertEquals("success", providerProfile.status)
      val providerUser = assertNotNull(providerProfile.user)
      assertEquals("Japheth", providerUser.firstName)
      assertEquals("ADMINISTRATOR", providerUser.role)
      assertTrue(providerUser.status == true)
      assertEquals("0", providerUser.locationInfo?.country)
      assertEquals("Kenya", providerUser.locationInfo?.countryName)
      assertEquals("Japheth", IclAuth.currentProviderUser()?.firstName)
    } finally {
      client.close()
    }
  }

  @Test
  fun login_skipsProviderProfileFetchForFirstTimeUsers() = runTest {
    IclAuth.clear()
    val sessionStore = InMemoryAuthSessionStore.also { it.session = null }
    val config =
      resolveLoginConfig(
        screenConfig = LoginScreenConfig(endpoint = "/login"),
        authConfig =
          IclAuthConfig(baseAuthUrl = "https://auth.example.com", sessionStore = sessionStore),
      )
    var providerProfileRequested = false
    val client =
      HttpClient(
        MockEngine { request ->
          when {
            request.method == HttpMethod.Post && request.url.encodedPath == "/login" ->
              respond(
                content =
                  """{"access_token":"abc123","refresh_token":"refresh123","expires_in":15552000,"refresh_expires_in":72000,"token_type":"Bearer","session_state":"session-1","scope":"email organization profile openid","firstLogin":true,"status":"success"}""",
                status = HttpStatusCode.OK,
                headers =
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )

            request.method == HttpMethod.Get && request.url.encodedPath == "/provider/me" -> {
              providerProfileRequested = true
              respond(
                content = """{"message":"should not be called"}""",
                status = HttpStatusCode.InternalServerError,
                headers =
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )
            }

            else -> error("Unexpected request: ${request.method.value} ${request.url}")
          }
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result = service.login(config = config, username = "32645167", password = "secret")

      val success = assertIs<LoginAttemptResult.Success>(result)
      assertEquals("32645167", success.value.username)
      assertEquals(true, success.value.tokenResponse?.firstLogin)
      assertNull(success.value.providerProfile)
      assertEquals("Bearer abc123", sessionStore.session?.authorizationHeader)
      assertNull(IclAuth.currentProviderProfile())
      assertEquals(false, providerProfileRequested)
    } finally {
      client.close()
    }
  }

  @Test
  fun parseLoginTokenResponse_extractsKnownTokenFields() {
    val tokenResponse =
      parseLoginTokenResponse(
        """{"access_token":"access-1","expires_in":15552000,"refresh_expires_in":72000,"refresh_token":"refresh-1","token_type":"Bearer","not-before-policy":0,"session_state":"session-123","scope":"email organization profile openid","firstLogin":false,"status":"success"}"""
      )

    assertNotNull(tokenResponse)
    assertEquals("access-1", tokenResponse.accessToken)
    assertEquals(15_552_000L, tokenResponse.expiresIn)
    assertEquals(72_000L, tokenResponse.refreshExpiresIn)
    assertEquals("refresh-1", tokenResponse.refreshToken)
    assertEquals("Bearer", tokenResponse.tokenType)
    assertEquals(0L, tokenResponse.notBeforePolicy)
    assertEquals("session-123", tokenResponse.sessionState)
    assertEquals("email organization profile openid", tokenResponse.scope)
    assertEquals(false, tokenResponse.firstLogin)
    assertEquals("success", tokenResponse.status)
  }

  @Test
  fun parseProviderProfile_extractsKnownUserFields() {
    val providerProfile =
      parseProviderProfile(
        """{"status":"success","user":{"firstName":"Japheth","lastName":"Kiprotich","fhirPractitionerId":"cd40811a-b174-45d0-ad63-6ff56ed249df","practitionerRole":"ADMINISTRATOR","role":"ADMINISTRATOR","status":true,"id":"cd40811a-b174-45d0-ad63-6ff56ed249df","idNumber":"32645167","fullNames":"Japheth Kiprotich","phone":"0724743788","email":"jkiprotich@intellisoftkenya.com","locationInfo":{"facility":"","facilityName":"","ward":"","wardName":"","subCounty":"","subCountyName":"","county":"","countyName":"","country":"0","countryName":"Kenya"}}}"""
      )

    assertNotNull(providerProfile)
    assertEquals("success", providerProfile.status)
    val providerUser = assertNotNull(providerProfile.user)
    assertEquals("Japheth", providerUser.firstName)
    assertEquals("Kiprotich", providerUser.lastName)
    assertEquals("ADMINISTRATOR", providerUser.practitionerRole)
    assertEquals(true, providerUser.status)
    assertEquals("", providerUser.locationInfo?.facility)
    assertEquals("0", providerUser.locationInfo?.country)
    assertEquals("Kenya", providerUser.locationInfo?.countryName)
  }

  @Test
  fun toAuthSession_tracksExpiryAndHeader() {
    val issuedAt = Instant.parse("2026-07-02T09:00:00Z")

    val session =
      LoginTokenResponse(
          accessToken = "access-1",
          expiresIn = 3600,
          refreshExpiresIn = 7200,
          refreshToken = "refresh-1",
          tokenType = "Bearer",
          sessionState = "session-123",
          scope = "openid profile",
        )
        .toAuthSession(issuedAt = issuedAt)

    assertNotNull(session)
    assertEquals("Bearer access-1", session.authorizationHeader)
    assertEquals(Instant.parse("2026-07-02T10:00:00Z"), session.accessTokenExpiresAt)
    assertEquals(Instant.parse("2026-07-02T11:00:00Z"), session.refreshTokenExpiresAt)
  }

  @Test
  fun currentAuthorizationHeader_returnsNullAfterExpiry() {
    val sessionStore =
      object : AuthSessionStore {
        override var session: AuthSession? = null
      }
    IclAuth.initialize(
      IclAuthConfig(baseAuthUrl = "https://auth.example.com", sessionStore = sessionStore)
    )
    val issuedAt = Instant.parse("2026-07-02T09:00:00Z")
    IclAuth.updateSession(
      AuthSession(
        accessToken = "access-1",
        tokenType = "Bearer",
        issuedAt = issuedAt,
        accessTokenExpiresAt = Instant.parse("2026-07-02T10:00:00Z"),
      )
    )

    assertEquals(
      "Bearer access-1",
      IclAuth.currentAuthorizationHeader(now = Instant.parse("2026-07-02T09:30:00Z")),
    )
    assertNull(IclAuth.currentAuthorizationHeader(now = Instant.parse("2026-07-02T10:00:00Z")))
    assertEquals(
      emptyMap(),
      IclAuth.currentAuthHeaders(now = Instant.parse("2026-07-02T10:00:00Z")),
    )
  }

  @Test
  fun login_returnsFailureWhenProviderProfileRequestFails() = runTest {
    IclAuth.clear()
    val sessionStore = InMemoryAuthSessionStore.also { it.session = null }
    val config =
      resolveLoginConfig(
        screenConfig = LoginScreenConfig(endpoint = "/login"),
        authConfig =
          IclAuthConfig(baseAuthUrl = "https://auth.example.com", sessionStore = sessionStore),
      )
    val client =
      HttpClient(
        MockEngine { request ->
          when {
            request.method == HttpMethod.Post && request.url.encodedPath == "/login" ->
              respond(
                content = """{"access_token":"abc123","token_type":"Bearer","status":"success"}""",
                status = HttpStatusCode.OK,
                headers =
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )

            request.method == HttpMethod.Get && request.url.encodedPath == "/provider/me" ->
              respond(
                content = """{"message":"Unable to load provider profile"}""",
                status = HttpStatusCode.InternalServerError,
                headers =
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )

            else -> error("Unexpected request: ${request.method.value} ${request.url}")
          }
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result = service.login(config = config, username = "nurse", password = "secret")

      val failure = assertIs<LoginAttemptResult.Failure>(result)
      assertEquals("Unable to load provider profile", failure.value.message)
      assertEquals(500, failure.value.statusCode)
      assertNull(sessionStore.session)
      assertNull(IclAuth.currentProviderProfile())
    } finally {
      client.close()
    }
  }

  @Test
  fun login_returnsServerMessageWhenProvided() = runTest {
    val config =
      resolveLoginConfig(
        screenConfig = LoginScreenConfig(endpoint = "/login"),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )
    val client =
      HttpClient(
        MockEngine {
          respond(
            content = """{"message":"Credentials rejected by server"}""",
            status = HttpStatusCode.Unauthorized,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
          )
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result = service.login(config = config, username = "nurse", password = "wrong")

      val failure = assertIs<LoginAttemptResult.Failure>(result)
      assertEquals("Credentials rejected by server", failure.value.message)
      assertEquals(401, failure.value.statusCode)
    } finally {
      client.close()
    }
  }
}
